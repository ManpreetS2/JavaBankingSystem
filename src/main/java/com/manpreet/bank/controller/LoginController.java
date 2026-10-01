package com.manpreet.bank.controller;

import com.manpreet.bank.exception.AuthenticationException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.AuthFormValidator;
import com.manpreet.bank.ui.FieldFeedback;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.UiErrorMapper;
import com.manpreet.bank.ui.UiFeedback;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

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
        FieldFeedback.clearWhenEdited(usernameOrEmailField);
        FieldFeedback.clearWhenEdited(passwordField);

        Map<Object, Object> stageProperties = sceneManager.getStage().getProperties();
        String message = (String) stageProperties.remove("flashMessage");
        if (message != null) {
            UiFeedback.success(infoLabel, message);
        } else {
            UiFeedback.clear(infoLabel);
        }

        String identifier = (String) stageProperties.remove("loginIdentifier");
        if (identifier != null) {
            usernameOrEmailField.setText(identifier);
        }
        TextInputControl initialFocus = identifier != null ? passwordField : usernameOrEmailField;
        Platform.runLater(initialFocus::requestFocus);
    }

    @FXML
    private void handleLogin() {
        clearFeedback();
        Optional<AuthFormValidator.FieldError> fieldError = AuthFormValidator.validateLogin(
                usernameOrEmailField.getText(), passwordField.getText());
        if (fieldError.isPresent()) {
            showFieldError(fieldError.get());
            return;
        }
        try {
            UserSession session = sceneManager.getAppContext().getAuthService()
                    .authenticate(usernameOrEmailField.getText(), passwordField.getText());
            sceneManager.getAppContext().getSessionManager().startSession(session);
            passwordField.clear();
            sceneManager.showAuthenticatedShell();
        } catch (AuthenticationException | ValidationException e) {
            UiFeedback.error(errorLabel, e.getMessage());
            retryPassword();
        } catch (RuntimeException e) {
            UiFeedback.error(errorLabel, UiErrorMapper.toUserMessage(e));
            retryPassword();
        }
    }

    @FXML
    private void goToRegister() {
        sceneManager.showRegister();
    }

    private void showFieldError(AuthFormValidator.FieldError error) {
        TextInputControl field = error.field() == AuthFormValidator.Field.PASSWORD
                ? passwordField
                : usernameOrEmailField;
        UiFeedback.error(errorLabel, error.message());
        FieldFeedback.markInvalid(field, error.message());
        field.requestFocus();
    }

    /**
     * A failed sign-in clears the password and returns focus to it without indicating which credential was wrong.
     */
    private void retryPassword() {
        passwordField.clear();
        passwordField.requestFocus();
    }

    private void clearFeedback() {
        UiFeedback.clear(errorLabel);
        UiFeedback.clear(infoLabel);
        FieldFeedback.clear(usernameOrEmailField);
        FieldFeedback.clear(passwordField);
    }
}
