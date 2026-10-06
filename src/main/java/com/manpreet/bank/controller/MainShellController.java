package com.manpreet.bank.controller;

import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.ShellKeyboardShortcuts;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainShellController implements AppAwareController {

    public enum Section {
        DASHBOARD,
        ACCOUNTS,
        TRANSACTIONS,
        SETTINGS
    }

    @FXML
    private Label shellSectionLabel;
    @FXML
    private Label shellGreetingLabel;
    @FXML
    private Label shellUserLabel;
    @FXML
    private StackPane contentHost;
    @FXML
    private VBox sidebar;
    @FXML
    private Button dashboardNavButton;
    @FXML
    private Button accountsNavButton;
    @FXML
    private Button transactionsNavButton;
    @FXML
    private Button settingsNavButton;

    private SceneManager sceneManager;
    private Section currentSection = Section.DASHBOARD;
    private TransactionsController transactionsController;

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        shellGreetingLabel.setText("Welcome, " + session.firstName());
        shellUserLabel.setText(session.username() + " · " + session.email());
        installKeyboardShortcuts();
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        loadContent("/fxml/dashboard.fxml", Section.DASHBOARD);
    }

    @FXML
    private void showAccounts() {
        loadContent("/fxml/accounts.fxml", Section.ACCOUNTS);
    }

    @FXML
    private void showTransactions() {
        loadContent("/fxml/transactions.fxml", Section.TRANSACTIONS);
    }

    @FXML
    private void showSettings() {
        loadContent("/fxml/settings.fxml", Section.SETTINGS);
    }

    @FXML
    private void toggleTheme() {
        sceneManager.getAppContext().getThemeManager().toggleTheme();
        if (currentSection == Section.SETTINGS) {
            showSettings();
        }
    }

    @FXML
    private void handleLogout() {
        clearKeyboardShortcuts();
        sceneManager.getAppContext().getSessionManager().endSession();
        sceneManager.showLogin();
    }

    public void refreshCurrentSection() {
        switch (currentSection) {
            case ACCOUNTS -> showAccounts();
            case TRANSACTIONS -> showTransactions();
            case SETTINGS -> showSettings();
            default -> showDashboard();
        }
    }

    private void installKeyboardShortcuts() {
        Scene scene = sceneManager.getStage().getScene();
        if (scene == null) {
            return;
        }
        clearKeyboardShortcuts(scene);
        bind(scene, ShellKeyboardShortcuts.Action.DASHBOARD, this::showDashboard);
        bind(scene, ShellKeyboardShortcuts.Action.ACCOUNTS, this::showAccounts);
        bind(scene, ShellKeyboardShortcuts.Action.TRANSACTIONS, this::showTransactions);
        bind(scene, ShellKeyboardShortcuts.Action.SETTINGS, this::showSettings);
        bind(scene, ShellKeyboardShortcuts.Action.FOCUS_SEARCH, this::focusTransactionsSearch);
        // Shortcut+, also opens Settings when it does not conflict with the digit mapping.
        scene.getAccelerators().put(
                new KeyCodeCombination(
                        javafx.scene.input.KeyCode.COMMA,
                        javafx.scene.input.KeyCombination.SHORTCUT_DOWN),
                this::showSettings
        );
    }

    private void clearKeyboardShortcuts() {
        Scene scene = sceneManager == null ? null : sceneManager.getStage().getScene();
        if (scene != null) {
            clearKeyboardShortcuts(scene);
        }
    }

    private static void clearKeyboardShortcuts(Scene scene) {
        for (ShellKeyboardShortcuts.Action action : ShellKeyboardShortcuts.Action.values()) {
            scene.getAccelerators().remove(ShellKeyboardShortcuts.combination(action));
        }
        scene.getAccelerators().remove(new KeyCodeCombination(
                javafx.scene.input.KeyCode.COMMA,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
    }

    private static void bind(Scene scene, ShellKeyboardShortcuts.Action action, Runnable handler) {
        scene.getAccelerators().put(ShellKeyboardShortcuts.combination(action), handler);
    }

    private void focusTransactionsSearch() {
        if (requireSession() == null) {
            return;
        }
        if (currentSection != Section.TRANSACTIONS) {
            showTransactions();
        }
        if (transactionsController != null) {
            transactionsController.focusSearchField();
        }
    }

    private void loadContent(String fxmlPath, Section section) {
        if (requireSession() == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AppAwareController aware) {
                aware.setSceneManager(sceneManager);
            }
            if (controller instanceof ShellAwareController shellAware) {
                shellAware.setShellController(this);
            }
            transactionsController = controller instanceof TransactionsController tx ? tx : null;
            contentHost.getChildren().setAll(content);
            currentSection = section;
            updateNavStyles();
        } catch (IOException e) {
            contentHost.getChildren().setAll(placeholder("Unable to load screen", "Please try again."));
        }
    }

    private void updateNavStyles() {
        setActive(dashboardNavButton, currentSection == Section.DASHBOARD);
        setActive(accountsNavButton, currentSection == Section.ACCOUNTS);
        setActive(transactionsNavButton, currentSection == Section.TRANSACTIONS);
        setActive(settingsNavButton, currentSection == Section.SETTINGS);
        if (shellSectionLabel != null) {
            shellSectionLabel.setText(switch (currentSection) {
                case DASHBOARD -> "Dashboard";
                case ACCOUNTS -> "Accounts";
                case TRANSACTIONS -> "Transactions";
                case SETTINGS -> "Settings";
            });
        }
    }

    private static void setActive(Button button, boolean active) {
        button.getStyleClass().remove("sidebar-item-active");
        if (active) {
            button.getStyleClass().add("sidebar-item-active");
        }
    }

    private UserSession requireSession() {
        return sceneManager.getAppContext().getSessionManager().getCurrentSession()
                .orElseGet(() -> {
                    sceneManager.showLogin();
                    return null;
                });
    }

    private static VBox placeholder(String title, String body) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");
        Label bodyLabel = new Label(body);
        bodyLabel.getStyleClass().add("body-secondary");
        bodyLabel.setWrapText(true);
        VBox box = new VBox(12, titleLabel, bodyLabel);
        box.getStyleClass().addAll("content-root", "empty-state-card");
        return box;
    }
}
