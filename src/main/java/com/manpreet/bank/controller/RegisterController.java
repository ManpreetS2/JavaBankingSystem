package com.manpreet.bank.controller;

import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.AuthFormValidator;
import com.manpreet.bank.ui.AuthFormValidator.Field;
import com.manpreet.bank.ui.FieldFeedback;
import com.manpreet.bank.ui.MessageText;
import com.manpreet.bank.ui.SceneManager;
import com.manpreet.bank.ui.UiErrorMapper;
import com.manpreet.bank.ui.UiFeedback;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

public class RegisterController implements AppAwareController {

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;

    private SceneManager sceneManager;
    private final Map<Field, TextInputControl> fields = new EnumMap<>(Field.class);

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        UiFeedback.clear(errorLabel);
        fields.put(Field.FIRST_NAME, firstNameField);
        fields.put(Field.LAST_NAME, lastNameField);
        fields.put(Field.EMAIL, emailField);
        fields.put(Field.USERNAME, usernameField);
        fields.put(Field.PASSWORD, passwordField);
        fields.put(Field.CONFIRM_PASSWORD, confirmPasswordField);
        fields.values().forEach(FieldFeedback::clearWhenEdited);
        Platform.runLater(firstNameField::requestFocus);
    }

    @FXML
    private void handleRegister() {
        clearFeedback();
        Optional<AuthFormValidator.FieldError> fieldError = AuthFormValidator.validateRegistration(
                firstNameField.getText(),
                lastNameField.getText(),
                emailField.getText(),
                usernameField.getText(),
                passwordField.getText(),
                confirmPasswordField.getText()
        );
        if (fieldError.isPresent()) {
            showFieldError(fieldError.get());
            return;
        }

        try {
            UserSession session = sceneManager.getAppContext().getAuthService().register(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    usernameField.getText(),
                    passwordField.getText()
            );
            clearPasswords();
            Map<Object, Object> stageProperties = sceneManager.getStage().getProperties();
            stageProperties.put("flashMessage", "Account created. Please sign in.");
            stageProperties.put("loginIdentifier", session.username());
            sceneManager.showLogin();
        } catch (DuplicateUserException e) {
            String message = MessageText.asSentence(e.getMessage());
            UiFeedback.error(errorLabel, message);
            AuthFormValidator.fieldForDuplicate(e).map(fields::get).ifPresent(field -> {
                FieldFeedback.markInvalid(field, message);
                field.requestFocus();
            });
        } catch (ValidationException e) {
            UiFeedback.error(errorLabel, MessageText.asSentence(e.getMessage()));
        } catch (RuntimeException e) {
            UiFeedback.error(errorLabel, UiErrorMapper.toUserMessage(e));
        }
    }

    @FXML
    private void goToLogin() {
        clearPasswords();
        sceneManager.showLogin();
    }

    /**
     * Password errors clear both password fields; errors on other fields keep the entered password.
     */
    private void showFieldError(AuthFormValidator.FieldError error) {
        String message = MessageText.asSentence(error.message());
        UiFeedback.error(errorLabel, message);
        boolean passwordError = error.field() == Field.PASSWORD || error.field() == Field.CONFIRM_PASSWORD;
        if (passwordError) {
            clearPasswords();
        }
        TextInputControl field = passwordError ? passwordField : fields.get(error.field());
        FieldFeedback.markInvalid(field, message);
        field.requestFocus();
    }

    private void clearPasswords() {
        passwordField.clear();
        confirmPasswordField.clear();
    }

    private void clearFeedback() {
        UiFeedback.clear(errorLabel);
        fields.values().forEach(FieldFeedback::clear);
    }
}
