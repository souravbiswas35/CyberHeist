package com.cyberheist;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button signupButton;

    // FXML elements for window controls (from LoginPage.fxml modification)
    @FXML
    private Button minimizeButton;
    @FXML
    private Button maximizeButton;
    @FXML
    private Button fullscreenButton;

    // Field to hold the primary Stage reference
    private Stage primaryStage;

    /**
     * Setter method to receive the primary Stage from CyberHeistMain.
     * This is crucial for controlling the window state.
     * @param stage The primary Stage of the application.
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    private void initialize() {
        // You can add any initialization logic here if needed
    }



@FXML
void handleLogin() {
    String username = usernameField.getText();
    String password = passwordField.getText();

    // Query the database to validate the username and password
    String query = "SELECT * FROM users WHERE username = ? AND password = ?";

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(query)) {

        // Set the parameters for the query
        preparedStatement.setString(1, username);
        preparedStatement.setString(2, password);

        // Execute the query
        ResultSet resultSet = preparedStatement.executeQuery();

        // Check if user exists with the provided credentials
        if (resultSet.next()) {
            try {
                // Load the Game Menu FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("GameMenu.fxml"));
                Parent root = loader.load();

                // Get the GameMenuController and pass the primary Stage to it
                GameMenuController gameMenuController = loader.getController();
                if (primaryStage != null) {
                    System.out.println(primaryStage);
                    gameMenuController.setPrimaryStage(primaryStage);
                    // Also pass the gameMenuController instance itself for coin updates
                    gameMenuController.setUsername(username); // Self-reference for mini-games return
                    gameMenuController.loadUserData();
                }



                // Create a new Scene. The size here is for windowed mode if it's not full screen.
                Scene scene = new Scene(root, 1024, 800);
                scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());


                SceneManager sceneManager = SceneManager.getInstance();
                sceneManager.setPrimaryStage(primaryStage);
                sceneManager.setCurrentUsername(username);

                sceneManager.switchToScene("GameMenu.fxml");

                // Set the new scene on the primary stage
                if (primaryStage != null) {
                    primaryStage.setScene(scene);
                } else {
                    // Fallback if primaryStage wasn't set
                    Stage stage = (Stage) loginButton.getScene().getWindow();
                    stage.setScene(scene);
                }

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load Game Menu.");
            }
        } else {
            // If the user is not found or the password is incorrect
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Credentials\nPlease check your username and password.");
        }

    } catch (SQLException e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect to the database.");
    }
}


    @FXML
    private void handleSignup() {
        // Now using the loadScene helper method
        loadScene("SignUpPage.fxml", "Sign Up");
    }

    /**
     * Helper method to load a new FXML scene into the primary stage.
     * @param fxmlPath The path to the FXML file to load.
     * @param title The title to set for the stage.
     */
    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Set the primary stage if it's not already set (e.g., if this is the initial scene load)
            // Or get it from any control's scene if already displayed
            if (primaryStage == null) {
                primaryStage = (Stage) loginButton.getScene().getWindow();
            }

            Scene newScene;
            // For signup, use a fixed size for the new window, consistent with previous signup logic
            // For game menu, use larger size as previously defined
            if (fxmlPath.equals("SignUpPage.fxml")) {
                newScene = new Scene(root, 1024, 800); // Using the preferred height/width from SignUpPage.fxml design
            } else {
                newScene = new Scene(root, 1024, 800); // Default for game menu or other larger scenes
                newScene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            }


            primaryStage.setScene(newScene);
            primaryStage.setTitle(title);
            primaryStage.show();

            // Special handling for GameMenuController if loading GameMenu.fxml
            if (fxmlPath.equals("GameMenu.fxml")) {
                GameMenuController gameMenuController = loader.getController();
                if (gameMenuController != null) {
                    gameMenuController.setPrimaryStage(primaryStage);
                    gameMenuController.setGameMenuController(gameMenuController);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load " + title + " page: " + fxmlPath);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "General Error", "An unexpected error occurred while loading " + title + " page.");
        }
    }

    /**
     * Handles minimizing the application window.
     */
    @FXML
    private void handleMinimize() {
        if (primaryStage != null) {
            primaryStage.setIconified(true);
        }
    }

    /**
     * Handles maximizing/restoring the application window.
     */
    @FXML
    private void handleMaximize() {
        if (primaryStage != null) {
            primaryStage.setMaximized(!primaryStage.isMaximized());
        }
    }

    /**
     * Toggles full-screen mode for the application window.
     */
    @FXML
    private void handleToggleFullScreen() {
        if (primaryStage != null) {
            primaryStage.setFullScreen(!primaryStage.isFullScreen());
        }
    }

    // Helper method for showing alerts
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
