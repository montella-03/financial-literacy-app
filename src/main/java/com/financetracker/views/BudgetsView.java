package com.financetracker.views;

import com.financetracker.model.Budget;
import com.financetracker.service.PlanningService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Route(value = "budgets", layout = MainLayout.class)
@PageTitle("Budgets · Steady")
public class BudgetsView extends VerticalLayout {
    private final PlanningService service;
    private final DatePicker month = new DatePicker("Month (choose any day)");
    private final Div body = new Div();

    public BudgetsView(PlanningService service) {
        this.service = service;
        setPadding(false);
        setSpacing(false);
        addClassName("app-content");
        add(PageSupport.hero("Monthly plan", "Give every shilling <em>a purpose.</em>", "Set category limits and see what is left before the month ends."));
        month.setValue(LocalDate.now());
        month.addValueChangeListener(e -> refresh());
        TextField category = new TextField("Category");
        category.setPlaceholder("e.g. Food");
        category.setMaxLength(100);
        BigDecimalField limit = new BigDecimalField("Monthly limit (KES)");
        Button save = PageSupport.primary("Set budget");
        save.addClickListener(e -> FeatureSupport.attempt(() -> {
            if (month.getValue() == null) throw new IllegalArgumentException("Choose a month.");
            service.budget(category.getValue(), YearMonth.from(month.getValue()), limit.getValue());
            refresh();
        }));
        HorizontalLayout form = new HorizontalLayout(month, category, limit, save);
        form.addClassName("feature-toolbar");
        add(PageSupport.section("Your category budgets", "Category matching ignores letter case. Setting the same category updates its limit.", form, body));
        refresh();
    }

    private void refresh() {
        body.removeAll();
        if (month.getValue() == null) return;
        YearMonth selected = YearMonth.from(month.getValue());
        var spending = service.spending(selected);
        var budgets = service.budgets(selected);
        Grid<Budget> grid = new Grid<>(Budget.class, false);
        grid.setAllRowsVisible(true);
        grid.addClassName("steady-grid");
        grid.addColumn(Budget::getCategory).setHeader("Category");
        grid.addColumn(b -> MoneyFormat.kes(b.getLimitAmount())).setHeader("Limit");
        grid.addColumn(b -> MoneyFormat.kes(spending.getOrDefault(b.getCategory(), BigDecimal.ZERO))).setHeader("Spent");
        grid.addColumn(b -> MoneyFormat.kes(b.getLimitAmount().subtract(spending.getOrDefault(b.getCategory(), BigDecimal.ZERO)))).setHeader("Remaining");
        grid.addColumn(b -> {
            BigDecimal spent = spending.getOrDefault(b.getCategory(), BigDecimal.ZERO);
            return spent.compareTo(b.getLimitAmount()) > 0 ? "Over budget" : spent.compareTo(b.getLimitAmount().multiply(new BigDecimal("0.8"))) >= 0 ? "Near limit" : "Within budget";
        }).setHeader("Status");
        grid.addComponentColumn(b -> new Button("Remove limit", e -> {
            service.deleteBudget(b.getId());
            refresh();
        }));
        grid.setItems(budgets);
        body.add(grid);
        if (budgets.isEmpty())
            body.add(new Paragraph("No limits for this month. Add your first category budget above."));
        spending.forEach((name, total) -> {
            if (budgets.stream().noneMatch(b -> b.getCategory().equals(name)))
                body.add(new Paragraph("No budget set · " + name + ": " + MoneyFormat.kes(total) + " spent"));
        });
    }
}
