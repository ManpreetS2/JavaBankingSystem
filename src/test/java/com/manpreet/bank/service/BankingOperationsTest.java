package com.manpreet.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.exception.AccountAccessException;
import com.manpreet.bank.exception.InsufficientFundsException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.support.TestAppContext;
import com.manpreet.bank.util.MoneyUtil;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BankingOperationsTest {

    private static final String PASSWORD = "StrongTestPass1";

    @TempDir
    Path tempDir;

    private AppContext context;
    private AccountService accountService;
    private TransactionService transactionService;
    private UserSession owner;
    private UserSession other;
    private Account ownerChecking;
    private Account ownerSavings;
    private Account otherChecking;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("banking.db"));
        accountService = context.getAccountService();
        transactionService = context.getTransactionService();

        owner = context.getAuthService().register("Owner", "One", "owner@example.com", "owner1", PASSWORD);
        other = context.getAuthService().register("Other", "Two", "other@example.com", "other1", PASSWORD);

        ownerChecking = findAccount(owner.userId(), AccountType.CHECKING);
        ownerSavings = findAccount(owner.userId(), AccountType.SAVINGS);
        otherChecking = findAccount(other.userId(), AccountType.CHECKING);
    }

    @Test
    void depositAcceptsExactCentsAndRejectsInvalidAmountsAndForeignAccounts() {
        DepositResult result = accountService.deposit(
                owner.userId(), ownerChecking.getId(), new BigDecimal("25.50"), "Paycheck"
        );
        assertEquals(0, new BigDecimal("25.50").compareTo(result.account().getBalance()));
        assertEquals(TransactionType.DEPOSIT, result.transaction().getTransactionType());

        assertThrows(ValidationException.class,
                () -> accountService.deposit(owner.userId(), ownerChecking.getId(), BigDecimal.ZERO, null));
        assertThrows(ValidationException.class,
                () -> accountService.deposit(owner.userId(), ownerChecking.getId(), new BigDecimal("-1.00"), null));
        assertThrows(ValidationException.class,
                () -> accountService.deposit(owner.userId(), ownerChecking.getId(), new BigDecimal("1.999"), null));
        assertThrows(AccountAccessException.class,
                () -> accountService.deposit(owner.userId(), otherChecking.getId(), new BigDecimal("10.00"), null));
    }

    @Test
    void withdrawalEnforcesFundsAndDoesNotMutateOnFailure() {
        accountService.deposit(owner.userId(), ownerChecking.getId(), new BigDecimal("100.00"), "Seed");

        WithdrawalResult result = accountService.withdraw(
                owner.userId(), ownerChecking.getId(), new BigDecimal("40.00"), "ATM"
        );
        assertEquals(0, new BigDecimal("60.00").compareTo(result.account().getBalance()));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.withdraw(owner.userId(), ownerChecking.getId(), new BigDecimal("100.00"), null));

        Account reloaded = accountService.getAccount(owner.userId(), ownerChecking.getId());
        assertEquals(0, new BigDecimal("60.00").compareTo(reloaded.getBalance()));
        assertEquals(2, transactionService.getAccountHistory(owner.userId(), ownerChecking.getId()).size());
    }

    @Test
    void transferMovesMoneyAtomicallyWithPairedLedgerEntries() {
        accountService.deposit(owner.userId(), ownerChecking.getId(), new BigDecimal("200.00"), "Seed");

        TransferResult result = accountService.transfer(
                owner.userId(),
                ownerChecking.getId(),
                ownerSavings.getId(),
                new BigDecimal("75.00"),
                "Move"
        );

        assertEquals(0, new BigDecimal("125.00").compareTo(result.sourceAccount().getBalance()));
        assertEquals(0, new BigDecimal("75.00").compareTo(result.destinationAccount().getBalance()));
        assertEquals(TransactionType.TRANSFER_OUT, result.outgoingTransaction().getTransactionType());
        assertEquals(TransactionType.TRANSFER_IN, result.incomingTransaction().getTransactionType());
        assertEquals(ownerSavings.getId(), result.outgoingTransaction().getRelatedAccountId());
        assertEquals(ownerChecking.getId(), result.incomingTransaction().getRelatedAccountId());

        assertThrows(ValidationException.class,
                () -> accountService.transfer(owner.userId(), ownerChecking.getId(), ownerChecking.getId(),
                        new BigDecimal("1.00"), null));
        assertThrows(InsufficientFundsException.class,
                () -> accountService.transfer(owner.userId(), ownerChecking.getId(), ownerSavings.getId(),
                        new BigDecimal("1000.00"), null));
        assertThrows(AccountAccessException.class,
                () -> accountService.transfer(owner.userId(), ownerChecking.getId(), otherChecking.getId(),
                        new BigDecimal("1.00"), null));

        Account checking = accountService.getAccount(owner.userId(), ownerChecking.getId());
        Account savings = accountService.getAccount(owner.userId(), ownerSavings.getId());
        assertEquals(0, new BigDecimal("125.00").compareTo(checking.getBalance()));
        assertEquals(0, new BigDecimal("75.00").compareTo(savings.getBalance()));
    }

    @Test
    void transactionHistoryIsNewestFirstAndOwnershipScoped() {
        accountService.deposit(owner.userId(), ownerChecking.getId(), new BigDecimal("10.00"), "One");
        accountService.deposit(owner.userId(), ownerChecking.getId(), new BigDecimal("20.00"), "Two");
        accountService.deposit(other.userId(), otherChecking.getId(), new BigDecimal("99.00"), "Other");

        List<Transaction> recent = transactionService.getRecentActivity(owner.userId(), 10);
        assertEquals(2, recent.size());
        assertEquals("Two", recent.get(0).getDescription());
        assertTrue(recent.stream().noneMatch(tx -> "Other".equals(tx.getDescription())));

        assertThrows(AccountAccessException.class,
                () -> transactionService.getAccountHistory(owner.userId(), otherChecking.getId()));
        assertThrows(ValidationException.class,
                () -> transactionService.getRecentActivity(owner.userId(), 0));
    }

    private Account findAccount(long userId, AccountType type) {
        return accountService.getAccountsForUser(userId).stream()
                .filter(account -> account.getAccountType() == type)
                .findFirst()
                .orElseThrow();
    }
}
