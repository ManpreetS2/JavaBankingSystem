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
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public Stage getStage() {
        return stage;
    }

    public void showLogin() {
        show("/fxml/login.fxml", "Banking System — Login");
    }

    public void showRegister() {
        show("/fxml/register.fxml", "Banking System — Register");
    }

    public void showDashboard() {
        show("/fxml/dashboard.fxml", "Banking System — Dashboard");
    }

    private void show(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AppAwareController aware) {
                aware.setSceneManager(this);
            }
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1200, 760);
                scene.getStylesheets().add(
                        Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm()
                );
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load screen: " + fxmlPath, e);
        }
    }
}
