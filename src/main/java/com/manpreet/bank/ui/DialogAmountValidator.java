package com.manpreet.bank.ui;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lightweight UI-side amount checks for banking dialogs.
 * Service-layer validation remains authoritative.
 */
public final class DialogAmountValidator {

    private DialogAmountValidator() {
    }

    /**
     * @return empty when the amount text is acceptable for submission; otherwise a safe message
     */
    public static Optional<String> validate(String amountText) {
        if (amountText == null || amountText.isBlank()) {
            return Optional.of("Enter an amount.");
        }
        String trimmed = amountText.trim();
        final BigDecimal amount;
        try {
            amount = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            return Optional.of("Enter a valid amount such as 25.00");
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.of("Amount must be greater than zero.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of("Amount cannot be negative.");
        }
        return Optional.empty();
    }

    public static boolean isReady(String amountText) {
        return validate(amountText).isEmpty();
    }
}
