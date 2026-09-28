package com.manpreet.bank.util;

import com.manpreet.bank.exception.ValidationException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing using PBKDF2-HMAC-SHA256 with per-password random salts.
 * Encoded format: {@code pbkdf2-sha256$iterations$base64Salt$base64Hash}
 */
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int DEFAULT_ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final SecureRandom secureRandom;
    private final int iterations;

    public PasswordHasher() {
        this(DEFAULT_ITERATIONS, new SecureRandom());
    }

    /**
     * Package/test-friendly constructor. Production code should use {@link #PasswordHasher()}.
     */
    public PasswordHasher(int iterations) {
        this(iterations, new SecureRandom());
    }

    PasswordHasher(int iterations, SecureRandom secureRandom) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.iterations = iterations;
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    public String hash(String password) {
        Objects.requireNonNull(password, "password must not be null");
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        try {
            byte[] derived = derive(password.toCharArray(), salt, iterations);
            return PREFIX + "$"
                    + iterations + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(derived);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }

    public boolean verify(String password, String encodedHash) {
        Objects.requireNonNull(password, "password must not be null");
        if (encodedHash == null || encodedHash.isBlank()) {
            return false;
        }

        try {
            String[] parts = encodedHash.split("\\$");
            if (parts.length != 4 || !PREFIX.equals(parts[0])) {
                return false;
            }

            int storedIterations = Integer.parseInt(parts[1]);
            if (storedIterations < 1) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, storedIterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException | GeneralSecurityException e) {
            return false;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterationCount) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterationCount, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Ensures callers never accidentally treat encoded hashes as printable user content.
     */
    public static void assertLooksEncoded(String encodedHash) {
        if (encodedHash == null || !encodedHash.startsWith(PREFIX + "$")) {
            throw new ValidationException("Password hash format is invalid");
        }
        if (encodedHash.getBytes(StandardCharsets.UTF_8).length < 40) {
            throw new ValidationException("Password hash format is invalid");
        }
    }
}
