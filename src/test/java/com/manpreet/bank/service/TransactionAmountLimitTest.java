package com.manpreet.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.support.TestAppContext;
import com.manpreet.bank.ui.BankingOperationAttempt;
import com.manpreet.bank.ui.DialogAmountValidator;
import com.manpreet.bank.util.MoneyUtil;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransactionAmountLimitTest {

    private static final String INVALID = "Enter a valid amount such as 25.00";
    private static final String TOO_LARGE = "Amount cannot exceed $1,000,000.00.";

    @TempDir
    Path tempDir;

    private AppContext context;
    private UserSession session;
    private Account checking;
    private Account savings;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("limits.db"));
        session = context.getAuthService().register("Ada", "Lovelace", "ada@example.com", "ada", "correct-horse-battery");
        List<Account> accounts = context.getAccountService().getAccountsForUser(session.userId());
        checking = accounts.stream().filter(a -> a.getAccountType() == AccountType.CHECKING).findFirst().orElseThrow();
        savings = accounts.stream().filter(a -> a.getAccountType() == AccountType.SAVINGS).findFirst().orElseThrow();
    }

    @Test
    void dialogRejectsNotationThatBigDecimalWouldAccept() {
        for (String amount : List.of("1e3", "1E3", "1E+12", "1E+15", "2.5e-1", "+25", "0x10", "١٢", "1_000", "1 000",
                "1,000", "$25", "25-", "--5", ".", "-")) {
            assertEquals(Optional.of(INVALID), DialogAmountValidator.validate(amount), "amount \"" + amount + "\"");
        }
    }

    @Test
    void dialogAcceptsPlainDecimalsUpToTheMaximum() {
        for (String amount : List.of("25", "25.", ".5", "0.01", " 40 ", "999999.99", "1000000", "1000000.00")) {
            assertEquals(Optional.empty(), DialogAmountValidator.validate(amount), "amount \"" + amount + "\"");
        }
    }

    @Test
    void dialogRejectsAmountsAboveTheMaximum() {
        assertEquals(Optional.of(TOO_LARGE), DialogAmountValidator.validate("1000000.01"));
        assertEquals(Optional.of(TOO_LARGE), DialogAmountValidator.validate("1000001"));
        assertEquals(Optional.of(TOO_LARGE), DialogAmountValidator.validate("99999999999999999999"));
    }

    @Test
    void dialogKeepsExistingMessagesForSignAndPrecision() {
        assertEquals(Optional.of("Amount cannot be negative."), DialogAmountValidator.validate("-5"));
        assertEquals(Optional.of("Enter an amount greater than $0.00."), DialogAmountValidator.validate("0.00"));
        assertEquals(Optional.of("Amount cannot have more than 2 decimal places."),
                DialogAmountValidator.validate("1.0000"));
    }

    @Test
    void serviceAcceptsTheMaximumForEveryOperation() {
        BigDecimal max = MoneyUtil.MAX_TRANSACTION_AMOUNT;
        context.getAccountService().deposit(session.userId(), checking.getId(), max, "Large deposit");
        context.getAccountService().deposit(session.userId(), checking.getId(), max, "Second large deposit");
        context.getAccountService().transfer(session.userId(), checking.getId(), savings.getId(), max, "Large transfer");
        context.getAccountService().withdraw(session.userId(), savings.getId(), max, "Large withdrawal");

        assertEquals(new BigDecimal("1000000.00"), balanceOf(checking));
        assertEquals(new BigDecimal("0.00"), balanceOf(savings));
    }

    @Test
    void serviceRejectsAmountsAboveTheMaximumWithoutChangingBalances() {
        BigDecimal max = MoneyUtil.MAX_TRANSACTION_AMOUNT;
        BigDecimal overMax = new BigDecimal("1000000.01");
        context.getAccountService().deposit(session.userId(), checking.getId(), max, "Funding");
        context.getAccountService().deposit(session.userId(), checking.getId(), max, "Funding");

        ValidationException deposit = assertThrows(ValidationException.class, () -> context.getAccountService()
                .deposit(session.userId(), checking.getId(), overMax, "Too large"));
        ValidationException withdrawal = assertThrows(ValidationException.class, () -> context.getAccountService()
                .withdraw(session.userId(), checking.getId(), overMax, "Too large"));
        ValidationException transfer = assertThrows(ValidationException.class, () -> context.getAccountService()
                .transfer(session.userId(), checking.getId(), savings.getId(), overMax, "Too large"));
        assertThrows(ValidationException.class, () -> context.getAccountService()
                .deposit(session.userId(), checking.getId(), new BigDecimal("1E+15"), "Exponent"));

        assertEquals("Deposit amount cannot exceed $1,000,000.00", deposit.getMessage());
        assertEquals("Withdrawal amount cannot exceed $1,000,000.00", withdrawal.getMessage());
        assertEquals("Transfer amount cannot exceed $1,000,000.00", transfer.getMessage());
        assertEquals(new BigDecimal("2000000.00"), balanceOf(checking));
        assertEquals(new BigDecimal("0.00"), balanceOf(savings));
    }

    @Test
    void dialogSubmissionStopsOversizedAndExponentAmountsBeforeTheService() {
        List<BigDecimal> calls = new ArrayList<>();

        assertEquals(Optional.of(TOO_LARGE), BankingOperationAttempt.run("1000000.01", calls::add));
        assertEquals(Optional.of(INVALID), BankingOperationAttempt.run("1E+15", calls::add));
        assertTrue(calls.isEmpty());
    }

    private BigDecimal balanceOf(Account account) {
        return context.getAccountService().getAccountsForUser(session.userId()).stream()
                .filter(a -> a.getId() == account.getId())
                .findFirst()
                .orElseThrow()
                .getBalance();
    }
}
