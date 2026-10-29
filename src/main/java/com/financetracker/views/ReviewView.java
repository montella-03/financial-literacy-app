package com.financetracker.views;

import com.financetracker.service.PlanningService;
import com.financetracker.service.ReviewService;
import com.financetracker.service.TransactionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.YearMonth;

@Route(value = "review", layout = MainLayout.class)
@PageTitle("Monthly review · Steady")
public class ReviewView extends VerticalLayout {
    private final DatePicker month = new DatePicker("Review month (choose any day)");
    private final Div body = new Div();
    private final TransactionService transactions;
    private final PlanningService planning;
    private final ReviewService review;

    public ReviewView(TransactionService transactions, PlanningService planning, ReviewService review) {
        this.transactions = transactions;
        this.planning = planning;
        this.review = review;
        setPadding(false);
        setSpacing(false);
        addClassName("app-content");
        add(PageSupport.hero("Pause and reflect", "Small changes. <em>Visible progress.</em>", "Review your cash flow, savings activity, and recorded balances. Export your records anytime."));
        month.setValue(LocalDate.now());
        month.setMax(LocalDate.now());
        month.addValueChangeListener(e -> refresh());
        Button capture = new Button("Save this month’s balance snapshot", e -> FeatureSupport.attempt(() -> {
            planning.capture();
            refresh();
        }));
        HorizontalLayout controls = new HorizontalLayout(month, capture);
        controls.addClassName("feature-toolbar");
        add(PageSupport.section("Your monthly review", "Snapshots record today’s savings and debt balances. Previous months stay unchanged; historical balances aren’t estimated.", controls, body));
        HorizontalLayout exports = new HorizontalLayout(
                FeatureSupport.download("Monthly review CSV", "monthly-review.csv", () -> review.reviewCsv(selected())),
                FeatureSupport.download("Transactions CSV", "transactions.csv", review::transactionsCsv),
                FeatureSupport.download("Goals & history CSV", "savings.csv", review::goalsCsv),
                FeatureSupport.download("Debts CSV", "debts.csv", review::debtsCsv),
                FeatureSupport.download("Net worth CSV", "net-worth.csv", review::worthCsv),
                FeatureSupport.download("Bills & budgets CSV", "plans.csv", review::plansCsv));
        exports.addClassName("feature-toolbar");
        add(PageSupport.section("Your data, to keep", "CSV downloads include only your signed-in workspace. Text cells are escaped for spreadsheet safety.", exports));
        refresh();
    }

    private YearMonth selected() {
        return month.getValue() == null ? YearMonth.now() : YearMonth.from(month.getValue());
    }

    private void refresh() {
        body.removeAll();
        YearMonth m = selected();
        body.add(new Paragraph("Income: " + MoneyFormat.kes(transactions.sumIncome(m)) + " · Spending: " + MoneyFormat.kes(transactions.sumExpenses(m)) + " · Surplus: " + MoneyFormat.kes(transactions.surplus(m))));
        var change = transactions.sumExpenses(m).subtract(transactions.sumExpenses(m.minusMonths(1)));
        body.add(new Paragraph("Spending " + (change.signum() > 0 ? "increased by " : "decreased by ") + MoneyFormat.kes(change.abs()) + " compared with " + m.minusMonths(1) + "."));
        body.add(new Paragraph("Net recorded savings deposits minus withdrawals: " + MoneyFormat.kes(review.savingsMovement(m)) + ". Opening balances are excluded."));
        planning.spending(m).forEach((name, total) -> body.add(new Paragraph(name + " · " + MoneyFormat.kes(total))));
        var current = planning.snapshot(m);
        var previous = planning.snapshot(m.minusMonths(1));
        if (current.isEmpty()) body.add(new Paragraph("No balance snapshot for this month."));
        else {
            body.add(new Paragraph("Captured " + current.get().getCapturedOn() + ": debt " + MoneyFormat.kes(current.get().getDebtBalance()) + " · savings " + MoneyFormat.kes(current.get().getSavingsBalance())));
            if (previous.isPresent()) {
                var delta = previous.get().getDebtBalance().subtract(current.get().getDebtBalance());
                body.add(new Paragraph("Debt balance " + (delta.signum() >= 0 ? "reduced by " : "increased by ") + MoneyFormat.kes(delta.abs()) + " since the previous month’s snapshot."));
            } else body.add(new Paragraph("Save snapshots in consecutive months to compare debt balances."));
        }
    }
}
