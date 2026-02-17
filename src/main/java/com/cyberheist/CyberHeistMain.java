package com.cyberheist;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class CyberHeistMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginPage.fxml"));
        Parent root = loader.load();

        // Get the controller for LoginPage.fxml
        LoginController loginController = loader.getController();
        // Pass the primaryStage instance to the LoginController
        loginController.setPrimaryStage(primaryStage);

        // Initialize the scene with a default windowed size
        Scene scene = new Scene(root, 1024, 800);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        // Add Enter key event handler to the scene
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Call the login method from the controller when Enter is pressed
                loginController.handleLogin();
                event.consume(); // Consume the event to prevent further processing
            }
        });

        primaryStage.setTitle("Cyber Heist - Online Hacking Simulation");
        primaryStage.setScene(scene);

        // Allow the stage to be resizable (essential for maximize and manual resizing)
        primaryStage.setResizable(true);

        // If you want to disable the default ESC key to exit full screen
        // when full screen is *later activated* by a button, you can set this:
        primaryStage.setFullScreenExitHint(""); // Removes the "Press Esc to exit full screen" hint
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // Disables Esc key exit

        primaryStage.show();

        // Request focus on the scene so it can receive key events
        scene.getRoot().requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}