# Java Banking System

Portfolio-quality Java desktop banking application built with clean architecture.

## Project Goal

A modern desktop banking product with authentication, checking/savings accounts, deposits, withdrawals, transfers, transaction history, SQLite persistence, and a JavaFX UI.

## Tech Stack

- Java 21
- JavaFX 21
- Maven
- SQLite (Xerial JDBC)
- JUnit 5
- FXML + CSS

## Current Features

- Secure registration and login (PBKDF2-HMAC-SHA256)
- Checking and savings accounts created at registration
- Deposits, withdrawals, and internal transfers
- Atomic SQLite banking transactions
- Dedicated Transactions workspace with search, account/type/date filters, and pagination
- Ownership-scoped transaction history and filters
- CSV export from the Transactions screen (all filtered results, batched fetch, up to 10,000 transactions per export)
- Authenticated app shell with Dashboard, Accounts, and Transactions
- Shared light/dark design system for shell, workspaces, and banking dialogs
- Inline dialog validation with service-layer authoritative money rules
- Stable application-data database location for desktop launches

## Architecture

```
UI (FXML Controllers + App Shell)
  → Services (Auth, Account, Transaction, Export)
    → Repositories (JDBC)
      → SQLite
```

Composition root: `AppContext`

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for banking guarantees, filtering, themes, schema versioning, startup paths, and packaging notes.

## Security Model

- Passwords hashed with PBKDF2 (600,000 iterations), unique salt per password
- `UserSession` never carries password hashes
- Controllers never contain SQL or balance mutation logic
- Account operations enforce authenticated ownership

## Banking Guarantees

- Money uses `BigDecimal` (scale 2); stored as exact TEXT in SQLite
- Transfers update both balances and insert paired ledger entries in one JDBC transaction
- Failed operations roll back completely

## Quick Start

Requirements:

- JDK 21+
- Maven 3.9+

```bash
mvn clean test
mvn javafx:run
```

## Development

```bash
mvn clean test
mvn javafx:run
```

Optional database path override (development / debugging):

```bash
mvn javafx:run -Dbank.db.path=/tmp/banking-dev.db
```

## Tests

```bash
mvn clean test
```

Integration tests use JUnit `@TempDir` databases and never write to the normal application-data location.

## Demo Data

Demo seeding is **off by default** and never auto-logs anyone in.

Enable explicitly:

```bash
mvn javafx:run -Dbank.demo.seed=true
```

Documented demo credentials (public test-only):

- Username: `demouser`
- Password: `DemoPassword12`

Seeding uses normal `AuthService` / `AccountService` rules and is idempotent.

## Data Location

Normal desktop launches store SQLite under an OS application-data directory:

| Platform | Location |
|----------|----------|
| macOS | `~/Library/Application Support/BankingSystem/banking.db` |
| Windows | `%APPDATA%/BankingSystem/banking.db` |
| Linux | `$XDG_DATA_HOME/BankingSystem/banking.db` or `~/.local/share/BankingSystem/banking.db` |

Override with `-Dbank.db.path=/path/to/file.db`.

Legacy `./data/banking.db` from earlier development builds is **not** moved or deleted automatically. Point `-Dbank.db.path=./data/banking.db` at it if you still need that file.

## Packaging

This project is intentionally non-modular (no `module-info.java`). Prefer classpath packaging:

```bash
chmod +x scripts/package-app.sh
./scripts/package-app.sh
```

That builds an unsigned local app-image under `target/dist/` (macOS: `BankingSystem.app`).

Verified locally: `./scripts/package-app.sh` produced `target/dist/BankingSystem.app`, and launching
`BankingSystem.app/Contents/MacOS/BankingSystem` initialized the application-data database.

Signing and notarization are out of scope for this stage.

`mvn javafx:jlink` fails for this project (`jlink requires a module descriptor`); the project stays non-modular on purpose.

Place final icons in `src/main/resources/icons/` (`app.png` / `app.icns` / `app.ico`) when branding assets are ready.

## Screenshots

Screenshots will be added after final visual design work.

## Roadmap

- Final Figma visual polish
- Settings/profile preferences persistence
- External transfers / beneficiaries (future)
