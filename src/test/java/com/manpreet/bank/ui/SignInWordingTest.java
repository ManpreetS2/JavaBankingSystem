package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The app uses "Sign in", "Sign out", and "Create account" wording; this keeps the shell and window
 * titles consistent with the sign-in and registration screens.
 */
class SignInWordingTest {

    private static final Pattern LOG_IN_OR_OUT = Pattern.compile("(?i)\\blog ?(in|out)\\b");
    private static final Pattern TEXT_ATTRIBUTE = Pattern.compile("text=\"([^\"]*)\"");
    private static final Pattern WINDOW_TITLE = Pattern.compile("AppInfo\\.windowTitle\\(\"([^\"]*)\"\\)");

    @Test
    void shellUsesSignOutWording() throws IOException {
        String shell = Files.readString(Path.of("src/main/resources/fxml/main-shell.fxml"));

        assertTrue(shell.contains("text=\"Sign out\""), "Sidebar should offer Sign out");
        assertEquals(List.of(), matchingTexts(shell));
    }

    @Test
    void windowTitlesUseSignInWording() throws IOException {
        String sceneManager = Files.readString(Path.of("src/main/java/com/manpreet/bank/ui/SceneManager.java"));
        List<String> titles = new ArrayList<>();
        Matcher matcher = WINDOW_TITLE.matcher(sceneManager);
        while (matcher.find()) {
            titles.add(matcher.group(1));
        }

        assertEquals(List.of("Sign in", "Create account"), titles);
    }

    private static List<String> matchingTexts(String fxml) {
        List<String> found = new ArrayList<>();
        Matcher matcher = TEXT_ATTRIBUTE.matcher(fxml);
        while (matcher.find()) {
            if (LOG_IN_OR_OUT.matcher(matcher.group(1)).find()) {
                found.add(matcher.group(1));
            }
        }
        return found;
    }
}
