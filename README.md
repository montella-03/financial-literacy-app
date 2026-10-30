# Financial Literacy App

# This was created for a local use.... but ... you can contribute and we make it a real online app

Automated Personal Finance Tracker inspired by modern financial literacy principles.

**Stack:** Java 21 · Spring Boot 4 · Vaadin 25 · PostgreSQL

## Features

- Persistent email/password accounts with salted PBKDF2 password hashes, login attempt limits, and workspace isolation.
- Separate demo workspaces: exploring sample data never resets a real account.
- Manual transactions, editable categories, accounts, and transfers excluded from income/spending totals.
- CSV import with validation, exact duplicate detection, and atomic imports (all valid rows or no changes).
- Monthly category budgets showing spending, remaining amounts, and near-limit/over-budget status.
- Savings goals with dated deposits and withdrawals, a movement ledger, and an actual-versus-planned savings graph.
- Goal/date-range selectors, remaining savings, and required monthly contributions.
- Bills and principal-only debt payments, monthly recurrence, due/overdue reminders, and dashboard reminders for the
  next seven days.
- Monthly cash-flow reviews, category breakdowns, savings movements, and debt balance comparisons from saved snapshots.
- CSV exports for reviews, transactions, goals/history, debts, net worth, bills, and budgets.
- Net worth tracking and Avalanche/Snowball debt guidance.

## Getting started

Create an account on the entry screen, then sign in again on future visits to keep using the same records. Names are
optional; email addresses are normalized. Passwords must be 12–128 characters. Five failed logins for an address
temporarily block attempts for 15 minutes (per application instance).

Use **Explore a separate demo** for sample data. Each demo is independent and does not modify any account. Demo
workspaces are not recoverable after sign-out; their database records currently remain until administrator cleanup.

## CSV transactions

Download the template from **Transactions → CSV template**, then choose a CSV file or paste its contents into **Import
CSV**. Required headers, in this order:

```csv
date,type,amount,category,merchant,description,account,destination
2026-01-15,EXPENSE,850.00,Food,Grocer,Weekly groceries,Bank,
2026-01-16,TRANSFER,2000.00,Savings,,Move to savings,Bank,Savings
```

Dates use `YYYY-MM-DD` and cannot be in the future. Amounts are positive with at most two decimal places. Types are
`INCOME`, `EXPENSE`, and `TRANSFER`; transfers require distinct source and destination accounts. Quoted commas, escaped
quotes, and multiline fields are supported. Imports accept up to 5,000 rows / 2 MB.

Exact matches use date, type, amount, category, merchant, description, and accounts. Reimported matches are skipped,
including matches against manual records. Review genuine identical payments manually. CSV text exports neutralize
spreadsheet formulas; they are records for review rather than an exact backup/restore format.

## Savings, bills, and reviews

Create a goal with an opening balance, then use **Deposit / withdraw** to record changes. Withdrawals cannot make
savings negative. The chart shows actual recorded balances and a linear planned path to the target date; it does not
invent historical progress. Existing balances start tracking from today. Savings movements track allocation to goals and
do not automatically create income or expenses.

Bills show in-app reminders; no email, push notification, or bank debit is sent. Recording payment creates an expense
dated today. A linked debt is reduced by the same amount, so link principal-only payments and record interest
separately. Monthly recurrence preserves the original day, clamping to the end of shorter months. Avoid importing or
manually entering the same payment again.

Use **Review → Save this month’s balance snapshot** to record current savings/debt balances. You can update the current
month's snapshot; prior snapshots remain unchanged. Debt comparisons require snapshots in consecutive months and reflect
net balance changes, including new borrowing and edits. No historical balances are estimated.

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL database

## Database

The app expects a PostgreSQL database. Configuration is done via environment variables (or defaults in
`application.yml`):

```bash
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB_NAME=
export POSTGRES_USER=
export POSTGRES_PASSWORD=your-database-password
export POSTGRES_SSLMODE=require   # or disable for local
```

## Running locally

```bash
mvn spring-boot:run
```

The Vaadin UI will open at http://localhost:8080

## Ubuntu executable and automatic startup, when running locally(vm..etc)

Build the standalone production JAR and install the local service:

```bash
mvn -Pproduction package
./scripts/install-local.sh
```

The installer copies the executable JAR to `~/.local/share/steady/steady.jar`, creates a private `~/.config/steady/steady.env` using the current database settings, and installs `steady.service` in your user systemd manager. Existing environment files are preserved on updates. Database credentials and the database itself are unchanged.

User lingering is enabled so the service starts during boot without requiring desktop login. The service waits **10 seconds** before launching Java; application/database startup takes a few additional seconds. If the database is not ready, systemd retries automatically. Java 21+ is required at runtime; Maven and frontend build tools are not needed at boot.

Open **Steady** in Ubuntu Applications, run `~/.local/bin/steady`, or visit **http://localhost:8808**. The app binds to this laptop only (`127.0.0.1`). The browser opens when you use the launcher; boot startup runs the server in the background.

```bash
systemctl --user status steady.service
systemctl --user restart steady.service
systemctl --user stop steady.service
journalctl --user -u steady.service -f
```

To install future code changes, repeat the build and installer commands. To disable automatic startup:

```bash
systemctl --user disable --now steady.service
```

The JAR can also be run directly with `java -jar target/financial-literacy-app-0.0.1-SNAPSHOT.jar --vaadin.launch-browser=false`. Stop the service first if using its port, and supply the same database environment settings. The installed service uses port `8808` and its private configuration file, so shell exports made after installation do not change it.

## Project structure

```
src/main/java/com/financetracker/
├── model/          # JPA entities (Transaction, Debt, SavingGoal, NetWorthItem)
├── repository/     # Spring Data repositories
├── parser/         # Bank email parsers
├── service/        # Business logic + email polling + advice engine
├── config/         # Spring configuration
└── views/          # Vaadin UI views
```

## Existing database upgrade and account isolation

Back up the database before upgrading. The current `ddl-auto: update` setting adds the account, ownership, budget,
savings movement, bill, and snapshot schema. For an existing PostgreSQL database, also run
`src/main/resources/db/migration/upgrade-transfers.sql` before using transfers; this updates the original
transaction-type constraint. This SQL script is manual and is not run automatically.

The versioned entities use a database default of `0` so Hibernate can add optimistic-lock columns to populated tables. If an earlier upgrade left a missing or nullable `version` column, run `src/main/resources/db/migration/upgrade-record-versions.sql`. It adds missing columns and backfills null versions while preserving existing versions and financial records. The script is safe to repeat and is not run automatically.

Existing financial rows have no owner and are deliberately hidden from new accounts. They are preserved; no account can
automatically claim them.

To migrate existing data, an administrator must verify its owner, create that account, and assign the appropriate legacy
rows in `transactions`, `saving_goals`, `debts`, and `net_worth_items` to that account's `user_accounts.id` via
`owner_id`. Do this selectively for each owner; never assign all legacy rows in a shared database to one user. Unowned
savings balances begin history tracking on the first new movement.

## Security and deployment

Set `POSTGRES_PASSWORD`; a database password is no longer embedded in the configuration. Use HTTPS and set
`SESSION_COOKIE_SECURE=true` in production. Session cookies are HTTP-only and SameSite Lax, with a 30-minute inactivity
timeout. Password reset and email verification are not implemented yet; accounts are email/password based, without
verified email ownership. Rate limits are in memory and should move to shared storage for multi-instance deployment.
Financial records are scoped in services; bank email ingestion must supply verified account ownership before it can be
enabled.

## Tests

```bash
mvn test
```

Tests use an isolated H2 database in PostgreSQL compatibility mode and do not access your configured financial database.
They cover account persistence/password hashing, ownership checks, demo isolation, CSV parsing/import deduplication and
rollback, transfers, month boundaries, budgets, savings movements, recurring payments, and spreadsheet-safe export.

## Future integrations

Bank email parsing exists as a foundation; automatic account-aware IMAP polling is not wired up. CSV import and manual
entry are the available ingestion paths. Production database migrations, account recovery, and verified email delivery
remain follow-up work.

![img_2.png](img_2.png)

![img_1.png](img_1.png)


![img_3.png](img_3.png)

saving metrics

![img_4.png](img_4.png)