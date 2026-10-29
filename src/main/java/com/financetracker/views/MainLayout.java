package com.financetracker.views;

import com.financetracker.session.AppSession;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout implements AfterNavigationObserver, BeforeEnterObserver {

	private final Nav headerNav = new Nav();
	private final Nav drawerNav = new Nav();

	public MainLayout() {
		addClassName("app-shell");
		setPrimarySection(Section.NAVBAR);

		DrawerToggle toggle = new DrawerToggle();
		toggle.addClassName("app-drawer-toggle");
		toggle.getStyle().set("color", "#fff");

		Anchor brand = new Anchor("", "Steady");
		brand.addClassName("brand-wordmark");
		brand.getStyle().set("margin-right", "auto");

		headerNav.addClassName("app-nav");
		headerNav.getStyle().set("display", "none");
		populate(headerNav);

		drawerNav.addClassName("app-nav");
		populate(drawerNav);

		Button exit = new Button("Exit", e -> {
			AppSession.clear();
			UI.getCurrent().navigate(LoginView.class);
		});
		exit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
		exit.addClassName("app-exit");

		Span mode = new Span();
		mode.addClassName("app-mode-badge");
		if (AppSession.isEntered()) {
			mode.setText(AppSession.isDemoMode() ? "Demo" : "Live");
		}

		Div navbar = new Div(toggle, brand, headerNav, mode, exit);
		navbar.addClassName("app-header");
		navbar.getStyle()
				.set("display", "flex")
				.set("align-items", "center")
				.set("width", "100%")
				.set("gap", "var(--space-4)");

		addToNavbar(true, navbar);

		Div drawerWrap = new Div(drawerNav);
		drawerWrap.getStyle().set("padding", "var(--space-5) var(--space-4)");
		addToDrawer(drawerWrap);

		getElement().executeJs("""
				const mq = window.matchMedia('(min-width: 900px)');
				const headerNav = $0;
				const toggle = $1;
				const sync = () => {
				  headerNav.style.display = mq.matches ? 'flex' : 'none';
				  toggle.style.display = mq.matches ? 'none' : '';
				};
				sync();
				mq.addEventListener('change', sync);
				""", headerNav.getElement(), toggle.getElement());
	}

	private void populate(Nav nav) {
		nav.removeAll();
		nav.add(
				link("Overview", DashboardView.class),
				link("Transactions", TransactionsView.class),
				link("Goals", GoalsView.class),
				link("Debts", DebtsView.class),
				link("Net worth", NetWorthView.class),
				link("Advice", AdviceView.class)
		);
	}

	private RouterLink link(String label, Class<? extends Component> view) {
		return new RouterLink(label, view);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (!AppSession.isEntered()) {
			event.rerouteTo(LoginView.class);
		}
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		String path = event.getLocation().getPath();
		highlight(headerNav, path);
		highlight(drawerNav, path);
	}

	private void highlight(Nav nav, String path) {
		nav.getChildren().forEach(component -> {
			if (!(component instanceof RouterLink link)) {
				return;
			}
			String href = link.getHref();
			boolean root = href == null || href.isBlank() || ".".equals(href);
			boolean active = root
					? path == null || path.isBlank()
					: path.equals(href) || path.startsWith(href + "/");
			if (active) {
				link.getElement().setAttribute("highlight", true);
			} else {
				link.getElement().removeAttribute("highlight");
			}
		});
	}
}
