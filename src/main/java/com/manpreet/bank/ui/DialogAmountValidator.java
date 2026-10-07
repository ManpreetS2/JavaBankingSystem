package com.manpreet.bank.ui;

import com.manpreet.bank.util.CurrencyFormatter;
import com.manpreet.bank.util.MoneyUtil;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lightweight UI-side amount checks for banking dialogs.
 * Service-layer validation remains authoritative.
 */
public final class DialogAmountValidator {

    /**
     * Plain decimal entry only: ASCII digits with an optional decimal point and leading minus sign.
     * Rejects exponent notation such as {@code 1E+15}, which {@link BigDecimal} would otherwise accept.
     */
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)");

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
        if (!PLAIN_DECIMAL.matcher(trimmed).matches()) {
            return Optional.of("Enter a valid amount such as 25.00");
        }
        final BigDecimal amount;
        try {
            amount = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            return Optional.of("Enter a valid amount such as 25.00");
        }
        if (amount.scale() > MoneyUtil.SCALE) {
            return Optional.of("Amount cannot have more than 2 decimal places.");
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.of("Enter an amount greater than $0.00.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of("Amount cannot be negative.");
        }
        if (amount.compareTo(MoneyUtil.MAX_TRANSACTION_AMOUNT) > 0) {
            return Optional.of("Amount cannot exceed " + CurrencyFormatter.format(MoneyUtil.MAX_TRANSACTION_AMOUNT) + ".");
        }
        return Optional.empty();
    }

    public static boolean isReady(String amountText) {
        return validate(amountText).isEmpty();
    }
}
