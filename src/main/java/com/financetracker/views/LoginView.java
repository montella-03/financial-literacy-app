package com.financetracker.views;

import com.financetracker.service.DummyDataService;
import com.financetracker.session.AppSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("login")
@PageTitle("Enter · Steady")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

	private final DummyDataService dummyDataService;

	public LoginView(DummyDataService dummyDataService) {
		this.dummyDataService = dummyDataService;

		setSizeFull();
		setPadding(false);
		setSpacing(false);
		setJustifyContentMode(JustifyContentMode.CENTER);
		setAlignItems(Alignment.CENTER);
		addClassName("login-page");

		Div card = new Div();
		card.addClassName("login-card");

		Span brand = new Span("Steady");
		brand.addClassName("brand-wordmark");
		brand.addClassName("login-brand");

		Span eyebrow = PageSupport.eyebrow("Personal finance");
		H1 title = PageSupport.title("Know where<br/><em>you stand.</em>");

		Paragraph lede = new Paragraph(
				"Track cash flow, goals, and debt with calm clarity. Choose how you want to begin.");
		lede.addClassName("lede");

		TextField name = new TextField("Your name (optional)");
		name.setWidthFull();
		name.setPlaceholder("e.g. Chris");

		Checkbox useDemo = new Checkbox("Load demo data so I can explore the app");
		useDemo.setValue(true);
		useDemo.addClassName("login-demo-check");

		Paragraph note = new Paragraph(
				"Starting resets this workspace. Demo mode fills realistic sample numbers; "
						+ "without demo you begin with a clean slate.");
		note.addClassName("login-note");

		Button enter = new Button("Enter Steady", event -> start(name.getValue(), useDemo.getValue()));
		enter.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		enter.setWidthFull();

		card.add(brand, eyebrow, title, lede, name, useDemo, note, enter);
		add(card);
	}

	private void start(String name, boolean demo) {
		if (demo) {
			dummyDataService.resetWithDemo();
		} else {
			dummyDataService.resetEmpty();
		}
		AppSession.enter(name, demo);
		UI.getCurrent().navigate(DashboardView.class);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (AppSession.isEntered()) {
			event.forwardTo(DashboardView.class);
		}
	}
}
