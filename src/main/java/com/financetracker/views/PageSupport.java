package com.financetracker.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

final class PageSupport {

    private PageSupport() {
    }

    static VerticalLayout page() {
        VerticalLayout page = new VerticalLayout();
        page.setPadding(false);
        page.setSpacing(false);
        page.setWidthFull();
        page.addClassName("app-content");
        return page;
    }

    static Div hero(String eyebrowText, String titleHtml, String lede) {
        Div hero = new Div();
        hero.addClassName("page-hero");
        hero.add(eyebrow(eyebrowText));
        hero.add(title(titleHtml));
        Paragraph copy = new Paragraph(lede);
        copy.addClassName("lede");
        hero.add(copy);
        return hero;
    }

    static Span eyebrow(String text) {
        Span span = new Span(text);
        span.addClassName("eyebrow");
        return span;
    }

    static H1 title(String html) {
        H1 title = new H1();
        title.addClassName("page-title");
        title.getElement().setProperty("innerHTML", html);
        return title;
    }

    static Div section(String titleText, String copy, Component... body) {
        Div section = new Div();
        section.addClassName("page-section");
        H2 heading = new H2(titleText);
        heading.addClassName("section-title");
        section.add(heading);
        if (copy != null && !copy.isBlank()) {
            Paragraph paragraph = new Paragraph(copy);
            paragraph.addClassName("section-copy");
            section.add(paragraph);
        }
        for (Component component : body) {
            section.add(component);
        }
        return section;
    }

    static Div money(String amount, String modifierClass) {
        Div money = new Div();
        money.setText(amount);
        money.addClassName("money-display");
        if (modifierClass != null && !modifierClass.isBlank()) {
            money.addClassName(modifierClass);
        }
        return money;
    }

    static Button primary(String label) {
        Button button = new Button(label);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return button;
    }

    static Div emptyState(String titleText, String body, Button action) {
        Div empty = new Div();
        empty.addClassName("empty-state");
        empty.add(eyebrow("Getting started"));
        H2 heading = new H2(titleText);
        heading.addClassName("section-title");
        empty.add(heading);
        Paragraph paragraph = new Paragraph(body);
        paragraph.addClassName("lede");
        empty.add(paragraph);
        if (action != null) {
            empty.add(action);
        }
        return empty;
    }
}
