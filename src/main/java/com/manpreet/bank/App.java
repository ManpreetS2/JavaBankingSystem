package com.manpreet.bank;

import com.manpreet.bank.database.DatabaseInitializer;
import com.manpreet.bank.database.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the banking application.
 * Initializes the SQLite database before launching the UI.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label placeholder = new Label("Banking System — Foundation Ready");
        placeholder.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        StackPane root = new StackPane(placeholder);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 1200, 760);

        primaryStage.setTitle("Banking System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            new DatabaseInitializer(databaseManager).initialize();
            System.out.println("Database ready at: " + databaseManager.getDatabasePath().toAbsolutePath());
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
