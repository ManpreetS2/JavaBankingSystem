package com.manpreet.bank.controller;

import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.SceneManager;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        shellGreetingLabel.setText("Welcome, " + session.firstName());
        shellUserLabel.setText(session.username() + " · " + session.email());
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
