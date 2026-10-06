# Manual QA checklist

This checklist separates checks verified by automated release-candidate QA from items that still need human eyes.

Last autonomous QA pass branch: `feature/release-candidate-hardening`.

## AUTOMATICALLY VERIFIED

These were exercised by automated tests, packaging scripts, and/or non-interactive startup in the autonomous QA run:

- Full banking journey with persistence across context rebuild (deposit / transfer / withdraw balances and ledger)
- Auth rejection paths: wrong password, unknown username, duplicate username/email (including case-insensitive email duplicate)
- Money validation: zero/negative/over-scale rejected; `0.01`, `1.00`, `999.99` accepted; exact-balance withdraw/transfer allowed
- Ownership isolation and same-account transfer rejection
- Transfer paired ledger (`TRANSFER_OUT` / `TRANSFER_IN`) and failed-transfer non-mutation for insufficient funds / foreign account
- Session start/clear; `UserSession` and `User.toString()` omit password material
- Schema version 2, foreign keys ON, uniqueness/FK/check constraints, legacy/future schema rejection, exact TEXT money storage
- Demo mode off by default; explicit seed property; idempotent complete seed; partial-seed safety; ownership isolation
- Transaction search literals (`%`, `_`, `\`), filters, pagination, ownership scoping
- CSV export escaping, UTF-8, formula neutralization (`= + - @` tab CR), masked accounts, export limits (including 10,000 / 10,001 count guards), no password/hash leakage
- Theme preference load/save/fallback/rollback/restart via isolated stores (no real user Preferences node)
- FXML controller/`fx:id`/`onAction` bindings and duplicate-`fx:id` guard for all seven FXML screens
- Light/dark required stylesheet selectors for tables, date pickers, dialog labels, primary focus
- Startup failure messaging strips SQL/JDBC diagnostics
- Monetary production paths contain no `double`/`float` money calculations
- `mvn clean test` green
- `./scripts/verify-release.sh` / packaging produces `target/dist/BankingSystem.app`
- `mvn javafx:run` process launch with schema/version/database log lines (no claimed click-through)
- Temporary-database startup path (`-Dbank.db.path`) creates/uses the intended file outside the repo
- Packaged `BankingSystem.app` launches as a process; no `.db` written inside the `.app` bundle
- Repo hygiene: no tracked `.env`, DB files, or `target/` artifacts

## REQUIRES HUMAN VISUAL CHECK

These cannot be reliably signed off without a human looking at the running UI:

- Visual polish of Login / Register / Dashboard / Accounts / Transactions / Settings at **1200×760** and near **1000×650**
- Light and dark theme overall look (contrast, empty states, selected table rows, dialogs) beyond stylesheet presence
- Sidebar active-state appearance while navigating between sections
- Keyboard-only traversal feel (Tab order, focus rings on every interactive control)
- Banking dialog usability (Deposit / Withdraw / Transfer) including focus return after validation errors
- Confirm the Banking System window is foregrounded and titled correctly during a local GUI session
- Final screenshot assets under `docs/screenshots/`
- Final application icon artwork (`app.png` / `app.icns`)
- Full click-through theme toggle via UI (macOS System Events assistive access was unavailable in autonomous QA)

## Demo credentials (intentional, non-secret)

Documented demo-only login when `-Dbank.demo.seed=true`:

| Field | Value |
|-------|--------|
| Username | `demouser` |
| Password | `DemoPassword12` |
