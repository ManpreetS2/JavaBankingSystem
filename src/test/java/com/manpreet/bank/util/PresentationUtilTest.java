package com.manpreet.bank.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PresentationUtilTest {

    @Test
    void currencyFormatterUsesUsdGrouping() {
        assertEquals("$0.00", CurrencyFormatter.format(new BigDecimal("0")));
        assertEquals("$1,400.00", CurrencyFormatter.format(new BigDecimal("1400")));
        assertEquals("$2,000.50", CurrencyFormatter.format(new BigDecimal("2000.5")));
    }

    @Test
    void accountNumberMaskKeepsLastFourDigits() {
        assertEquals("•••• 7890", AccountNumberFormatter.mask("CHK-1234567890"));
        assertEquals("Checking •••• 7890",
                AccountNumberFormatter.displayLabel(AccountType.CHECKING, "CHK-1234567890"));
    }

    @Test
    void dateTimeFormatterHandlesRelativeDays() {
        LocalDateTime today = LocalDate.now().atTime(13, 42);
        assertTrue(DateTimeDisplayFormatter.format(today).startsWith("Today"));
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atTime(16, 15);
        assertTrue(DateTimeDisplayFormatter.format(yesterday).startsWith("Yesterday"));
    }

    @Test
    void transactionPresentationSignsByType() {
        Transaction deposit = new Transaction(
                1L, 1L, null, TransactionType.DEPOSIT, new BigDecimal("10.00"), "x", LocalDateTime.now()
        );
        Transaction withdrawal = new Transaction(
                2L, 1L, null, TransactionType.WITHDRAWAL, new BigDecimal("10.00"), "y", LocalDateTime.now()
        );
        assertEquals("+$10.00", TransactionPresentation.signedAmount(deposit));
        assertEquals("-$10.00", TransactionPresentation.signedAmount(withdrawal));
    }
}
