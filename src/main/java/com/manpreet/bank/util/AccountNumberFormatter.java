package com.manpreet.bank.util;

import com.manpreet.bank.model.AccountType;
import java.util.Objects;

/**
 * Masks simulated account numbers for UI display.
 */
public final class AccountNumberFormatter {

    private AccountNumberFormatter() {
    }

    public static String mask(String accountNumber) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        String digits = accountNumber.replaceAll("\\D", "");
        String lastFour = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
        return "•••• " + lastFour;
    }

    public static String displayLabel(AccountType accountType, String accountNumber) {
        Objects.requireNonNull(accountType, "accountType must not be null");
        String typeLabel = switch (accountType) {
            case CHECKING -> "Checking";
            case SAVINGS -> "Savings";
        };
        return typeLabel + " " + mask(accountNumber);
    }
}
