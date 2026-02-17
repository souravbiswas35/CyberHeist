package com.cyberheist;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage; // Import Stage
import org.w3c.dom.ls.LSOutput;

import java.sql.*;

import java.sql.Connection;

public class GameMenuController {
    private String username;

    public Button aboutButton;
    @FXML
    private Label coinsLabel;

    @FXML
    private Label diamondsLabel;

    @FXML
    private Button miniGamesButton;

    @FXML
    private Button toolsButton;

    @FXML
    private Button upgradeButton;

    @FXML
    private Button attackButton;

    @FXML
    private Button profileButton;

    private int coins = 100; // Starting coins
    private int diamonds = 20; // Starting diamonds

    // Reference to the primary Stage, passed from CyberHeistMain -> LoginController
    private Stage primaryStage;

    // Self-reference to this GameMenuController instance, passed from LoginController
    private GameMenuController gameMenuController;

    /**
     * Setter for the primary Stage.
     * Called by the previous controller (e.g., LoginController) to provide the Stage reference.
     * @param primaryStage The primary Stage of the application.
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Setter for this GameMenuController instance itself.
     * This is used by subsequent controllers (like MiniGamesController) to call methods
     * on the correct, existing GameMenuController instance to update coins.
     * @param gameMenuController The instance of this controller.
     */
    public void setGameMenuController(GameMenuController gameMenuController) {
        this.gameMenuController = gameMenuController;
    }

    public void setUsername(String username) {
        this.username = username;  // Set the username
        System.out.println(username);
    }

    public String getUsername() {
        return username;
    }

    @FXML
    private void handleViewProfile() {
      /*  try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("PlayerProfile.fxml"));
            Parent root = loader.load();

            // Get the PlayerProfileController and pass the username
            PlayerProfileController playerProfileController = loader.getController();
            playerProfileController.setUsername(this.username);  // Pass the username here

            Scene scene = new Scene(root, 1024, 800);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            Stage stage = (Stage) primaryStage; // or get current stage
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
            // Handle error
        }*/
        SceneManager.getInstance().switchToScene("PlayerProfile.fxml");
    }


    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);  // Optional: you can set a header if needed
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void initialize(){
        updateDisplay();
    }
    @FXML
    public void loadUserData() {
        // Check if username is null or empty
        System.out.println(username);
        if (username == null || username.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "No username provided.");
            return;
        }

        // Retrieve user data (coins and diamonds) from the database
        String query = "SELECT coins, diamonds FROM users WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Set the username parameter in the query
            preparedStatement.setString(1, username);  // Use the passed username
            ResultSet resultSet = preparedStatement.executeQuery();

            // Check if user exists in the database
            if (resultSet.next()) {
                coins = resultSet.getInt("coins");
                diamonds = resultSet.getInt("diamonds");
                updateDisplay();
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "User not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load user data.");
        }
    }


    private void updateDisplay() {
        coinsLabel.setText("Coins: " + coins);
        diamondsLabel.setText("Diamonds: " + diamonds);
    }

    @FXML
    private void handleMiniGames() {
       // loadScene("MiniGames.fxml");
        SceneManager.getInstance().switchToScene("MiniGames.fxml");
    }

    @FXML
    private void handleTools() {
        //loadScene("Tools.fxml");
        SceneManager.getInstance().switchToScene("Tools.fxml");
    }

    @FXML
    private void handleUpgrade() {
        //loadScene("Upgrade.fxml");
        SceneManager.getInstance().switchToScene("Upgrade.fxml");
    }

    @FXML
    private void handleAbout(){
        SceneManager.getInstance().switchToScene("About.fxml");
       // loadScene("About.fxml");
    }

    @FXML
    private void handleAttack() {

        //loadScene("Attack.fxml");
        SceneManager.getInstance().switchToScene("Attack.fxml");
    }

    //@FXML
   // private void handleProfile() { loadScene("PlayerProfile.fxml"); }

    private void loadScene(String fxmlFile) {
        try {
            // Check if primaryStage is null (shouldn't be if setup is correct)
            if (primaryStage == null) {
                // Fallback: try to get stage from any FXML element
                primaryStage = (Stage) miniGamesButton.getScene().getWindow();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 800);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            // Pass references to the next controller based on the FXML being loaded
            if (fxmlFile.equals("MiniGames.fxml")) {
              //  MiniGamesController miniGamesController = loader.getController();
               // miniGamesController.setPrimaryStage(primaryStage);
               // miniGamesController.setGameMenuController(this.gameMenuController);
                SceneManager.getInstance().switchToScene("MiniGames.fxml");
            } else if (fxmlFile.equals("Attack.fxml")) {
                AttackController attackController = loader.getController();
                attackController.setGameMenuController(this.gameMenuController);
                // If AttackController needs primaryStage, also pass it:
                // attackController.setPrimaryStage(primaryStage);
            } else if (fxmlFile.equals("Upgrade.fxml")) {
                UpgradeController upgradeController = loader.getController(); // ✅ FIXED!
                upgradeController.setGameMenuController(this.gameMenuController);
                // If UpgradeController needs primaryStage, also pass it:
                // upgradeController.setPrimaryStage(primaryStage);
            } else if (fxmlFile.equals("Tools.fxml")) {
                // Add ToolsController handling if needed
                // ToolsController toolsController = loader.getController();
                // toolsController.setGameMenuController(this.gameMenuController);
            } else if (fxmlFile.equals("About.fxml")) {
                // Add AboutController handling if needed
                // AboutController aboutController = loader.getController();
                // aboutController.setGameMenuController(this.gameMenuController);
            } else if (fxmlFile.equals("PlayerProfile.fxml")){

            }

            // Set the new scene on the existing primary stage
            primaryStage.setScene(scene);
            // The stage will retain its full-screen or maximized state automatically.
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading scene: " + fxmlFile);
        }
    }

    public void addCoins(int amount) {
        coins += amount;
        updateDisplay();
        updateCoinsInDatabase();
        System.out.println("Coins added: " + amount + ". Total coins: " + coins);
    }

    public void addDiamonds(int amount) {
        diamonds += amount;
        updateDisplay();
        updateDiamondsInDatabase();
        System.out.println("Diamonds added: " + amount + ". Total diamonds: " + diamonds);
    }
    
    private void updateCoinsInDatabase() {
        if (username == null || username.isEmpty()) {
            System.out.println("Cannot update coins: username is not set");
            return;
        }
        
        String query = "UPDATE users SET coins = ? WHERE username = ?";
        
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            
            preparedStatement.setInt(1, coins);
            preparedStatement.setString(2, username);
            
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Coins updated in database for user: " + username);
            } else {
                System.out.println("Failed to update coins in database. No rows affected.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Database error while updating coins: " + e.getMessage());
        }
    }
    
    private void updateDiamondsInDatabase() {
        if (username == null || username.isEmpty()) {
            System.out.println("Cannot update diamonds: username is not set");
            return;
        }
        
        String query = "UPDATE users SET diamonds = ? WHERE username = ?";
        
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            
            preparedStatement.setInt(1, diamonds);
            preparedStatement.setString(2, username);
            
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Diamonds updated in database for user: " + username);
            } else {
                System.out.println("Failed to update diamonds in database. No rows affected.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Database error while updating diamonds: " + e.getMessage());
        }
    }

    public int getCoins() {
        return coins;
    }

    public int getDiamonds() {
        return diamonds;
    }


    public void setCurrentUserId(int currentUserId) {

    }
}