package com.manpreet.bank;

import com.manpreet.bank.ui.SceneManager;
import java.util.Objects;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the banking application.
 * Initializes the SQLite database before launching the UI.
 */
public class App extends Application {

    private static AppContext appContext;

    @Override
    public void start(Stage primaryStage) {
        Objects.requireNonNull(appContext, "Application context was not initialized");
        applyIcon(primaryStage);
        primaryStage.setTitle(AppInfo.windowTitle());
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.setOnCloseRequest(event -> Platform.exit());

        SceneManager sceneManager = new SceneManager(primaryStage, appContext);
        sceneManager.showLogin();
    }

    @Override
    public void stop() {
        // Connections are opened per operation and closed by callers; no shared pool to shut down.
        appContext = null;
    }

    public static void main(String[] args) {
        try {
            appContext = AppStartup.createContext();
            AppStartup.initialize(appContext);
            System.out.println(AppStartup.startupSummary(appContext));
            if (AppStartup.isDemoSeedEnabled()) {
                System.out.println("Demo login: " + com.manpreet.bank.service.DemoDataSeeder.DEMO_USERNAME
                        + " / (see README for the documented demo password)");
            }
        } catch (Exception e) {
            System.err.println("FATAL: " + AppStartup.safeStartupFailureMessage(e));
            System.exit(1);
            return;
        }

        launch(args);
    }

    private static void applyIcon(Stage stage) {
        var iconUrl = App.class.getResource("/icons/app.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
    }
}
