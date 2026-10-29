package com.financetracker.views;

import com.financetracker.model.SavingGoal;
import com.financetracker.model.SavingMovement;
import com.financetracker.service.SavingGoalService;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

final class SavingsChart extends Div {
    private final SavingGoalService service;
    private final ComboBox<SavingGoal> goal = new ComboBox<>("Goal");
    private final DatePicker from = new DatePicker("From"), to = new DatePicker("To");
    private final Div plot = new Div(), summary = new Div(), history = new Div();

    SavingsChart(SavingGoalService service) {
        this.service = service;
        setWidthFull();
        goal.setItemLabelGenerator(SavingGoal::getName);
        from.setValue(LocalDate.now().minusMonths(1));
        to.setValue(LocalDate.now().plusMonths(6));
        HorizontalLayout controls = new HorizontalLayout(goal, from, to);
        controls.addClassName("feature-toolbar");
        add(controls, summary, plot, history);
        goal.addValueChangeListener(e -> {
            SavingGoal selected = e.getValue();
            if (selected != null && (e.getOldValue() == null || !selected.getId().equals(e.getOldValue().getId()))) {
                LocalDate baseline = selected.getStartDate() == null ? LocalDate.now() : selected.getStartDate();
                from.setValue(baseline.minusDays(7));
                to.setValue(selected.getTargetDate() != null && selected.getTargetDate().isAfter(baseline) ? selected.getTargetDate() : LocalDate.now().plusMonths(1));
            }
            render();
        });
        from.addValueChangeListener(e -> render());
        to.addValueChangeListener(e -> render());
    }

    private static String point(double x, double y) {
        return String.format(Locale.ROOT, "%.2f,%.2f ", x, y);
    }

    void updateGoals(List<SavingGoal> goals) {
        Long selected = goal.getValue() == null ? null : goal.getValue().getId();
        goal.setItems(goals);
        if (!goals.isEmpty())
            goal.setValue(goals.stream().filter(g -> g.getId().equals(selected)).findFirst().orElse(goals.getFirst()));
        render();
    }

    private void render() {
        plot.removeAll();
        summary.removeAll();
        history.removeAll();
        SavingGoal g = goal.getValue();
        if (g == null) {
            plot.add(new Paragraph("Create a goal to start your savings story."));
            return;
        }
        LocalDate start = from.getValue(), end = to.getValue();
        if (start == null || end == null || !end.isAfter(start)) {
            plot.add(new Paragraph("Choose an end date after the start date."));
            return;
        }
        LocalDate baseline = g.getStartDate() == null ? LocalDate.now() : g.getStartDate();
        BigDecimal opening = g.getOpeningAmount() == null ? g.getCurrentAmount() : g.getOpeningAmount();
        var movements = service.history(g.getId());
        BigDecimal remaining = g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO);
        long months = g.getTargetDate() == null ? 0 : Math.max(1, ChronoUnit.MONTHS.between(YearMonth.now(), YearMonth.from(g.getTargetDate())) + 1);
        String pace = g.getTargetDate() == null ? "Add a target date to see your planned pace." : g.getTargetDate().isBefore(LocalDate.now()) && remaining.signum() > 0 ? "Target date passed. Review your deadline." : MoneyFormat.kes(remaining.divide(BigDecimal.valueOf(months), 0, RoundingMode.CEILING)) + " / month to target";
        summary.add(new Paragraph(MoneyFormat.kes(remaining) + " remaining · " + pace));
        TreeSet<LocalDate> dates = new TreeSet<>(List.of(start, end, baseline, LocalDate.now()));
        movements.forEach(m -> dates.add(m.getDate()));
        if (g.getTargetDate() != null) dates.add(g.getTargetDate());
        dates.removeIf(d -> d.isBefore(start) || d.isAfter(end));
        double max = Math.max(1, Math.max(g.getTargetAmount().doubleValue(), g.getCurrentAmount().doubleValue()));
        BigDecimal peak = opening;
        for (var m : movements) {
            peak = peak.add(m.getAmount());
            max = Math.max(max, peak.doubleValue());
        }
        max *= 1.1;
        long range = ChronoUnit.DAYS.between(start, end);
        StringBuilder actual = new StringBuilder(), planned = new StringBuilder();
        LocalDate previous = null;
        double previousY = 0;
        for (LocalDate d : dates) {
            double x = 54 + ChronoUnit.DAYS.between(start, d) * 690.0 / range;
            if (!d.isBefore(baseline) && !d.isAfter(LocalDate.now())) {
                BigDecimal balance = opening;
                for (var m : movements) if (!m.getDate().isAfter(d)) balance = balance.add(m.getAmount());
                double y = 240 - balance.doubleValue() / max * 200;
                if (previous != null) actual.append(point(x, previousY));
                actual.append(point(x, y));
                previous = d;
                previousY = y;
            }
            if (g.getTargetDate() != null && !d.isBefore(baseline)) {
                long duration = Math.max(1, ChronoUnit.DAYS.between(baseline, g.getTargetDate()));
                double fraction = Math.min(1, Math.max(0, ChronoUnit.DAYS.between(baseline, d) / (double) duration));
                double value = opening.doubleValue() + (g.getTargetAmount().doubleValue() - opening.doubleValue()) * fraction;
                planned.append(point(x, 240 - value / max * 200));
            }
        }
        String svg = "<div class='savings-plot'><svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 790 290' role='img' aria-label='Savings progress: solid green actual savings, dashed planned progress'>";
        for (int i = 0; i < 5; i++) {
            int y = 40 + i * 50;
            svg += "<line x1='54' y1='" + y + "' x2='744' y2='" + y + "' stroke='#e3e9df'/><text x='48' y='" + (y + 4) + "' text-anchor='end' font-size='11' fill='#63746b'>" + String.format(Locale.ROOT, "%.0fk", max * (4 - i) / 4 / 1000) + "</text>";
        }
        svg += "<polyline points='" + planned + "' fill='none' stroke='#9b854f' stroke-width='3' stroke-dasharray='7 6'/><polyline points='" + actual + "' fill='none' stroke='#245b43' stroke-width='3'/>";
        if (previous != null) {
            double x = 54 + ChronoUnit.DAYS.between(start, previous) * 690.0 / range;
            svg += "<circle cx='" + x + "' cy='" + previousY + "' r='5' fill='#245b43'/>";
        }
        svg += "<text x='54' y='272' font-size='12' fill='#63746b'>" + start + "</text><text x='744' y='272' text-anchor='end' font-size='12' fill='#63746b'>" + end + "</text></svg></div>";
        plot.add(new Html(svg), new Paragraph("● Actual savings   ┄ Planned progress · KES · opening balance " + MoneyFormat.kes(opening) + " on " + baseline));
        var visible = movements.stream().filter(m -> !m.getDate().isBefore(start) && !m.getDate().isAfter(end)).toList();
        if (visible.isEmpty())
            history.add(new Paragraph("No deposits or withdrawals in this date range. Existing balances begin tracking today; past progress is not estimated."));
        else {
            com.vaadin.flow.component.grid.Grid<SavingMovement> grid = new com.vaadin.flow.component.grid.Grid<>(SavingMovement.class, false);
            grid.addClassName("steady-grid");
            grid.setAllRowsVisible(true);
            grid.addColumn(SavingMovement::getDate).setHeader("Date");
            grid.addColumn(m -> MoneyFormat.kes(m.getAmount())).setHeader("Movement");
            grid.addColumn(SavingMovement::getNote).setHeader("Note");
            grid.setItems(visible);
            history.add(grid);
        }
    }
}
