package com.manpreet.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.support.TestAppContext;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoDataSeederTest {

    @TempDir
    Path tempDir;

    private AppContext context;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("demo-seeder.db"));
    }

    @Test
    void emptyDemoStateCreatesCompleteSeed() {
        UserSession session = context.getDemoDataSeeder().seedIfAbsent();
        assertEquals(DemoDataSeeder.SeedState.COMPLETE, context.getDemoDataSeeder().resolveSeedState(session.userId()));

        List<Account> accounts = context.getAccountService().getAccountsForUser(session.userId());
        Account checking = find(accounts, AccountType.CHECKING);
        Account savings = find(accounts, AccountType.SAVINGS);
        assertEquals(new BigDecimal("1740.00"), checking.getBalance());
        assertEquals(new BigDecimal("700.00"), savings.getBalance());

        Set<String> markers = context.getTransactionService()
                .findExistingDescriptions(session.userId(), DemoDataSeeder.EXPECTED_SEED_DESCRIPTIONS);
        assertEquals(DemoDataSeeder.SEED_MARKER_DESCRIPTIONS, markers);
        assertEquals(1, countExactDescription(session.userId(), DemoDataSeeder.DESC_PAYCHECK));
    }

    @Test
    void completeDemoStateIsIdempotent() {
        UserSession session = context.getDemoDataSeeder().seedIfAbsent();
        int count = totalOwnedTransactions(session.userId());

        context.getDemoDataSeeder().seedIfAbsent();
        assertEquals(count, totalOwnedTransactions(session.userId()));
        assertEquals(1, countExactDescription(session.userId(), DemoDataSeeder.DESC_PAYCHECK));
    }

    @Test
    void drainedCompleteSeedDoesNotReseed() {
        UserSession session = context.getDemoDataSeeder().seedIfAbsent();
        drainDemoBalancesToZero(session.userId());

        List<Account> drained = context.getAccountService().getAccountsForUser(session.userId());
        assertTrue(drained.stream().allMatch(a -> a.getBalance().compareTo(BigDecimal.ZERO) == 0));
        assertTrue(context.getDemoDataSeeder().hasCompleteSeedActivity(session.userId()));

        int countAfterDrain = totalOwnedTransactions(session.userId());
        context.getDemoDataSeeder().seedIfAbsent();
        assertEquals(countAfterDrain, totalOwnedTransactions(session.userId()));
        assertEquals(1, countExactDescription(session.userId(), DemoDataSeeder.DESC_PAYCHECK));
    }

    @Test
    void partialSeedStateIsRejectedWithActionableMessage() {
        UserSession session = context.getAuthService().register(
                "Demo",
                "User",
                DemoDataSeeder.DEMO_EMAIL,
                DemoDataSeeder.DEMO_USERNAME,
                DemoDataSeeder.DEMO_PASSWORD
        );
        Account checking = find(
                context.getAccountService().getAccountsForUser(session.userId()),
                AccountType.CHECKING
        );
        context.getAccountService().deposit(
                session.userId(),
                checking.getId(),
                new BigDecimal("2500.00"),
                DemoDataSeeder.DESC_PAYCHECK
        );

        assertEquals(DemoDataSeeder.SeedState.PARTIAL, context.getDemoDataSeeder().resolveSeedState(session.userId()));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> context.getDemoDataSeeder().seedIfAbsent()
        );
        assertTrue(error.getMessage().contains(DemoDataSeeder.INCOMPLETE_SEED_MESSAGE));
        assertTrue(error.getMessage().contains("Missing markers:"));
        assertTrue(error.getMessage().contains(DemoDataSeeder.DESC_EMERGENCY_FUND));
        assertEquals(1, countExactDescription(session.userId(), DemoDataSeeder.DESC_PAYCHECK));
        assertEquals(
                Set.of(DemoDataSeeder.DESC_PAYCHECK),
                context.getTransactionService().findExistingDescriptions(
                        session.userId(),
                        DemoDataSeeder.EXPECTED_SEED_DESCRIPTIONS
                )
        );
    }

    @Test
    void otherUsersAreUnaffectedByDemoSeed() {
        UserSession other = context.getAuthService().register(
                "Other",
                "Person",
                "other@example.com",
                "otheruser",
                "OtherPassword12"
        );
        context.getDemoDataSeeder().seedIfAbsent();

        assertEquals(0, totalOwnedTransactions(other.userId()));
        assertTrue(context.getTransactionService()
                .findExistingDescriptions(other.userId(), DemoDataSeeder.EXPECTED_SEED_DESCRIPTIONS)
                .isEmpty());
        List<Account> otherAccounts = context.getAccountService().getAccountsForUser(other.userId());
        assertTrue(otherAccounts.stream().allMatch(a -> a.getBalance().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    void completeSeedRemainsDetectedAfterMoreThanOneHundredNewerTransactions() {
        UserSession session = context.getDemoDataSeeder().seedIfAbsent();
        Account checking = find(
                context.getAccountService().getAccountsForUser(session.userId()),
                AccountType.CHECKING
        );

        for (int i = 0; i < 110; i++) {
            context.getAccountService().deposit(
                    session.userId(),
                    checking.getId(),
                    new BigDecimal("1.00"),
                    "Noise activity " + i
            );
        }

        List<Transaction> recent = context.getTransactionService().search(
                session.userId(),
                new TransactionFilter(null, null, null, null, null, 100, 0)
        );
        assertEquals(100, recent.size());
        assertTrue(recent.stream()
                .map(Transaction::getDescription)
                .noneMatch(DemoDataSeeder.SEED_MARKER_DESCRIPTIONS::contains));

        assertEquals(DemoDataSeeder.SeedState.COMPLETE, context.getDemoDataSeeder().resolveSeedState(session.userId()));
        int before = totalOwnedTransactions(session.userId());
        context.getDemoDataSeeder().seedIfAbsent();
        assertEquals(before, totalOwnedTransactions(session.userId()));
        assertEquals(1, countExactDescription(session.userId(), DemoDataSeeder.DESC_PAYCHECK));
    }

    private void drainDemoBalancesToZero(long userId) {
        List<Account> accounts = context.getAccountService().getAccountsForUser(userId);
        Account checking = find(accounts, AccountType.CHECKING);
        Account savings = find(accounts, AccountType.SAVINGS);

        if (savings.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            context.getAccountService().transfer(
                    userId,
                    savings.getId(),
                    checking.getId(),
                    savings.getBalance(),
                    "Demo cleanup transfer"
            );
        }
        Account refreshedChecking = find(
                context.getAccountService().getAccountsForUser(userId),
                AccountType.CHECKING
        );
        if (refreshedChecking.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            context.getAccountService().withdraw(
                    userId,
                    refreshedChecking.getId(),
                    refreshedChecking.getBalance(),
                    "Demo cleanup withdrawal"
            );
        }
    }

    private int totalOwnedTransactions(long userId) {
        return (int) context.getTransactionService().count(
                userId,
                new TransactionFilter(null, null, null, null, null, 20, 0)
        );
    }

    private long countExactDescription(long userId, String description) {
        long total = 0;
        int offset = 0;
        while (true) {
            List<Transaction> page = context.getTransactionService().search(
                    userId,
                    new TransactionFilter(null, null, null, null, null, 100, offset)
            );
            if (page.isEmpty()) {
                break;
            }
            total += page.stream()
                    .map(Transaction::getDescription)
                    .filter(description::equals)
                    .count();
            offset += page.size();
            if (page.size() < 100) {
                break;
            }
        }
        return total;
    }

    private static Account find(List<Account> accounts, AccountType type) {
        return accounts.stream()
                .filter(account -> account.getAccountType() == type)
                .findFirst()
                .orElseThrow();
    }
}
