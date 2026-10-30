# Contributing

Contributions to the Financial Literacy App (Steady) are welcome, including bug fixes, features, tests, documentation, and accessibility improvements. Read the [README](README.md) for existing features and behavior before making changes.

## Development setup

1. Fork the repository if needed, clone your copy, and create a branch for your change.
2. Install Java 21+, Maven 3.9+, and PostgreSQL.
3. Create a separate development database and configure the environment variables below.
4. Run the application from the repository root with `mvn spring-boot:run`.

Example configuration for Bash or Git Bash:

```bash
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB_NAME=steady_dev
export POSTGRES_USER=steady_dev
export POSTGRES_PASSWORD='your-local-password'
export POSTGRES_SSLMODE=disable
```

For Windows PowerShell:

```powershell
$env:POSTGRES_HOST = 'localhost'
$env:POSTGRES_PORT = '5432'
$env:POSTGRES_DB_NAME = 'steady_dev'
$env:POSTGRES_USER = 'steady_dev'
$env:POSTGRES_PASSWORD = 'your-local-password'
$env:POSTGRES_SSLMODE = 'disable'
```

These examples assume a local PostgreSQL database without SSL. Use the SSL setting required by your database. The database and user must already exist, with permissions to create and update application tables. The application uses Hibernate schema updates, so develop against disposable data rather than personal financial records.

Open http://localhost:8080 and create a test account or explore a separate demo. The Maven build runs Vaadin frontend preparation; the first build may need network access to download dependencies and frontend tooling. Desktop installation is optional; see the README for Linux and Windows installers.

## Where code belongs

Java code lives under `src/main/java/com/financetracker/`:

| Directory | Purpose |
| --- | --- |
| `views/` | Vaadin screens and UI components |
| `service/` | Business rules, calculations, and application operations |
| `model/` | Persistent entities and domain data |
| `repository/` | Database access |
| `session/` | Workspace session state and navigation guards |
| `parser/` | Bank email parsing |
| `config/` | Application configuration |

Tests live under `src/test/java/com/financetracker/`. Installer entry points live in `scripts/`, with launchers and service definitions in `deploy/`.

## Making changes

- Keep each change focused and follow the surrounding Java and Vaadin conventions.
- Keep business rules in services so they can be tested independently of views.
- Use `BigDecimal` for monetary calculations and make rounding behavior explicit.
- Preserve account and demo workspace isolation across reads, updates, and deletes.
- Validate imported data and preserve the documented transaction, savings, and debt behavior. Update documentation when that behavior changes.
- Use synthetic data in tests, screenshots, CSV examples, and email fixtures. Do not commit credentials, real bank emails, or personal financial records.
- Keep generated frontend files, build output, and local configuration out of commits.

## Checking your work

Run the automated tests from the repository root:

```bash
mvn test
```

The existing Spring Boot integration tests configure an in-memory H2 database in PostgreSQL compatibility mode. They do not require your development PostgreSQL database. Changes involving PostgreSQL-specific behavior should also be checked against a disposable PostgreSQL database.

For a production build, including the Vaadin frontend:

```bash
mvn -Pproduction package
```

Add meaningful regression tests for changed business behavior, especially money calculations, date boundaries, input validation, and account isolation. For UI changes, manually check the affected flow, keyboard navigation, and narrow layouts. For installer changes, test on the target operating system and state any platform you could not verify.

Documentation-only changes do not require an application build; check commands, links, and formatting instead.

## Reporting issues and opening pull requests

For bugs, include reproduction steps, expected and actual behavior, operating system, Java version, and relevant logs with sensitive information removed. For larger features, describe the proposed behavior in an issue before investing in a broad implementation.

In your pull request:

- Explain the problem and what changes for the user.
- Link the related issue, if there is one.
- List the checks you ran and their results; identify anything untested.
- Include screenshots for visible UI changes, using synthetic data.
- Document configuration, database, or installation changes and any manual steps needed.

Keep review discussions constructive and address feedback with focused updates.
