package com.manpreet.bank.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Consistent USD currency presentation for the UI.
 */
public final class CurrencyFormatter {

    private static final NumberFormat USD = NumberFormat.getCurrencyInstance(Locale.US);

    private CurrencyFormatter() {
    }

    public static String format(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        return USD.format(amount);
    }

    public static String formatSigned(BigDecimal amount, boolean credit) {
        String formatted = format(amount.abs());
        return credit ? "+" + formatted : "-" + formatted;
    }
}
