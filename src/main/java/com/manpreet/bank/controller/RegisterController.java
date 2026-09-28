package com.manpreet.bank.controller;

import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.ui.AppAwareController;
import com.manpreet.bank.ui.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

    @Override
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @FXML
    private void handleRegister() {
        errorLabel.setText("");
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (password == null || !password.equals(confirm)) {
            errorLabel.setText("Password and confirmation do not match");
            clearPasswords();
            return;
        }

        try {
            sceneManager.getAppContext().getAuthService().register(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    usernameField.getText(),
                    password
            );
            clearPasswords();
            sceneManager.getStage().getProperties().put(
                    "flashMessage",
                    "Account created. Please sign in."
            );
            sceneManager.showLogin();
        } catch (ValidationException | DuplicateUserException e) {
            errorLabel.setText(e.getMessage());
            clearPasswords();
        } catch (RuntimeException e) {
            errorLabel.setText("Registration failed. Please try again.");
            clearPasswords();
        }
    }

    @FXML
    private void goToLogin() {
        sceneManager.showLogin();
    }

    private void clearPasswords() {
        passwordField.clear();
        confirmPasswordField.clear();
    }
}
