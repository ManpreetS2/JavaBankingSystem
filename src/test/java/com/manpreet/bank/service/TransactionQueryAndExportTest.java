package com.manpreet.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.exception.AccountAccessException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.support.TestAppContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransactionQueryAndExportTest {

    private static final String PASSWORD = "StrongTestPass1";

    @TempDir
    Path tempDir;

    private AppContext context;
    private UserSession owner;
    private UserSession other;
    private Account ownerChecking;
    private Account ownerSavings;
    private Account otherChecking;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("query.db"));
        owner = context.getAuthService().register("Owner", "One", "ownerq@example.com", "ownerq", PASSWORD);
        other = context.getAuthService().register("Other", "Two", "otherq@example.com", "otherq", PASSWORD);
        ownerChecking = find(owner.userId(), AccountType.CHECKING);
        ownerSavings = find(owner.userId(), AccountType.SAVINGS);
        otherChecking = find(other.userId(), AccountType.CHECKING);

        context.getAccountService().deposit(owner.userId(), ownerChecking.getId(), new BigDecimal("100.00"), "Alpha deposit");
        context.getAccountService().transfer(
                owner.userId(), ownerChecking.getId(), ownerSavings.getId(), new BigDecimal("25.00"), "Move funds"
        );
        context.getAccountService().withdraw(owner.userId(), ownerChecking.getId(), new BigDecimal("10.00"), "Coffee, \"special\"");
        context.getAccountService().deposit(other.userId(), otherChecking.getId(), new BigDecimal("50.00"), "Other deposit");
    }

    @Test
    void filtersByAccountTypeSearchAndRejectsForeignAccount() {
        List<Transaction> all = context.getTransactionService()
                .search(owner.userId(), TransactionFilter.recent(20));
        assertEquals(4, all.size());
        assertEquals(TransactionType.WITHDRAWAL, all.get(0).getTransactionType());

        List<Transaction> checkingOnly = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(ownerChecking.getId(), null, null, null, null, 20, 0)
        );
        assertTrue(checkingOnly.stream().allMatch(tx -> tx.getAccountId() == ownerChecking.getId()));

        List<Transaction> withdrawals = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(null, TransactionType.WITHDRAWAL, null, null, null, 20, 0)
        );
        assertEquals(1, withdrawals.size());

        List<Transaction> search = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(null, null, null, null, "alpha", 20, 0)
        );
        assertEquals(1, search.size());

        assertThrows(AccountAccessException.class, () -> context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(otherChecking.getId(), null, null, null, null, 20, 0)
        ));
    }

    @Test
    void supportsDateRangeAndPagination() {
        LocalDate today = LocalDate.now();
        List<Transaction> ranged = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(null, null, today, today, null, 20, 0)
        );
        assertFalse(ranged.isEmpty());

        List<Transaction> page = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(null, null, null, null, null, 2, 1)
        );
        assertEquals(2, page.size());
        assertEquals(4, context.getTransactionService().count(owner.userId(), TransactionFilter.recent(20)));
    }

    @Test
    void csvExportEscapesAndMasksAccounts() {
        String csv = context.getTransactionExportService().exportCsv(
                owner.userId(),
                TransactionFilter.recent(20)
        );
        assertTrue(csv.startsWith("Date,Account,Type,Description,Amount"));
        assertTrue(csv.contains("Checking ••••"));
        assertTrue(csv.contains("\"Coffee, \"\"special\"\"\"") || csv.contains("Coffee"));
        assertFalse(csv.toLowerCase().contains("password"));
        assertFalse(csv.contains(PASSWORD));

        byte[] bytes = context.getTransactionExportService().exportCsvBytes(
                owner.userId(), TransactionFilter.recent(5)
        );
        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("Date,Account"));
    }

    @Test
    void demoDataSeederIsIdempotent() {
        UserSession first = context.getDemoDataSeeder().seedIfAbsent();
        UserSession second = context.getDemoDataSeeder().seedIfAbsent();
        assertEquals(first.userId(), second.userId());
        assertEquals(2, context.getAccountService().getAccountsForUser(first.userId()).size());
    }

    private Account find(long userId, AccountType type) {
        return context.getAccountService().getAccountsForUser(userId).stream()
                .filter(account -> account.getAccountType() == type)
                .findFirst()
                .orElseThrow();
    }
}
