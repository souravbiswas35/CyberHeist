package com.cyberheist;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage; // Import Stage class
import java.sql.*;
import java.io.IOException; // Import IOException

public class SignUpController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField emailField;

    @FXML
    private Button registerButton;

    // You might want a reference to the primary stage if you need to control the main window from here
    private Stage primaryStage;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Handles the registration logic when the "Register" button is clicked.
     */
    @FXML
    private void handleRegister() {
        String name = nameField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String email = emailField.getText();

        // Basic validation (can be enhanced)
        if (name.isEmpty() || username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Registration Failed", "All fields are required!");
            return;
        }

        // Insert the new user into the database
        String query = "INSERT INTO users (name, username, password, email) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, name);
            preparedStatement.setString(2, username);
            preparedStatement.setString(3, password);
            preparedStatement.setString(4, email);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Registration Successful", "User successfully registered!");
                // Optionally, redirect the user to login screen after registration
                handleGoBack(null); // Automatically go back after successful registration
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", "Failed to register the user. Please try again.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to connect to the database.");
        }
    }

    /**
     * Helper method for showing alerts.
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Handles navigating back to the login page.
     * @param actionEvent The event that triggered this method (e.g., button click).
     */
    @FXML // Add @FXML annotation if this method is directly called from FXML
    public void handleGoBack(ActionEvent actionEvent) {
        try {
            // Load the Login Page FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginPage.fxml"));
            Parent root = loader.load();

            // Get the LoginController and pass the primary Stage to it
            LoginController loginController = loader.getController();
            // Get the current stage from the button's scene, or use primaryStage if available
            Stage stage = (Stage) (actionEvent != null ? ((Button) actionEvent.getSource()).getScene().getWindow() : (primaryStage != null ? primaryStage : null));

            // Fallback if stage is still null (shouldn't happen if called from a valid UI context)
            if (stage == null) {
                // If the method was called internally (e.g., after registration) and primaryStage wasn't set
                // You might need a more robust way to get the stage, or ensure primaryStage is always set
                // For now, let's assume it's always called from a button click or primaryStage is set.
                System.err.println("Could not determine current stage for navigation.");
                return;
            }

            // Important: Pass the same primary stage back to the LoginController
            loginController.setPrimaryStage(stage);

            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight()); // Maintain current window size
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm()); // Apply CSS

            stage.setScene(scene);
            // The stage will retain its maximized/fullscreen state if it had one before navigating away.
            // If you want to force it to a specific state (e.g., not full screen), do it here:
            // stage.setFullScreen(false);
            // stage.setMaximized(false);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the Login Page.");
        }
    }
}