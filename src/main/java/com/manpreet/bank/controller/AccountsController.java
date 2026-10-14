package com.manpreet.bank.controller;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.BankingDialogs;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.TransactionRowViewModel;
import com.manpreet.bank.ui.TransactionViewMapper;
import com.manpreet.bank.ui.UiErrorMapper;
import com.manpreet.bank.ui.UiFeedback;
import com.manpreet.bank.ui.UiWindows;
import com.manpreet.bank.util.AccountNumberFormatter;
import com.manpreet.bank.util.CurrencyFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class AccountsController implements AppAwareController, ShellAwareController {

    private static final int ACTIVITY_LIMIT = 20;

    @FXML
    private Label statusLabel;
    @FXML
    private VBox checkingCard;
    @FXML
    private VBox savingsCard;
    @FXML
    private Label checkingNumberLabel;
    @FXML
    private Label checkingBalanceLabel;
    @FXML
    private Label checkingOpenedLabel;
    @FXML
    private Label savingsNumberLabel;
    @FXML
    private Label savingsBalanceLabel;
    @FXML
    private Label savingsOpenedLabel;
    @FXML
    private Label activityScopeLabel;
    @FXML
    private Label activityScopeChip;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private TableView<TransactionRowViewModel> transactionsTable;
    @FXML
    private TableColumn<TransactionRowViewModel, String> typeColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> descriptionColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> dateColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> amountColumn;

    private SceneManager sceneManager;
    private MainShellController shellController;
    private Account checking;
    private Account savings;
    private Account selectedAccount;
    private Map<Long, Account> accountsById = Map.of();

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        typeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().typeLabel()));
        descriptionColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().description()));
        dateColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().dateLabel()));
        amountColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().signedAmount()));
        amountColumn.setCellFactory(column -> signedAmountCell());
        UiFeedback.clear(statusLabel);
        refreshAccounts();
    }

    @Override
    public void setShellController(MainShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    private void depositChecking() {
        mutate(() -> BankingDialogs.showDeposit(
                        sceneManager.getAppContext(), requireSession(), checking, UiWindows.from(statusLabel)),
                "Deposit complete.");
    }

    @FXML
    private void withdrawChecking() {
        mutate(() -> BankingDialogs.showWithdraw(
                        sceneManager.getAppContext(), requireSession(), checking, UiWindows.from(statusLabel)),
                "Withdrawal complete.");
    }

    @FXML
    private void depositSavings() {
        mutate(() -> BankingDialogs.showDeposit(
                        sceneManager.getAppContext(), requireSession(), savings, UiWindows.from(statusLabel)),
                "Deposit complete.");
    }

    @FXML
    private void transferBetweenAccounts() {
        mutate(() -> BankingDialogs.showTransfer(
                        sceneManager.getAppContext(), requireSession(), UiWindows.from(statusLabel)),
                "Transfer complete.");
    }

    @FXML
    private void showCheckingActivity() {
        if (checking != null) {
            loadActivity(checking);
        }
    }

    @FXML
    private void showSavingsActivity() {
        if (savings != null) {
            loadActivity(savings);
        }
    }

    private void mutate(java.util.function.Supplier<java.util.Optional<Boolean>> action, String successMessage) {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        UiFeedback.clear(statusLabel);
        action.get().ifPresent(success -> {
            if (success) {
                UiFeedback.success(statusLabel, successMessage);
                refreshAccounts();
            }
        });
    }

    private void refreshAccounts() {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        try {
            List<Account> accounts = sceneManager.getAppContext().getAccountService()
                    .getAccountsForUser(session.userId());
            checking = accounts.stream().filter(a -> a.getAccountType() == AccountType.CHECKING).findFirst().orElse(null);
            savings = accounts.stream().filter(a -> a.getAccountType() == AccountType.SAVINGS).findFirst().orElse(null);
            accountsById = new HashMap<>();
            if (checking != null) {
                accountsById.put(checking.getId(), checking);
            }
            if (savings != null) {
                accountsById.put(savings.getId(), savings);
            }
            bind(checking, checkingNumberLabel, checkingBalanceLabel, checkingOpenedLabel);
            bind(savings, savingsNumberLabel, savingsBalanceLabel, savingsOpenedLabel);
            Account target = selectedAccount != null && accountsById.containsKey(selectedAccount.getId())
                    ? accountsById.get(selectedAccount.getId())
                    : checking;
            if (target != null) {
                loadActivity(target);
            }
        } catch (RuntimeException e) {
            UiFeedback.error(statusLabel, UiErrorMapper.toUserMessage(e));
            transactionsTable.getItems().clear();
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
            transactionsTable.setVisible(false);
            transactionsTable.setManaged(false);
        }
    }

    private void loadActivity(Account account) {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        selectedAccount = account;
        String label = AccountNumberFormatter.displayLabel(account.getAccountType(), account.getAccountNumber());
        activityScopeLabel.setText("Showing recent activity for " + label);
        activityScopeChip.setText(account.getAccountType() == AccountType.CHECKING ? "Checking" : "Savings");
        updateSelectedCardStyles();

        List<Transaction> history = sceneManager.getAppContext().getTransactionService()
                .getAccountHistory(session.userId(), account.getId(), ACTIVITY_LIMIT);
        List<TransactionRowViewModel> rows = TransactionViewMapper.toRows(history, accountsById);
        transactionsTable.setItems(FXCollections.observableArrayList(rows));
        boolean empty = rows.isEmpty();
        emptyStateBox.setVisible(empty);
        emptyStateBox.setManaged(empty);
        transactionsTable.setVisible(!empty);
        transactionsTable.setManaged(!empty);
    }

    private void updateSelectedCardStyles() {
        checkingCard.getStyleClass().remove("account-card-selected");
        savingsCard.getStyleClass().remove("account-card-selected");
        if (selectedAccount == null) {
            return;
        }
        if (checking != null && selectedAccount.getId() == checking.getId()) {
            checkingCard.getStyleClass().add("account-card-selected");
        } else if (savings != null && selectedAccount.getId() == savings.getId()) {
            savingsCard.getStyleClass().add("account-card-selected");
        }
    }

    private static void bind(Account account, Label number, Label balance, Label opened) {
        if (account == null) {
            number.setText("Unavailable");
            balance.setText(CurrencyFormatter.format(java.math.BigDecimal.ZERO));
            opened.setText("");
            return;
        }
        number.setText(AccountNumberFormatter.mask(account.getAccountNumber()));
        balance.setText(CurrencyFormatter.format(account.getBalance()));
        opened.setText("Opened " + account.getCreatedAt().toLocalDate());
    }

    private static TableCell<TransactionRowViewModel, String> signedAmountCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("amount-credit", "amount-debit");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                TransactionRowViewModel row = getTableRow() == null ? null : getTableRow().getItem();
                if (row != null) {
                    getStyleClass().add(row.credit() ? "amount-credit" : "amount-debit");
                }
            }
        };
    }

    private UserSession requireSession() {
        return sceneManager.getAppContext().getSessionManager().getCurrentSession()
                .orElseGet(() -> {
                    sceneManager.showLogin();
                    return null;
                });
    }
}
