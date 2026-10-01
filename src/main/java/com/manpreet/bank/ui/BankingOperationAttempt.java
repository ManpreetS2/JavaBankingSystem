package com.manpreet.bank.ui;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Runs a banking operation submitted from a dialog and reports why it was rejected, if it was,
 * so the dialog can stay open with the entered values instead of closing first.
 */
public final class BankingOperationAttempt {

    /**
     * The service call to make once the amount text is valid. Throws when the operation is rejected.
     */
    @FunctionalInterface
    public interface Operation {
        void run(BigDecimal amount);
    }

    private BankingOperationAttempt() {
    }

    /**
     * @return empty when the operation succeeded; otherwise a user-facing message explaining the rejection
     */
    public static Optional<String> run(String amountText, Operation operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        Optional<String> amountError = DialogAmountValidator.validate(amountText);
        if (amountError.isPresent()) {
            return amountError;
        }
        final BigDecimal amount;
        try {
            amount = new BigDecimal(amountText.trim());
        } catch (NumberFormatException e) {
            return Optional.of("Enter a valid amount such as 25.00");
        }
        try {
            operation.run(amount);
            return Optional.empty();
        } catch (RuntimeException e) {
            return Optional.of(MessageText.asSentence(UiErrorMapper.toUserMessage(e)));
        }
    }
}
