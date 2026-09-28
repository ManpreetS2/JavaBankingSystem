package com.manpreet.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bank account owned by a user. Monetary amounts use {@link BigDecimal}.
 */
public class Account {

    private long id;
    private long userId;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public Account() {
    }

    public Account(long id,
                   long userId,
                   String accountNumber,
                   AccountType accountType,
                   BigDecimal balance,
                   LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Account{"
                + "id=" + id
                + ", userId=" + userId
                + ", accountNumber='" + accountNumber + '\''
                + ", accountType=" + accountType
                + ", balance=" + balance
                + ", createdAt=" + createdAt
                + '}';
    }
}
