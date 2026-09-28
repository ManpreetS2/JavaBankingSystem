package com.manpreet.bank.controller;

import com.manpreet.bank.exception.AccountAccessException;
import com.manpreet.bank.exception.InsufficientFundsException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.service.AccountService;
import com.manpreet.bank.service.TransactionService;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.SceneManager;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class DashboardController implements AppAwareController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label totalBalanceLabel;
    @FXML
    private Label checkingBalanceLabel;
    @FXML
    private Label savingsBalanceLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TableView<TransactionRow> transactionsTable;
    @FXML
    private TableColumn<TransactionRow, String> typeColumn;
    @FXML
    private TableColumn<TransactionRow, String> amountColumn;
    @FXML
    private TableColumn<TransactionRow, String> descriptionColumn;
    @FXML
    private TableColumn<TransactionRow, String> createdAtColumn;

    private SceneManager sceneManager;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        refresh();
    }

    @FXML
    private void handleDeposit() {
        Optional<UserSession> session = currentSession();
        if (session.isEmpty()) {
            return;
        }
        Account checking = requireAccount(AccountType.CHECKING);
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Deposit");
        dialog.setHeaderText("Deposit to Checking");
        dialog.setContentText("Amount (USD):");
        dialog.showAndWait().ifPresent(amountText -> {
            try {
                sceneManager.getAppContext().getAccountService().deposit(
                        session.get().userId(),
                        checking.getId(),
                        new BigDecimal(amountText.trim()),
                        "Deposit"
                );
                statusLabel.setText("Deposit successful.");
                refresh();
            } catch (NumberFormatException e) {
                showError("Enter a valid amount such as 25.00");
            } catch (ValidationException | AccountAccessException | InsufficientFundsException e) {
                showError(e.getMessage());
            } catch (RuntimeException e) {
                showError("Deposit failed. Please try again.");
            }
        });
    }

    @FXML
    private void handleWithdraw() {
        Optional<UserSession> session = currentSession();
        if (session.isEmpty()) {
            return;
        }
        Account checking = requireAccount(AccountType.CHECKING);
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Withdraw");
        dialog.setHeaderText("Withdraw from Checking");
        dialog.setContentText("Amount (USD):");
        dialog.showAndWait().ifPresent(amountText -> {
            try {
                sceneManager.getAppContext().getAccountService().withdraw(
                        session.get().userId(),
                        checking.getId(),
                        new BigDecimal(amountText.trim()),
                        "Withdrawal"
                );
                statusLabel.setText("Withdrawal successful.");
                refresh();
            } catch (NumberFormatException e) {
                showError("Enter a valid amount such as 25.00");
            } catch (ValidationException | AccountAccessException | InsufficientFundsException e) {
                showError(e.getMessage());
            } catch (RuntimeException e) {
                showError("Withdrawal failed. Please try again.");
            }
        });
    }

    @FXML
    private void handleTransfer() {
        Optional<UserSession> session = currentSession();
        if (session.isEmpty()) {
            return;
        }

        Account checking = requireAccount(AccountType.CHECKING);
        Account savings = requireAccount(AccountType.SAVINGS);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Transfer");
        dialog.setHeaderText("Transfer between your accounts");

        ComboBox<String> directionBox = new ComboBox<>(FXCollections.observableArrayList(
                "Checking → Savings",
                "Savings → Checking"
        ));
        directionBox.getSelectionModel().selectFirst();
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Direction"), 0, 0);
        grid.add(directionBox, 1, 0);
        grid.add(new Label("Amount"), 0, 1);
        grid.add(amountField, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
            return;
        }

        try {
            boolean checkingToSavings = directionBox.getSelectionModel().getSelectedIndex() == 0;
            long sourceId = checkingToSavings ? checking.getId() : savings.getId();
            long destinationId = checkingToSavings ? savings.getId() : checking.getId();
            sceneManager.getAppContext().getAccountService().transfer(
                    session.get().userId(),
                    sourceId,
                    destinationId,
                    new BigDecimal(amountField.getText().trim()),
                    "Transfer"
            );
            statusLabel.setText("Transfer successful.");
            refresh();
        } catch (NumberFormatException e) {
            showError("Enter a valid amount such as 25.00");
        } catch (ValidationException | AccountAccessException | InsufficientFundsException e) {
            showError(e.getMessage());
        } catch (RuntimeException e) {
            showError("Transfer failed. Please try again.");
        }
    }

    @FXML
    private void handleRefreshTransactions() {
        refresh();
        statusLabel.setText("Balances and transactions refreshed.");
    }

    @FXML
    private void handleLogout() {
        sceneManager.getAppContext().getSessionManager().endSession();
        sceneManager.showLogin();
    }

    private void refresh() {
        Optional<UserSession> session = currentSession();
        if (session.isEmpty()) {
            return;
        }

        UserSession user = session.get();
        welcomeLabel.setText("Welcome, " + user.firstName());

        AccountService accountService = sceneManager.getAppContext().getAccountService();
        TransactionService transactionService = sceneManager.getAppContext().getTransactionService();

        List<Account> accounts = accountService.getAccountsForUser(user.userId());
        Account checking = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.CHECKING)
                .findFirst()
                .orElseThrow();
        Account savings = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.SAVINGS)
                .findFirst()
                .orElseThrow();

        totalBalanceLabel.setText(currencyFormat.format(accountService.getTotalBalance(user.userId())));
        checkingBalanceLabel.setText(currencyFormat.format(checking.getBalance()));
        savingsBalanceLabel.setText(currencyFormat.format(savings.getBalance()));

        List<TransactionRow> rows = transactionService.getRecentActivity(user.userId(), 20).stream()
                .map(TransactionRow::from)
                .toList();
        transactionsTable.setItems(FXCollections.observableArrayList(rows));
    }

    private Optional<UserSession> currentSession() {
        Optional<UserSession> session = sceneManager.getAppContext().getSessionManager().getCurrentSession();
        if (session.isEmpty()) {
            sceneManager.showLogin();
        }
        return session;
    }

    private Account requireAccount(AccountType type) {
        UserSession session = currentSession().orElseThrow();
        return sceneManager.getAppContext().getAccountService().getAccountsForUser(session.userId()).stream()
                .filter(account -> account.getAccountType() == type)
                .findFirst()
                .orElseThrow();
    }

    private void showError(String message) {
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Unable to complete action");
        alert.showAndWait();
    }

    public static class TransactionRow {
        private final String type;
        private final String amount;
        private final String description;
        private final String createdAt;

        public TransactionRow(String type, String amount, String description, String createdAt) {
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.createdAt = createdAt;
        }

        public static TransactionRow from(Transaction transaction) {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
            return new TransactionRow(
                    transaction.getTransactionType().name(),
                    format.format(transaction.getAmount()),
                    transaction.getDescription() == null ? "" : transaction.getDescription(),
                    transaction.getCreatedAt().toString()
            );
        }

        public String getType() {
            return type;
        }

        public String getAmount() {
            return amount;
        }

        public String getDescription() {
            return description;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }
}
