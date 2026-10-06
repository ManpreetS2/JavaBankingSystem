package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Controls whose default styling ignores the theme tokens must be styled by both theme stylesheets.
 * Without these rules the dark theme showed light table headers, white date fields, and a light calendar.
 */
class ThemeStylesheetCoverageTest {

    private static final List<String> REQUIRED_SELECTORS = List.of(
            ".transactions-table .column-header,",
            ".transactions-table .table-row-cell {",
            ".transactions-table .table-row-cell:odd {",
            ".transactions-table .table-row-cell:selected {",
            ".date-picker > .text-field {",
            ".date-picker > .arrow-button > .arrow {",
            ".date-picker-popup {",
            ".date-picker-popup .calendar-grid {",
            ".date-picker-popup .day-cell {",
            ".date-picker-popup .day-cell:selected {"
    );

    @Test
    void bothThemesStyleTableHeadersAndDatePickers() throws IOException {
        for (String theme : List.of("theme-light.css", "theme-dark.css")) {
            String css = Files.readString(Path.of("src/main/resources/css", theme));
            for (String selector : REQUIRED_SELECTORS) {
                assertTrue(css.contains(selector), theme + " is missing " + selector);
            }
        }
    }
}
