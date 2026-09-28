package com.manpreet.bank.service;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;

public record TransferResult(
        Account sourceAccount,
        Account destinationAccount,
        Transaction outgoingTransaction,
        Transaction incomingTransaction
) {
}
