package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class UiPresentationHelperTest {

    @Test
    void dialogAmountValidatorRejectsBlankZeroNegativeAndInvalid() {
        assertEquals(Optional.of("Enter an amount."), DialogAmountValidator.validate(""));
        assertEquals(Optional.of("Enter an amount."), DialogAmountValidator.validate("   "));
        assertEquals(Optional.of("Enter a valid amount such as 25.00"), DialogAmountValidator.validate("abc"));
        assertEquals(Optional.of("Amount must be greater than zero."), DialogAmountValidator.validate("0"));
        assertEquals(Optional.of("Amount must be greater than zero."), DialogAmountValidator.validate("0.00"));
        assertEquals(Optional.of("Amount cannot be negative."), DialogAmountValidator.validate("-1.00"));
        assertTrue(DialogAmountValidator.validate("25.00").isEmpty());
        assertTrue(DialogAmountValidator.isReady("10.50"));
        assertFalse(DialogAmountValidator.isReady(""));
    }

    @Test
    void uiFeedbackKindsExposeStableNames() {
        assertEquals("SUCCESS", UiFeedback.Kind.SUCCESS.name());
        assertEquals("ERROR", UiFeedback.Kind.ERROR.name());
        assertEquals("INFO", UiFeedback.Kind.INFO.name());
    }
}
