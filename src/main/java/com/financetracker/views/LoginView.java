package com.financetracker.views;

import com.financetracker.service.DummyDataService;
import com.financetracker.session.AppSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
    private final com.financetracker.service.AccountService accounts;

    public LoginView(DummyDataService dummyDataService, com.financetracker.service.AccountService accounts) {
        this.dummyDataService = dummyDataService;
        this.accounts = accounts;

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

        TextField email = new TextField("Email");
        email.setWidthFull();
        email.setMaxLength(254);
        email.getElement().setAttribute("autocomplete", "username");
        com.vaadin.flow.component.textfield.PasswordField password = new com.vaadin.flow.component.textfield.PasswordField("Password");
        password.setWidthFull();
        password.setMaxLength(128);
        password.getElement().setAttribute("autocomplete", "current-password");
        Paragraph note = new Paragraph("Your account keeps your records between visits. New accounts need a password of at least 12 characters.");
        note.addClassName("login-note");
        Button signIn = PageSupport.primary("Sign in");
        signIn.setWidthFull();
        signIn.addClickListener(e -> authenticate(email.getValue(), name.getValue(), password.getValue(), false));
        Button register = new Button("Create account", e -> authenticate(email.getValue(), name.getValue(), password.getValue(), true));
        register.setWidthFull();
        Button demo = new Button("Explore a separate demo", e -> startDemo(name.getValue()));
        demo.setWidthFull();
        card.add(brand, eyebrow, title, lede, name, email, password, note, signIn, register, demo);

        Div intro = new Div();
        intro.addClassName("login-intro");
        Span introLabel = new Span("A LITTLE MORE CLARITY. EVERY DAY.");
        introLabel.addClassName("eyebrow");
        H1 introTitle = new H1("Good things grow from small steps.");
        introTitle.addClassName("login-intro-title");
        Paragraph introCopy = new Paragraph("Make room for what matters. Bring your money, goals, and next steps together in one calm space.");
        Div art = new Div(new Span("01 / Understand your cash flow"), new Span("02 / Build toward your goals"), new Span("03 / Move forward with confidence"));
        art.addClassName("login-steps");
        intro.add(introLabel, introTitle, introCopy, art);
        Div layout = new Div(intro, card);
        layout.addClassName("login-layout");
        add(layout);
    }

    private void authenticate(String email, String name, String password, boolean register) {
        try {
            var user = register ? accounts.register(email, name, password) : accounts.login(email, password);
            com.vaadin.flow.server.VaadinService.reinitializeSession(com.vaadin.flow.server.VaadinService.getCurrentRequest());
            AppSession.enter(user.getId(), user.getDisplayName(), false);
            UI.getCurrent().navigate(DashboardView.class);
        } catch (IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException ex) {
            com.vaadin.flow.component.notification.Notification.show(ex instanceof IllegalArgumentException ? ex.getMessage() : "Unable to create account. Try signing in instead.");
        }
    }

    private void startDemo(String name) {
        AppSession.enter("demo-" + java.util.UUID.randomUUID(), name, true);
        dummyDataService.createDemo();
        UI.getCurrent().navigate(DashboardView.class);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (AppSession.isEntered()) {
            event.forwardTo(DashboardView.class);
        }
    }
}
