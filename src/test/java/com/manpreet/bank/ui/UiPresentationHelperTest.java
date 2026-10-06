package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class UiPresentationHelperTest {

    @Test
    void dialogAmountValidatorRejectsBlankZeroNegativeInvalidAndOverPrecision() {
        assertEquals(Optional.of("Enter an amount."), DialogAmountValidator.validate(""));
        assertEquals(Optional.of("Enter an amount."), DialogAmountValidator.validate("   "));
        assertEquals(Optional.of("Enter a valid amount such as 25.00"), DialogAmountValidator.validate("abc"));
        assertEquals(Optional.of("Enter an amount greater than $0.00."), DialogAmountValidator.validate("0"));
        assertEquals(Optional.of("Enter an amount greater than $0.00."), DialogAmountValidator.validate("0.00"));
        assertEquals(Optional.of("Amount cannot be negative."), DialogAmountValidator.validate("-1.00"));
        assertEquals(Optional.of("Amount cannot have more than 2 decimal places."),
                DialogAmountValidator.validate("10.001"));
        assertEquals(Optional.of("Amount cannot have more than 2 decimal places."),
                DialogAmountValidator.validate("1.234"));
        assertEquals(Optional.of("Amount cannot have more than 2 decimal places."),
                DialogAmountValidator.validate("0.001"));
        assertFalse(DialogAmountValidator.isReady("10.123"));
    }

    @Test
    void dialogAmountValidatorAcceptsValidMoneyScale() {
        assertTrue(DialogAmountValidator.validate("10").isEmpty());
        assertTrue(DialogAmountValidator.validate("10.5").isEmpty());
        assertTrue(DialogAmountValidator.validate("10.50").isEmpty());
        assertTrue(DialogAmountValidator.validate("0.01").isEmpty());
        assertTrue(DialogAmountValidator.isReady("25.00"));
    }

    @Test
    void uiFeedbackKindsExposeStableNames() {
        assertEquals("SUCCESS", UiFeedback.Kind.SUCCESS.name());
        assertEquals("ERROR", UiFeedback.Kind.ERROR.name());
        assertEquals("INFO", UiFeedback.Kind.INFO.name());
    }
}
