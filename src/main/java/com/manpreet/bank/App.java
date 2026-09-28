package com.manpreet.bank;

import com.manpreet.bank.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the banking application.
 * Initializes the SQLite database before launching the UI.
 */
public class App extends Application {

    private static AppContext appContext;

    @Override
    public void start(Stage primaryStage) {
        SceneManager sceneManager = new SceneManager(primaryStage, appContext);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        sceneManager.showLogin();
    }

    public static void main(String[] args) {
        try {
            appContext = new AppContext();
            appContext.initializeDatabase();
            System.out.println(
                    "Database ready at: "
                            + appContext.getDatabaseManager().getDatabasePath().toAbsolutePath()
            );
        } catch (Exception e) {
            System.err.println("FATAL: Database initialization failed.");
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
            return;
        }

        launch(args);
    }
}
