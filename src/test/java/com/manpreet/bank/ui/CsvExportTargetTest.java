package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvExportTargetTest {

    @TempDir
    Path dir;

    @Test
    void appendsTheExtensionInTheSameDirectory() {
        assertEquals(dir.resolve("transactions.csv"), CsvExportTarget.withCsvExtension(dir.resolve("transactions")));
        assertEquals(dir.resolve("report.txt.csv"), CsvExportTarget.withCsvExtension(dir.resolve("report.txt")));
        assertEquals(dir.resolve("my export.csv"), CsvExportTarget.withCsvExtension(dir.resolve("my export")));
    }

    @Test
    void keepsNamesThatAlreadyEndInCsvInAnyCase() {
        Path lower = dir.resolve("transactions.csv");
        Path upper = dir.resolve("TRANSACTIONS.CSV");
        Path mixed = dir.resolve("Transactions.Csv");

        assertEquals(lower, CsvExportTarget.withCsvExtension(lower));
        assertEquals(upper, CsvExportTarget.withCsvExtension(upper));
        assertEquals(mixed, CsvExportTarget.withCsvExtension(mixed));
    }

    @Test
    void confirmsWhenTheAppendedNameReplacesAnExistingFile() throws IOException {
        Path chosen = dir.resolve("transactions");
        Path target = CsvExportTarget.withCsvExtension(chosen);
        Files.writeString(target, "existing");

        assertTrue(CsvExportTarget.requiresOverwriteConfirmation(chosen, target));
    }

    @Test
    void doesNotConfirmWhenTheAppendedNameIsNew() {
        Path chosen = dir.resolve("transactions");

        assertFalse(CsvExportTarget.requiresOverwriteConfirmation(chosen, CsvExportTarget.withCsvExtension(chosen)));
    }

    @Test
    void leavesOverwriteOfTheTypedNameToTheSaveDialog() throws IOException {
        // The save dialog already asked before returning a name that exists.
        Path chosen = dir.resolve("transactions.csv");
        Files.writeString(chosen, "existing");

        assertFalse(CsvExportTarget.requiresOverwriteConfirmation(chosen, CsvExportTarget.withCsvExtension(chosen)));
    }

    @Test
    void confirmsEvenWhenOnlyTheTypedNameIsMissing() throws IOException {
        // "transactions" does not exist, but "transactions.csv" does; the dialog never saw that name.
        Path chosen = dir.resolve("transactions");
        Files.writeString(dir.resolve("transactions.csv"), "existing");

        assertFalse(Files.exists(chosen));
        assertTrue(CsvExportTarget.requiresOverwriteConfirmation(chosen, CsvExportTarget.withCsvExtension(chosen)));
    }

    @Test
    void rejectsMissingPaths() {
        assertThrows(NullPointerException.class, () -> CsvExportTarget.withCsvExtension(null));
        assertThrows(NullPointerException.class, () -> CsvExportTarget.requiresOverwriteConfirmation(null, dir));
        assertThrows(NullPointerException.class, () -> CsvExportTarget.requiresOverwriteConfirmation(dir, null));
    }
}
