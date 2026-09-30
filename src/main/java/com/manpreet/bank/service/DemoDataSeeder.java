package com.manpreet.bank.service;

import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.session.UserSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Optional explicit demo-data facility.
 * Never runs during normal production startup unless {@code -Dbank.demo.seed=true} is set.
 */
public class DemoDataSeeder {

    public static final String DEMO_USERNAME = "demouser";
    public static final String DEMO_EMAIL = "demo@example.com";
    public static final String DEMO_PASSWORD = "DemoPassword12";

    public static final String DESC_PAYCHECK = "Paycheck deposit";
    public static final String DESC_EMERGENCY_FUND = "Emergency fund transfer";
    public static final String DESC_ATM = "ATM withdrawal";
    public static final String DESC_TRANSFER_TO_SAVINGS = "Transfer to savings";

    /**
     * Stable descriptions used as the durable seed marker.
     * Presence of any marker means sample activity already ran (balances may later be zero).
     */
    static final Set<String> SEED_MARKER_DESCRIPTIONS = Set.of(
            DESC_PAYCHECK,
            DESC_EMERGENCY_FUND,
            DESC_ATM,
            DESC_TRANSFER_TO_SAVINGS
    );

    private final AuthService authService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public DemoDataSeeder(AuthService authService,
                          AccountService accountService,
                          TransactionService transactionService) {
        this.authService = Objects.requireNonNull(authService);
        this.accountService = Objects.requireNonNull(accountService);
        this.transactionService = Objects.requireNonNull(transactionService);
    }

    /**
     * Creates a representative demo user and sample activity when missing.
     * Safe to call repeatedly: existing demo username/activity is left unchanged even if balances
     * are later spent down to zero.
     */
    public UserSession seedIfAbsent() {
        try {
            UserSession session = authService.register(
                    "Demo",
                    "User",
                    DEMO_EMAIL,
                    DEMO_USERNAME,
                    DEMO_PASSWORD
            );
            populateActivity(session.userId());
            return session;
        } catch (DuplicateUserException e) {
            UserSession existing = authService.authenticate(DEMO_USERNAME, DEMO_PASSWORD);
            populateActivity(existing.userId());
            return existing;
        }
    }

    boolean hasSeedActivity(long userId) {
        List<Transaction> rows = transactionService.search(
                userId,
                new TransactionFilter(null, null, null, null, null, 100, 0)
        );
        return rows.stream()
                .map(Transaction::getDescription)
                .anyMatch(SEED_MARKER_DESCRIPTIONS::contains);
    }

    private void populateActivity(long userId) {
        if (hasSeedActivity(userId)) {
            return;
        }

        List<Account> accounts = accountService.getAccountsForUser(userId);
        Account checking = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.CHECKING)
                .findFirst()
                .orElseThrow();
        Account savings = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.SAVINGS)
                .findFirst()
                .orElseThrow();

        accountService.deposit(userId, checking.getId(), new BigDecimal("2500.00"), DESC_PAYCHECK);
        accountService.transfer(
                userId,
                checking.getId(),
                savings.getId(),
                new BigDecimal("500.00"),
                DESC_EMERGENCY_FUND
        );
        accountService.withdraw(userId, checking.getId(), new BigDecimal("60.00"), DESC_ATM);
        accountService.transfer(
                userId,
                checking.getId(),
                savings.getId(),
                new BigDecimal("200.00"),
                DESC_TRANSFER_TO_SAVINGS
        );
    }
}
