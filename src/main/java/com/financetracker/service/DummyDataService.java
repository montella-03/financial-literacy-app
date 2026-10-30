package com.financetracker.service;

import com.financetracker.model.Debt;
import com.financetracker.model.NetWorthItem;
import com.financetracker.model.NetWorthItem.ItemType;
import com.financetracker.model.SavingGoal;
import com.financetracker.model.Transaction;
import com.financetracker.model.Transaction.TransactionType;
import com.financetracker.repository.DebtRepository;
import com.financetracker.repository.NetWorthItemRepository;
import com.financetracker.repository.SavingGoalRepository;
import com.financetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DummyDataService {

    private final SavingTargetService savingTargets;
    private final TransactionRepository transactions;
    private final SavingGoalRepository goals;
    private final DebtRepository debts;
    private final com.financetracker.repository.SavingMovementRepository movements;
    private final NetWorthItemRepository netWorthItems;

    public DummyDataService(
            SavingTargetService savingTargets,
            TransactionRepository transactions,
            SavingGoalRepository goals,
            DebtRepository debts,
            NetWorthItemRepository netWorthItems,
            com.financetracker.repository.SavingMovementRepository movements) {
        this.savingTargets = savingTargets;
        this.movements = movements;
        this.transactions = transactions;
        this.goals = goals;
        this.debts = debts;
        this.netWorthItems = netWorthItems;
    }

    private static Transaction tx(
            BigDecimal amount,
            TransactionType type,
            String category,
            String merchant,
            String description,
            LocalDateTime when) {
        return Transaction.builder()
                .ownerId(WorkspaceAccess.owner())
                .amount(amount)
                .type(type)
                .category(category)
                .merchant(merchant)
                .description(description)
                .transactionDate(when)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static NetWorthItem item(String name, BigDecimal value, ItemType type, String notes) {
        return NetWorthItem.builder()
                .ownerId(WorkspaceAccess.owner())
                .name(name)
                .value(value)
                .type(type)
                .notes(notes)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public void createDemo() {
        if (!com.financetracker.session.AppSession.isDemoMode())
            throw new SecurityException("Sample data is only available in demo mode.");
        seedDemo();
    }

    private void seedDemo() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        int[] expected = {12000, 15000, 18000, 10000, 14000, 20000, 12000, 16000, 22000, 18000, 20000, 25000};
        for (int year = today.getYear() - 1; year <= today.getYear(); year++) {
            java.util.Map<Integer, BigDecimal> targets = new java.util.HashMap<>();
            for (int month = 1; month <= 12; month++) targets.put(month, BigDecimal.valueOf(expected[month - 1]));
            savingTargets.saveYear(year, targets);
        }


        transactions.save(tx(new BigDecimal("185000"), TransactionType.INCOME, "Salary", "Acme Ltd",
                "Monthly salary", monthStart.plusDays(1).atTime(9, 0)));
        transactions.save(tx(new BigDecimal("12000"), TransactionType.INCOME, "Side hustle", "Freelance",
                "Design gig", monthStart.plusDays(12).atTime(16, 30)));

        transactions.save(tx(new BigDecimal("45000"), TransactionType.EXPENSE, "Housing", "Rent",
                "Monthly rent", monthStart.plusDays(2).atTime(10, 0)));
        transactions.save(tx(new BigDecimal("8500"), TransactionType.EXPENSE, "Food", "Naivas",
                "Groceries", monthStart.plusDays(5).atTime(18, 20)));
        transactions.save(tx(new BigDecimal("3200"), TransactionType.EXPENSE, "Transport", "Uber",
                "Commutes", monthStart.plusDays(7).atTime(8, 15)));
        transactions.save(tx(new BigDecimal("15000"), TransactionType.EXPENSE, "Utilities", "KPLC + Safaricom",
                "Power and data", monthStart.plusDays(8).atTime(11, 0)));
        transactions.save(tx(new BigDecimal("6200"), TransactionType.EXPENSE, "Wants", "Java House",
                "Dining out", monthStart.plusDays(14).atTime(19, 45)));
        transactions.save(tx(new BigDecimal("4000"), TransactionType.EXPENSE, "Health", "Pharmacy",
                "Medicine", monthStart.plusDays(18).atTime(12, 0)));
        transactions.save(tx(new BigDecimal("2500"), TransactionType.EXPENSE, "Wants", "Netflix",
                "Subscriptions", monthStart.plusDays(3).atTime(7, 0)));

        seedSavings("Emergency fund", "300000", "60000", "HIGH", today.plusMonths(8),
                new int[]{8000, 10000, 12000, 6000, 9000, 14000, 7000, 11000, 15000, 10000, 12000, 16000}, today);
        seedSavings("Laptop upgrade", "120000", "10000", "MEDIUM", today.plusMonths(5),
                new int[]{2000, 3000, 4000, 2000, 3000, 5000, 2000, 4000, 5000, 3000, 4000, 6000}, today);

        debts.save(Debt.builder()
                .ownerId(WorkspaceAccess.owner())
                .name("Credit card")
                .balance(new BigDecimal("64200"))
                .interestRate(new BigDecimal("28.50"))
                .minimumPayment(new BigDecimal("5000"))
                .notes("Highest APR — Avalanche focus")
                .build());
        debts.save(Debt.builder()
                .ownerId(WorkspaceAccess.owner())
                .name("Fuliza / short loan")
                .balance(new BigDecimal("12000"))
                .interestRate(new BigDecimal("12.00"))
                .minimumPayment(new BigDecimal("2000"))
                .notes("Clear after card extras")
                .build());

        netWorthItems.save(item("M-Pesa + cash", new BigDecimal("24500"), ItemType.ASSET, null));
        netWorthItems.save(item("Bank savings", new BigDecimal("210000"), ItemType.ASSET, "Emergency fund home"));
        netWorthItems.save(item("Laptop / gear", new BigDecimal("80000"), ItemType.ASSET, "Estimated resale"));
        netWorthItems.save(item("Credit card", new BigDecimal("64200"), ItemType.LIABILITY, "Matches debt balance"));
        netWorthItems.save(item("Short loan", new BigDecimal("12000"), ItemType.LIABILITY, null));
    }
    private void seedSavings(String name, String target, String opening, String priority,
                             LocalDate deadline, int[] deposits, LocalDate today) {
        LocalDate start = LocalDate.of(today.getYear() - 1, 1, 1);
        SavingGoal goal = goals.save(SavingGoal.builder()
                .ownerId(WorkspaceAccess.owner()).name(name)
                .targetAmount(new BigDecimal(target)).openingAmount(new BigDecimal(opening))
                .currentAmount(new BigDecimal(opening)).startDate(start)
                .targetDate(deadline).priority(priority).completed(false).build());
        BigDecimal balance = goal.getOpeningAmount();
        // A complete prior year and the current year to date make the year selector useful too.
        for (LocalDate month = start; !month.isAfter(today); month = month.plusMonths(1)) {
            BigDecimal deposit = BigDecimal.valueOf(deposits[month.getMonthValue() - 1]);
            saveMovement(goal, month, deposit, "Demo · monthly savings deposit");
            balance = balance.add(deposit);
            if (month.getMonthValue() == 4 || month.getMonthValue() == 8) {
                LocalDate withdrawalDate = month.plusDays(14);
                if (!withdrawalDate.isAfter(today)) {
                    BigDecimal withdrawal = deposit.multiply(BigDecimal.valueOf(-2));
                    saveMovement(goal, withdrawalDate, withdrawal, "Demo · planned expense withdrawal");
                    balance = balance.add(withdrawal);
                }
            }
        }
        goal.setCurrentAmount(balance);
        goal.setCompleted(balance.compareTo(goal.getTargetAmount()) >= 0);
        goals.save(goal);
    }

    private void saveMovement(SavingGoal goal, LocalDate date, BigDecimal amount, String note) {
        var movement = new com.financetracker.model.SavingMovement();
        movement.setOwnerId(goal.getOwnerId());
        movement.setGoalId(goal.getId());
        movement.setDate(date);
        movement.setAmount(amount);
        movement.setNote(note);
        movements.save(movement);
    }

}
