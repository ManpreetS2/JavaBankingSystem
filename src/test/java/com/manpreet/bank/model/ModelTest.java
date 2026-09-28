package com.manpreet.bank.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ModelTest {

    @Test
    void userConstructionStoresExpectedFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 12, 0);

        User user = new User(
                1L,
                "Manpreet",
                "Singh",
                "manpreet@example.com",
                "manpreet",
                "hashed-password-value",
                createdAt
        );

        assertEquals(1L, user.getId());
        assertEquals("Manpreet", user.getFirstName());
        assertEquals("Singh", user.getLastName());
        assertEquals("manpreet@example.com", user.getEmail());
        assertEquals("manpreet", user.getUsername());
        assertEquals("hashed-password-value", user.getPasswordHash());
        assertEquals(createdAt, user.getCreatedAt());
    }

    @Test
    void userToStringDoesNotIncludePasswordHash() {
        User user = new User(
                1L,
                "Manpreet",
                "Singh",
                "manpreet@example.com",
                "manpreet",
                "super-secret-hash",
                LocalDateTime.of(2026, 9, 27, 12, 0)
        );

        String text = user.toString();
        assertFalse(text.contains("super-secret-hash"));
        assertFalse(text.toLowerCase().contains("password"));
    }

    @Test
    void transientUsersAreNotEqualByDefaultIdentity() {
        User first = new User();
        User second = new User();

        assertNotEquals(first, second);
    }

    @Test
    void accountUsesBigDecimalBalanceAndAccountType() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 12, 0);
        BigDecimal balance = new BigDecimal("1500.75");

        Account account = new Account(
                10L,
                1L,
                "CHK-10001",
                AccountType.CHECKING,
                balance,
                createdAt
        );

        assertEquals(10L, account.getId());
        assertEquals(1L, account.getUserId());
        assertEquals("CHK-10001", account.getAccountNumber());
        assertEquals(AccountType.CHECKING, account.getAccountType());
        assertEquals(0, balance.compareTo(account.getBalance()));
        assertEquals(createdAt, account.getCreatedAt());
        assertEquals(BigDecimal.class, account.getBalance().getClass());
    }

    @Test
    void transactionSupportsOptionalRelatedAccount() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 12, 0);

        Transaction deposit = new Transaction(
                100L,
                10L,
                null,
                TransactionType.DEPOSIT,
                new BigDecimal("50.00"),
                "Initial deposit",
                createdAt
        );

        Transaction transferOut = new Transaction(
                101L,
                10L,
                20L,
                TransactionType.TRANSFER_OUT,
                new BigDecimal("25.50"),
                "Transfer to savings",
                createdAt
        );

        assertNull(deposit.getRelatedAccountId());
        assertEquals(TransactionType.DEPOSIT, deposit.getTransactionType());
        assertEquals(0, new BigDecimal("50.00").compareTo(deposit.getAmount()));

        assertEquals(20L, transferOut.getRelatedAccountId());
        assertEquals(TransactionType.TRANSFER_OUT, transferOut.getTransactionType());
        assertEquals(AccountType.SAVINGS, AccountType.valueOf("SAVINGS"));
        assertEquals(TransactionType.TRANSFER_IN, TransactionType.valueOf("TRANSFER_IN"));
        assertEquals(TransactionType.WITHDRAWAL, TransactionType.valueOf("WITHDRAWAL"));
    }
}
