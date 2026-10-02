package com.manpreet.bank.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Resolves the file a CSV export writes to from the path chosen in the save dialog.
 */
public final class CsvExportTarget {

    private static final String EXTENSION = ".csv";

    private CsvExportTarget() {
    }

    /**
     * Returns the chosen path, with {@code .csv} appended when the name does not already end with it.
     */
    public static Path withCsvExtension(Path chosen) {
        Objects.requireNonNull(chosen, "chosen must not be null");
        String name = chosen.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) {
            return chosen;
        }
        return chosen.resolveSibling(name + EXTENSION);
    }

    /**
     * True when the extension was appended and the resulting file already exists.
     * The save dialog only confirmed overwriting the name that was typed, so this replacement still needs confirmation.
     */
    public static boolean requiresOverwriteConfirmation(Path chosen, Path target) {
        Objects.requireNonNull(chosen, "chosen must not be null");
        Objects.requireNonNull(target, "target must not be null");
        return !target.equals(chosen) && Files.exists(target);
    }
}
