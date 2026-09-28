package com.manpreet.bank.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Consistent date/time presentation for transaction lists.
 */
public final class DateTimeDisplayFormatter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private DateTimeDisplayFormatter() {
    }

    public static String format(LocalDateTime value) {
        Objects.requireNonNull(value, "value must not be null");
        LocalDate today = LocalDate.now();
        LocalDate date = value.toLocalDate();
        if (date.equals(today)) {
            return "Today, " + TIME.format(value);
        }
        if (date.equals(today.minusDays(1))) {
            return "Yesterday, " + TIME.format(value);
        }
        return FULL.format(value);
    }
}
