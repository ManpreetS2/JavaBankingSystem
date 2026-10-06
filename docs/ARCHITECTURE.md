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
- `AppStartup` resolves the database path, initializes schema, and optionally seeds demo data when `-Dbank.demo.seed=true`.
- `ApplicationPaths` chooses the OS application-data directory (or `-Dbank.db.path`) for normal desktop launches.
- `AppInfo` centralizes product name / version metadata for titles and diagnostics.

## Login and registration forms

- `AuthFormValidator` runs the same `InputValidator` rules as `AuthService` before submit and reports the first invalid field in on-screen order; `AuthService` remains the authority
- `FieldFeedback` marks the invalid field with `input-error`, exposes the message as accessible help, and clears the marker when the field is edited
- The first field is focused on load, and Enter submits through each form's default button
- Registration keeps the entered password when the error is in another field and clears both password fields only for password errors
- Duplicate username or email errors focus the conflicting field
- After registration, the login form is prefilled with the new username and focuses the password field
- A failed sign-in clears the password without indicating which credential was wrong
- Messages shown on these forms are formatted as sentences by `MessageText`, matching dialog and status feedback; service messages are unchanged

## Authenticated application shell

After login, navigation uses `main-shell.fxml` / `MainShellController`:

- Persistent sidebar: Dashboard, Accounts, Transactions, Settings, Sign out
- Content host swaps views inside one stage
- Transactions loads a dedicated workspace inside the shell content region
- Settings loads `settings.fxml` inside the shell content region
- Sign out clears `SessionManager` and returns to the sign-in screen
- Authenticated content requires a valid session

## Transactions workspace

`transactions.fxml` / `TransactionsController` provide:

- Filter controls: search, account, transaction type, start/end date
- Server-side filtering via `TransactionFilter` (controllers do not filter in memory)
- Pagination with page size 20 using repository `limit` / `offset` / `count`
- Summary metrics: matching result count (current filters) plus all-activity deposit/withdrawal/transfer totals
- Row selection detail panel (masked account labels only)
- CSV export through `FileChooser`, writing UTF-8 bytes from `TransactionExportService`

Shared presentation:

- `TransactionRowViewModel` — immutable UI row
- `TransactionViewMapper` — maps ledger rows using a once-loaded `Map<Long, Account>` (no N+1)
- `PaginationState` — pure page index / total / enablement helpers

Dashboard and Accounts reuse the same mapper for consistent type labels, signed amounts, and transfer wording.

## Banking dialogs

`BankingDialogs` provides the Deposit, Withdraw, and Transfer dialogs shared by Dashboard and Accounts:

- The confirm button runs the service operation through `BankingOperationAttempt` before the dialog closes
- A rejected operation (insufficient funds, service validation, or an unexpected failure) keeps the dialog open with the entered values, shows the reason inline, and returns focus to the amount
- Only a successful operation closes the dialog; callers receive `Optional.of(true)` on success and an empty result on cancel

## Settings / Profile

`settings.fxml` / `SettingsController` provide:

- Read-only profile details (name, username, email) from the active `UserSession` via `ProfileSummary`; no repository access and no credential fields
- Light/dark theme selection through `ThemeManager.setTheme`, with labels and confirmation text from `ThemeOptions`
- The selector reflects the active theme on load, and the shell reloads Settings when the sidebar toggle changes the theme so both controls stay in sync
- Theme preference is persisted across restarts via `ThemePreferenceStore` (Java Preferences); profile editing and password change are not implemented

## Theme infrastructure

- `Theme` (`LIGHT` / `DARK`)
- `ThemeManager` applies `base.css`, `components.css`, and the active theme stylesheet to managed scenes
- `ThemePreferenceStore` / `PreferencesThemePreferenceStore` load and save the selected theme as an application preference (not banking/SQLite data)
- Startup defaults to `LIGHT` when no preference exists or the stored value is invalid
- Short-lived dialog roots use `ThemeManager.applyTo(Parent)` without remaining registered for theme updates
- Controllers do not load CSS ad hoc

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
- `DemoDataSeeder` — optional demo dataset; production startup seeds only when `-Dbank.demo.seed=true`

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
- Clean databases (version 0, no banking tables) initialize at the current version
- Unversioned legacy development databases that already contain tables are rejected with an actionable recreate message; they are never silently relabeled as current
- Databases with a newer unsupported `user_version` are rejected
- `user_version` is set only after schema/index creation succeeds inside a transaction
- Older local databases that predate structural constraints should be regenerated by deleting the database file reported in the startup log (legacy `./data/banking.db` is not auto-migrated)

## Application data paths

Resolution order for the SQLite file:

1. Explicit `AppContext(Path)` / `DatabaseManager(Path)` injection (tests)
2. System property `-Dbank.db.path=...`
3. OS application-data directory + `banking.db`

Parent directories are created before SQLite opens. Failures surface as concise startup errors.

## Demo seeding

- Disabled by default
- Enabled only with `-Dbank.demo.seed=true`
- Uses normal auth/account/transaction services (no bypass login, no weakened hashing)
- Seed state is derived from stable marker descriptions (not balances): empty → seed all; complete → no-op; partial → fail fast with an actionable recreate message
- Marker detection uses an ownership-scoped description query, not the newest-100 activity page
- Spending a complete demo dataset to zero must not recreate sample activity on the next seed

## Packaging lifecycle

- Development: `mvn javafx:run`
- Distribution: `scripts/package-app.sh` builds an unsigned `jpackage` app-image under `target/dist/`
- The project remains non-modular; `javafx:jlink` is not the primary packaging path

## Money

- Domain money uses `BigDecimal`, never `double`/`float`
- Canonical USD scale is 2 decimal places
- Values requiring more than 2 decimals are rejected
- A single deposit, withdrawal, or transfer is limited to `MoneyUtil.MAX_TRANSACTION_AMOUNT` ($1,000,000.00), enforced by `AccountService`
- Dialog amount entry accepts plain decimals only; exponent notation such as `1E+15` is rejected before it reaches the service
- SQLite stores balances/amounts as exact text strings such as `1400.00`
- UI formatting uses `CurrencyFormatter` only

## Transaction filtering

`TransactionFilter` supports:

- account scope (must be owned by authenticated user)
- transaction type
- start/end dates (`>= start`, `< end+1 day`)
- search text (description / owned account labels), case-insensitive for all letters through the `unicode_lower` SQL function that `DatabaseManager` registers on each connection; SQLite's built-in `LOWER()` only handles ASCII
- limit/offset pagination (1–100)

Queries always join through the authenticated user's accounts.

## CSV export

`TransactionExportService` exports:

`Date, Account, Type, Description, Amount`

- UTF-8
- Proper CSV escaping for commas/quotes/newlines
- Masked account labels
- No password hashes or credentials
- Counts matching rows first; exports of more than 10,000 matching transactions are rejected with a validation message (no partial CSV is produced)
- Fetches matching rows internally in batches of 100
- Export respects the current filter criteria and is not limited to the visible UI page
- Descriptions beginning with `=`, `+`, `-`, `@`, tab, or carriage return are prefixed with `'` so spreadsheets do not evaluate them as formulas

Search text normalization for filters uses `Locale.ROOT` for technical lowercase comparisons.

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

Integration tests use JUnit `@TempDir` SQLite databases and never write to the normal application-data database location.

The core journey test covers register → deposit $2000 → transfer $500 → withdraw $100 → rebuild services → re-authenticate → verify balances `$1400.00` / `$500.00`.
