package com.manpreet.bank.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.exception.ValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyUtilTest {

    @Test
    void normalizesValidScale() {
        assertEquals(new BigDecimal("10.00"), MoneyUtil.requireValidAmount(new BigDecimal("10"), "amount"));
        assertEquals(new BigDecimal("10.50"), MoneyUtil.requireValidAmount(new BigDecimal("10.5"), "amount"));
        assertEquals(new BigDecimal("10.55"), MoneyUtil.requireValidAmount(new BigDecimal("10.55"), "amount"));
        assertEquals(new BigDecimal("0.01"), MoneyUtil.requirePositiveAmount(new BigDecimal("0.01"), "amount"));
        assertEquals(new BigDecimal("1.00"), MoneyUtil.requirePositiveAmount(new BigDecimal("1.00"), "amount"));
        assertEquals(new BigDecimal("999.99"), MoneyUtil.requireTransactionAmount(new BigDecimal("999.99"), "amount"));
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThrows(ValidationException.class,
                () -> MoneyUtil.requireValidAmount(new BigDecimal("10.999"), "amount"));
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThrows(ValidationException.class,
                () -> MoneyUtil.requirePositiveAmount(new BigDecimal("0.00"), "amount"));
        assertThrows(ValidationException.class,
                () -> MoneyUtil.requirePositiveAmount(new BigDecimal("-1.00"), "amount"));
    }

    @Test
    void compareUsesNumericalEquality() {
        assertEquals(0, MoneyUtil.compare(new BigDecimal("1.0"), new BigDecimal("1.00")));
        assertTrue(MoneyUtil.compare(new BigDecimal("2.00"), new BigDecimal("1.00")) > 0);
    }
}

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(10_000);

    @Test
    void hashIsNotPlaintext() {
        String hash = hasher.hash("CorrectHorseBatteryStaple");
        assertFalse(hash.contains("CorrectHorseBatteryStaple"));
        assertTrue(hash.startsWith("pbkdf2-sha256$"));
    }

    @Test
    void verifiesCorrectPasswordAndRejectsWrongPassword() {
        String hash = hasher.hash("CorrectHorseBatteryStaple");
        assertTrue(hasher.verify("CorrectHorseBatteryStaple", hash));
        assertFalse(hasher.verify("WrongPassword!!", hash));
    }

    @Test
    void identicalPasswordsReceiveDifferentHashes() {
        String first = hasher.hash("SamePassword123");
        String second = hasher.hash("SamePassword123");
        assertNotEquals(first, second);
        assertTrue(hasher.verify("SamePassword123", first));
        assertTrue(hasher.verify("SamePassword123", second));
    }

    @Test
    void malformedHashFailsSafely() {
        assertFalse(hasher.verify("password", "not-a-hash"));
        assertFalse(hasher.verify("password", "pbkdf2-sha256$bad"));
        assertFalse(hasher.verify("password", null));
    }

    @Test
    void unicodePasswordWorks() {
        String password = "ПарольSafe12!";
        String hash = hasher.hash(password);
        assertTrue(hasher.verify(password, hash));
    }
}

class InputValidatorTest {

    @Test
    void normalizesEmailAndUsername() {
        assertEquals("user@example.com", InputValidator.normalizeEmail("  User@Example.COM "));
        assertEquals("test.user-1", InputValidator.normalizeUsername("  Test.User-1 "));
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(ValidationException.class, () -> InputValidator.validatePassword("short"));
    }
}
