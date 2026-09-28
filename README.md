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
- FXML + CSS (prepared for upcoming UI work)

## Current Development Status

**Day 1 — Foundation**

- Maven project configured for Java 21
- Domain models: `User`, `Account`, `Transaction`, `AccountType`, `TransactionType`
- SQLite connection management with foreign keys enabled
- Schema initialization with CHECK constraints and useful indexes
- Minimal JavaFX launch window
- Package layout for controllers, services, repositories, and UI resources
- Unit tests for models and database behavior
- GitHub Actions CI for compilation and tests

Not yet implemented: authentication, account operations, transfers, transaction history UI, or a full dashboard.

## Architecture

```
com.manpreet.bank
├── App                 # JavaFX entry point + startup wiring
├── model               # Domain entities and enums
├── controller          # UI controllers (Day 2+)
├── service             # Business logic (Day 2+)
├── repository          # Data access (Day 2+)
├── database            # JDBC connection + schema setup
└── util                # Shared helpers (Day 2+)
```

Money is represented with `BigDecimal` in the domain and stored as exact text strings in SQLite so decimal precision is preserved. Passwords are stored only as hashes.

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

On first launch the app creates `./data/banking.db` and initializes the `users`, `accounts`, and `transactions` tables. If database initialization fails, the application exits with a clear error instead of continuing.

SQLite data is generated locally under `./data/` and is gitignored — database files are never committed.
