# Java Banking System v1.0.0

**Published:** October 14, 2026 · [Official GitHub Release](https://github.com/ManpreetS2/JavaBankingSystem/releases/tag/v1.0.0)

**Downloads:** [macOS application ZIP](https://github.com/ManpreetS2/JavaBankingSystem/releases/download/v1.0.0/BankingSystem-v1.0.0-macOS.zip) · [70-second demo MP4](https://github.com/ManpreetS2/JavaBankingSystem/releases/download/v1.0.0/JavaBankingSystem-demo.mp4)

**Release commit:** [`867a3a7`](https://github.com/ManpreetS2/JavaBankingSystem/commit/867a3a7295fdd910ffb06cecba6c1081c47b6d97). The annotated `v1.0.0` tag is an immutable snapshot; later documentation improvements on `main` do not change the tagged build.

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

The published MP4 is a captioned walkthrough of authentic JavaFX captures, not a recording of UI clicks. See [DEMO.md](DEMO.md) for a hands-on interactive walkthrough. Demo credentials are for the optional seed only:

- Username: `demouser`
- Password: `DemoPassword12`
