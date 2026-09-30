package com.manpreet.bank.service;

import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.session.UserSession;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     * Stable descriptions for the complete sample dataset, in seed order.
     */
    public static final List<String> EXPECTED_SEED_DESCRIPTIONS = List.of(
            DESC_PAYCHECK,
            DESC_EMERGENCY_FUND,
            DESC_ATM,
            DESC_TRANSFER_TO_SAVINGS
    );

    static final Set<String> SEED_MARKER_DESCRIPTIONS = Set.copyOf(EXPECTED_SEED_DESCRIPTIONS);

    public static final String INCOMPLETE_SEED_MESSAGE =
            "Demo seed data is incomplete for user '" + DEMO_USERNAME + "'. "
                    + "Delete the database file shown in the startup log (or point "
                    + "-Dbank.db.path at a fresh file) and restart with -Dbank.demo.seed=true. "
                    + "Partially seeded demo data is not automatically repaired.";

    enum SeedState {
        EMPTY,
        PARTIAL,
        COMPLETE
    }

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
     * Creates a representative demo user and complete sample activity when missing.
     * Safe to call repeatedly once the full seed set exists, even if balances later reach zero.
     * Partial seed states fail fast with {@link #INCOMPLETE_SEED_MESSAGE}.
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

    boolean hasCompleteSeedActivity(long userId) {
        return resolveSeedState(userId) == SeedState.COMPLETE;
    }

    SeedState resolveSeedState(long userId) {
        Set<String> present = transactionService.findExistingDescriptions(userId, EXPECTED_SEED_DESCRIPTIONS);
        if (present.isEmpty()) {
            return SeedState.EMPTY;
        }
        if (present.containsAll(SEED_MARKER_DESCRIPTIONS)) {
            return SeedState.COMPLETE;
        }
        return SeedState.PARTIAL;
    }

    private void populateActivity(long userId) {
        SeedState state = resolveSeedState(userId);
        if (state == SeedState.COMPLETE) {
            return;
        }
        if (state == SeedState.PARTIAL) {
            Set<String> present = transactionService.findExistingDescriptions(userId, EXPECTED_SEED_DESCRIPTIONS);
            Set<String> missing = EXPECTED_SEED_DESCRIPTIONS.stream()
                    .filter(description -> !present.contains(description))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            throw new IllegalStateException(
                    INCOMPLETE_SEED_MESSAGE
                            + " Missing markers: " + String.join(", ", missing) + "."
            );
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
