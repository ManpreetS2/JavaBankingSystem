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
- Ownership-scoped transaction history and filters
- CSV export service for filtered transactions
- Authenticated app shell with Dashboard and Accounts
- Light/dark theme infrastructure
- Neutral production-oriented UI foundation (final visual polish deferred)

## Architecture

```
UI (FXML Controllers + App Shell)
  → Services (Auth, Account, Transaction, Export)
    → Repositories (JDBC)
      → SQLite
```

Composition root: `AppContext`

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for banking guarantees, filtering, themes, and schema versioning.

## Security Model

- Passwords hashed with PBKDF2 (600,000 iterations), unique salt per password
- `UserSession` never carries password hashes
- Controllers never contain SQL or balance mutation logic
- Account operations enforce authenticated ownership

## Banking Guarantees

- Money uses `BigDecimal` (scale 2); stored as exact TEXT in SQLite
- Transfers update both balances and insert paired ledger entries in one JDBC transaction
- Failed operations roll back completely

## How to Run

Requirements:

- JDK 21+
- Maven 3.9+

```bash
mvn clean test
mvn javafx:run
```

On first launch the app creates `./data/banking.db`. Database files are gitignored.

If an older local database predates schema changes, delete `./data/banking.db` once so it regenerates.

Optional demo data (explicit only):

Use `DemoDataSeeder` via application code/tests. It is not auto-run at startup.

## Screenshots

Screenshots will be added after final visual design work.

## Roadmap

- Final Figma visual polish
- Dedicated transactions workspace integration
- Settings/profile preferences persistence
- External transfers / beneficiaries (future)
