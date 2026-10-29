package com.financetracker.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "advice", layout = MainLayout.class)
@PageTitle("Advice · Steady")
public class AdviceView extends VerticalLayout {

    public AdviceView() {
        setPadding(false);
        setSpacing(false);
        setSizeFull();
        addClassName("app-content");

        add(PageSupport.hero(
                "Guidance",
                "Calm counsel,<br/><em>not noise.</em>",
                "50/30/20, debt payoff order, and surplus tips — grounded in your numbers once data is live."
        ));

        Paragraph rule = new Paragraph();
        rule.addClassName("advice-line");
        rule.getElement().setProperty("innerHTML",
                "<strong>50 / 30 / 20</strong> — aim for needs, wants, and saving or debt payoff. "
                        + "We will map your categories onto this once transactions are in.");

        Paragraph debt = new Paragraph();
        debt.addClassName("advice-line");
        debt.getElement().setProperty("innerHTML",
                "<strong>Avalanche</strong> kills interest fastest. <strong>Snowball</strong> builds momentum. "
                        + "Both work — consistency matters more than the label.");

        Paragraph surplus = new Paragraph();
        surplus.addClassName("advice-line");
        surplus.getElement().setProperty("innerHTML",
                "<strong>Surplus habit</strong> — decide the split before the month spends it: "
                        + "emergency fund, goals, then high-interest debt.");

        Div body = new Div(rule, debt, surplus);
        body.addClassName("stack-gap-lg");

        add(PageSupport.section(
                "Principles we will apply",
                "Live numbers will replace these placeholders. The principles stay the same.",
                body
        ));
    }
}
