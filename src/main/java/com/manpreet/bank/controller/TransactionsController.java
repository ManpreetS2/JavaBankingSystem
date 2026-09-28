package com.manpreet.bank.controller;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.service.TransactionFilter;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.PaginationState;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.TransactionRowViewModel;
import com.manpreet.bank.ui.TransactionViewMapper;
import com.manpreet.bank.ui.UiErrorMapper;
import com.manpreet.bank.util.AccountNumberFormatter;
import com.manpreet.bank.util.CurrencyFormatter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

public class TransactionsController implements AppAwareController, ShellAwareController {

    @FXML
    private Label statusLabel;
    @FXML
    private Label matchingCountLabel;
    @FXML
    private Label depositsTotalLabel;
    @FXML
    private Label withdrawalsTotalLabel;
    @FXML
    private Label transfersInTotalLabel;
    @FXML
    private Label transfersOutTotalLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<AccountOption> accountFilter;
    @FXML
    private ComboBox<TypeOption> typeFilter;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Label emptyStateLabel;
    @FXML
    private TableView<TransactionRowViewModel> transactionsTable;
    @FXML
    private TableColumn<TransactionRowViewModel, String> dateColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> accountColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> typeColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> descriptionColumn;
    @FXML
    private TableColumn<TransactionRowViewModel, String> amountColumn;
    @FXML
    private Button previousPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Label pageLabel;
    @FXML
    private TextArea detailArea;

    private SceneManager sceneManager;
    private MainShellController shellController;
    private final PaginationState pagination = new PaginationState();
    private Map<Long, Account> accountsById = Map.of();
    private TransactionFilter activeFilter = TransactionFilter.recent(PaginationState.PAGE_SIZE);

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        configureTable();
        configureFilters();
        loadSummaries();
        applyFilters(true);
    }

    @Override
    public void setShellController(MainShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    private void handleSearch() {
        applyFilters(true);
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        accountFilter.getSelectionModel().selectFirst();
        typeFilter.getSelectionModel().selectFirst();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        statusLabel.getStyleClass().setAll("success-text");
        statusLabel.setText("");
        detailArea.clear();
        applyFilters(true);
    }

    @FXML
    private void handlePreviousPage() {
        if (!pagination.hasPrevious()) {
            return;
        }
        pagination.previous();
        loadPage();
    }

    @FXML
    private void handleNextPage() {
        if (!pagination.hasNext()) {
            return;
        }
        pagination.next();
        loadPage();
    }

    @FXML
    private void handleExportCsv() {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        TransactionFilter filter = buildFilterWithoutPaging();
        if (filter == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export transactions");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        chooser.setInitialFileName("transactions-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv");
        var file = chooser.showSaveDialog(transactionsTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        Path target = file.toPath();
        if (!target.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            target = target.resolveSibling(target.getFileName().toString() + ".csv");
        }
        try {
            byte[] bytes = sceneManager.getAppContext().getTransactionExportService()
                    .exportCsvBytes(session.userId(), filter);
            Files.write(target, bytes);
            statusLabel.getStyleClass().setAll("success-text");
            statusLabel.setText("Export complete.");
        } catch (IOException e) {
            statusLabel.getStyleClass().setAll("error-text");
            statusLabel.setText("Unable to export transactions. Please try again.");
        } catch (RuntimeException e) {
            statusLabel.getStyleClass().setAll("error-text");
            statusLabel.setText(UiErrorMapper.toUserMessage(e));
        }
    }

    private void configureTable() {
        dateColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().dateLabel()));
        accountColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().accountLabel()));
        typeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().typeLabel()));
        descriptionColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().description()));
        amountColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().signedAmount()));
        amountColumn.setCellFactory(column -> new TableCell<>() {
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
        });
        transactionsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                detailArea.clear();
                return;
            }
            detailArea.setText(formatDetails(selected));
        });
    }

    private void configureFilters() {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        List<Account> accounts = sceneManager.getAppContext().getAccountService()
                .getAccountsForUser(session.userId());
        accountsById = accounts.stream().collect(Collectors.toMap(Account::getId, Function.identity(), (a, b) -> a, HashMap::new));

        accountFilter.setItems(FXCollections.observableArrayList());
        accountFilter.getItems().add(new AccountOption(null, "All accounts"));
        accounts.stream()
                .sorted((a, b) -> a.getAccountType().compareTo(b.getAccountType()))
                .forEach(account -> accountFilter.getItems().add(new AccountOption(
                        account.getId(),
                        AccountNumberFormatter.displayLabel(account.getAccountType(), account.getAccountNumber())
                )));
        accountFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(AccountOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public AccountOption fromString(String string) {
                return null;
            }
        });
        accountFilter.getSelectionModel().selectFirst();

        typeFilter.setItems(FXCollections.observableArrayList(
                new TypeOption(null, "All types"),
                new TypeOption(TransactionType.DEPOSIT, "Deposit"),
                new TypeOption(TransactionType.WITHDRAWAL, "Withdrawal"),
                new TypeOption(TransactionType.TRANSFER_IN, "Transfer In"),
                new TypeOption(TransactionType.TRANSFER_OUT, "Transfer Out")
        ));
        typeFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(TypeOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public TypeOption fromString(String string) {
                return null;
            }
        });
        typeFilter.getSelectionModel().selectFirst();
    }

    private void applyFilters(boolean resetPage) {
        TransactionFilter filter = buildFilterWithoutPaging();
        if (filter == null) {
            return;
        }
        activeFilter = filter;
        if (resetPage) {
            pagination.reset();
        }
        loadPage();
    }

    private void loadPage() {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        try {
            long total = sceneManager.getAppContext().getTransactionService()
                    .count(session.userId(), activeFilter);
            pagination.setTotalCount(total);
            matchingCountLabel.setText(Long.toString(total));

            TransactionFilter pageFilter = activeFilter.withPaging(
                    pagination.getPageSize(),
                    pagination.getOffset()
            );
            List<Transaction> page = sceneManager.getAppContext().getTransactionService()
                    .search(session.userId(), pageFilter);
            List<TransactionRowViewModel> rows = TransactionViewMapper.toRows(page, accountsById);
            transactionsTable.setItems(FXCollections.observableArrayList(rows));

            boolean empty = rows.isEmpty();
            emptyStateLabel.setVisible(empty);
            emptyStateLabel.setManaged(empty);
            transactionsTable.setVisible(!empty);
            transactionsTable.setManaged(!empty);

            pageLabel.setText(pagination.pageLabel());
            previousPageButton.setDisable(!pagination.hasPrevious());
            nextPageButton.setDisable(!pagination.hasNext());
            statusLabel.getStyleClass().setAll("success-text");
            if (statusLabel.getText() == null || statusLabel.getText().startsWith("End date")
                    || statusLabel.getText().startsWith("Something")
                    || statusLabel.getText().startsWith("Unable")) {
                statusLabel.setText("");
            }
        } catch (RuntimeException e) {
            statusLabel.getStyleClass().setAll("error-text");
            statusLabel.setText(UiErrorMapper.toUserMessage(e));
            transactionsTable.getItems().clear();
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
        }
    }

    private void loadSummaries() {
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        try {
            var tx = sceneManager.getAppContext().getTransactionService();
            depositsTotalLabel.setText(CurrencyFormatter.format(tx.totalDeposits(session.userId(), null, null)));
            withdrawalsTotalLabel.setText(CurrencyFormatter.format(tx.totalWithdrawals(session.userId(), null, null)));
            transfersInTotalLabel.setText(CurrencyFormatter.format(tx.totalTransfersIn(session.userId(), null, null)));
            transfersOutTotalLabel.setText(CurrencyFormatter.format(tx.totalTransfersOut(session.userId(), null, null)));
        } catch (RuntimeException e) {
            statusLabel.getStyleClass().setAll("error-text");
            statusLabel.setText(UiErrorMapper.toUserMessage(e));
        }
    }

    private TransactionFilter buildFilterWithoutPaging() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start != null && end != null && end.isBefore(start)) {
            statusLabel.getStyleClass().setAll("error-text");
            statusLabel.setText("End date cannot be before start date.");
            return null;
        }
        AccountOption account = accountFilter.getSelectionModel().getSelectedItem();
        TypeOption type = typeFilter.getSelectionModel().getSelectedItem();
        String search = searchField.getText();
        return new TransactionFilter(
                account == null ? null : account.id(),
                type == null ? null : type.type(),
                start,
                end,
                search == null || search.isBlank() ? null : search.trim(),
                PaginationState.PAGE_SIZE,
                0
        );
    }

    private static String formatDetails(TransactionRowViewModel row) {
        StringBuilder details = new StringBuilder();
        details.append("Type: ").append(row.typeLabel()).append('\n');
        details.append("Amount: ").append(row.signedAmount()).append('\n');
        details.append("Account: ").append(row.accountLabel()).append('\n');
        if (row.relatedAccountLabel() != null) {
            details.append("Related account: ").append(row.relatedAccountLabel()).append('\n');
        }
        details.append("Description: ").append(row.description()).append('\n');
        details.append("Date: ").append(row.dateLabel());
        return details.toString();
    }

    private UserSession requireSession() {
        return sceneManager.getAppContext().getSessionManager().getCurrentSession()
                .orElseGet(() -> {
                    sceneManager.showLogin();
                    return null;
                });
    }

    public record AccountOption(Long id, String label) {
    }

    public record TypeOption(TransactionType type, String label) {
    }
}
