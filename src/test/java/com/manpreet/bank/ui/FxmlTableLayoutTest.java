package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Transaction tables must fit the content area at the 1000px minimum window width, where about 690px
 * are available. Their preferred column widths total 740 to 850px, so each table uses a constrained
 * resize policy, and Amount and Date keep a minimum width while Description absorbs the difference.
 */
class FxmlTableLayoutTest {

    private static final Pattern TABLE_VIEW = Pattern.compile("<TableView\\b[\\s\\S]*?</TableView>");
    private static final Pattern CONSTRAINED_POLICY =
            Pattern.compile("<TableView fx:constant=\"CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS\"/>");

    @Test
    void everyTableFitsItsColumnsToTheAvailableWidth() throws IOException {
        List<String> tables = tablesInScreens();
        assertFalse(tables.isEmpty(), "Expected at least one TableView");
        for (String table : tables) {
            assertTrue(table.contains("<columnResizePolicy>") && CONSTRAINED_POLICY.matcher(table).find(),
                    "TableView must use CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS:\n" + firstLine(table));
        }
    }

    @Test
    void amountAndDateColumnsKeepAMinimumWidth() throws IOException {
        for (String table : tablesInScreens()) {
            assertTrue(columnDeclares(table, "amountColumn", "minWidth=\"120\""),
                    "Amount column needs minWidth=\"120\":\n" + firstLine(table));
            assertTrue(columnDeclares(table, "dateColumn", "minWidth=\"130\""),
                    "Date column needs minWidth=\"130\":\n" + firstLine(table));
        }
    }

    private static List<String> tablesInScreens() throws IOException {
        try (Stream<Path> files = Files.list(Path.of("src/main/resources/fxml"))) {
            return files.filter(path -> path.toString().endsWith(".fxml"))
                    .sorted()
                    .flatMap(path -> {
                        try {
                            Matcher matcher = TABLE_VIEW.matcher(Files.readString(path));
                            return matcher.results().map(result -> path.getFileName() + ": " + result.group());
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .toList();
        }
    }

    private static boolean columnDeclares(String table, String columnId, String attribute) {
        Matcher column = Pattern.compile("<TableColumn fx:id=\"" + columnId + "\"[^>]*>").matcher(table);
        return column.find() && column.group().contains(attribute);
    }

    private static String firstLine(String table) {
        int end = table.indexOf('\n');
        return end < 0 ? table : table.substring(0, end);
    }
}
