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

class MessageTextTest {

    private static final Pattern TEXT_ATTRIBUTE = Pattern.compile("(?:text|promptText)=\"([^\"]*)\"");

    @Test
    void addsATerminalPeriodToServiceMessages() {
        assertEquals("Email format is invalid.", MessageText.asSentence("Email format is invalid"));
        assertEquals("Invalid username/email or password.",
                MessageText.asSentence("Invalid username/email or password"));
    }

    @Test
    void keepsExistingTerminalPunctuation() {
        assertEquals("Enter an amount.", MessageText.asSentence("Enter an amount."));
        assertEquals("Are you sure?", MessageText.asSentence("Are you sure?"));
        assertEquals("Done!", MessageText.asSentence("Done!"));
        assertEquals("Wait...", MessageText.asSentence("Wait..."));
    }

    @Test
    void trimsSurroundingWhitespaceBeforePunctuating() {
        assertEquals("Username is required.", MessageText.asSentence("  Username is required \n"));
        assertEquals("Saved.", MessageText.asSentence(" Saved. "));
    }

    @Test
    void returnsEmptyTextForMissingMessages() {
        assertEquals("", MessageText.asSentence(null));
        assertEquals("", MessageText.asSentence(""));
        assertEquals("", MessageText.asSentence("   "));
    }

    @Test
    void everyAuthFormValidationMessageRendersAsASentence() {
        List<String> messages = new ArrayList<>();
        AuthFormValidator.validateLogin("", "").ifPresent(e -> messages.add(e.message()));
        AuthFormValidator.validateLogin("ada", "").ifPresent(e -> messages.add(e.message()));
        AuthFormValidator.validateRegistration("", "", "", "", "", "").ifPresent(e -> messages.add(e.message()));
        AuthFormValidator.validateRegistration("Ada", "Lovelace", "ada@example.com", "ada", "x".repeat(12), "y")
                .ifPresent(e -> messages.add(e.message()));

        assertEquals(4, messages.size());
        for (String message : messages) {
            String sentence = MessageText.asSentence(message);
            assertTrue(sentence.endsWith("."), "Not a sentence: " + sentence);
            assertTrue(!sentence.endsWith(".."), "Double period: " + sentence);
        }
    }

    @Test
    void authScreensUseSignInWording() throws IOException {
        Pattern logIn = Pattern.compile("(?i)\\blog ?in\\b");
        List<String> violations = new ArrayList<>();
        for (String file : List.of("login.fxml", "register.fxml")) {
            String content = Files.readString(Path.of("src/main/resources/fxml", file));
            Matcher matcher = TEXT_ATTRIBUTE.matcher(content);
            while (matcher.find()) {
                if (logIn.matcher(matcher.group(1)).find()) {
                    violations.add(file + ": \"" + matcher.group(1) + "\"");
                }
            }
        }
        assertTrue(violations.isEmpty(), "Use \"Sign in\" wording on auth screens: " + violations);
    }
}
