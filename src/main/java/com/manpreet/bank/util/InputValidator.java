package com.manpreet.bank.util;

import com.manpreet.bank.exception.ValidationException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Registration and identity input validation/normalization.
 */
public final class InputValidator {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MAX_NAME_LENGTH = 100;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}][\\p{L}\\p{M}' .,-]*$");

    private InputValidator() {
    }

    public static String normalizeName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(fieldName + " must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException(fieldName + " contains invalid characters");
        }
        return trimmed;
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("Email format is invalid");
        }
        return normalized;
    }

    public static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < MIN_USERNAME_LENGTH || normalized.length() > MAX_USERNAME_LENGTH) {
            throw new ValidationException(
                    "Username must be between " + MIN_USERNAME_LENGTH + " and " + MAX_USERNAME_LENGTH + " characters"
            );
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException(
                    "Username may only contain letters, numbers, underscore, dot, and hyphen"
            );
        }
        return normalized;
    }

    public static String validatePassword(String password) {
        if (password == null) {
            throw new ValidationException("Password is required");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at most " + MAX_PASSWORD_LENGTH + " characters");
        }
        return password;
    }

    public static String normalizeLoginIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ValidationException("Username or email is required");
        }
        return identifier.trim().toLowerCase(Locale.ROOT);
    }
}
