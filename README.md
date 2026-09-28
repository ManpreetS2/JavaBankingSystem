# Java Banking System

Portfolio-quality Java desktop banking application built with clean architecture.

## Project Goal

Build a modern banking system with authentication, checking/savings accounts, deposits, withdrawals, transfers, transaction history, SQLite persistence, and a polished JavaFX UI.

## Tech Stack

- Java 21
- JavaFX 21
- Maven
- SQLite (Xerial JDBC)
- JUnit 5
- FXML + CSS

## Current Development Status

**Core banking engine implemented**

Working today:

- Secure registration and login (PBKDF2 password hashing)
- Automatic checking + savings account creation
- Deposits, withdrawals, and internal transfers
- Atomic SQLite transactions for banking operations
- Transaction history with account ownership checks
- Persistence across application/service restarts
- Neutral JavaFX login / register / dashboard shell (final Figma styling later)

Still deferred:

- Final visual design / polish from Figma
- External transfers / beneficiaries
- Budgets, charts, bill pay, admin tools

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for layering and banking guarantees.

## Architecture

```
UI (FXML Controllers)
  → Services (Auth, Account, Transaction)
    → Repositories (JDBC)
      → SQLite
```

Money uses `BigDecimal` and is stored as exact TEXT in SQLite.

## How to Run

Requirements:

- JDK 21 or newer
- Maven 3.9+

Run tests:

```bash
mvn clean test
```

Launch the application:

```bash
mvn javafx:run
```

On first launch the app creates `./data/banking.db` and initializes schema. If an older local database was created before schema changes, delete `./data/banking.db` once so it can be regenerated. Database files are gitignored and never committed.

If database initialization fails, the application exits with a clear error instead of continuing.
