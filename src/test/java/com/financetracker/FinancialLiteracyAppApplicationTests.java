package com.financetracker;

import com.financetracker.model.*;
import com.financetracker.repository.UserAccountRepository;
import com.financetracker.service.*;
import com.financetracker.session.AppSession;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:steady;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "vaadin.launch-browser=false"
})
class FinancialLiteracyAppApplicationTests {
    @Autowired
    TransactionService transactions;
    @Autowired
    TransactionImportService imports;
    @Autowired
    SavingGoalService goals;
    @Autowired
    PlanningService planning;
    @Autowired
    DebtService debts;
    @Autowired
    NetWorthService worth;
    @Autowired
    DummyDataService demo;
    @Autowired
    AccountService accounts;
    @Autowired
    UserAccountRepository users;
    @Autowired
    ReviewService review;
    private String owner;

    @BeforeEach
    void session() {
        Map<String, Object> attributes = new HashMap<>();
        VaadinSession session = mock(VaadinSession.class);
        doAnswer(i -> {
            attributes.put(i.getArgument(0), i.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        when(session.getAttribute(anyString())).thenAnswer(i -> attributes.get(i.getArgument(0)));
        VaadinSession.setCurrent(session);
        owner = UUID.randomUUID().toString();
        AppSession.enter(owner, "Test", false);
    }

    @AfterEach
    void clear() {
        VaadinSession.setCurrent(null);
    }

    @Test
    void accountsPersistAndPasswordsAreHashed() {
        String email = UUID.randomUUID() + "@example.com";
        var user = accounts.register(email, "Ada", "a-long-password-123");
        assertNotEquals("a-long-password-123", users.findById(user.getId()).orElseThrow().getPasswordHash());
        AppSession.clear();
        assertEquals(user.getId(), accounts.login(email.toUpperCase(), "a-long-password-123").getId());
        assertThrows(IllegalArgumentException.class, () -> accounts.login(email, "wrong"));
        assertThrows(IllegalArgumentException.class, () -> accounts.register("bad", "A", "short"));
    }

    @Test
    void accountIsolationIncludesReadsUpdatesDeletesAndDemo() {
        Transaction transaction = transactions.save(tx("100", Transaction.TransactionType.INCOME, LocalDate.now()));
        SavingGoal goal = goals.save(goal("1000", "10"));
        Debt debt = new Debt();
        debt.setName("Loan");
        debt.setBalance(new BigDecimal("500"));
        debts.save(debt);
        NetWorthItem item = new NetWorthItem();
        item.setName("Cash");
        item.setValue(BigDecimal.TEN);
        item.setType(NetWorthItem.ItemType.ASSET);
        worth.save(item);
        String other = UUID.randomUUID().toString();
        AppSession.enter(other, "Other", false);
        assertTrue(transactions.findAll().isEmpty());
        assertEquals(BigDecimal.ZERO, transactions.sumIncome(YearMonth.now()));
        assertTrue(goals.findAll().isEmpty());
        assertTrue(debts.findAll().isEmpty());
        assertTrue(worth.findAll().isEmpty());
        assertThrows(SecurityException.class, () -> transactions.save(transaction));
        assertThrows(SecurityException.class, () -> transactions.delete(transaction.getId()));
        assertThrows(SecurityException.class, () -> goals.contribute(goal.getId(), BigDecimal.ONE, LocalDate.now(), ""));
        assertThrows(SecurityException.class, () -> debts.delete(debt.getId()));
        assertThrows(SecurityException.class, () -> worth.save(item));
        AppSession.enter("demo-" + UUID.randomUUID(), "Demo", true);
        demo.createDemo();
        assertFalse(transactions.findAll().isEmpty());
        AppSession.enter(owner, "Test", false);
        assertEquals(1, transactions.findAll().size());
        assertEquals(1, goals.findAll().size());
        AppSession.clear();
        assertThrows(SecurityException.class, transactions::findAll);
    }

    @Test
    void csvImportIsAtomicIdempotentAndTransfersDoNotAffectCashFlow() {
        String header = "date,type,amount,category,merchant,description,account,destination\r\n";
        String line = LocalDate.now() + ",EXPENSE,200,Food,\"Shop, Inc\",\"Lunch \"\"deal\"\"\",Bank,\r\n";
        var result = imports.importCsv(header + line + line);
        assertEquals(1, result.imported());
        assertEquals(1, result.duplicates());
        assertEquals(0, imports.importCsv(header + line).imported());
        assertThrows(IllegalArgumentException.class, () -> imports.importCsv(header + LocalDate.now() + ",INCOME,50,Salary,Acme,,Bank,\ninvalid,row"));
        assertEquals(1, transactions.findAll().size());
        Transaction transfer = tx("500", Transaction.TransactionType.TRANSFER, LocalDate.now());
        transfer.setAccountName("Bank");
        transfer.setDestinationAccount("Savings");
        transactions.save(transfer);
        assertEquals(0, new BigDecimal("200").compareTo(transactions.sumExpenses(YearMonth.now())));
        assertEquals(0, BigDecimal.ZERO.compareTo(transactions.sumIncome(YearMonth.now())));
        assertTrue(review.transactionsCsv().contains("Shop, Inc"));
    }

    @Test
    void monthlyBoundariesAreExclusiveAndBudgetsMatchCategories() {
        YearMonth previous = YearMonth.now().minusMonths(1);
        transactions.save(tx("100", Transaction.TransactionType.EXPENSE, previous.atEndOfMonth()));
        transactions.save(tx("50", Transaction.TransactionType.EXPENSE, YearMonth.now().atDay(1)));
        assertEquals(0, new BigDecimal("100").compareTo(transactions.sumExpenses(previous)));
        planning.budget(" FOOD ", YearMonth.now(), new BigDecimal("40"));
        planning.budget("food", YearMonth.now(), new BigDecimal("45"));
        assertEquals(1, planning.budgets(YearMonth.now()).size());
        assertEquals(0, new BigDecimal("50").compareTo(planning.spending(YearMonth.now()).get("food")));
    }

    @Test
    void savingsLedgerRetainsHistoryAndRejectsNegativeBalances() {
        SavingGoal goal = goals.save(goal("1000", "100"));
        goals.contribute(goal.getId(), new BigDecimal("50"), LocalDate.now(), "Deposit");
        goals.contribute(goal.getId(), new BigDecimal("-20"), LocalDate.now(), "Withdrawal");
        assertEquals(2, goals.history(goal.getId()).size());
        assertEquals(0, new BigDecimal("130").compareTo(goals.findAll().getFirst().getCurrentAmount()));
        assertThrows(IllegalArgumentException.class, () -> goals.contribute(goal.getId(), new BigDecimal("-131"), LocalDate.now(), ""));
        assertThrows(IllegalArgumentException.class, () -> goals.contribute(goal.getId(), BigDecimal.ONE, LocalDate.now().plusDays(1), ""));
        assertEquals(0, new BigDecimal("30").compareTo(review.savingsMovement(YearMonth.now())));
        goals.delete(goal.getId());
        assertTrue(goals.findAll().isEmpty());
    }

    @Test
    void recurringBillPaymentReducesDebtOnceAndClampsMonthEnd() {
        Debt debt = new Debt();
        debt.setName("Loan");
        debt.setBalance(new BigDecimal("500"));
        debts.save(debt);
        Bill bill = new Bill();
        bill.setName("Loan payment");
        bill.setAmount(new BigDecimal("100"));
        bill.setDueDate(LocalDate.of(2026, 1, 31));
        bill.setMonthly(true);
        bill.setDebtId(debt.getId());
        planning.saveBill(bill);
        planning.payBill(bill.getId());
        assertEquals(1, transactions.findAll().size());
        assertEquals(0, new BigDecimal("400").compareTo(debts.findAll().getFirst().getBalance()));
        assertThrows(IllegalArgumentException.class, () -> planning.payBill(bill.getId()));
        Bill feb = planning.bills().stream().filter(b -> b.getPaidOn() == null).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 2, 28), feb.getDueDate());
        planning.payBill(feb.getId());
        assertTrue(planning.bills().stream().anyMatch(b -> LocalDate.of(2026, 3, 31).equals(b.getDueDate())));
        planning.capture();
        assertTrue(planning.snapshot(YearMonth.now()).isPresent());
        assertTrue(planning.snapshot(YearMonth.now().minusMonths(1)).isEmpty());
    }

    @Test
    void plansSnapshotsAndExportsStayWithinTheWorkspace() {
        planning.budget("Food", YearMonth.now(), new BigDecimal("1000"));
        Bill bill = new Bill();
        bill.setName("Private bill");
        bill.setAmount(BigDecimal.TEN);
        bill.setDueDate(LocalDate.now());
        planning.saveBill(bill);
        transactions.save(tx("123", Transaction.TransactionType.INCOME, LocalDate.now()));
        planning.capture();
        AppSession.enter(UUID.randomUUID().toString(), "Other", false);
        assertTrue(planning.allBudgets().isEmpty());
        assertTrue(planning.bills().isEmpty());
        assertTrue(planning.snapshot(YearMonth.now()).isEmpty());
        assertThrows(SecurityException.class, () -> planning.payBill(bill.getId()));
        assertThrows(SecurityException.class, () -> planning.deleteBill(bill.getId()));
        assertFalse(review.transactionsCsv().contains("123"));
        assertFalse(review.plansCsv().contains("Private bill"));
    }

    @Test
    void failedDebtPaymentRollsBackExpenseAndBillState() {
        Debt debt = new Debt();
        debt.setName("Small loan");
        debt.setBalance(BigDecimal.TEN);
        debts.save(debt);
        Bill bill = new Bill();
        bill.setName("Too much");
        bill.setAmount(new BigDecimal("20"));
        bill.setDueDate(LocalDate.now());
        bill.setDebtId(debt.getId());
        planning.saveBill(bill);
        assertThrows(IllegalArgumentException.class, () -> planning.payBill(bill.getId()));
        assertTrue(transactions.findAll().isEmpty());
        assertNull(planning.bills().getFirst().getPaidOn());
        assertThrows(IllegalArgumentException.class, () -> debts.delete(debt.getId()));
    }

    @Test
    void spreadsheetExportsEscapeFormulasAndCsvHandlesMultilineFields() {
        assertEquals("\"'=SUM(A1)\"", CsvSupport.cell("=SUM(A1)"));
        assertTrue(CsvSupport.cell("  =SUM(A1)\nmore").startsWith("\"'"));
        assertEquals("two\nlines", CsvSupport.parse("a,b\n1,\"two\nlines\"\n").get(1).get(1));
        assertThrows(IllegalArgumentException.class, () -> CsvSupport.parse("a,\"unclosed"));
    }

    private Transaction tx(String amount, Transaction.TransactionType type, LocalDate date) {
        Transaction t = new Transaction();
        t.setAmount(new BigDecimal(amount));
        t.setType(type);
        t.setCategory("Food");
        t.setTransactionDate(date.atTime(12, 0));
        return t;
    }

    private SavingGoal goal(String target, String current) {
        SavingGoal g = new SavingGoal();
        g.setName("Emergency");
        g.setTargetAmount(new BigDecimal(target));
        g.setCurrentAmount(new BigDecimal(current));
        g.setTargetDate(LocalDate.now().plusMonths(6));
        return g;
    }
}
