---
name: siricondos-design
description: >-
  Applies the SiriCondos-inspired visual design system to Vaadin views in the
  financial literacy app — generous spacing, clean layouts, paper/forest palette.
  Use when building or restyling Vaadin UI, themes, layouts, CSS, dashboards,
  forms, grids, navigation, spacing, or when the user mentions design, colours,
  branding, Siricondos, or look-and-feel.
---

# Siricondos Design System (Vaadin)

Source of truth for colours/feel: https://dev.siricondos.com/

Adapt that editorial real-estate aesthetic to a calm personal-finance product — warm paper surfaces, forest ink,
restrained gold accent. Do **not** copy SiriCondos copy or real-estate UI patterns; reuse palette, type, and spacing
language only.

## Core tokens

Map these into Vaadin/Lumo theme CSS (prefer `frontend/themes/<theme>/styles.css` + `:root` / `html`):

| Token            | Hex       | Use                                                        |
|------------------|-----------|------------------------------------------------------------|
| `--paper`        | `#f7f5ef` | App / page background                                      |
| `--ink`          | `#252b27` | Primary text, headings                                     |
| `--muted`        | `#717369` | Secondary labels, hints                                    |
| `--accent`       | `#b29c78` | Soft gold accent (links underline, focus ring, highlights) |
| `--forest`       | `#293a2e` | Primary buttons, nav active, key CTAs                      |
| `--forest-hover` | `#41523e` | Primary button hover                                       |
| `--forest-deep`  | `#202a23` | Footer / dark chrome / sidebar dark                        |
| `--surface`      | `#ffffff` | Interactive panels that need contrast                      |
| `--sage`         | `#eceee4` | Subtle section fills, zebra rows                           |
| `--sand`         | `#d5c5ab` | Borders / dividers (low contrast)                          |

Body wash used on Siricondos: `#faf8f4` — optional outer shell; prefer `--paper` for main content.

Finance semantics (keep brand-aligned, not neon):

- Positive / income: deepen forest (`#293f34` or `#667b62`)
- Negative / expense / overdue: warm brick `#8c4a3a` (not default red)
- Warning / goal at risk: accent gold `#b29c78` or sand `#d5c5ab`

## Typography

| Role                | Family                                 | Notes                                                       |
|---------------------|----------------------------------------|-------------------------------------------------------------|
| Display / big money | `"Cormorant Garamond", Georgia, serif` | Letter-spacing ~`-0.03em`; use for hero totals, page titles |
| Body / UI           | `"DM Sans", Arial, sans-serif`         | Default for grids, forms, nav                               |
| Mono / figures      | `"DM Mono", ui-monospace, monospace`   | Optional for account refs, OTP-like codes                   |

Eyebrow labels: DM Sans, 10–11px, `letter-spacing: 0.15em–0.2em`, uppercase, weight 500, colour `--muted`.

Load fonts via theme `@font-face` or Google Fonts in the Vaadin theme — do not rely on system defaults for display.

## Vaadin / Lumo mapping

When theming, set Lumo props to these tokens, e.g.:

```css
html {
  --lumo-base-color: var(--paper);
  --lumo-body-text-color: var(--ink);
  --lumo-secondary-text-color: var(--muted);
  --lumo-primary-color: var(--forest);
  --lumo-primary-contrast-color: #fff;
  --lumo-primary-text-color: var(--forest);
  --lumo-success-color: #293f34;
  --lumo-error-color: #8c4a3a;
  --lumo-border-radius: 3px; /* Siricondos uses small radii, not pills */
}
```

Prefer modest radius (`2–4px` primary, ~`12px` only for large dialogs). Avoid rounded-full pills, purple gradients,
glow, and dense card chrome.

## Spacing (non-negotiable)

Favour **great spacing** — calm, editorial breathing room over dense finance dashboards.

| Scale       | Value  | Use                                            |
|-------------|--------|------------------------------------------------|
| `--space-1` | `4px`  | Tight icon gaps only                           |
| `--space-2` | `8px`  | Inline label/control gaps                      |
| `--space-3` | `16px` | Related controls in a row/form field stack     |
| `--space-4` | `24px` | Between form groups / grid toolbar and content |
| `--space-5` | `40px` | Between subsections inside a view              |
| `--space-6` | `64px` | Between major page sections                    |
| `--space-7` | `96px` | Top/bottom page padding on desktop             |

Rules:

- Page content: generous horizontal padding (`24–48px` mobile, `48–64px` desktop); max content width ~`1200–1280px`,
  centred.
- Never pack metrics edge-to-edge — leave empty space; one primary figure can sit alone.
- Grid rows and form fields: comfortable height; avoid cramming filters against the table.
- Nav items and CTAs: ample hit targets; spacing between nav links ≥ `24–32px`.
- If a view feels busy, **add space or remove chrome** — do not add more boxes.

## Layout & composition

- App shell: light paper background; optional deep-forest sidebar or top nav with white wordmark.
- Dashboard: one clear composition — brand/product name + one primary money figure + short supporting line; avoid
  packing stats strips into the first viewport.
- Sections: one job each (transactions, goals, debts, advice). One headline + one short sentence.
- Cards: only when wrapping an interaction (edit goal, pay debt). Prefer open layouts with sage separators over boxed
  grids of cards.
- Motion: subtle — hover background on forest buttons, soft image/transform transitions if any; 2–3 intentional motions
  max per major view.

## Component cues

- Primary button: `#27342c` / `#293a2e` fill, white 11–13px medium label, radius ~2px; hover `#3d4c3f` / `#41523e`.
- Text links: ink colour, underline grows to accent/ink on hover.
- Grids: hairline sand/sage borders; display font only for money cells if it stays readable.
- Empty states: editorial tone, muted body, one forest CTA.

## Checklist before shipping a view

- [ ] Uses paper / ink / forest / accent — not default Lumo blue/purple
- [ ] Display font on titles or hero amounts; DM Sans elsewhere
- [ ] Generous spacing (sections breathe; no cramped toolbars or stacked cards)
- [ ] No purple-on-white, cream+terracotta cliché, or newspaper broadsheet layout
- [ ] Mobile: nav collapses; primary CTA still reachable
- [ ] Currency formatting consistent (prefer locale-aware; KES-friendly when relevant)

## Reference

Full token dump and CSS excerpts: [palette.md](palette.md)
