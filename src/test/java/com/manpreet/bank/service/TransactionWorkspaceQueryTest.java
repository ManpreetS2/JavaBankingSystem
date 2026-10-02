package com.manpreet.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.AppContext;
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

class TransactionWorkspaceQueryTest {

    private static final String PASSWORD = "StrongTestPass1";

    @TempDir
    Path tempDir;

    private AppContext context;
    private UserSession owner;
    private Account checking;
    private Account savings;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("workspace.db"));
        owner = context.getAuthService().register("Work", "Space", "workspace@example.com", "workspace", PASSWORD);
        checking = find(AccountType.CHECKING);
        savings = find(AccountType.SAVINGS);

        context.getAccountService().deposit(owner.userId(), checking.getId(), new BigDecimal("1000.00"), "Seed deposit");
        context.getAccountService().transfer(
                owner.userId(), checking.getId(), savings.getId(), new BigDecimal("200.00"), "Seed transfer"
        );
        context.getAccountService().withdraw(owner.userId(), checking.getId(), new BigDecimal("50.00"), "Seed withdraw");
    }

    @Test
    void combinedFiltersAndPaginationResetScope() {
        LocalDate today = LocalDate.now();

        List<Transaction> checkingWithdrawals = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(checking.getId(), TransactionType.WITHDRAWAL, today, today, null, 20, 0)
        );
        assertEquals(1, checkingWithdrawals.size());

        List<Transaction> searchAndDate = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(null, null, today, today, "seed", 20, 0)
        );
        assertEquals(4, searchAndDate.size());

        List<Transaction> typeAndDate = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(null, TransactionType.TRANSFER_OUT, today, today, null, 20, 0)
        );
        assertEquals(1, typeAndDate.size());

        List<Transaction> accountTypeDate = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(checking.getId(), TransactionType.DEPOSIT, today, today, "seed", 20, 0)
        );
        assertEquals(1, accountTypeDate.size());

        for (int i = 0; i < 25; i++) {
            context.getAccountService().deposit(
                    owner.userId(), checking.getId(), new BigDecimal("1.00"), "Page item " + i
            );
        }
        long total = context.getTransactionService().count(
                owner.userId(), new TransactionFilter(null, null, null, null, null, 20, 0)
        );
        assertTrue(total >= 29);

        List<Transaction> page1 = context.getTransactionService().search(
                owner.userId(), new TransactionFilter(null, null, null, null, null, 20, 0)
        );
        List<Transaction> page2 = context.getTransactionService().search(
                owner.userId(), new TransactionFilter(null, null, null, null, null, 20, 20)
        );
        assertEquals(20, page1.size());
        assertFalse(page2.isEmpty());
        assertTrue(page2.size() <= 20);

        List<Transaction> filteredPage = context.getTransactionService().search(
                owner.userId(),
                new TransactionFilter(null, TransactionType.DEPOSIT, null, null, "Page item", 10, 0)
        );
        assertEquals(10, filteredPage.size());
        assertEquals(25, context.getTransactionService().count(
                owner.userId(),
                new TransactionFilter(null, TransactionType.DEPOSIT, null, null, "Page item", 20, 0)
        ));
    }

    @Test
    void accountHistoryHonorsRequestedLimit() {
        for (int i = 0; i < 30; i++) {
            context.getAccountService().deposit(
                    owner.userId(), checking.getId(), new BigDecimal("1.00"), "History " + i
            );
        }
        List<Transaction> limited = context.getTransactionService()
                .getAccountHistory(owner.userId(), checking.getId(), 20);
        assertEquals(20, limited.size());
        assertTrue(limited.stream().allMatch(tx -> tx.getAccountId() == checking.getId()));
    }

    @Test
    void exportPagesThroughAllFilteredMatches() {
        for (int i = 0; i < 120; i++) {
            context.getAccountService().deposit(
                    owner.userId(), checking.getId(), new BigDecimal("1.00"), "Bulk export " + i
            );
        }

        String csv = context.getTransactionExportService().exportCsv(
                owner.userId(),
                new TransactionFilter(null, TransactionType.DEPOSIT, null, null, "Bulk export", 20, 0)
        );
        long dataRows = csv.lines().skip(1).filter(line -> !line.isBlank()).count();
        assertEquals(120, dataRows);
        assertFalse(csv.contains(checking.getAccountNumber()));
        assertFalse(csv.contains(PASSWORD));

        String empty = context.getTransactionExportService().exportCsv(
                owner.userId(),
                new TransactionFilter(null, null, null, null, "no-such-match-xyz", 20, 0)
        );
        assertEquals("Date,Account,Type,Description,Amount\n", empty);

        byte[] bytes = context.getTransactionExportService().exportCsvBytes(
                owner.userId(),
                new TransactionFilter(checking.getId(), TransactionType.WITHDRAWAL, null, null, null, 20, 0)
        );
        String decoded = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(decoded.contains("Withdrawal"));
        assertTrue(decoded.contains("-$50.00"));
    }

    @Test
    void totalsReflectAllActivity() {
        assertEquals(0, new BigDecimal("1000.00").compareTo(
                context.getTransactionService().totalDeposits(owner.userId(), null, null)));
        assertEquals(0, new BigDecimal("50.00").compareTo(
                context.getTransactionService().totalWithdrawals(owner.userId(), null, null)));
        assertEquals(0, new BigDecimal("200.00").compareTo(
                context.getTransactionService().totalTransfersIn(owner.userId(), null, null)));
        assertEquals(0, new BigDecimal("200.00").compareTo(
                context.getTransactionService().totalTransfersOut(owner.userId(), null, null)));
    }

    private Account find(AccountType type) {
        return context.getAccountService().getAccountsForUser(owner.userId()).stream()
                .filter(account -> account.getAccountType() == type)
                .findFirst()
                .orElseThrow();
    }
}
