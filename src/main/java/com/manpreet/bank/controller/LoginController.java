package com.manpreet.bank.controller;

import com.manpreet.bank.exception.AuthenticationException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.UiErrorMapper;
import com.manpreet.bank.ui.UiFeedback;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements AppAwareController {

    @FXML
    private TextField usernameOrEmailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Label infoLabel;

    private SceneManager sceneManager;

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        UiFeedback.clear(errorLabel);
        String message = (String) sceneManager.getStage().getProperties().get("flashMessage");
        if (message != null) {
            UiFeedback.success(infoLabel, message);
            sceneManager.getStage().getProperties().remove("flashMessage");
        } else {
            UiFeedback.clear(infoLabel);
        }
    }

    @FXML
    private void handleLogin() {
        UiFeedback.clear(errorLabel);
        UiFeedback.clear(infoLabel);
        try {
            UserSession session = sceneManager.getAppContext().getAuthService()
                    .authenticate(usernameOrEmailField.getText(), passwordField.getText());
            sceneManager.getAppContext().getSessionManager().startSession(session);
            passwordField.clear();
            sceneManager.showAuthenticatedShell();
        } catch (AuthenticationException | ValidationException e) {
            UiFeedback.error(errorLabel, e.getMessage());
            passwordField.clear();
        } catch (RuntimeException e) {
            UiFeedback.error(errorLabel, UiErrorMapper.toUserMessage(e));
            passwordField.clear();
        }
    }

    @FXML
    private void goToRegister() {
        sceneManager.showRegister();
    }
}
