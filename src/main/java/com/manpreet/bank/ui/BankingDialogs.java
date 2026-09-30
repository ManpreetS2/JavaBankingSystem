package com.manpreet.bank.ui;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.util.AccountNumberFormatter;
import com.manpreet.bank.util.CurrencyFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Shared functional banking dialogs used by dashboard and accounts screens.
 */
public final class BankingDialogs {

    private BankingDialogs() {
    }

    public static Optional<Boolean> showDeposit(AppContext context, UserSession session, Account preferred) {
        AccountSelection selection = chooseAccount(context, session, preferred, "Deposit", "Current balance");
        if (selection == null) {
            return Optional.empty();
        }
        try {
            context.getAccountService().deposit(
                    session.userId(),
                    selection.account().getId(),
                    selection.amount(),
                    selection.description()
            );
            return Optional.of(true);
        } catch (RuntimeException e) {
            showError(e);
            return Optional.of(false);
        }
    }

    public static Optional<Boolean> showWithdraw(AppContext context, UserSession session, Account preferred) {
        AccountSelection selection = chooseAccount(context, session, preferred, "Withdraw", "Available balance");
        if (selection == null) {
            return Optional.empty();
        }
        try {
            context.getAccountService().withdraw(
                    session.userId(),
                    selection.account().getId(),
                    selection.amount(),
                    selection.description()
            );
            return Optional.of(true);
        } catch (RuntimeException e) {
            showError(e);
            return Optional.of(false);
        }
    }

    public static Optional<Boolean> showTransfer(AppContext context, UserSession session) {
        List<Account> accounts = context.getAccountService().getAccountsForUser(session.userId());
        if (accounts.size() < 2) {
            showMessage("Transfer requires both checking and savings accounts.");
            return Optional.empty();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Transfer");
        dialog.setHeaderText("Move money between your accounts");

        ComboBox<Account> fromBox = accountCombo(accounts);
        ComboBox<Account> toBox = accountCombo(accounts);
        fromBox.getSelectionModel().select(find(accounts, AccountType.CHECKING));
        toBox.getSelectionModel().select(find(accounts, AccountType.SAVINGS));

        Label availableLabel = new Label();
        availableLabel.getStyleClass().add("caption");
        Runnable updateAvailable = () -> {
            Account source = fromBox.getSelectionModel().getSelectedItem();
            availableLabel.setText(source == null
                    ? ""
                    : "Available balance: " + CurrencyFormatter.format(source.getBalance()));
        };
        fromBox.valueProperty().addListener((obs, old, value) -> updateAvailable.run());
        updateAvailable.run();

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");
        amountField.getStyleClass().add("input");
        TextField descriptionField = new TextField("Transfer");
        descriptionField.getStyleClass().add("input");

        Label validationLabel = new Label();
        validationLabel.getStyleClass().add("error-text");
        validationLabel.setWrapText(true);

        Runnable refreshValidation = () -> {
            Optional<String> amountError = DialogAmountValidator.validate(amountField.getText());
            Account from = fromBox.getSelectionModel().getSelectedItem();
            Account to = toBox.getSelectionModel().getSelectedItem();
            if (amountError.isPresent()) {
                setInputError(amountField, true);
                validationLabel.setText(amountError.get());
            } else if (from != null && to != null && from.getId() == to.getId()) {
                setInputError(amountField, false);
                validationLabel.setText("Source and destination accounts must be different.");
            } else {
                setInputError(amountField, false);
                validationLabel.setText("");
            }
        };
        amountField.textProperty().addListener((obs, old, value) -> refreshValidation.run());
        fromBox.valueProperty().addListener((obs, old, value) -> refreshValidation.run());
        toBox.valueProperty().addListener((obs, old, value) -> refreshValidation.run());
        refreshValidation.run();

        GridPane grid = formGrid();
        grid.add(labeled("From"), 0, 0);
        grid.add(fromBox, 1, 0);
        grid.add(availableLabel, 1, 1);
        grid.add(labeled("To"), 0, 2);
        grid.add(toBox, 1, 2);
        grid.add(labeled("Amount"), 0, 3);
        grid.add(amountField, 1, 3);
        grid.add(labeled("Description"), 0, 4);
        grid.add(descriptionField, 1, 4);

        VBox content = new VBox(12, grid, validationLabel);
        content.getStyleClass().add("dialog-container");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStyleClass().add("dialog-container");
        ButtonType transferButton = new ButtonType("Transfer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(transferButton, ButtonType.CANCEL);

        BooleanBinding sameAccount = Bindings.createBooleanBinding(
                () -> {
                    Account from = fromBox.getSelectionModel().getSelectedItem();
                    Account to = toBox.getSelectionModel().getSelectedItem();
                    return from != null && to != null && from.getId() == to.getId();
                },
                fromBox.valueProperty(),
                toBox.valueProperty()
        );
        dialog.getDialogPane().lookupButton(transferButton).disableProperty().bind(
                amountField.textProperty().isEmpty()
                        .or(fromBox.valueProperty().isNull())
                        .or(toBox.valueProperty().isNull())
                        .or(sameAccount)
                        .or(Bindings.createBooleanBinding(
                                () -> !DialogAmountValidator.isReady(amountField.getText()),
                                amountField.textProperty()
                        ))
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != transferButton) {
            return Optional.empty();
        }

        Account from = fromBox.getSelectionModel().getSelectedItem();
        Account to = toBox.getSelectionModel().getSelectedItem();
        Optional<String> amountError = DialogAmountValidator.validate(amountField.getText());
        if (amountError.isPresent()) {
            showMessage(amountError.get());
            return Optional.of(false);
        }
        if (from.getId() == to.getId()) {
            showMessage("Source and destination accounts must be different");
            return Optional.of(false);
        }

        try {
            context.getAccountService().transfer(
                    session.userId(),
                    from.getId(),
                    to.getId(),
                    new BigDecimal(amountField.getText().trim()),
                    descriptionField.getText()
            );
            return Optional.of(true);
        } catch (NumberFormatException e) {
            showMessage("Enter a valid amount such as 25.00");
            return Optional.of(false);
        } catch (RuntimeException e) {
            showError(e);
            return Optional.of(false);
        }
    }

    private static AccountSelection chooseAccount(AppContext context,
                                                  UserSession session,
                                                  Account preferred,
                                                  String action,
                                                  String balanceCaption) {
        List<Account> accounts = context.getAccountService().getAccountsForUser(session.userId());
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(action);
        dialog.setHeaderText(action + " money");

        ComboBox<Account> accountBox = accountCombo(accounts);
        if (preferred != null) {
            accounts.stream()
                    .filter(account -> account.getId() == preferred.getId())
                    .findFirst()
                    .ifPresent(account -> accountBox.getSelectionModel().select(account));
        } else {
            accountBox.getSelectionModel().selectFirst();
        }

        Label balanceLabel = new Label();
        balanceLabel.getStyleClass().add("caption");
        Runnable updateBalance = () -> {
            Account selected = accountBox.getSelectionModel().getSelectedItem();
            balanceLabel.setText(selected == null
                    ? ""
                    : balanceCaption + ": " + CurrencyFormatter.format(selected.getBalance()));
        };
        accountBox.valueProperty().addListener((obs, old, value) -> updateBalance.run());
        updateBalance.run();

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");
        amountField.getStyleClass().add("input");
        TextField descriptionField = new TextField(action);
        descriptionField.getStyleClass().add("input");

        Label validationLabel = new Label();
        validationLabel.getStyleClass().add("error-text");
        validationLabel.setWrapText(true);
        amountField.textProperty().addListener((obs, old, value) -> {
            Optional<String> error = DialogAmountValidator.validate(value);
            setInputError(amountField, error.isPresent());
            validationLabel.setText(error.orElse(""));
        });

        GridPane grid = formGrid();
        grid.add(labeled("Account"), 0, 0);
        grid.add(accountBox, 1, 0);
        grid.add(balanceLabel, 1, 1);
        grid.add(labeled("Amount"), 0, 2);
        grid.add(amountField, 1, 2);
        grid.add(labeled("Description"), 0, 3);
        grid.add(descriptionField, 1, 3);

        VBox content = new VBox(12, grid, validationLabel);
        content.getStyleClass().add("dialog-container");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStyleClass().add("dialog-container");
        ButtonType actionButton = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(actionButton, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(actionButton).disableProperty().bind(
                amountField.textProperty().isEmpty()
                        .or(accountBox.valueProperty().isNull())
                        .or(Bindings.createBooleanBinding(
                                () -> !DialogAmountValidator.isReady(amountField.getText()),
                                amountField.textProperty()
                        ))
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != actionButton) {
            return null;
        }

        Optional<String> amountError = DialogAmountValidator.validate(amountField.getText());
        if (amountError.isPresent()) {
            showMessage(amountError.get());
            return null;
        }

        try {
            return new AccountSelection(
                    accountBox.getSelectionModel().getSelectedItem(),
                    new BigDecimal(amountField.getText().trim()),
                    descriptionField.getText()
            );
        } catch (NumberFormatException e) {
            showMessage("Enter a valid amount such as 25.00");
            return null;
        }
    }

    private static Label labeled(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private static void setInputError(TextField field, boolean error) {
        field.getStyleClass().remove("input-error");
        if (error && field.getText() != null && !field.getText().isBlank()) {
            field.getStyleClass().add("input-error");
        }
    }

    private static ComboBox<Account> accountCombo(List<Account> accounts) {
        ComboBox<Account> comboBox = new ComboBox<>(FXCollections.observableArrayList(accounts));
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account account) {
                if (account == null) {
                    return "";
                }
                return AccountNumberFormatter.displayLabel(account.getAccountType(), account.getAccountNumber())
                        + " — " + CurrencyFormatter.format(account.getBalance());
            }

            @Override
            public Account fromString(String string) {
                return null;
            }
        });
        comboBox.getStyleClass().add("input");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    private static Account find(List<Account> accounts, AccountType type) {
        return accounts.stream()
                .filter(account -> account.getAccountType() == type)
                .findFirst()
                .orElse(accounts.getFirst());
    }

    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(4));
        return grid;
    }

    private static void showError(RuntimeException error) {
        showMessage(UiErrorMapper.toUserMessage(error));
    }

    private static void showMessage(String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Notice");
        dialog.setContentText(message);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("dialog-container");
        dialog.showAndWait();
    }

    private record AccountSelection(Account account, BigDecimal amount, String description) {
    }
}
