package com.manpreet.bank.ui;

import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.util.InputValidator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Pre-submit checks for the login and registration forms.
 * Delegates to {@link InputValidator} so messages and rules match the service layer, and reports
 * the first failing field in on-screen order so the form can focus it.
 * The service layer remains the authority; this only improves feedback before a request is made.
 */
public final class AuthFormValidator {

    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String PASSWORD_MISMATCH = "Password and confirmation do not match";

    public enum Field {
        USERNAME_OR_EMAIL,
        FIRST_NAME,
        LAST_NAME,
        EMAIL,
        USERNAME,
        PASSWORD,
        CONFIRM_PASSWORD
    }

    public record FieldError(Field field, String message) {
    }

    private record Check(Field field, Runnable validation) {
    }

    private AuthFormValidator() {
    }

    public static Optional<FieldError> validateLogin(String usernameOrEmail, String password) {
        Optional<FieldError> identifierError = run(new Check(
                Field.USERNAME_OR_EMAIL,
                () -> InputValidator.normalizeLoginIdentifier(usernameOrEmail)
        ));
        if (identifierError.isPresent()) {
            return identifierError;
        }
        // Passwords are never trimmed, so only a truly empty value counts as missing.
        if (password == null || password.isEmpty()) {
            return Optional.of(new FieldError(Field.PASSWORD, PASSWORD_REQUIRED));
        }
        return Optional.empty();
    }

    public static Optional<FieldError> validateRegistration(String firstName,
                                                            String lastName,
                                                            String email,
                                                            String username,
                                                            String password,
                                                            String confirmPassword) {
        List<Check> checks = List.of(
                new Check(Field.FIRST_NAME, () -> InputValidator.normalizeName(firstName, "First name")),
                new Check(Field.LAST_NAME, () -> InputValidator.normalizeName(lastName, "Last name")),
                new Check(Field.EMAIL, () -> InputValidator.normalizeEmail(email)),
                new Check(Field.USERNAME, () -> InputValidator.normalizeUsername(username)),
                new Check(Field.PASSWORD, () -> InputValidator.validatePassword(password))
        );
        for (Check check : checks) {
            Optional<FieldError> error = run(check);
            if (error.isPresent()) {
                return error;
            }
        }
        if (!Objects.equals(password, confirmPassword)) {
            return Optional.of(new FieldError(Field.CONFIRM_PASSWORD, PASSWORD_MISMATCH));
        }
        return Optional.empty();
    }

    /**
     * Identifies which registration field a duplicate-account error refers to, if it can be determined.
     */
    public static Optional<Field> fieldForDuplicate(DuplicateUserException error) {
        Objects.requireNonNull(error, "error must not be null");
        String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("email")) {
            return Optional.of(Field.EMAIL);
        }
        if (message.contains("username")) {
            return Optional.of(Field.USERNAME);
        }
        return Optional.empty();
    }

    private static Optional<FieldError> run(Check check) {
        try {
            check.validation().run();
            return Optional.empty();
        } catch (ValidationException e) {
            return Optional.of(new FieldError(check.field(), e.getMessage()));
        }
    }
}
