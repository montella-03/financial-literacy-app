package com.financetracker.service;

import com.financetracker.model.*;
import com.financetracker.repository.BillRepository;
import com.financetracker.repository.BudgetRepository;
import com.financetracker.repository.DebtRepository;
import com.financetracker.repository.MonthlySnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class PlanningService {
    private final BudgetRepository budgets;
    private final BillRepository bills;
    private final TransactionService transactions;
    private final DebtRepository debts;
    private final MonthlySnapshotRepository snapshots;
    private final SavingGoalService goals;

    public PlanningService(BudgetRepository budgets, BillRepository bills, TransactionService transactions, DebtRepository debts, MonthlySnapshotRepository snapshots, SavingGoalService goals) {
        this.budgets = budgets;
        this.bills = bills;
        this.transactions = transactions;
        this.debts = debts;
        this.snapshots = snapshots;
        this.goals = goals;
    }

    public List<Budget> allBudgets() {
        return budgets.findByOwnerId(WorkspaceAccess.owner());
    }

    public List<Budget> budgets(YearMonth month) {
        return budgets.findByOwnerId(WorkspaceAccess.owner()).stream().filter(b -> b.getMonth().equals(month.toString())).toList();
    }

    @Transactional
    public void budget(String category, YearMonth month, BigDecimal limit) {
        MoneyChecks.positive(limit);
        if (category == null || category.isBlank() || category.trim().length() > 100)
            throw new IllegalArgumentException("Enter a category of at most 100 characters.");
        String key = category.trim().toLowerCase(Locale.ROOT);
        Budget row = budgets(month).stream().filter(b -> b.getCategory().equals(key)).findFirst().orElseGet(Budget::new);
        row.setOwnerId(WorkspaceAccess.owner());
        row.setCategory(key);
        row.setMonth(month.toString());
        row.setLimitAmount(limit);
        budgets.save(row);
    }

    @Transactional
    public void deleteBudget(Long id) {
        WorkspaceAccess.delete(id, budgets);
    }

    public Map<String, BigDecimal> spending(YearMonth month) {
        Map<String, BigDecimal> result = new TreeMap<>();
        transactions.findAll().stream().filter(t -> t.getType() == Transaction.TransactionType.EXPENSE && YearMonth.from(t.getTransactionDate()).equals(month))
                .forEach(t -> result.merge(t.getCategory() == null || t.getCategory().isBlank() ? "uncategorized" : t.getCategory().trim().toLowerCase(Locale.ROOT), t.getAmount(), BigDecimal::add));
        return result;
    }

    public List<Bill> bills() {
        return bills.findByOwnerId(WorkspaceAccess.owner()).stream().sorted(Comparator.comparing(Bill::getDueDate)).toList();
    }

    public String billStatus(Bill bill) {
        if (bill.getPaidOn() != null) return "Paid on " + bill.getPaidOn();
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), bill.getDueDate());
        return days < 0 ? "Overdue by " + (-days) + " days" : days == 0 ? "Due today" : days <= 7 ? "Due in " + days + " days" : "Upcoming";
    }

    @Transactional
    public void saveBill(Bill bill) {
        WorkspaceAccess.prepare(bill, bills);
        MoneyChecks.positive(bill.getAmount());
        if (bill.getName() == null || bill.getName().isBlank() || bill.getDueDate() == null)
            throw new IllegalArgumentException("Name, amount, and due date are required.");
        if (bill.getDebtId() != null) {
            var debt = debts.findById(bill.getDebtId()).orElseThrow();
            WorkspaceAccess.prepare(debt, debts);
        }
        bill.setDueDay(bill.getDueDate().getDayOfMonth());
        bills.save(bill);
    }

    @Transactional
    public void deleteBill(Long id) {
        WorkspaceAccess.delete(id, bills);
    }

    @Transactional
    public void payBill(Long id) {
        Bill bill = bills.findById(id).orElseThrow();
        WorkspaceAccess.prepare(bill, bills);
        if (bill.getPaidOn() != null) throw new IllegalArgumentException("This bill is already paid.");
        bill.setPaidOn(LocalDate.now());
        bills.save(bill);
        Transaction tx = new Transaction();
        tx.setType(Transaction.TransactionType.EXPENSE);
        tx.setAmount(bill.getAmount());
        tx.setCategory(bill.getCategory() == null || bill.getCategory().isBlank() ? "Bills" : bill.getCategory());
        tx.setMerchant(bill.getName());
        tx.setDescription("Bill payment for " + bill.getDueDate());
        tx.setTransactionDate(LocalDate.now().atTime(12, 0));
        transactions.save(tx);
        if (bill.getDebtId() != null) {
            Debt debt = debts.findById(bill.getDebtId()).orElseThrow();
            WorkspaceAccess.prepare(debt, debts);
            if (debt.getBalance().compareTo(bill.getAmount()) < 0)
                throw new IllegalArgumentException("Payment exceeds the linked debt balance. Edit the bill amount first.");
            debt.setBalance(debt.getBalance().subtract(bill.getAmount()));
            debts.save(debt);
        }
        if (bill.isMonthly()) {
            Bill next = new Bill();
            next.setOwnerId(bill.getOwnerId());
            next.setName(bill.getName());
            next.setAmount(bill.getAmount());
            YearMonth month = YearMonth.from(bill.getDueDate()).plusMonths(1);
            int day = bill.getDueDay() == 0 ? bill.getDueDate().getDayOfMonth() : bill.getDueDay();
            next.setDueDate(month.atDay(Math.min(day, month.lengthOfMonth())));
            next.setDueDay(day);
            next.setCategory(bill.getCategory());
            next.setDebtId(bill.getDebtId());
            next.setMonthly(true);
            bills.save(next);
        }
    }

    @Transactional
    public MonthlySnapshot capture() {
        YearMonth month = YearMonth.now();
        MonthlySnapshot snapshot = snapshots.findByOwnerId(WorkspaceAccess.owner()).stream().filter(s -> s.getMonth().equals(month.toString())).findFirst().orElseGet(MonthlySnapshot::new);
        snapshot.setOwnerId(WorkspaceAccess.owner());
        snapshot.setMonth(month.toString());
        snapshot.setCapturedOn(LocalDate.now());
        snapshot.setDebtBalance(debts.findByOwnerId(WorkspaceAccess.owner()).stream().map(Debt::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add));
        snapshot.setSavingsBalance(goals.findAll().stream().map(SavingGoal::getCurrentAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        return snapshots.save(snapshot);
    }

    public Optional<MonthlySnapshot> snapshot(YearMonth month) {
        return snapshots.findByOwnerId(WorkspaceAccess.owner()).stream().filter(s -> s.getMonth().equals(month.toString())).findFirst();
    }
}
