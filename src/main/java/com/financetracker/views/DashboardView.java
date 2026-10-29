package com.financetracker.views;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

import com.financetracker.model.Debt;
import com.financetracker.model.SavingGoal;
import com.financetracker.service.DebtService;
import com.financetracker.service.SavingGoalService;
import com.financetracker.service.TransactionService;
import com.financetracker.session.AppSession;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Overview · Steady")
public class DashboardView extends VerticalLayout {

	private final TransactionService transactionService;
	private final SavingGoalService goalService;
	private final DebtService debtService;

	public DashboardView(
			TransactionService transactionService,
			SavingGoalService goalService,
			DebtService debtService) {
		this.transactionService = transactionService;
		this.goalService = goalService;
		this.debtService = debtService;

		setPadding(false);
		setSpacing(false);
		setSizeFull();
		addClassName("app-content");

		YearMonth month = YearMonth.now();
		BigDecimal income = transactionService.sumIncome(month);
		BigDecimal expenses = transactionService.sumExpenses(month);
		BigDecimal surplus = transactionService.surplus(month);

		add(buildHero(surplus));
		add(buildPulse(income, expenses));
		add(buildAdvice(surplus));
		add(buildGoalsPreview());
		add(buildDebtFocus());
	}

	private Div buildHero(BigDecimal surplus) {
		Div hero = new Div();
		hero.addClassName("page-hero");
		hero.add(PageSupport.eyebrow("This month · Hello, " + AppSession.displayName()));
		hero.add(PageSupport.title("Your money,<br/><em>at a glance.</em>"));

		Paragraph lede = new Paragraph(
				"Surplus is what remains after income and spending. Protect it first — then decide whether to save or pay debt.");
		lede.addClassName("lede");
		hero.add(lede);

		String modifier = surplus.signum() >= 0 ? "positive" : "negative";
		hero.add(PageSupport.money(MoneyFormat.kes(surplus), modifier));

		Span caption = new Span(AppSession.isDemoMode()
				? "Monthly surplus · demo workspace"
				: "Monthly surplus · your workspace");
		caption.addClassName("quiet-meta");
		hero.add(caption);
		return hero;
	}

	private Div buildPulse(BigDecimal income, BigDecimal expenses) {
		Div row = new Div(metric("Income", MoneyFormat.kes(income)), metric("Spending", MoneyFormat.kes(expenses)));
		row.addClassName("split-row");
		return PageSupport.section(
				"Cash flow",
				"See the shape of the month before diving into every line item.",
				row
		);
	}

	private Div buildAdvice(BigDecimal surplus) {
		Paragraph line = new Paragraph();
		line.addClassName("advice-line");
		if (surplus.signum() > 0) {
			BigDecimal toEmergency = surplus.multiply(new BigDecimal("0.5")).setScale(0, RoundingMode.DOWN);
			line.getElement().setProperty("innerHTML",
					"With this surplus, put about <strong>" + MoneyFormat.kes(toEmergency)
							+ "</strong> toward your emergency fund and use the rest on your highest-interest debt.");
		} else if (surplus.signum() < 0) {
			line.setText(
					"This month is running a deficit. Pause non-essential wants, then review the biggest expense categories.");
		} else {
			line.setText(
					"You are breaking even. Add a small automatic transfer to savings so surplus becomes a habit.");
		}

		RouterLink link = new RouterLink("Read full advice ", AdviceView.class);
		link.addClassName("editorial-link");
		link.add(new Span("↗"));

		Div body = new Div(line, link);
		body.addClassName("stack-gap-md");

		return PageSupport.section("One clear next step", "Advice should be specific — not a lecture.", body);
	}

	private Div buildGoalsPreview() {
		List<SavingGoal> goals = goalService.findActive();
		if (goals.isEmpty()) {
			RouterLink link = new RouterLink("Create a goal ", GoalsView.class);
			link.addClassName("editorial-link");
			link.add(new Span("→"));
			return PageSupport.section(
					"Saving goals",
					"Name the destination. Progress becomes visible.",
					link
			);
		}

		UnorderedList list = new UnorderedList();
		list.addClassName("quiet-list");
		goals.stream().limit(3).forEach(goal -> list.add(goalRow(goal)));

		RouterLink link = new RouterLink("Manage goals ", GoalsView.class);
		link.addClassName("editorial-link");
		link.add(new Span("→"));

		Div body = new Div(list, link);
		body.addClassName("stack-gap-md");
		return PageSupport.section("Saving goals", "Name the destination. Progress becomes visible.", body);
	}

	private Div buildDebtFocus() {
		return debtService.avalancheFocus()
				.map(this::debtSection)
				.orElseGet(() -> {
					RouterLink link = new RouterLink("Add a debt ", DebtsView.class);
					link.addClassName("editorial-link");
					link.add(new Span("→"));
					return PageSupport.section(
							"Debt focus",
							"Pay minimums everywhere. Then attack one balance with intent.",
							link
					);
				});
	}

	private Div debtSection(Debt debt) {
		Div block = new Div();
		block.addClassName("metric-block");

		Span label = new Span("Focus next · Avalanche");
		label.addClassName("metric-label");

		Span name = new Span(debt.getName()
				+ (debt.getInterestRate() != null ? " · " + debt.getInterestRate() + "% APR" : ""));
		name.getStyle().set("font-weight", "500");

		Div amount = new Div(MoneyFormat.kes(debt.getBalance()));
		amount.addClassName("money-inline");

		Paragraph tip = new Paragraph(
				"Highest interest first. Every extra shilling here saves the most over time.");
		tip.addClassName("section-copy");
		tip.getStyle().set("margin-top", "var(--space-3)");

		RouterLink link = new RouterLink("See debt plan ", DebtsView.class);
		link.addClassName("editorial-link");
		link.add(new Span("→"));

		block.add(label, name, amount, tip, link);
		return PageSupport.section(
				"Debt focus",
				"Pay minimums everywhere. Then attack one balance with intent.",
				block
		);
	}

	private static Div metric(String label, String value) {
		Div block = new Div();
		block.addClassName("metric-block");
		Span caption = new Span(label);
		caption.addClassName("metric-label");
		Div amount = new Div(value);
		amount.addClassName("money-inline");
		block.add(caption, amount);
		return block;
	}

	private static ListItem goalRow(SavingGoal goal) {
		BigDecimal target = goal.getTargetAmount() == null ? BigDecimal.ZERO : goal.getTargetAmount();
		BigDecimal current = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();
		int percent = target.signum() == 0
				? 0
				: current.multiply(BigDecimal.valueOf(100)).divide(target, 0, RoundingMode.HALF_UP).intValue();
		percent = Math.min(100, Math.max(0, percent));

		Span title = new Span(goal.getName());
		title.getStyle().set("font-weight", "500");
		Span meta = new Span(MoneyFormat.kes(current) + " of " + MoneyFormat.kes(target));
		meta.addClassName("quiet-meta");

		Div left = new Div(title, meta);
		left.addClassName("stack-gap-md");
		left.getStyle().set("gap", "var(--space-2)");

		Div track = new Div();
		track.addClassName("progress-track");
		Div fill = new Div();
		fill.addClassName("progress-fill");
		fill.getStyle().set("width", percent + "%");
		track.add(fill);

		Div wrap = new Div(left, track);
		wrap.setWidthFull();
		wrap.addClassName("stack-gap-md");
		wrap.getStyle().set("gap", "var(--space-2)");

		ListItem item = new ListItem(wrap);
		item.getStyle().set("display", "block");
		return item;
	}
}
