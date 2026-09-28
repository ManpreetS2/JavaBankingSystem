# Architecture

This document explains how the Java Banking System is structured for portfolio and interview review.

## Layering

```
UI (FXML + Controllers + App Shell)
        ↓
Service layer (business rules)
        ↓
Repository layer (SQL / JDBC mapping)
        ↓
SQLite
```

Composition root:

- `AppContext` constructs repositories, services, password hashing, account-number generation, theme management, export, and session management.
- Controllers receive access through `SceneManager` and never create their own `DatabaseManager`.

## Authenticated application shell

After login, navigation uses `main-shell.fxml` / `MainShellController`:

- Persistent sidebar: Dashboard, Accounts, Transactions, Settings, Logout
- Content host swaps views inside one stage
- Transactions and Settings are integration targets with safe placeholders until those dedicated screens land
- Logout clears `SessionManager` and returns to login
- Authenticated content requires a valid session

## Theme infrastructure

- `Theme` (`LIGHT` / `DARK`)
- `ThemeManager` applies `base.css`, `components.css`, and the active theme stylesheet to managed scenes
- Controllers do not load CSS ad hoc
- Theme persistence is deferred to the settings experience

## Responsibilities

### Controllers
- Read form fields
- Perform UI-only checks (for example password confirmation)
- Call services
- Display safe error messages via `UiErrorMapper`
- Navigate screens / refresh view models

Controllers do **not** contain SQL, hash passwords, or mutate balances directly.

### Services
- `AuthService` — registration, login, initial checking/savings account creation
- `AccountService` — deposits, withdrawals, transfers, ownership checks
- `TransactionService` — recent/account history and filtered search with ownership enforcement
- `TransactionExportService` — CSV export of filtered results
- `DemoDataSeeder` — optional explicit demo dataset (never automatic at startup)

### Repositories
- Own SQL and `ResultSet` mapping
- Provide connection-aware methods for multi-step transactional work
- Map money as SQLite `TEXT` ↔ `BigDecimal`

### Database
- `DatabaseManager` opens connections, enables `PRAGMA foreign_keys = ON`, and runs `executeInTransaction(...)`
- `DatabaseInitializer` creates tables/indexes and sets `PRAGMA user_version`

## Schema versioning

SQLite `PRAGMA user_version` records the schema revision.

- Current version: `DatabaseInitializer.CURRENT_SCHEMA_VERSION`
- Clean databases initialize at the current version
- Future upgrades can add step-wise migrations when `user_version` is lower than current
- Old local development databases that predate structural constraints may still be regenerated manually

## Money

- Domain money uses `BigDecimal`, never `double`/`float`
- Canonical USD scale is 2 decimal places
- Values requiring more than 2 decimals are rejected
- SQLite stores balances/amounts as exact text strings such as `1400.00`
- UI formatting uses `CurrencyFormatter` only

## Transaction filtering

`TransactionFilter` supports:

- account scope (must be owned by authenticated user)
- transaction type
- start/end dates (`>= start`, `< end+1 day`)
- search text (description / owned account labels)
- limit/offset pagination (1–100)

Queries always join through the authenticated user's accounts.

## CSV export

`TransactionExportService` exports:

`Date, Account, Type, Description, Amount`

- UTF-8
- Proper CSV escaping for commas/quotes/newlines
- Masked account labels
- No password hashes or credentials

## Atomic transfers

A transfer runs inside one JDBC transaction:

1. Validate source and destination ownership
2. Ensure sufficient funds
3. Update both balances
4. Insert `TRANSFER_OUT` on the source (`related_account_id` = destination)
5. Insert `TRANSFER_IN` on the destination (`related_account_id` = source)
6. Commit

Any failure rolls back the entire unit of work.

## Password storage

- Algorithm: PBKDF2-HMAC-SHA256
- Production iterations: 600,000
- Unique random 16-byte salt per password
- Encoded format: `pbkdf2-sha256$iterations$base64Salt$base64Hash`
- `User.toString()` never includes `passwordHash`

## Session safety

`UserSession` exposes only userId, username, firstName, lastName, email.

## Ownership rules

Before reading or mutating an account, services verify:

`account.userId == authenticatedUserId`

## Testing

Integration tests use JUnit `@TempDir` SQLite databases and never write to `./data/banking.db`.

The core journey test covers register → deposit $2000 → transfer $500 → withdraw $100 → rebuild services → re-authenticate → verify balances `$1400.00` / `$500.00`.
