package com.manpreet.bank.ui;

import com.manpreet.bank.AppContext;
import java.io.IOException;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Loads FXML scenes and keeps a single primary stage for navigation.
 */
public class SceneManager {

    private final Stage stage;
    private final AppContext appContext;

    public SceneManager(Stage stage, AppContext appContext) {
        this.stage = Objects.requireNonNull(stage);
        this.appContext = Objects.requireNonNull(appContext);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public Stage getStage() {
        return stage;
    }

    public void showLogin() {
        showFullScreen("/fxml/login.fxml", "Banking System — Login");
    }

    public void showRegister() {
        showFullScreen("/fxml/register.fxml", "Banking System — Register");
    }

    public void showAuthenticatedShell() {
        if (appContext.getSessionManager().getCurrentSession().isEmpty()) {
            showLogin();
            return;
        }
        showFullScreen("/fxml/main-shell.fxml", "Banking System");
    }

    /**
     * Compatibility alias for authenticated navigation.
     */
    @Deprecated
    public void showDashboard() {
        showAuthenticatedShell();
    }

    private void showFullScreen(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1200, 760);
                stage.setScene(scene);
                appContext.getThemeManager().registerScene(scene);
            } else {
                scene.setRoot(root);
                appContext.getThemeManager().registerScene(scene);
            }
            if (controller instanceof AppAwareController aware) {
                aware.setSceneManager(this);
            }
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load screen: " + fxmlPath, e);
        }
    }
}
