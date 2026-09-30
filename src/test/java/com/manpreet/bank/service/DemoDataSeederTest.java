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

        List<Transaction> rows = context.getTransactionService().search(
                session.userId(),
                new TransactionFilter(null, null, null, null, null, 100, 0)
        );
        Set<String> descriptions = rows.stream()
                .map(Transaction::getDescription)
                .collect(Collectors.toSet());
        assertTrue(descriptions.contains("Paycheck deposit"));
        assertTrue(descriptions.contains("ATM withdrawal"));
        assertTrue(descriptions.contains("Emergency fund transfer"));
        assertTrue(descriptions.contains("Transfer to savings"));

        int count = rows.size();
        context.getDemoDataSeeder().seedIfAbsent();
        assertEquals(
                count,
                context.getTransactionService().search(
                        session.userId(),
                        new TransactionFilter(null, null, null, null, null, 100, 0)
                ).size()
        );
    }
}
