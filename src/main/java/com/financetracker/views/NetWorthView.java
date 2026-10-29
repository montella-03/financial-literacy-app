package com.financetracker.views;

import java.math.BigDecimal;

import com.financetracker.model.NetWorthItem;
import com.financetracker.model.NetWorthItem.ItemType;
import com.financetracker.service.NetWorthService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "net-worth", layout = MainLayout.class)
@PageTitle("Net worth · Steady")
public class NetWorthView extends VerticalLayout {

	private final NetWorthService service;
	private final Grid<NetWorthItem> grid = new Grid<>(NetWorthItem.class, false);
	private final Div summary = new Div();

	public NetWorthView(NetWorthService service) {
		this.service = service;
		setPadding(false);
		setSpacing(false);
		setSizeFull();
		addClassName("app-content");

		add(PageSupport.hero(
				"Balance sheet",
				"What you own,<br/><em>minus what you owe.</em>",
				"Net worth is the long view — assets and liabilities in one honest number."
		));

		summary.addClassName("page-hero");
		summary.getStyle().set("margin-bottom", "0");

		Button add = PageSupport.primary("Add item");
		add.addClickListener(e -> openEditor(new NetWorthItem()));

		HorizontalLayout toolbar = new HorizontalLayout(add);
		toolbar.setPadding(false);
		toolbar.addClassName("view-toolbar");

		configureGrid();
		add(summary);
		add(PageSupport.section("Assets & liabilities", "Update values as life changes.", toolbar, grid));
		refresh();
	}

	private void configureGrid() {
		grid.addClassName("steady-grid");
		grid.setWidthFull();
		grid.setAllRowsVisible(true);
		grid.addColumn(NetWorthItem::getName).setHeader("Name").setFlexGrow(1);
		grid.addColumn(item -> item.getType() == null ? "—" : item.getType().name()).setHeader("Type");
		grid.addColumn(item -> MoneyFormat.kes(item.getValue())).setHeader("Value");
		grid.addColumn(NetWorthItem::getNotes).setHeader("Notes").setFlexGrow(1);
		grid.addComponentColumn(item -> {
			Button edit = new Button("Edit", e -> openEditor(item));
			edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			Button delete = new Button("Delete", e -> confirmDelete(item));
			delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
			return new HorizontalLayout(edit, delete);
		}).setHeader("").setAutoWidth(true);
	}

	private void refresh() {
		grid.setItems(service.findAll());
		BigDecimal net = service.netWorth();
		summary.removeAll();
		Span eyebrow = PageSupport.eyebrow("Net worth");
		Div money = PageSupport.money(MoneyFormat.kes(net), net.signum() >= 0 ? "positive" : "negative");
		Paragraph detail = new Paragraph(
				"Assets " + MoneyFormat.kes(service.sum(ItemType.ASSET))
						+ " · Liabilities " + MoneyFormat.kes(service.sum(ItemType.LIABILITY)));
		detail.addClassName("quiet-meta");
		summary.add(eyebrow, money, detail);
	}

	private void openEditor(NetWorthItem item) {
		boolean create = item.getId() == null;
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(create ? "Add item" : "Edit item");
		dialog.setWidth("480px");

		TextField name = new TextField("Name");
		name.setValue(nullToEmpty(item.getName()));
		name.setWidthFull();

		ComboBox<ItemType> type = new ComboBox<>("Type");
		type.setItems(ItemType.values());
		type.setValue(item.getType() != null ? item.getType() : ItemType.ASSET);
		type.setWidthFull();

		BigDecimalField value = new BigDecimalField("Value");
		value.setValue(item.getValue());
		value.setWidthFull();

		TextArea notes = new TextArea("Notes");
		notes.setValue(nullToEmpty(item.getNotes()));
		notes.setWidthFull();

		dialog.add(new FormLayout(name, type, value, notes));

		Button cancel = new Button("Cancel", e -> dialog.close());
		Button save = PageSupport.primary("Save");
		save.addClickListener(e -> {
			if (name.getValue() == null || name.getValue().isBlank()
					|| type.getValue() == null || value.getValue() == null) {
				notifyError("Name, type, and value are required.");
				return;
			}
			item.setName(name.getValue().trim());
			item.setType(type.getValue());
			item.setValue(value.getValue());
			item.setNotes(notes.getValue());
			service.save(item);
			dialog.close();
			refresh();
			Notification.show("Saved", 2000, Notification.Position.BOTTOM_START)
					.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		});
		dialog.getFooter().add(cancel, save);
		dialog.open();
	}

	private void confirmDelete(NetWorthItem item) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Delete item?");
		dialog.add("Remove “" + item.getName() + "”?");
		Button cancel = new Button("Cancel", e -> dialog.close());
		Button delete = new Button("Delete", e -> {
			service.delete(item.getId());
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
