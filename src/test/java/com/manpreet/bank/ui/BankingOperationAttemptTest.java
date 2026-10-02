package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.exception.BankingOperationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.support.TestAppContext;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BankingOperationAttemptTest {

    @TempDir
    Path tempDir;

    private AppContext context;
    private UserSession session;
    private Account checking;
    private Account savings;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("attempt.db"));
        session = context.getAuthService().register("Ada", "Lovelace", "ada@example.com", "ada", "correct-horse-battery");
        List<Account> accounts = context.getAccountService().getAccountsForUser(session.userId());
        checking = accounts.stream().filter(a -> a.getAccountType() == AccountType.CHECKING).findFirst().orElseThrow();
        savings = accounts.stream().filter(a -> a.getAccountType() == AccountType.SAVINGS).findFirst().orElseThrow();
        context.getAccountService().deposit(session.userId(), checking.getId(), new BigDecimal("100.00"), "Opening");
    }

    @Test
    void successfulOperationReportsNoRejection() {
        Optional<String> rejection = BankingOperationAttempt.run("40.00", amount -> context.getAccountService()
                .withdraw(session.userId(), checking.getId(), amount, "Groceries"));

        assertEquals(Optional.empty(), rejection);
        assertEquals(new BigDecimal("60.00"), balanceOf(checking));
    }

    @Test
    void insufficientFundsIsReportedAsASentenceAndLeavesTheBalanceUnchanged() {
        Optional<String> withdrawal = BankingOperationAttempt.run("150.00", amount -> context.getAccountService()
                .withdraw(session.userId(), checking.getId(), amount, "Rent"));
        Optional<String> transfer = BankingOperationAttempt.run("150.00", amount -> context.getAccountService()
                .transfer(session.userId(), checking.getId(), savings.getId(), amount, "Move"));

        assertEquals(Optional.of("Insufficient funds for this withdrawal."), withdrawal);
        assertEquals(Optional.of("Insufficient funds for this transfer."), transfer);
        assertEquals(new BigDecimal("100.00"), balanceOf(checking));
        assertEquals(new BigDecimal("0.00"), balanceOf(savings));
    }

    @Test
    void serviceValidationIsReported() {
        Optional<String> rejection = BankingOperationAttempt.run("10.00", amount -> context.getAccountService()
                .deposit(session.userId(), checking.getId(), amount, "x".repeat(256)));

        assertEquals(Optional.of("Description must be at most 255 characters."), rejection);
        assertEquals(new BigDecimal("100.00"), balanceOf(checking));
    }

    @Test
    void invalidAmountsNeverReachTheService() {
        List<BigDecimal> calls = new ArrayList<>();
        for (String amount : List.of("", "   ", "abc", "0", "-5", "1.001", "$25", "1,000")) {
            Optional<String> rejection = BankingOperationAttempt.run(amount, calls::add);
            assertEquals(DialogAmountValidator.validate(amount), rejection, "amount \"" + amount + "\"");
        }
        assertEquals(List.of(), calls);
    }

    @Test
    void trimsTheAmountBeforeRunningTheOperation() {
        List<BigDecimal> calls = new ArrayList<>();

        assertEquals(Optional.empty(), BankingOperationAttempt.run("  25.50 ", calls::add));
        assertEquals(List.of(new BigDecimal("25.50")), calls);
    }

    @Test
    void unexpectedFailuresDoNotExposeInternalDetails() {
        Optional<String> rejection = BankingOperationAttempt.run("10.00", amount -> {
            throw new IllegalStateException("SQLITE_BUSY: database is locked");
        });
        Optional<String> bankingFailure = BankingOperationAttempt.run("10.00", amount -> {
            throw new BankingOperationException("", new RuntimeException("SELECT * FROM accounts"));
        });

        assertEquals(Optional.of("Something went wrong. Please try again."), rejection);
        assertEquals(Optional.of("Unable to complete the banking operation. Please try again."), bankingFailure);
    }

    @Test
    void rejectsAMissingOperation() {
        assertThrows(NullPointerException.class, () -> BankingOperationAttempt.run("10.00", null));
    }

    private BigDecimal balanceOf(Account account) {
        return context.getAccountService().getAccountsForUser(session.userId()).stream()
                .filter(a -> a.getId() == account.getId())
                .findFirst()
                .orElseThrow()
                .getBalance();
    }
}
