# Architecture

This document explains how the Java Banking System is structured for portfolio and interview review.

## Layering

```
UI (FXML + Controllers)
        ↓
Service layer (business rules)
        ↓
Repository layer (SQL / JDBC mapping)
        ↓
SQLite
```

Composition root:

- `AppContext` constructs repositories, services, password hashing, account-number generation, and session management.
- Controllers receive access through `SceneManager` and never create their own `DatabaseManager`.

## Responsibilities

### Controllers
- Read form fields
- Perform UI-only checks (for example password confirmation)
- Call services
- Display safe error messages
- Navigate screens

Controllers do **not** contain SQL, hash passwords, or mutate balances directly.

### Services
- `AuthService` — registration, login, initial checking/savings account creation
- `AccountService` — deposits, withdrawals, transfers, ownership checks
- `TransactionService` — recent/account history with ownership enforcement

### Repositories
- Own SQL and `ResultSet` mapping
- Provide connection-aware methods for multi-step transactional work
- Map money as SQLite `TEXT` ↔ `BigDecimal`

### Database
- `DatabaseManager` opens connections, enables `PRAGMA foreign_keys = ON`, and runs `executeInTransaction(...)`
- `DatabaseInitializer` creates tables, CHECK constraints, and indexes

## Money

- Domain money uses `BigDecimal`, never `double`/`float`
- Canonical USD scale is 2 decimal places
- Values requiring more than 2 decimals are rejected (no silent rounding loss)
- SQLite stores balances/amounts as exact text strings such as `1400.00`

## Atomic transfers

A transfer runs inside one JDBC transaction:

1. Validate source and destination ownership
2. Ensure sufficient funds
3. Update both balances
4. Insert `TRANSFER_OUT` on the source (`related_account_id` = destination)
5. Insert `TRANSFER_IN` on the destination (`related_account_id` = source)
6. Commit

Any failure rolls back the entire unit of work. Half-finished transfers are impossible.

Two ledger entries exist so each account has a complete local history while still linking to the counterpart account.

## Password storage

- Algorithm: PBKDF2-HMAC-SHA256
- Production iterations: 600,000
- Unique random 16-byte salt per password
- 256-bit derived key
- Encoded format: `pbkdf2-sha256$iterations$base64Salt$base64Hash`
- Verification uses constant-time comparison
- `User.toString()` never includes `passwordHash`

## Session safety

`UserSession` exposes only:

- userId
- username
- firstName
- lastName
- email

The JavaFX layer never carries password hashes.

## Ownership rules

Even in a local desktop simulation, services treat `userId` as the authenticated actor.

Before reading or mutating an account, services verify:

`account.userId == authenticatedUserId`

Transaction history queries join through the user's accounts so another user's ledger cannot be retrieved through service APIs.

## Testing

Integration tests use JUnit `@TempDir` SQLite databases.

They never write to `./data/banking.db`.

The core journey test covers:

1. Register user
2. Deposit $2,000 to checking
3. Transfer $500 to savings
4. Withdraw $100 from checking
5. Assert balances `1400.00` / `500.00`
6. Rebuild services against the same DB file
7. Re-authenticate and confirm persistence
