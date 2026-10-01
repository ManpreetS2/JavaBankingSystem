package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.service.AuthService;
import com.manpreet.bank.support.TestAppContext;
import com.manpreet.bank.ui.AuthFormValidator.Field;
import com.manpreet.bank.ui.AuthFormValidator.FieldError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthFormValidatorTest {

    private static final String VALID_PASSWORD = "correct-horse-battery";

    @TempDir
    Path tempDir;

    @Test
    void loginRequiresIdentifierBeforePassword() {
        FieldError expected = new FieldError(Field.USERNAME_OR_EMAIL, "Username or email is required");

        assertEquals(Optional.of(expected), AuthFormValidator.validateLogin(null, null));
        assertEquals(Optional.of(expected), AuthFormValidator.validateLogin("", VALID_PASSWORD));
        assertEquals(Optional.of(expected), AuthFormValidator.validateLogin("   ", ""));
    }

    @Test
    void loginRequiresANonEmptyPassword() {
        FieldError expected = new FieldError(Field.PASSWORD, AuthFormValidator.PASSWORD_REQUIRED);

        assertEquals(Optional.of(expected), AuthFormValidator.validateLogin("ada", null));
        assertEquals(Optional.of(expected), AuthFormValidator.validateLogin("ada", ""));
    }

    @Test
    void loginTreatsWhitespacePasswordsAsRealPasswords() {
        // Registration accepts any 12+ characters, including spaces, so the form must not reject them.
        assertEquals(Optional.empty(), AuthFormValidator.validateLogin("ada", " ".repeat(12)));
        assertEquals(Optional.empty(), AuthFormValidator.validateLogin(" ada@example.com ", "x"));
    }

    @Test
    void registrationReportsTheFirstInvalidFieldInScreenOrder() {
        assertField(Field.FIRST_NAME, register("", "", "", "", "", "x"));
        assertField(Field.LAST_NAME, register("Ada", "", "", "", "", "x"));
        assertField(Field.EMAIL, register("Ada", "Lovelace", "", "", "", "x"));
        assertField(Field.USERNAME, register("Ada", "Lovelace", "ada@example.com", "", "", "x"));
        assertField(Field.PASSWORD, register("Ada", "Lovelace", "ada@example.com", "ada", "", "x"));
        assertField(Field.CONFIRM_PASSWORD,
                register("Ada", "Lovelace", "ada@example.com", "ada", VALID_PASSWORD, "x"));
        assertEquals(Optional.empty(),
                register("Ada", "Lovelace", "ada@example.com", "ada", VALID_PASSWORD, VALID_PASSWORD));
    }

    @Test
    void registrationHandlesNullFieldsWithoutThrowing() {
        assertField(Field.FIRST_NAME, register(null, null, null, null, null, null));
        assertField(Field.PASSWORD, register("Ada", "Lovelace", "ada@example.com", "ada", null, null));
    }

    @Test
    void passwordPolicyIsReportedBeforeAMismatch() {
        assertEquals(Optional.of(new FieldError(Field.PASSWORD, "Password must be at least 12 characters")),
                register("Ada", "Lovelace", "ada@example.com", "ada", "short", "different"));
    }

    @Test
    void confirmationMustMatchExactlyWithoutTrimming() {
        FieldError mismatch = new FieldError(Field.CONFIRM_PASSWORD, AuthFormValidator.PASSWORD_MISMATCH);

        assertEquals(Optional.of(mismatch),
                register("Ada", "Lovelace", "ada@example.com", "ada", VALID_PASSWORD, VALID_PASSWORD + " "));
        assertEquals(Optional.of(mismatch),
                register("Ada", "Lovelace", "ada@example.com", "ada", VALID_PASSWORD, VALID_PASSWORD.toUpperCase()));
        assertEquals(Optional.of(mismatch),
                register("Ada", "Lovelace", "ada@example.com", "ada", VALID_PASSWORD, null));
    }

    @Test
    void registrationAcceptsInputTheServiceNormalizes() {
        assertEquals(Optional.empty(), register(
                "  Ada  ", " Lovelace ", " ADA@Example.COM ", " Ada.Lovelace ", VALID_PASSWORD, VALID_PASSWORD));
        assertEquals(Optional.empty(), register(
                "Zoë", "O'Brien-Núñez", "zoe@example.com", "zoe_o-b", VALID_PASSWORD, VALID_PASSWORD));
    }

    @Test
    void registrationBoundariesMatchTheGuidanceShownOnTheForm() throws IOException {
        String registerFxml = Files.readString(Path.of("src/main/resources/fxml/register.fxml"));
        assertTrue(registerFxml.contains("Use at least 12 characters"));
        assertTrue(registerFxml.contains("3 to 30 characters: letters, numbers, dot, underscore, or hyphen"));

        assertField(Field.PASSWORD, withPassword("x".repeat(11)));
        assertEquals(Optional.empty(), withPassword("x".repeat(12)));
        assertEquals(Optional.empty(), withPassword("x".repeat(128)));
        assertField(Field.PASSWORD, withPassword("x".repeat(129)));

        assertField(Field.USERNAME, withUsername("ab"));
        assertEquals(Optional.empty(), withUsername("abc"));
        assertEquals(Optional.empty(), withUsername("a".repeat(30)));
        assertField(Field.USERNAME, withUsername("a".repeat(31)));
        assertEquals(Optional.empty(), withUsername("a.b_c-d"));
        assertField(Field.USERNAME, withUsername("ada lovelace"));
        assertField(Field.USERNAME, withUsername("ada+1"));
    }

    @Test
    void formAndServiceAgreeOnEveryRejectionAndItsMessage() {
        AuthService authService = TestAppContext.create(tempDir.resolve("agree.db")).getAuthService();
        List<List<String>> rejected = List.of(
                List.of("", "Lovelace", "a1@example.com", "agree1", VALID_PASSWORD),
                List.of("R2D2", "Lovelace", "a2@example.com", "agree2", VALID_PASSWORD),
                List.of("Ada", "   ", "a3@example.com", "agree3", VALID_PASSWORD),
                List.of("Ada", "Lovelace", "not-an-email", "agree4", VALID_PASSWORD),
                List.of("Ada", "Lovelace", "a5@example", "agree5", VALID_PASSWORD),
                List.of("Ada", "Lovelace", "a6@example.com", "ab", VALID_PASSWORD),
                List.of("Ada", "Lovelace", "a7@example.com", "bad name", VALID_PASSWORD),
                List.of("Ada", "Lovelace", "a8@example.com", "agree8", "x".repeat(11)),
                List.of("Ada", "Lovelace", "a9@example.com", "agree9", "x".repeat(129))
        );

        for (List<String> input : rejected) {
            Optional<FieldError> formError = register(
                    input.get(0), input.get(1), input.get(2), input.get(3), input.get(4), input.get(4));
            assertTrue(formError.isPresent(), "Form accepted input the service rejects: " + input);
            ValidationException serviceError = assertThrows(ValidationException.class, () -> authService.register(
                    input.get(0), input.get(1), input.get(2), input.get(3), input.get(4)));
            assertEquals(serviceError.getMessage(), formError.get().message(), "Message drift for " + input);
        }
    }

    @Test
    void formAndServiceAgreeOnAcceptedInput() {
        AuthService authService = TestAppContext.create(tempDir.resolve("accept.db")).getAuthService();
        List<List<String>> accepted = List.of(
                List.of("Ada", "Lovelace", "ada@example.com", "ada", VALID_PASSWORD),
                List.of(" Grace ", " Hopper ", " GRACE@Example.com ", " Grace.H ", " ".repeat(12)),
                List.of("Zoë", "O'Brien-Núñez", "zoe@example.com", "zoe_o-b", "x".repeat(128))
        );

        for (List<String> input : accepted) {
            assertEquals(Optional.empty(), register(
                    input.get(0), input.get(1), input.get(2), input.get(3), input.get(4), input.get(4)));
            assertDoesNotThrow(() -> authService.register(
                    input.get(0), input.get(1), input.get(2), input.get(3), input.get(4)));
        }
    }

    @Test
    void duplicateAccountErrorsFromTheServiceMapToTheirField() {
        AppContext context = TestAppContext.create(tempDir.resolve("dupes.db"));
        AuthService authService = context.getAuthService();
        authService.register("Ada", "Lovelace", "ada@example.com", "ada", VALID_PASSWORD);

        DuplicateUserException sameUsername = assertThrows(DuplicateUserException.class,
                () -> authService.register("Ada", "Byron", "other@example.com", "ADA", VALID_PASSWORD));
        DuplicateUserException sameEmail = assertThrows(DuplicateUserException.class,
                () -> authService.register("Ada", "Byron", "Ada@Example.com", "ada2", VALID_PASSWORD));

        assertEquals(Optional.of(Field.USERNAME), AuthFormValidator.fieldForDuplicate(sameUsername));
        assertEquals(Optional.of(Field.EMAIL), AuthFormValidator.fieldForDuplicate(sameEmail));
    }

    @Test
    void duplicateErrorsWithoutAFieldHintMapToNoField() {
        assertEquals(Optional.empty(), AuthFormValidator.fieldForDuplicate(
                new DuplicateUserException("An account with these details already exists")));
        assertEquals(Optional.empty(), AuthFormValidator.fieldForDuplicate(new DuplicateUserException(null)));
        assertThrows(NullPointerException.class, () -> AuthFormValidator.fieldForDuplicate(null));
    }

    private static Optional<FieldError> register(String firstName,
                                                 String lastName,
                                                 String email,
                                                 String username,
                                                 String password,
                                                 String confirmPassword) {
        return AuthFormValidator.validateRegistration(firstName, lastName, email, username, password, confirmPassword);
    }

    private static Optional<FieldError> withPassword(String password) {
        return register("Ada", "Lovelace", "ada@example.com", "ada", password, password);
    }

    private static Optional<FieldError> withUsername(String username) {
        return register("Ada", "Lovelace", "ada@example.com", username, VALID_PASSWORD, VALID_PASSWORD);
    }

    private static void assertField(Field expected, Optional<FieldError> actual) {
        assertTrue(actual.isPresent(), "Expected a " + expected + " error");
        assertEquals(expected, actual.get().field(), () -> "Unexpected error: " + actual.get());
    }
}
