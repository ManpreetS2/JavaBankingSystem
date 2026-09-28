package com.manpreet.bank.exception;

public class BankingOperationException extends RuntimeException {

    public BankingOperationException(String message) {
        super(message);
    }

    public BankingOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
