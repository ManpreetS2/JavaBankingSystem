# Java Banking System v1.0.0

Local desktop banking simulation for portfolio and interview demonstration. Java 21, JavaFX, SQLite, and Maven.

This release is not a production bank and does not connect to a payment network.

## Highlights

- Account registration and sign-in
- Checking and savings accounts
- Deposits, withdrawals, and internal transfers
- Searchable, filterable transaction history with pagination
- CSV export of the signed-in user's transactions
- Light and dark themes, remembered between launches
- Optional demo mode for walkthroughs
- Unsigned macOS app image via `jpackage`

## Banking behavior

- Balances use `BigDecimal` (scale 2) and are stored as exact text
- A transfer updates both account balances and writes the paired ledger rows in one JDBC transaction
- A failed transfer rolls back
- Account and transaction access is limited to the authenticated user

## Security and data integrity

- PBKDF2-HMAC-SHA256 password hashing with a unique salt and 600,000 iterations
- The signed-in session does not carry the password hash
- Repository access uses prepared statements
- CSV export masks account numbers
- Schema version checks refuse a newer database than the application understands
- The database file lives in the OS application-data directory, or at an explicit `-Dbank.db.path`

## Testing

- 197 automated tests (JUnit 5)
- Coverage includes banking journeys, ownership, money limits, export, schema safeguards, theme preference, demo seeding, and presentation rules
- GitHub Actions runs the suite on pull requests

## Packaging

```bash
./scripts/package-app.sh
```

On macOS this produces `target/dist/BankingSystem.app`. The bundle uses `src/main/resources/icons/app.icns` when that file is present. JavaFX also loads `app.png` as the window icon.

The Maven project version, display version, and package version are all `1.0.0`. The macOS app image is unsigned and not notarized.

## Known limitations

- Local desktop app only
- No real banking network and no external transfers
- No cards, loans, bill pay, or cloud sync
- Profile editing and password change are not included
- Distribution is unsigned and not notarized

## Demo

See [DEMO.md](DEMO.md). Demo credentials are for the optional seed only:

- Username: `demouser`
- Password: `DemoPassword12`
