package com.manpreet.bank.ui;

import com.manpreet.bank.exception.AccountAccessException;
import com.manpreet.bank.exception.AuthenticationException;
import com.manpreet.bank.exception.BankingOperationException;
import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.exception.EntityNotFoundException;
import com.manpreet.bank.exception.InsufficientFundsException;
import com.manpreet.bank.exception.ValidationException;

/**
 * Maps domain exceptions to safe, user-facing messages.
 */
public final class UiErrorMapper {

    private UiErrorMapper() {
    }

    public static String toUserMessage(Throwable error) {
        if (error == null) {
            return "Something went wrong. Please try again.";
        }
        if (error instanceof ValidationException
                || error instanceof DuplicateUserException
                || error instanceof AuthenticationException
                || error instanceof InsufficientFundsException
                || error instanceof AccountAccessException
                || error instanceof EntityNotFoundException) {
            return error.getMessage();
        }
        if (error instanceof BankingOperationException) {
            return error.getMessage() == null || error.getMessage().isBlank()
                    ? "Unable to complete the banking operation. Please try again."
                    : error.getMessage();
        }
        return "Something went wrong. Please try again.";
    }
}
