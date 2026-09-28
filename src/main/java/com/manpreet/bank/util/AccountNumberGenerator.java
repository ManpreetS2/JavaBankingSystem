package com.manpreet.bank.util;

import com.manpreet.bank.model.AccountType;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * Generates simulated account numbers such as {@code CHK-1234567890}.
 */
public class AccountNumberGenerator {

    private static final int DIGIT_COUNT = 10;
    private static final int MAX_ATTEMPTS = 12;

    private final SecureRandom secureRandom;

    public AccountNumberGenerator() {
        this(new SecureRandom());
    }

    public AccountNumberGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    public String generate(AccountType accountType) {
        Objects.requireNonNull(accountType, "accountType must not be null");
        String prefix = switch (accountType) {
            case CHECKING -> "CHK";
            case SAVINGS -> "SAV";
        };
        return prefix + "-" + randomDigits();
    }

    public int maxAttempts() {
        return MAX_ATTEMPTS;
    }

    private String randomDigits() {
        StringBuilder builder = new StringBuilder(DIGIT_COUNT);
        for (int i = 0; i < DIGIT_COUNT; i++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }
}
