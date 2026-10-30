package com.financetracker.views;

import com.financetracker.model.SavingMovement;
import com.financetracker.service.SavingGoalService;
import com.financetracker.service.SavingMetrics;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

@Route(value = "saving-metrics", layout = MainLayout.class)
@PageTitle("Saving metrics · Steady")
public class SavingMetricsView extends VerticalLayout {
    private final ComboBox<Integer> year = new ComboBox<>("Year");
    private final RadioButtonGroup<String> mode = new RadioButtonGroup<>();
    private final Div summary = new Div(), chart = new Div();
    private final Grid<SavingMetrics.Month> table = new Grid<>(SavingMetrics.Month.class, false);
    private final List<SavingMovement> movements;
    private final com.financetracker.service.SavingTargetService targetService;
    private java.util.Map<Integer, BigDecimal> targets = new java.util.HashMap<>();
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

    public SavingMetricsView(SavingGoalService service, com.financetracker.service.SavingTargetService targetService) {
        this.targetService = targetService;
        movements = service.movements();
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("app-content");
        addClassName("saving-metrics-page");
        TreeSet<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        years.add(LocalDate.now().getYear());
        years.add(LocalDate.now().getYear() + 1);
        targetService.findAll().forEach(t -> years.add(java.time.YearMonth.parse(t.getMonth()).getYear()));
        movements.forEach(m -> years.add(m.getDate().getYear()));
        year.setItems(years);
        year.setValue(LocalDate.now().getYear());
        year.setAllowCustomValue(false);
        year.setWidth("140px");
        mode.setLabel("View");
        mode.setItems("Chart", "Table");
        mode.setValue("Chart");
        var edit = new com.vaadin.flow.component.button.Button("Edit expected savings", e -> editTargets());
        HorizontalLayout controls = new HorizontalLayout(year, mode, edit);
        controls.addClassName("feature-toolbar");
        summary.addClassName("saving-metrics-summary");
        table.addClassName("steady-grid");
        table.setAllRowsVisible(true);
        table.addColumn(m -> m.month().format(MONTH) + (m.month().equals(java.time.YearMonth.now()) ? " · to date" : "")).setHeader("Month");
        table.addColumn(m -> expected(m)).setHeader("Expected savings");
        table.addColumn(m -> value(m, m.deposits())).setHeader("Deposits");
        table.addColumn(m -> value(m, m.withdrawals())).setHeader("Withdrawals");
        table.addColumn(m -> value(m, m.net())).setHeader("Net saved");
        table.addColumn(m -> m.future() || !targets.containsKey(m.month().getMonthValue()) ? "—" : MoneyFormat.kes(m.net().subtract(targets.get(m.month().getMonthValue())))).setHeader("Actual − expected");
        table.getColumns().forEach(c -> c.setAutoWidth(true));
        add(PageSupport.section("Saving metrics", "Your monthly savings plan against what you actually saved.", controls, summary, chart, table,
                new Paragraph("Expected amounts are monthly targets across all goals. Actual savings = deposits minus withdrawals; opening balances are excluded. Current month is to date. Future months show targets only.")));
        year.addValueChangeListener(e -> render());
        mode.addValueChangeListener(e -> render());
        render();
    }

    private String value(SavingMetrics.Month month, BigDecimal amount) {
        return month.future() ? "—" : MoneyFormat.kes(amount);
    }

    private void render() {
        if (year.getValue() == null) { year.setValue(LocalDate.now().getYear()); return; }
        targets = new java.util.HashMap<>();
        targetService.findAll().stream().filter(t -> java.time.YearMonth.parse(t.getMonth()).getYear() == year.getValue())
                .forEach(t -> targets.put(java.time.YearMonth.parse(t.getMonth()).getMonthValue(), t.getAmount()));
        var months = SavingMetrics.monthly(movements, year.getValue(), LocalDate.now());
        summary.removeAll();
        BigDecimal deposits = months.stream().map(SavingMetrics.Month::deposits).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal withdrawals = months.stream().map(SavingMetrics.Month::withdrawals).reduce(BigDecimal.ZERO, BigDecimal::add);
        metric("Expected · full year", targets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        metric("Actually saved · to date", deposits.subtract(withdrawals));
        BigDecimal variance = months.stream().filter(m -> !m.future() && targets.containsKey(m.month().getMonthValue()))
                .map(m -> m.net().subtract(targets.get(m.month().getMonthValue()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        metric("Actual − expected · planned months to date", variance);
        table.setItems(months);
        table.setVisible("Table".equals(mode.getValue()));
        chart.setVisible(!table.isVisible());
        chart.removeAll();
        chart.add(new Html(svg(months, targets, LocalDate.now())));
        Div legend = new Div(new Span("▰ Expected savings"), new Span("━ Actual net savings"), new Span("│ Today"));
        legend.addClassName("saving-metrics-legend");
        chart.add(legend);
        if (targets.isEmpty()) chart.add(new Paragraph("No expected savings set for this year. Use Edit expected savings to build your monthly plan."));
        if (deposits.signum() == 0 && withdrawals.signum() == 0)
            chart.add(new Paragraph("No savings movements recorded in this year. Record a deposit or withdrawal under Goals to start tracking."));
    }

    private void metric(String label, BigDecimal amount) {
        Div card = new Div(new Span(label), PageSupport.money(MoneyFormat.kes(amount), null));
        card.addClassName("saving-metric");
        summary.add(card);
    }

    private String expected(SavingMetrics.Month month) {
        BigDecimal amount = targets.get(month.month().getMonthValue());
        return amount == null ? "Not set" : MoneyFormat.kes(amount);
    }

    private void editTargets() {
        int selectedYear = year.getValue();
        var dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.setHeaderTitle("Expected savings · " + selectedYear);
        dialog.setWidth("660px");
        dialog.add(new Paragraph("Set the amount you expect to save each month across all goals. Leave blank for no target; enter 0 for a planned zero."));
        var form = new com.vaadin.flow.component.formlayout.FormLayout();
        java.util.Map<Integer, com.vaadin.flow.component.textfield.BigDecimalField> fields = new java.util.LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            var field = new com.vaadin.flow.component.textfield.BigDecimalField(java.time.YearMonth.of(selectedYear, month).format(MONTH) + " · KES");
            field.setValue(targets.get(month));
            fields.put(month, field);
            form.add(field);
        }
        dialog.add(form);
        var save = PageSupport.primary("Save expected savings");
        save.addClickListener(e -> FeatureSupport.attempt(() -> {
            if (fields.values().stream().anyMatch(com.vaadin.flow.component.textfield.BigDecimalField::isInvalid))
                throw new IllegalArgumentException("Correct the invalid amounts before saving.");
            java.util.Map<Integer, BigDecimal> amounts = new java.util.HashMap<>();
            fields.forEach((month, field) -> amounts.put(month, field.getValue()));
            targetService.saveYear(selectedYear, amounts);
            dialog.close();
            render();
        }));
        dialog.getFooter().add(new com.vaadin.flow.component.button.Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private static String n(double number) { return String.format(Locale.ROOT, "%.2f", number); }

    static String svg(List<SavingMetrics.Month> months, java.util.Map<Integer, BigDecimal> targets, LocalDate today) {
        double min = 0, max = 1;
        for (var m : months) {
            max = Math.max(max, targets.getOrDefault(m.month().getMonthValue(), BigDecimal.ZERO).doubleValue());
            if (!m.future()) {
                min = Math.min(min, m.net().doubleValue());
                max = Math.max(max, m.net().doubleValue());
            }
        }
        double low = min < 0 ? min * 1.15 : 0, high = max * 1.18;
        double range = high - low;
        java.util.function.DoubleUnaryOperator y = value -> 460 - (value - low) / range * 410;
        double zero = y.applyAsDouble(0);
        StringBuilder s = new StringBuilder("<div class='saving-metrics-plot'><svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1160 520' role='img' aria-label='Expected monthly savings shown as blue blocks, actual monthly net savings as a grey line. Exact amounts in Table view.'><defs><linearGradient id='actual-fill' x1='0' y1='0' x2='0' y2='1'><stop offset='0%' stop-color='#8798a3' stop-opacity='.22'/><stop offset='100%' stop-color='#8798a3' stop-opacity='.03'/></linearGradient></defs><text x='18' y='24' fill='#8293a0' font-size='12'>Savings / KES</text>");
        for (int i = 0; i <= 5; i++) {
            double value = low + range * i / 5, py = y.applyAsDouble(value);
            s.append("<line x1='100' x2='1132' y1='").append(n(py)).append("' y2='").append(n(py)).append("' stroke='#dfe5eb'/><text x='88' y='").append(n(py + 4)).append("' text-anchor='end' fill='#8293a0' font-size='12'>").append(String.format(Locale.ROOT, "%,.0f", value)).append("</text>");
        }
        StringBuilder points = new StringBuilder(), dots = new StringBuilder();
        double firstX = 143, lastX = firstX;
        for (int i = 0; i < months.size(); i++) {
            var m = months.get(i);
            double left = 100 + i * 86, x = left + 43;
            s.append("<text x='").append(n(x)).append("' y='488' text-anchor='middle' fill='#8293a0' font-size='12'>").append(m.month().format(MONTH)).append(" ").append(m.month().getYear()).append("</text>");
            BigDecimal expected = targets.get(m.month().getMonthValue());
            if (expected != null) {
                double top = y.applyAsDouble(expected.doubleValue());
                s.append("<rect x='").append(n(left)).append("' y='").append(n(top)).append("' width='86' height='").append(n(zero - top)).append("' fill='#bfeaf5' fill-opacity='.65' stroke='#37b4dd' stroke-width='1'><title>").append(m.month()).append(" expected: ").append(MoneyFormat.kes(expected)).append("</title></rect>");
                if (zero - top > 35) s.append("<text x='").append(n(left + 7)).append("' y='").append(n(top + 17)).append("' fill='#35758f' font-size='11'>").append(String.format(Locale.ROOT, "%,.0f", expected)).append("</text>");
            }
            if (m.future()) continue;
            double actualY = y.applyAsDouble(m.net().doubleValue());
            points.append(n(x)).append(',').append(n(actualY)).append(' ');
            lastX = x;
            dots.append("<circle cx='").append(n(x)).append("' cy='").append(n(actualY)).append("' r='4' fill='#fff' stroke='#8596a1' stroke-width='2'><title>").append(m.month()).append(" actual: ").append(MoneyFormat.kes(m.net())).append("</title></circle>");
        }
        if (!points.isEmpty()) {
            s.append("<polygon points='").append(n(firstX)).append(',').append(n(zero)).append(' ').append(points).append(n(lastX)).append(',').append(n(zero)).append("' fill='url(#actual-fill)'/>");
            s.append("<polyline points='").append(points).append("' fill='none' stroke='#8596a1' stroke-width='3' stroke-linejoin='round'/>").append(dots);
        }
        s.append("<line x1='100' x2='1132' y1='").append(n(zero)).append("' y2='").append(n(zero)).append("' stroke='#95aab7'/>");
        if (months.getFirst().month().getYear() == today.getYear()) {
            double x = 100 + (today.getMonthValue() - 1 + (today.getDayOfMonth() - 1.0) / today.lengthOfMonth()) * 86;
            s.append("<line x1='").append(n(x)).append("' x2='").append(n(x)).append("' y1='40' y2='460' stroke='#dd5272' stroke-width='1.5'/><text x='").append(n(x)).append("' y='30' text-anchor='middle' fill='#c54161' font-size='12'>Today</text>");
        }
        return s.append("</svg></div>").toString();
    }
}
