package com.manpreet.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ledger entry for an account. Monetary amounts use {@link BigDecimal}.
 */
public class Transaction {

    private long id;
    private long accountId;
    private Long relatedAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(long id,
                       long accountId,
                       Long relatedAccountId,
                       TransactionType transactionType,
                       BigDecimal amount,
                       String description,
                       LocalDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.relatedAccountId = relatedAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public Long getRelatedAccountId() {
        return relatedAccountId;
    }

    public void setRelatedAccountId(Long relatedAccountId) {
        this.relatedAccountId = relatedAccountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Transaction{"
                + "id=" + id
                + ", accountId=" + accountId
                + ", relatedAccountId=" + relatedAccountId
                + ", transactionType=" + transactionType
                + ", amount=" + amount
                + ", description='" + description + '\''
                + ", createdAt=" + createdAt
                + '}';
    }
}
