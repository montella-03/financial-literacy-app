package com.financetracker.views;

import com.financetracker.model.Debt;
import com.financetracker.service.DebtService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "debts", layout = MainLayout.class)
@PageTitle("Debts · Steady")
public class DebtsView extends VerticalLayout {

	private final DebtService service;
	private final Grid<Debt> grid = new Grid<>(Debt.class, false);

	public DebtsView(DebtService service) {
		this.service = service;
		setPadding(false);
		setSpacing(false);
		setSizeFull();
		addClassName("app-content");

		add(PageSupport.hero(
				"Debt plan",
				"Pay with<br/><em>intention.</em>",
				"List every balance. Avalanche targets the highest interest first."
		));

		Button add = PageSupport.primary("Add debt");
		add.addClickListener(e -> openEditor(new Debt()));

		HorizontalLayout toolbar = new HorizontalLayout(add);
		toolbar.setPadding(false);
		toolbar.addClassName("view-toolbar");

		configureGrid();
		add(PageSupport.section("Balances", "Minimums everywhere — extras on the focus debt.", toolbar, grid));
		refresh();
	}

	private void configureGrid() {
		grid.addClassName("steady-grid");
		grid.setWidthFull();
		grid.setAllRowsVisible(true);
		grid.addColumn(Debt::getName).setHeader("Debt").setFlexGrow(1);
		grid.addColumn(debt -> MoneyFormat.kes(debt.getBalance())).setHeader("Balance");
		grid.addColumn(debt -> debt.getInterestRate() == null ? "—" : debt.getInterestRate() + "%")
				.setHeader("APR");
		grid.addColumn(debt -> MoneyFormat.kes(debt.getMinimumPayment())).setHeader("Minimum");
		grid.addComponentColumn(debt -> {
			Button edit = new Button("Edit", e -> openEditor(debt));
			edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			Button delete = new Button("Delete", e -> confirmDelete(debt));
			delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
			return new HorizontalLayout(edit, delete);
		}).setHeader("").setAutoWidth(true);
	}

	private void refresh() {
		grid.setItems(service.findAll());
	}

	private void openEditor(Debt debt) {
		boolean create = debt.getId() == null;
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(create ? "Add debt" : "Edit debt");
		dialog.setWidth("480px");

		TextField name = new TextField("Name");
		name.setValue(nullToEmpty(debt.getName()));
		name.setWidthFull();

		BigDecimalField balance = new BigDecimalField("Balance");
		balance.setValue(debt.getBalance());
		balance.setWidthFull();

		BigDecimalField rate = new BigDecimalField("Interest rate %");
		rate.setValue(debt.getInterestRate());
		rate.setWidthFull();

		BigDecimalField minimum = new BigDecimalField("Minimum payment");
		minimum.setValue(debt.getMinimumPayment());
		minimum.setWidthFull();

		TextArea notes = new TextArea("Notes");
		notes.setValue(nullToEmpty(debt.getNotes()));
		notes.setWidthFull();

		dialog.add(new FormLayout(name, balance, rate, minimum, notes));

		Button cancel = new Button("Cancel", e -> dialog.close());
		Button save = PageSupport.primary("Save");
		save.addClickListener(e -> {
			if (name.getValue() == null || name.getValue().isBlank() || balance.getValue() == null) {
				notifyError("Name and balance are required.");
				return;
			}
			debt.setName(name.getValue().trim());
			debt.setBalance(balance.getValue());
			debt.setInterestRate(rate.getValue());
			debt.setMinimumPayment(minimum.getValue());
			debt.setNotes(notes.getValue());
			service.save(debt);
			dialog.close();
			refresh();
			Notification.show("Saved", 2000, Notification.Position.BOTTOM_START)
					.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		});
		dialog.getFooter().add(cancel, save);
		dialog.open();
	}

	private void confirmDelete(Debt debt) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Delete debt?");
		dialog.add("Remove “" + debt.getName() + "”?");
		Button cancel = new Button("Cancel", e -> dialog.close());
		Button delete = new Button("Delete", e -> {
			service.delete(debt.getId());
			dialog.close();
			refresh();
		});
		delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
		dialog.getFooter().add(cancel, delete);
		dialog.open();
	}

	private static void notifyError(String message) {
		Notification.show(message, 3000, Notification.Position.MIDDLE)
				.addThemeVariants(NotificationVariant.LUMO_ERROR);
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
