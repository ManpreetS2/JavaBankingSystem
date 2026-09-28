package com.manpreet.bank.controller;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.BankingDialogs;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.UiErrorMapper;
import com.manpreet.bank.util.AccountNumberFormatter;
import com.manpreet.bank.util.CurrencyFormatter;
import com.manpreet.bank.util.DateTimeDisplayFormatter;
import com.manpreet.bank.util.TransactionPresentation;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AccountsController implements AppAwareController, ShellAwareController {

    @FXML
    private Label statusLabel;
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
    private Label emptyStateLabel;
    @FXML
    private TableView<ActivityRow> transactionsTable;
    @FXML
    private TableColumn<ActivityRow, String> typeColumn;
    @FXML
    private TableColumn<ActivityRow, String> descriptionColumn;
    @FXML
    private TableColumn<ActivityRow, String> dateColumn;
    @FXML
    private TableColumn<ActivityRow, String> amountColumn;

    private SceneManager sceneManager;
    private MainShellController shellController;
    private Account checking;
    private Account savings;

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        refreshAccounts();
    }

    @Override
    public void setShellController(MainShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    private void depositChecking() {
        mutate(() -> BankingDialogs.showDeposit(sceneManager.getAppContext(), requireSession(), checking));
    }

    @FXML
    private void withdrawChecking() {
        mutate(() -> BankingDialogs.showWithdraw(sceneManager.getAppContext(), requireSession(), checking));
    }

    @FXML
    private void depositSavings() {
        mutate(() -> BankingDialogs.showDeposit(sceneManager.getAppContext(), requireSession(), savings));
    }

    @FXML
    private void transferBetweenAccounts() {
        mutate(() -> BankingDialogs.showTransfer(sceneManager.getAppContext(), requireSession()));
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

    private void mutate(java.util.function.Supplier<java.util.Optional<Boolean>> action) {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        action.get().ifPresent(success -> {
            if (success) {
                statusLabel.setText("Account updated.");
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
            bind(checking, checkingNumberLabel, checkingBalanceLabel, checkingOpenedLabel);
            bind(savings, savingsNumberLabel, savingsBalanceLabel, savingsOpenedLabel);
            if (checking != null) {
                loadActivity(checking);
            }
        } catch (RuntimeException e) {
            statusLabel.getStyleClass().setAll("error-text");
            statusLabel.setText(UiErrorMapper.toUserMessage(e));
        }
    }

    private void loadActivity(Account account) {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        List<Transaction> history = sceneManager.getAppContext().getTransactionService()
                .getAccountHistory(session.userId(), account.getId());
        List<ActivityRow> rows = history.stream().limit(20).map(ActivityRow::from).toList();
        transactionsTable.setItems(FXCollections.observableArrayList(rows));
        boolean empty = rows.isEmpty();
        emptyStateLabel.setVisible(empty);
        emptyStateLabel.setManaged(empty);
        emptyStateLabel.setText(empty ? "No transactions for this account yet." : "");
        transactionsTable.setVisible(!empty);
        transactionsTable.setManaged(!empty);
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

    private UserSession requireSession() {
        return sceneManager.getAppContext().getSessionManager().getCurrentSession()
                .orElseGet(() -> {
                    sceneManager.showLogin();
                    return null;
                });
    }

    public static class ActivityRow {
        private final String type;
        private final String description;
        private final String date;
        private final String amount;

        public ActivityRow(String type, String description, String date, String amount) {
            this.type = type;
            this.description = description;
            this.date = date;
            this.amount = amount;
        }

        public static ActivityRow from(Transaction transaction) {
            return new ActivityRow(
                    TransactionPresentation.typeLabel(transaction.getTransactionType()),
                    transaction.getDescription() == null ? "" : transaction.getDescription(),
                    DateTimeDisplayFormatter.format(transaction.getCreatedAt()),
                    TransactionPresentation.signedAmount(transaction)
            );
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public String getDate() {
            return date;
        }

        public String getAmount() {
            return amount;
        }
    }
}
