package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * A default button fires on Enter anywhere in its scene, so only screens with a form to submit declare one.
 * Without this, pressing Enter on the Dashboard opened the Deposit dialog.
 */
class FxmlDefaultButtonTest {

    private static final Pattern DEFAULT_BUTTON = Pattern.compile("defaultButton\\s*=\\s*\"true\"");

    private static final Map<String, Integer> EXPECTED_DEFAULT_BUTTONS = Map.of(
            "login.fxml", 1,
            "register.fxml", 1,
            "transactions.fxml", 1,
            "dashboard.fxml", 0,
            "accounts.fxml", 0,
            "settings.fxml", 0,
            "main-shell.fxml", 0
    );

    @Test
    void onlyFormScreensDeclareADefaultButton() throws IOException {
        for (Map.Entry<String, Integer> expected : EXPECTED_DEFAULT_BUTTONS.entrySet()) {
            String content = Files.readString(Path.of("src/main/resources/fxml", expected.getKey()));
            assertEquals(expected.getValue(), countDefaultButtons(content),
                    expected.getKey() + " default button count");
        }
    }

    @Test
    void everyScreenIsCoveredByTheExpectation() throws IOException {
        try (var files = Files.list(Path.of("src/main/resources/fxml"))) {
            long screens = files.filter(path -> path.toString().endsWith(".fxml")).count();
            assertEquals(EXPECTED_DEFAULT_BUTTONS.size(), screens,
                    "Add new FXML screens to EXPECTED_DEFAULT_BUTTONS");
        }
    }

    private static int countDefaultButtons(String content) {
        Matcher matcher = DEFAULT_BUTTON.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
