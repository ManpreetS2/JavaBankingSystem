package com.manpreet.bank.controller;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.BankingDialogs;
import com.manpreet.bank.ui.DashboardDataLoader;
import com.manpreet.bank.ui.DashboardViewModel;
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

public class DashboardController implements AppAwareController, ShellAwareController {

    @FXML
    private Label subtitleLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label totalBalanceLabel;
    @FXML
    private Label checkingNumberLabel;
    @FXML
    private Label checkingBalanceLabel;
    @FXML
    private Label savingsNumberLabel;
    @FXML
    private Label savingsBalanceLabel;
    @FXML
    private Label emptyStateLabel;
    @FXML
    private TableView<TransactionRow> transactionsTable;
    @FXML
    private TableColumn<TransactionRow, String> typeColumn;
    @FXML
    private TableColumn<TransactionRow, String> accountColumn;
    @FXML
    private TableColumn<TransactionRow, String> descriptionColumn;
    @FXML
    private TableColumn<TransactionRow, String> dateColumn;
    @FXML
    private TableColumn<TransactionRow, String> amountColumn;

    private SceneManager sceneManager;
    private MainShellController shellController;
    private DashboardViewModel viewModel;

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        accountColumn.setCellValueFactory(new PropertyValueFactory<>("account"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        refreshDashboard();
    }

    @Override
    public void setShellController(MainShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    private void handleDeposit() {
        UserSession session = requireSession();
        if (session == null || viewModel == null) {
            return;
        }
        BankingDialogs.showDeposit(sceneManager.getAppContext(), session, viewModel.checking())
                .ifPresent(success -> {
                    if (success) {
                        statusLabel.setText("Deposit successful.");
                        refreshDashboard();
                    }
                });
    }

    @FXML
    private void handleWithdraw() {
        UserSession session = requireSession();
        if (session == null || viewModel == null) {
            return;
        }
        BankingDialogs.showWithdraw(sceneManager.getAppContext(), session, viewModel.checking())
                .ifPresent(success -> {
                    if (success) {
                        statusLabel.setText("Withdrawal successful.");
                        refreshDashboard();
                    }
                });
    }

    @FXML
    private void handleTransfer() {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        BankingDialogs.showTransfer(sceneManager.getAppContext(), session)
                .ifPresent(success -> {
                    if (success) {
                        statusLabel.setText("Transfer successful.");
                        refreshDashboard();
                    }
                });
    }

    private void refreshDashboard() {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        try {
            viewModel = new DashboardDataLoader(sceneManager.getAppContext()).load(session);
            totalBalanceLabel.setText(CurrencyFormatter.format(viewModel.totalBalance()));
            bindAccount(viewModel.checking(), checkingNumberLabel, checkingBalanceLabel);
            bindAccount(viewModel.savings(), savingsNumberLabel, savingsBalanceLabel);

            List<TransactionRow> rows = viewModel.recentTransactions().stream()
                    .map(tx -> toRow(tx, viewModel))
                    .toList();
            transactionsTable.setItems(FXCollections.observableArrayList(rows));
            boolean empty = rows.isEmpty();
            emptyStateLabel.setVisible(empty);
            emptyStateLabel.setManaged(empty);
            transactionsTable.setVisible(!empty);
            transactionsTable.setManaged(!empty);
            subtitleLabel.setText("Hello " + session.firstName() + ", here is your banking overview.");
        } catch (RuntimeException e) {
            statusLabel.getStyleClass().setAll("error-text");
            statusLabel.setText(UiErrorMapper.toUserMessage(e));
        }
    }

    private static void bindAccount(Account account, Label numberLabel, Label balanceLabel) {
        if (account == null) {
            numberLabel.setText("Account unavailable");
            balanceLabel.setText(CurrencyFormatter.format(java.math.BigDecimal.ZERO));
            return;
        }
        numberLabel.setText(AccountNumberFormatter.mask(account.getAccountNumber()));
        balanceLabel.setText(CurrencyFormatter.format(account.getBalance()));
    }

    private static TransactionRow toRow(Transaction transaction, DashboardViewModel model) {
        String accountLabel = "Account";
        if (model.checking() != null && model.checking().getId() == transaction.getAccountId()) {
            accountLabel = "Checking";
        } else if (model.savings() != null && model.savings().getId() == transaction.getAccountId()) {
            accountLabel = "Savings";
        }
        return new TransactionRow(
                TransactionPresentation.typeLabel(transaction.getTransactionType()),
                accountLabel,
                transaction.getDescription() == null ? "" : transaction.getDescription(),
                DateTimeDisplayFormatter.format(transaction.getCreatedAt()),
                TransactionPresentation.signedAmount(transaction)
        );
    }

    private UserSession requireSession() {
        return sceneManager.getAppContext().getSessionManager().getCurrentSession()
                .orElseGet(() -> {
                    sceneManager.showLogin();
                    return null;
                });
    }

    public static class TransactionRow {
        private final String type;
        private final String account;
        private final String description;
        private final String date;
        private final String amount;

        public TransactionRow(String type, String account, String description, String date, String amount) {
            this.type = type;
            this.account = account;
            this.description = description;
            this.date = date;
            this.amount = amount;
        }

        public String getType() {
            return type;
        }

        public String getAccount() {
            return account;
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
