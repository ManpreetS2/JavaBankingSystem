# Demo walkthrough

[▶ Watch the published v1.0.0 demo video](https://github.com/ManpreetS2/JavaBankingSystem/releases/download/v1.0.0/JavaBankingSystem-demo.mp4) · [📦 Download the macOS app](https://github.com/ManpreetS2/JavaBankingSystem/releases/download/v1.0.0/BankingSystem-v1.0.0-macOS.zip) · [GitHub Release](https://github.com/ManpreetS2/JavaBankingSystem/releases/tag/v1.0.0)

The 70-second silent video is a captioned walkthrough assembled from **real JavaFX scene captures**. It is not a recording of cursor interactions. To demonstrate real navigation and banking actions, use the steps below with a disposable demo database.

This is a local banking simulation: money does not move through a real payment network.
## Launch demo mode

Use a disposable database. Do not point this at a database you care about.

```bash
mvn javafx:run -Dbank.demo.seed=true -Dbank.db.path=/tmp/java-banking-portfolio-demo.db
```

Demo seeding is off unless `-Dbank.demo.seed=true` is set. The seed uses the normal registration, account, and transaction services.

### Demo-only credentials

| Field | Value |
|-------|--------|
| Username | `demouser` |
| Password | `DemoPassword12` |

These credentials exist only for walkthroughs. They are not a production secret, and demo mode does not bypass authentication.

## Suggested walkthrough

1. Sign in with the demo user.
2. On the Dashboard, point out the total balance, checking and savings cards, quick actions, and recent activity.
3. Open Accounts. Select checking, then savings. Show Deposit, Withdraw, and Transfer next to the selected account.
4. Open Transfer. Show the source account, available balance, destination, amount, and description. Cancel without submitting if you only need to show the form.
5. Open Transactions. Search a description, change account or type, and show the detail panel and pagination.
6. Open Settings. Show the read-only profile, then switch Light and Dark. The choice is remembered for the next launch.
7. Optional: Shortcut+1 through Shortcut+4 move between Dashboard, Accounts, Transactions, and Settings. Shortcut+F focuses search only while Transactions is already open.

## What to show

- Sign-in and a session that stays on the signed-in user
- Checking and savings balances
- Deposit, withdrawal, and an internal transfer
- Searchable, filterable transaction history and CSV export
- Light and dark appearance

## Technical talking points

- JavaFX UI, service layer, JDBC repositories, SQLite
- Money is `BigDecimal` scale 2, stored as exact text
- A transfer updates both balances and writes both ledger rows in one transaction
- Queries are limited to accounts owned by the signed-in user
- Passwords are PBKDF2-HMAC-SHA256 with a unique salt
- Repository SQL uses prepared statements
- Theme preference is stored and reapplied on startup
- `mvn clean test` is the automated suite (197 tests in v1.0.0)
- `./scripts/package-app.sh` builds an unsigned local app image

## What not to claim

- Not a licensed bank, payment processor, or production financial system
- Not PCI compliant
- No external transfers, cards, loans, or bill pay
- No cloud backend
- The packaged app is unsigned and not notarized

## Scope boundaries

Profile editing, password change, statements, and scheduled transfers are not in this version. See [RELEASE_NOTES_v1.0.0.md](RELEASE_NOTES_v1.0.0.md).
