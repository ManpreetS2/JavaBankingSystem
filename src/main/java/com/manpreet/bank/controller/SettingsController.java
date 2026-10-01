package com.manpreet.bank.controller;

import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.ProfileSummary;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.Theme;
import com.manpreet.bank.ui.ThemeManager;
import com.manpreet.bank.ui.ThemeOptions;
import com.manpreet.bank.ui.UiErrorMapper;
import com.manpreet.bank.ui.UiFeedback;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

public class SettingsController implements AppAwareController {

    @FXML
    private Label statusLabel;
    @FXML
    private Label displayNameLabel;
    @FXML
    private Label usernameHandleLabel;
    @FXML
    private Label firstNameValueLabel;
    @FXML
    private Label lastNameValueLabel;
    @FXML
    private Label usernameValueLabel;
    @FXML
    private Label emailValueLabel;
    @FXML
    private RadioButton lightThemeOption;
    @FXML
    private RadioButton darkThemeOption;

    private final ToggleGroup themeGroup = new ToggleGroup();
    private SceneManager sceneManager;
    private boolean syncingThemeSelection;

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        UiFeedback.clear(statusLabel);
        UserSession session = requireSession();
        if (session == null) {
            return;
        }
        bindProfile(ProfileSummary.from(session));
        bindThemeOptions();
    }

    private void bindProfile(ProfileSummary profile) {
        displayNameLabel.setText(profile.displayName());
        usernameHandleLabel.setText(profile.usernameHandle());
        firstNameValueLabel.setText(profile.firstName());
        lastNameValueLabel.setText(profile.lastName());
        usernameValueLabel.setText(profile.username());
        emailValueLabel.setText(profile.email());
    }

    private void bindThemeOptions() {
        configureThemeOption(lightThemeOption, Theme.LIGHT);
        configureThemeOption(darkThemeOption, Theme.DARK);
        selectCurrentTheme();
        themeGroup.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (!syncingThemeSelection && selected != null && selected.getUserData() instanceof Theme theme) {
                applyTheme(theme);
            }
        });
    }

    private void configureThemeOption(RadioButton option, Theme theme) {
        option.setToggleGroup(themeGroup);
        option.setUserData(theme);
        option.setText(ThemeOptions.label(theme));
        option.setAccessibleText(ThemeOptions.accessibleName(theme));
    }

    private void applyTheme(Theme theme) {
        try {
            themeManager().setTheme(theme);
            UiFeedback.success(statusLabel, ThemeOptions.appliedMessage(theme));
        } catch (RuntimeException e) {
            UiFeedback.error(statusLabel, UiErrorMapper.toUserMessage(e));
            selectCurrentTheme();
        }
    }

    /**
     * Mirrors the active theme in the selector without re-applying it.
     */
    private void selectCurrentTheme() {
        Toggle current = themeManager().getCurrentTheme() == Theme.DARK ? darkThemeOption : lightThemeOption;
        syncingThemeSelection = true;
        try {
            themeGroup.selectToggle(current);
        } finally {
            syncingThemeSelection = false;
        }
    }

    private ThemeManager themeManager() {
        return sceneManager.getAppContext().getThemeManager();
    }

    private UserSession requireSession() {
        return sceneManager.getAppContext().getSessionManager().getCurrentSession()
                .orElseGet(() -> {
                    sceneManager.showLogin();
                    return null;
                });
    }
}
