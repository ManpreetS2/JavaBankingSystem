package com.manpreet.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.stream.Collectors;
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
    void seedsRepresentativeActivityOnce() {
        UserSession session = context.getDemoDataSeeder().seedIfAbsent();
        List<Account> accounts = context.getAccountService().getAccountsForUser(session.userId());
        Account checking = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.CHECKING)
                .findFirst()
                .orElseThrow();
        Account savings = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.SAVINGS)
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("1740.00"), checking.getBalance());
        assertEquals(new BigDecimal("700.00"), savings.getBalance());

        List<Transaction> rows = searchAll(session.userId());
        Set<String> descriptions = rows.stream()
                .map(Transaction::getDescription)
                .collect(Collectors.toSet());
        assertTrue(descriptions.containsAll(DemoDataSeeder.SEED_MARKER_DESCRIPTIONS));

        int count = rows.size();
        context.getDemoDataSeeder().seedIfAbsent();
        assertEquals(count, searchAll(session.userId()).size());
    }

    @Test
    void doesNotReseedAfterBalancesReturnToZero() {
        UserSession session = context.getDemoDataSeeder().seedIfAbsent();
        long paycheckMarkers = countDescription(session.userId(), DemoDataSeeder.DESC_PAYCHECK);
        assertEquals(1, paycheckMarkers);

        drainDemoBalancesToZero(session.userId());

        List<Account> drained = context.getAccountService().getAccountsForUser(session.userId());
        assertTrue(drained.stream().allMatch(a -> a.getBalance().compareTo(BigDecimal.ZERO) == 0));
        assertTrue(context.getDemoDataSeeder().hasSeedActivity(session.userId()));

        int countAfterDrain = searchAll(session.userId()).size();
        context.getDemoDataSeeder().seedIfAbsent();
        assertEquals(countAfterDrain, searchAll(session.userId()).size());
        assertEquals(1, countDescription(session.userId(), DemoDataSeeder.DESC_PAYCHECK));
    }

    private void drainDemoBalancesToZero(long userId) {
        List<Account> accounts = context.getAccountService().getAccountsForUser(userId);
        Account checking = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.CHECKING)
                .findFirst()
                .orElseThrow();
        Account savings = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.SAVINGS)
                .findFirst()
                .orElseThrow();

        if (savings.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            context.getAccountService().transfer(
                    userId,
                    savings.getId(),
                    checking.getId(),
                    savings.getBalance(),
                    "Demo cleanup transfer"
            );
        }
        Account refreshedChecking = context.getAccountService().getAccountsForUser(userId).stream()
                .filter(a -> a.getAccountType() == AccountType.CHECKING)
                .findFirst()
                .orElseThrow();
        if (refreshedChecking.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            context.getAccountService().withdraw(
                    userId,
                    refreshedChecking.getId(),
                    refreshedChecking.getBalance(),
                    "Demo cleanup withdrawal"
            );
        }
    }

    private List<Transaction> searchAll(long userId) {
        return context.getTransactionService().search(
                userId,
                new TransactionFilter(null, null, null, null, null, 100, 0)
        );
    }

    private long countDescription(long userId, String description) {
        return searchAll(userId).stream()
                .map(Transaction::getDescription)
                .filter(description::equals)
                .count();
    }
}
