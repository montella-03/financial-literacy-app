package com.financetracker.service;

import com.financetracker.model.SavingMovement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
public class ReviewService {
    private final TransactionService transactions;
    private final SavingGoalService goals;
    private final DebtService debts;
    private final NetWorthService worth;
    private final PlanningService planning;

    public ReviewService(TransactionService transactions, SavingGoalService goals, DebtService debts, NetWorthService worth, PlanningService planning) {
        this.transactions = transactions;
        this.goals = goals;
        this.debts = debts;
        this.worth = worth;
        this.planning = planning;
    }

    public BigDecimal savingsMovement(YearMonth month) {
        return goals.findAll().stream().flatMap(g -> goals.history(g.getId()).stream()).filter(m -> YearMonth.from(m.getDate()).equals(month)).map(SavingMovement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String reviewCsv(YearMonth month) {
        StringBuilder csv = new StringBuilder(CsvSupport.row("month", "metric", "amount_KES"));
        csv.append(CsvSupport.row(month, "Income", transactions.sumIncome(month))).append(CsvSupport.row(month, "Expenses", transactions.sumExpenses(month)))
                .append(CsvSupport.row(month, "Surplus", transactions.surplus(month))).append(CsvSupport.row(month, "Net savings movements (excludes opening balances)", savingsMovement(month)));
        planning.spending(month).forEach((category, amount) -> csv.append(CsvSupport.row(month, "Spending: " + category, amount)));
        planning.snapshot(month).ifPresent(s -> {
            csv.append(CsvSupport.row(month, "Debt balance captured " + s.getCapturedOn(), s.getDebtBalance()));
            csv.append(CsvSupport.row(month, "Savings balance captured " + s.getCapturedOn(), s.getSavingsBalance()));
        });
        return csv.toString();
    }

    public String transactionsCsv() {
        StringBuilder csv = new StringBuilder("date,type,amount,category,merchant,description,account,destination\r\n");
        transactions.findAll().forEach(t -> csv.append(CsvSupport.row(t.getTransactionDate().toLocalDate(), t.getType(), t.getAmount(), t.getCategory(), t.getMerchant(), t.getDescription(), t.getAccountName(), t.getDestinationAccount())));
        return csv.toString();
    }

    public String goalsCsv() {
        StringBuilder csv = new StringBuilder(CsvSupport.row("goal", "target", "target_date", "current", "opening_balance", "tracking_start", "movement_date", "movement_amount", "note"));
        goals.findAll().forEach(g -> {
            csv.append(CsvSupport.row(g.getName(), g.getTargetAmount(), g.getTargetDate(), g.getCurrentAmount(), g.getOpeningAmount(), g.getStartDate(), "", "", ""));
            goals.history(g.getId()).forEach(m -> csv.append(CsvSupport.row(g.getName(), "", "", "", "", "", m.getDate(), m.getAmount(), m.getNote())));
        });
        return csv.toString();
    }

    public String debtsCsv() {
        StringBuilder csv = new StringBuilder(CsvSupport.row("name", "balance", "interest_rate", "minimum_payment", "notes"));
        debts.findAll().forEach(d -> csv.append(CsvSupport.row(d.getName(), d.getBalance(), d.getInterestRate(), d.getMinimumPayment(), d.getNotes())));
        return csv.toString();
    }

    public String worthCsv() {
        StringBuilder csv = new StringBuilder(CsvSupport.row("name", "type", "value", "updated", "notes"));
        worth.findAll().forEach(w -> csv.append(CsvSupport.row(w.getName(), w.getType(), w.getValue(), w.getUpdatedAt(), w.getNotes())));
        return csv.toString();
    }

    public String plansCsv() {
        StringBuilder csv = new StringBuilder(CsvSupport.row("kind", "name", "amount", "date_or_month", "status"));
        planning.bills().forEach(b -> csv.append(CsvSupport.row("Bill", b.getName(), b.getAmount(), b.getDueDate(), planning.billStatus(b))));
        planning.allBudgets().forEach(b -> csv.append(CsvSupport.row("Budget", b.getCategory(), b.getLimitAmount(), b.getMonth(), "")));
        return csv.toString();
    }
}
