package com.manpreet.bank.controller;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.service.TransactionFilter;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.CsvExportTarget;
import com.manpreet.bank.ui.PaginationState;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.TransactionRowViewModel;
import com.manpreet.bank.ui.TransactionViewMapper;
import com.manpreet.bank.ui.UiErrorMapper;
import com.manpreet.bank.ui.UiFeedback;
import com.manpreet.bank.ui.UiWindows;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
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
    private VBox emptyStateBox;
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
    private Label detailPlaceholderLabel;
    @FXML
    private GridPane detailGrid;
    @FXML
    private Label detailTypeLabel;
    @FXML
    private Label detailAmountLabel;
    @FXML
    private Label detailAccountLabel;
    @FXML
    private Label detailRelatedLabel;
    @FXML
    private Label detailDescriptionLabel;
    @FXML
    private Label detailDateLabel;

    private SceneManager sceneManager;
    private MainShellController shellController;
    private final PaginationState pagination = new PaginationState();
    /**
     * True while the status shows a filter, load, or export error; the next successful load clears it.
     * Export results such as "Export complete." stay until the user acts again.
     */
    private boolean statusClearsOnNextLoad;
    private Map<Long, Account> accountsById = Map.of();
    private TransactionFilter activeFilter = TransactionFilter.recent(PaginationState.PAGE_SIZE);

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        configureTable();
        configureFilters();
        clearDetails();
        clearStatus();
        loadSummaries();
        applyFilters(true);
    }

    @Override
    public void setShellController(MainShellController shellController) {
        this.shellController = shellController;
    }

    /**
     * Moves keyboard focus to the transaction search field (shell Shortcut+F).
     */
    public void focusSearchField() {
        if (searchField != null) {
            searchField.requestFocus();
            searchField.selectAll();
        }
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
        clearStatus();
        clearDetails();
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
        FileChooser.ExtensionFilter csvFilter = new FileChooser.ExtensionFilter("CSV files", "*.csv");
        chooser.getExtensionFilters().add(csvFilter);
        chooser.setSelectedExtensionFilter(csvFilter);
        chooser.setInitialFileName("transactions-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv");
        var file = chooser.showSaveDialog(transactionsTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        exportTo(session, filter, file.toPath());
    }

    private void exportTo(UserSession session, TransactionFilter filter, Path chosen) {
        Path target = CsvExportTarget.withCsvExtension(chosen);
        if (CsvExportTarget.requiresOverwriteConfirmation(chosen, target) && !confirmReplace(target)) {
            showResult(UiFeedback.Kind.INFO, "Export canceled.");
            return;
        }
        try {
            byte[] bytes = sceneManager.getAppContext().getTransactionExportService()
                    .exportCsvBytes(session.userId(), filter);
            Files.write(target, bytes);
            showResult(UiFeedback.Kind.SUCCESS, "Transactions exported.");
        } catch (IOException e) {
            showError("Unable to export transactions. Please try again.");
        } catch (RuntimeException e) {
            showError(UiErrorMapper.toUserMessage(e));
        }
    }

    private boolean confirmReplace(Path target) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Export transactions");
        dialog.setHeaderText("Replace existing file?");
        dialog.setContentText(target.getFileName() + " already exists. Replacing it overwrites its contents.");
        ButtonType replace = new ButtonType("Replace", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(replace, ButtonType.CANCEL);
        dialog.getDialogPane().getStyleClass().add("dialog-container");
        dialog.initOwner(UiWindows.from(statusLabel));
        sceneManager.getAppContext().getThemeManager().applyTo(dialog.getDialogPane());
        return dialog.showAndWait().filter(replace::equals).isPresent();
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
                clearDetails();
                return;
            }
            showDetails(selected);
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
            emptyStateBox.setVisible(empty);
            emptyStateBox.setManaged(empty);
            transactionsTable.setVisible(!empty);
            transactionsTable.setManaged(!empty);

            pageLabel.setText(pagination.pageLabel());
            previousPageButton.setDisable(!pagination.hasPrevious());
            nextPageButton.setDisable(!pagination.hasNext());
            if (statusClearsOnNextLoad) {
                clearStatus();
            }
        } catch (RuntimeException e) {
            showError(UiErrorMapper.toUserMessage(e));
            transactionsTable.getItems().clear();
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
            transactionsTable.setVisible(false);
            transactionsTable.setManaged(false);
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
            showError(UiErrorMapper.toUserMessage(e));
        }
    }

    private TransactionFilter buildFilterWithoutPaging() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start != null && end != null && end.isBefore(start)) {
            showError("End date cannot be before start date.");
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

    private void showDetails(TransactionRowViewModel row) {
        detailPlaceholderLabel.setVisible(false);
        detailPlaceholderLabel.setManaged(false);
        detailGrid.setVisible(true);
        detailGrid.setManaged(true);
        detailTypeLabel.setText(row.typeLabel());
        detailAmountLabel.setText(row.signedAmount());
        detailAmountLabel.getStyleClass().removeAll("amount-credit", "amount-debit");
        detailAmountLabel.getStyleClass().add(row.credit() ? "amount-credit" : "amount-debit");
        detailAccountLabel.setText(row.accountLabel());
        detailRelatedLabel.setText(row.relatedAccountLabel() == null ? "—" : row.relatedAccountLabel());
        detailDescriptionLabel.setText(row.description() == null || row.description().isBlank()
                ? "—"
                : row.description());
        detailDateLabel.setText(row.dateLabel());
    }

    private void clearDetails() {
        detailPlaceholderLabel.setVisible(true);
        detailPlaceholderLabel.setManaged(true);
        detailGrid.setVisible(false);
        detailGrid.setManaged(false);
        detailTypeLabel.setText("");
        detailAmountLabel.setText("");
        detailAccountLabel.setText("");
        detailRelatedLabel.setText("");
        detailDescriptionLabel.setText("");
        detailDateLabel.setText("");
    }

    private void showError(String message) {
        UiFeedback.error(statusLabel, message);
        statusClearsOnNextLoad = true;
    }

    private void showResult(UiFeedback.Kind kind, String message) {
        UiFeedback.show(statusLabel, kind, message);
        statusClearsOnNextLoad = false;
    }

    private void clearStatus() {
        UiFeedback.clear(statusLabel);
        statusClearsOnNextLoad = false;
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
