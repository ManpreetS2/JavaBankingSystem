package com.manpreet.bank.util;

import com.manpreet.bank.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * USD money helpers. Domain amounts use {@link BigDecimal} with a fixed scale of 2.
 */
public final class MoneyUtil {

    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY);

    /**
     * Largest amount accepted for a single deposit, withdrawal, or transfer.
     */
    public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("1000000.00");

    private MoneyUtil() {
    }

    public static BigDecimal requireValidAmount(BigDecimal amount, String fieldName) {
        Objects.requireNonNull(amount, fieldName + " must not be null");
        if (amount.scale() > SCALE) {
            throw new ValidationException(fieldName + " cannot have more than " + SCALE + " decimal places");
        }
        return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal requirePositiveAmount(BigDecimal amount, String fieldName) {
        BigDecimal normalized = requireValidAmount(amount, fieldName);
        if (normalized.compareTo(ZERO) <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero");
        }
        return normalized;
    }

    /**
     * Validates a deposit, withdrawal, or transfer amount: positive, at most two decimal places,
     * and no larger than {@link #MAX_TRANSACTION_AMOUNT}.
     */
    public static BigDecimal requireTransactionAmount(BigDecimal amount, String fieldName) {
        BigDecimal normalized = requirePositiveAmount(amount, fieldName);
        if (normalized.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new ValidationException(
                    fieldName + " cannot exceed " + CurrencyFormatter.format(MAX_TRANSACTION_AMOUNT));
        }
        return normalized;
    }

    public static boolean isZero(BigDecimal amount) {
        return requireValidAmount(amount, "amount").compareTo(ZERO) == 0;
    }

    public static int compare(BigDecimal left, BigDecimal right) {
        return requireValidAmount(left, "left").compareTo(requireValidAmount(right, "right"));
    }

    public static String toStorageString(BigDecimal amount) {
        return requireValidAmount(amount, "amount").toPlainString();
    }

    public static BigDecimal fromStorageString(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Stored monetary value is missing");
        }
        try {
            return requireValidAmount(new BigDecimal(value), "stored amount");
        } catch (NumberFormatException e) {
            throw new ValidationException("Stored monetary value is invalid");
        }
    }
}
