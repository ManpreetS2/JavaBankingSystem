package com.manpreet.bank.ui;

import java.util.Objects;

/**
 * User-facing labels and confirmation text for theme selection.
 */
public final class ThemeOptions {

    private ThemeOptions() {
    }

    public static String label(Theme theme) {
        Objects.requireNonNull(theme, "theme must not be null");
        return switch (theme) {
            case LIGHT -> "Light";
            case DARK -> "Dark";
        };
    }

    /**
     * Full name announced by assistive technology, since the visible label alone is a single word.
     */
    public static String accessibleName(Theme theme) {
        return label(theme) + " theme";
    }

    public static String appliedMessage(Theme theme) {
        return accessibleName(theme) + " applied.";
    }
}
