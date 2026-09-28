package com.manpreet.bank.exception;

public class AccountAccessException extends RuntimeException {

    public AccountAccessException(String message) {
        super(message);
    }
}
