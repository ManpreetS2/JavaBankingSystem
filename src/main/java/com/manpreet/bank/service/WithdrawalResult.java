package com.manpreet.bank.service;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;

public record WithdrawalResult(Account account, Transaction transaction) {
}
