---
name: finance-tracker
description: >-
  Guides clean architecture and features for the Financial Literacy App (Java 21,
  Spring Boot 4, Vaadin 25, PostgreSQL). Use when adding models, repositories,
  services, email parsing, advice engines, Vaadin views, or when the user
  discusses budgets, transactions, net worth, debts, saving goals, 50/30/20,
  avalanche, snowball, or code quality.
---

# Financial Literacy App

Personal finance tracker that helps people understand cash flow, build savings, and pay down debt — with optional bank-email ingestion.

## Stack (locked)

- Java 21, Spring Boot 4, Vaadin 25, PostgreSQL, Lombok
- Package root: `com.financetracker`
- UI theming: follow skill **siricondos-design** for all Vaadin views

## Package layout

```
com.financetracker/
├── model/          # JPA entities
├── repository/     # Spring Data JPA
├── parser/         # Bank email parsers
├── service/        # Domain logic, polling, advice
├── config/         # Spring / Vaadin / mail config
└── views/          # Vaadin routes & layouts
```

Keep business rules in `service/`, not in views. Views call services; services use repositories.

## Domain model (existing)

| Entity | Purpose |
|--------|---------|
| `Transaction` | `INCOME` / `EXPENSE`, amount, category, merchant, date, optional `sourceEmailId` |
| `Debt` | Name, balance, interest rate, minimum payment |
| `SavingGoal` | Target/current amounts, date, priority, completed |
| `NetWorthItem` | `ASSET` / `LIABILITY` with value |

Money fields: `BigDecimal` with `precision = 19, scale = 2`. Never use `double` for money.

## Product principles

1. **Clarity over clutter** — show what matters (surplus, runway, goal progress) before dense tables; give the UI room to breathe.
2. **Actionable advice** — every insight should suggest a next step (cut category X, pay debt Y first).
3. **Trust** — show sources (manual vs email-parsed); never silently overwrite user categorization without an obvious undo/edit path.
4. **Privacy** — credentials via env vars; do not hardcode secrets; minimize raw email retention (`rawEmailSnippet` is already capped).
5. **Clean code** — small focused classes, clear names, no dead code or speculative abstraction.

## Clean code

- One responsibility per class/method; extract a service when a view grows fat.
- Prefer readable names over comments; comment only non-obvious why.
- No unused imports, commented-out blocks, or leftover TODOs without a ticket.
- Match existing style (Lombok where already used, constructor injection, records/DTOs for advice results).
- Keep methods short; avoid nested conditionals — early return.
- Tests for parsers and advice math; keep UI logic thin.
- Do not “clean up” unrelated files while shipping a feature.

## Planned capabilities

- IMAP poll of bank notification emails → parse → categorize → persist `Transaction`
- Income vs expense and monthly/yearly surplus
- Net worth = assets − liabilities
- Saving goals with progress
- Debt payoff advice: **Avalanche** (highest interest first) and **Snowball** (lowest balance first)
- **50/30/20** insights (needs / wants / savings) with gentle, concrete tips

## Implementation conventions

### Backend

- Prefer constructor injection; `@Service` / `@Repository` stereotypes
- Validation via `spring-boot-starter-validation` on inputs/DTOs when exposed
- Idempotent email ingest: unique `sourceEmailId` on `Transaction`
- Advice methods: pure-ish functions in a dedicated service (e.g. `AdviceService`) taking domain objects, returning DTOs/records for the UI
- Config under `app.email.*` in `application.yml` for poll interval, folder, allowed senders

### Vaadin UI

- `@Route` views under `views/`; shared `MainLayout` with nav (Dashboard, Transactions, Goals, Debts, Net Worth, Advice)
- Use service beans injected into views; refresh grids after mutations
- Format money with `NumberFormat` / locale; keep display font for hero totals only
- Apply **siricondos-design** tokens and spacing scale; no default Lumo blue chrome
- Prefer composition helpers over giant `add(...)` blocks when a view section repeats

### Parsers

- Extend `BankEmailParser` with bank-specific patterns; keep regexes/tests co-located
- Fail soft: unparsed mail should log and skip, not crash the poller

## Suggested build order

1. Theme + `MainLayout` + empty Dashboard (design system live)
2. CRUD for Transactions / Goals / Debts / Net Worth (manual entry first)
3. Dashboard aggregates + 50/30/20 snapshot
4. AdviceService (avalanche / snowball / surplus tips)
5. IMAP polling + parsers + categorization

## Do not

- Put SQL or IMAP credentials in committed files or skills
- Mix UI styling decisions without **siricondos-design**
- Invent parallel entity models when existing ones fit — extend carefully
- Cram UI or over-engineer layers “just in case”
