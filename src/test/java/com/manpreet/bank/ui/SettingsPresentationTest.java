package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.manpreet.bank.session.UserSession;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SettingsPresentationTest {

    @Test
    void profileSummaryUsesSessionIdentityFields() {
        ProfileSummary profile = ProfileSummary.from(
                new UserSession(7L, "ada", "Ada", "Lovelace", "ada@example.com"));

        assertEquals("Ada Lovelace", profile.displayName());
        assertEquals("Ada", profile.firstName());
        assertEquals("Lovelace", profile.lastName());
        assertEquals("ada", profile.username());
        assertEquals("@ada", profile.usernameHandle());
        assertEquals("ada@example.com", profile.email());
    }

    @Test
    void profileSummaryRejectsMissingSession() {
        assertThrows(NullPointerException.class, () -> ProfileSummary.from(null));
    }

    @Test
    void profileSummaryNormalizesWhitespaceFromStoredNames() {
        ProfileSummary profile = ProfileSummary.from(
                new UserSession(1L, " grace ", "  Grace  ", " Brewster   Hopper ", " grace@example.com "));

        assertEquals("Grace Brewster Hopper", profile.displayName());
        assertEquals("Brewster Hopper", profile.lastName());
        assertEquals("grace", profile.username());
        assertEquals("@grace", profile.usernameHandle());
        assertEquals("grace@example.com", profile.email());
    }

    @Test
    void profileSummaryOmitsBlankNamePartsWithoutStraySpaces() {
        assertEquals("Ada", ProfileSummary.from(
                new UserSession(1L, "ada", "Ada", null, "ada@example.com")).displayName());
        assertEquals("Ada", ProfileSummary.from(
                new UserSession(1L, "ada", "Ada", "   ", "ada@example.com")).displayName());
        assertEquals("Lovelace", ProfileSummary.from(
                new UserSession(1L, "ada", "", "Lovelace", "ada@example.com")).displayName());
    }

    @Test
    void profileSummaryFallsBackToUsernameWhenNamesAreMissing() {
        ProfileSummary profile = ProfileSummary.from(
                new UserSession(1L, "ada", null, " ", "ada@example.com"));

        assertEquals("ada", profile.displayName());
        assertEquals("", profile.firstName());
        assertEquals("", profile.lastName());
    }

    @Test
    void profileSummaryNeverRendersNullOrALoneHandleSymbol() {
        ProfileSummary profile = ProfileSummary.from(new UserSession(1L, null, null, null, null));

        assertEquals("", profile.displayName());
        assertEquals("", profile.username());
        assertEquals("", profile.usernameHandle());
        assertEquals("", profile.email());
    }

    @Test
    void profileSummaryPreservesNonAsciiAndPunctuatedNames() {
        ProfileSummary profile = ProfileSummary.from(
                new UserSession(1L, "zoe", "Zoë", "O'Brien-Núñez", "zoe@example.com"));

        assertEquals("Zoë O'Brien-Núñez", profile.displayName());
    }

    @Test
    void profileSummaryExposesOnlyDisplayableIdentityFields() {
        Set<String> components = new HashSet<>();
        for (RecordComponent component : ProfileSummary.class.getRecordComponents()) {
            components.add(component.getName());
        }

        assertEquals(Set.of("displayName", "firstName", "lastName", "username", "email"), components);
        for (String name : components) {
            String lower = name.toLowerCase(Locale.ROOT);
            assertFalse(lower.contains("password") || lower.contains("hash") || lower.contains("id"),
                    "Unexpected sensitive or internal field: " + name);
        }
    }

    @Test
    void everyThemeHasADistinctLabelAndConfirmation() {
        Set<String> labels = new HashSet<>();
        for (Theme theme : Theme.values()) {
            String label = ThemeOptions.label(theme);
            assertFalse(label.isBlank(), "Blank label for " + theme);
            labels.add(label);
            assertEquals(label + " theme", ThemeOptions.accessibleName(theme));
            assertEquals(label + " theme applied.", ThemeOptions.appliedMessage(theme));
        }

        assertEquals(Theme.values().length, labels.size());
        assertEquals("Light", ThemeOptions.label(Theme.LIGHT));
        assertEquals("Dark", ThemeOptions.label(Theme.DARK));
    }

    @Test
    void themeOptionsRejectMissingTheme() {
        assertThrows(NullPointerException.class, () -> ThemeOptions.label(null));
        assertThrows(NullPointerException.class, () -> ThemeOptions.accessibleName(null));
        assertThrows(NullPointerException.class, () -> ThemeOptions.appliedMessage(null));
    }

    @Test
    void profileValuesWrapInsteadOfTruncating() throws IOException {
        // Names may be up to 100 characters and emails up to 254, so these labels must wrap.
        String fxml = Files.readString(Path.of("src/main/resources/fxml/settings.fxml"));
        List<String> wrapped = List.of(
                "displayNameLabel",
                "usernameHandleLabel",
                "firstNameValueLabel",
                "lastNameValueLabel",
                "usernameValueLabel",
                "emailValueLabel"
        );
        for (String id : wrapped) {
            int start = fxml.indexOf("fx:id=\"" + id + "\"");
            assertTrue(start >= 0, "Missing label " + id);
            String element = fxml.substring(fxml.lastIndexOf('<', start), fxml.indexOf("/>", start));
            assertTrue(element.contains("wrapText=\"true\""), id + " must wrap long values");
        }
    }

    @Test
    void settingsControllerStaysOutOfPersistenceAndCredentialCode() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/com/manpreet/bank/controller/SettingsController.java"));
        List<String> forbidden = List.of(
                "java.sql",
                ".repository.",
                ".database.",
                "Repository()",
                "DatabaseManager",
                "PasswordHasher",
                "passwordHash",
                "SELECT ",
                "UPDATE ",
                "INSERT "
        );

        List<String> found = forbidden.stream().filter(source::contains).toList();
        if (!found.isEmpty()) {
            fail("SettingsController must use session and service APIs only, found: "
                    + Arrays.toString(found.toArray()));
        }
    }
}
