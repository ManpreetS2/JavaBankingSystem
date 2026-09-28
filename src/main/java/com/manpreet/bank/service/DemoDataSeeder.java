package com.manpreet.bank.service;

import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.session.UserSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Optional explicit demo-data facility. Never runs automatically at startup.
 */
public class DemoDataSeeder {

    public static final String DEMO_USERNAME = "demouser";
    public static final String DEMO_EMAIL = "demo@example.com";
    public static final String DEMO_PASSWORD = "DemoPassword12";

    private final AuthService authService;
    private final AccountService accountService;

    public DemoDataSeeder(AuthService authService, AccountService accountService) {
        this.authService = Objects.requireNonNull(authService);
        this.accountService = Objects.requireNonNull(accountService);
    }

    /**
     * Creates a representative demo user and sample activity when missing.
     * Safe to call repeatedly: existing demo username is left unchanged.
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
            return authService.authenticate(DEMO_USERNAME, DEMO_PASSWORD);
        }
    }

    private void populateActivity(long userId) {
        List<Account> accounts = accountService.getAccountsForUser(userId);
        Account checking = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.CHECKING)
                .findFirst()
                .orElseThrow();
        Account savings = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.SAVINGS)
                .findFirst()
                .orElseThrow();

        if (checking.getBalance().compareTo(BigDecimal.ZERO) > 0
                || savings.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            return;
        }

        accountService.deposit(userId, checking.getId(), new BigDecimal("2500.00"), "Paycheck deposit");
        accountService.transfer(
                userId,
                checking.getId(),
                savings.getId(),
                new BigDecimal("400.00"),
                "Transfer to savings"
        );
        accountService.withdraw(userId, checking.getId(), new BigDecimal("45.00"), "ATM withdrawal");
    }
}
