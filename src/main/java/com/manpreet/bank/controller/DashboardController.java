package com.manpreet.bank.controller;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.BankingDialogs;
import com.manpreet.bank.ui.DashboardDataLoader;
import com.manpreet.bank.ui.DashboardViewModel;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.TransactionRowViewModel;
import com.manpreet.bank.ui.TransactionViewMapper;
import com.manpreet.bank.ui.UiErrorMapper;
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
    private TableView<TransactionRowViewModel> transactionsTable;
    @FXML
    private TableColumn<TransactionRowViewModel, String> typeColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> accountColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> descriptionColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> dateColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> amountColumn;

    private SceneManager sceneManager;
    private MainShellController shellController;
    private DashboardViewModel viewModel;

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        typeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().typeLabel()));
        accountColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().accountLabel()));
        descriptionColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().description()));
        dateColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().dateLabel()));
        amountColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().signedAmount()));
        amountColumn.setCellFactory(column -> signedAmountCell());
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

            Map<Long, Account> accountsById = new HashMap<>();
            if (viewModel.checking() != null) {
                accountsById.put(viewModel.checking().getId(), viewModel.checking());
            }
            if (viewModel.savings() != null) {
                accountsById.put(viewModel.savings().getId(), viewModel.savings());
            }
            List<TransactionRowViewModel> rows = TransactionViewMapper.toRows(
                    viewModel.recentTransactions(), accountsById);
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
