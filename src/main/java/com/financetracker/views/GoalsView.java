package com.financetracker.views;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.financetracker.model.SavingGoal;
import com.financetracker.service.SavingGoalService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "goals", layout = MainLayout.class)
@PageTitle("Goals · Steady")
public class GoalsView extends VerticalLayout {

	private final SavingGoalService service;
	private final Grid<SavingGoal> grid = new Grid<>(SavingGoal.class, false);

	public GoalsView(SavingGoalService service) {
		this.service = service;
		setPadding(false);
		setSpacing(false);
		setSizeFull();
		addClassName("app-content");

		add(PageSupport.hero(
				"Saving goals",
				"Name it.<br/><em>Fund it.</em>",
				"Emergency fund, school fees, a trip — goals turn surplus into intention."
		));

		Button add = PageSupport.primary("Create goal");
		add.addClickListener(e -> openEditor(new SavingGoal()));

		HorizontalLayout toolbar = new HorizontalLayout(add);
		toolbar.setPadding(false);
		toolbar.addClassName("view-toolbar");

		configureGrid();
		add(PageSupport.section("Your goals", "Track progress without the noise.", toolbar, grid));
		refresh();
	}

	private void configureGrid() {
		grid.addClassName("steady-grid");
		grid.setWidthFull();
		grid.setAllRowsVisible(true);
		grid.addColumn(SavingGoal::getName).setHeader("Goal").setFlexGrow(1);
		grid.addColumn(goal -> MoneyFormat.kes(goal.getCurrentAmount())).setHeader("Saved");
		grid.addColumn(goal -> MoneyFormat.kes(goal.getTargetAmount())).setHeader("Target");
		grid.addColumn(this::progressLabel).setHeader("Progress");
		grid.addColumn(SavingGoal::getPriority).setHeader("Priority");
		grid.addComponentColumn(goal -> {
			Button edit = new Button("Edit", e -> openEditor(goal));
			edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			Button delete = new Button("Delete", e -> confirmDelete(goal));
			delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
			return new HorizontalLayout(edit, delete);
		}).setHeader("").setAutoWidth(true);
	}

	private String progressLabel(SavingGoal goal) {
		BigDecimal target = goal.getTargetAmount() == null ? BigDecimal.ZERO : goal.getTargetAmount();
		BigDecimal current = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();
		if (target.signum() == 0) {
			return "—";
		}
		int pct = current.multiply(BigDecimal.valueOf(100)).divide(target, 0, RoundingMode.HALF_UP).intValue();
		return Math.min(100, Math.max(0, pct)) + "%" + (goal.isCompleted() ? " · done" : "");
	}

	private void refresh() {
		grid.setItems(service.findAll());
	}

	private void openEditor(SavingGoal goal) {
		boolean create = goal.getId() == null;
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(create ? "Create goal" : "Edit goal");
		dialog.setWidth("480px");

		TextField name = new TextField("Name");
		name.setValue(nullToEmpty(goal.getName()));
		name.setWidthFull();

		BigDecimalField target = new BigDecimalField("Target amount");
		target.setValue(goal.getTargetAmount());
		target.setWidthFull();

		BigDecimalField current = new BigDecimalField("Saved so far");
		current.setValue(goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount());
		current.setWidthFull();

		DatePicker targetDate = new DatePicker("Target date");
		targetDate.setValue(goal.getTargetDate());
		targetDate.setWidthFull();

		ComboBox<String> priority = new ComboBox<>("Priority");
		priority.setItems("HIGH", "MEDIUM", "LOW");
		priority.setValue(goal.getPriority() == null ? "MEDIUM" : goal.getPriority());
		priority.setWidthFull();

		Checkbox completed = new Checkbox("Completed");
		completed.setValue(goal.isCompleted());

		dialog.add(new FormLayout(name, target, current, targetDate, priority, completed));

		Button cancel = new Button("Cancel", e -> dialog.close());
		Button save = PageSupport.primary("Save");
		save.addClickListener(e -> {
			if (name.getValue() == null || name.getValue().isBlank() || target.getValue() == null) {
				notifyError("Name and target are required.");
				return;
			}
			goal.setName(name.getValue().trim());
			goal.setTargetAmount(target.getValue());
			goal.setCurrentAmount(current.getValue() == null ? BigDecimal.ZERO : current.getValue());
			goal.setTargetDate(targetDate.getValue());
			goal.setPriority(priority.getValue());
			goal.setCompleted(completed.getValue());
			service.save(goal);
			dialog.close();
			refresh();
			Notification.show("Saved", 2000, Notification.Position.BOTTOM_START)
					.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		});
		dialog.getFooter().add(cancel, save);
		dialog.open();
	}

	private void confirmDelete(SavingGoal goal) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Delete goal?");
		dialog.add("Remove “" + goal.getName() + "” from your plan?");
		Button cancel = new Button("Cancel", e -> dialog.close());
		Button delete = new Button("Delete", e -> {
			service.delete(goal.getId());
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
