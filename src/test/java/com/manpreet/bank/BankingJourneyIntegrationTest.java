package com.manpreet.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.service.AccountService;
import com.manpreet.bank.service.AuthService;
import com.manpreet.bank.service.TransactionService;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.support.TestAppContext;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BankingJourneyIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void completeBankingJourneyPersistsAcrossServiceReconstruction() {
        Path databaseFile = tempDir.resolve("journey.db");
        AppContext context = TestAppContext.create(databaseFile);
        AuthService authService = context.getAuthService();
        AccountService accountService = context.getAccountService();
        TransactionService transactionService = context.getTransactionService();

        UserSession session = authService.register(
                "Test",
                "Banker",
                "test@example.com",
                "testbanker",
                "StrongTestPass1"
        );

        Account checking = findAccount(accountService, session.userId(), AccountType.CHECKING);
        Account savings = findAccount(accountService, session.userId(), AccountType.SAVINGS);
        assertEquals(0, new BigDecimal("0.00").compareTo(checking.getBalance()));
        assertEquals(0, new BigDecimal("0.00").compareTo(savings.getBalance()));

        accountService.deposit(session.userId(), checking.getId(), new BigDecimal("2000.00"), "Initial deposit");
        accountService.transfer(
                session.userId(),
                checking.getId(),
                savings.getId(),
                new BigDecimal("500.00"),
                "Move to savings"
        );
        accountService.withdraw(session.userId(), checking.getId(), new BigDecimal("100.00"), "Cash withdrawal");

        checking = findAccount(accountService, session.userId(), AccountType.CHECKING);
        savings = findAccount(accountService, session.userId(), AccountType.SAVINGS);
        assertEquals(0, new BigDecimal("1400.00").compareTo(checking.getBalance()));
        assertEquals(0, new BigDecimal("500.00").compareTo(savings.getBalance()));

        List<Transaction> history = transactionService.getRecentActivity(session.userId(), 20);
        Set<TransactionType> types = history.stream()
                .map(Transaction::getTransactionType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TransactionType.class)));
        assertTrue(types.contains(TransactionType.DEPOSIT));
        assertTrue(types.contains(TransactionType.TRANSFER_OUT));
        assertTrue(types.contains(TransactionType.TRANSFER_IN));
        assertTrue(types.contains(TransactionType.WITHDRAWAL));
        assertTrue(history.stream().anyMatch(tx ->
                tx.getTransactionType() == TransactionType.DEPOSIT
                        && tx.getAmount().compareTo(new BigDecimal("2000.00")) == 0));
        assertTrue(history.stream().anyMatch(tx ->
                tx.getTransactionType() == TransactionType.TRANSFER_OUT
                        && tx.getAmount().compareTo(new BigDecimal("500.00")) == 0));
        assertTrue(history.stream().anyMatch(tx ->
                tx.getTransactionType() == TransactionType.TRANSFER_IN
                        && tx.getAmount().compareTo(new BigDecimal("500.00")) == 0));
        assertTrue(history.stream().anyMatch(tx ->
                tx.getTransactionType() == TransactionType.WITHDRAWAL
                        && tx.getAmount().compareTo(new BigDecimal("100.00")) == 0));

        AppContext reloaded = TestAppContext.create(databaseFile);
        UserSession reauthenticated = reloaded.getAuthService()
                .authenticate("testbanker", "StrongTestPass1");

        Account reloadedChecking = findAccount(reloaded.getAccountService(), reauthenticated.userId(), AccountType.CHECKING);
        Account reloadedSavings = findAccount(reloaded.getAccountService(), reauthenticated.userId(), AccountType.SAVINGS);
        assertEquals(0, new BigDecimal("1400.00").compareTo(reloadedChecking.getBalance()));
        assertEquals(0, new BigDecimal("500.00").compareTo(reloadedSavings.getBalance()));

        List<Transaction> reloadedHistory = reloaded.getTransactionService()
                .getRecentActivity(reauthenticated.userId(), 20);
        assertEquals(history.size(), reloadedHistory.size());
        assertTrue(reloadedHistory.stream().anyMatch(tx ->
                tx.getTransactionType() == TransactionType.DEPOSIT
                        && tx.getAmount().compareTo(new BigDecimal("2000.00")) == 0));
    }

    private static Account findAccount(AccountService accountService, long userId, AccountType type) {
        return accountService.getAccountsForUser(userId).stream()
                .filter(account -> account.getAccountType() == type)
                .findFirst()
                .orElseThrow();
    }
}
