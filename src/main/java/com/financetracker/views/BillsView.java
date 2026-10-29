package com.financetracker.views;

import com.financetracker.model.Bill;
import com.financetracker.model.Debt;
import com.financetracker.service.DebtService;
import com.financetracker.service.PlanningService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "bills", layout = MainLayout.class)
@PageTitle("Bills · Steady")
public class BillsView extends VerticalLayout {
    private final PlanningService service;
    private final DebtService debts;
    private final Grid<Bill> grid = new Grid<>(Bill.class, false);

    public BillsView(PlanningService service, DebtService debts) {
        this.service = service;
        this.debts = debts;
        setPadding(false);
        setSpacing(false);
        addClassName("app-content");
        add(PageSupport.hero("Upcoming commitments", "Make space for <em>what’s due.</em>", "In-app reminders for bills and debt payments. Monthly bills roll forward after payment."));
        grid.addClassName("steady-grid");
        grid.setAllRowsVisible(true);
        grid.addColumn(Bill::getName).setHeader("Bill");
        grid.addColumn(b -> MoneyFormat.kes(b.getAmount())).setHeader("Amount");
        grid.addColumn(Bill::getDueDate).setHeader("Due");
        grid.addColumn(service::billStatus).setHeader("Reminder");
        grid.addColumn(b -> b.isMonthly() ? "Monthly" : "Once").setHeader("Repeats");
        grid.addComponentColumn(b -> {
            Button pay = new Button("Record payment", e -> confirmPayment(b));
            pay.setEnabled(b.getPaidOn() == null);
            Button edit = new Button("Edit", e -> editor(b));
            edit.setEnabled(b.getPaidOn() == null);
            Button delete = new Button("Delete", e -> {
                Dialog d = new Dialog();
                d.setHeaderTitle("Delete bill?");
                d.add("Recorded payments remain in your transaction history.");
                d.getFooter().add(new Button("Cancel", x -> d.close()), new Button("Delete", x -> {
                    service.deleteBill(b.getId());
                    d.close();
                    refresh();
                }));
                d.open();
            });
            return new HorizontalLayout(pay, edit, delete);
        }).setAutoWidth(true);
        add(PageSupport.section("Your bills", "Recording a payment creates an expense. Don’t record it again through a bank import.", new Button("Add bill", e -> editor(new Bill())), grid));
        refresh();
    }

    private void refresh() {
        grid.setItems(service.bills());
    }

    private void confirmPayment(Bill bill) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Record payment?");
        d.add(new Paragraph("Record " + MoneyFormat.kes(bill.getAmount()) + " paid today for " + bill.getName() + ". This adds an expense" + (bill.getDebtId() == null ? "." : " and reduces the linked debt balance. Enter principal-only payments here; interest should be a separate expense.")));
        d.getFooter().add(new Button("Cancel", e -> d.close()), new Button("Confirm payment", e -> FeatureSupport.attempt(() -> {
            service.payBill(bill.getId());
            d.close();
            refresh();
        })));
        d.open();
    }

    private void editor(Bill bill) {
        Dialog d = new Dialog();
        d.setWidth("550px");
        d.setHeaderTitle(bill.getId() == null ? "Add bill" : "Edit bill");
        TextField name = new TextField("Name");
        name.setValue(bill.getName() == null ? "" : bill.getName());
        name.setMaxLength(150);
        BigDecimalField amount = new BigDecimalField("Amount (KES)");
        amount.setValue(bill.getAmount());
        DatePicker due = new DatePicker("Due date");
        due.setValue(bill.getDueDate() == null ? java.time.LocalDate.now() : bill.getDueDate());
        TextField category = new TextField("Category");
        category.setValue(bill.getCategory() == null ? "Bills" : bill.getCategory());
        category.setMaxLength(100);
        Checkbox repeat = new Checkbox("Repeat monthly");
        repeat.setValue(bill.isMonthly());
        ComboBox<Debt> debt = new ComboBox<>("Linked debt (optional, principal only)");
        var choices = debts.findAll();
        debt.setItems(choices);
        debt.setItemLabelGenerator(Debt::getName);
        debt.setClearButtonVisible(true);
        choices.stream().filter(x -> x.getId().equals(bill.getDebtId())).findFirst().ifPresent(debt::setValue);
        d.add(new FormLayout(name, amount, due, category, repeat, debt));
        d.getFooter().add(new Button("Cancel", e -> d.close()), new Button("Save", e -> FeatureSupport.attempt(() -> {
            bill.setName(name.getValue());
            bill.setAmount(amount.getValue());
            bill.setDueDate(due.getValue());
            bill.setCategory(category.getValue());
            bill.setMonthly(repeat.getValue());
            bill.setDebtId(debt.getValue() == null ? null : debt.getValue().getId());
            service.saveBill(bill);
            d.close();
            refresh();
        })));
        d.open();
    }
}
