package com.cyberheist;

import com.cyberheist.network.GameClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.Random;

public class AttackController {

    @FXML
    private Button createRoomButton;

    @FXML
    private Button joinRoomButton;

    @FXML
    private TextField roomCodeField;

    @FXML
    private Button backButton;

    @FXML
    private Button mainMenuButton;

    private Random random = new Random();

    // Field to hold the reference to GameMenuController
    private GameMenuController gameMenuController;
    private GameClient gameClient;

    // Setter for GameMenuController, called when GameMenu loads this scene
    public void setGameMenuController(GameMenuController controller) {
        this.gameMenuController = controller;
    }

    @FXML
    private void initialize() {
        // Initialize game client
        gameClient = new GameClient();
        
        // Set up message handlers for client
        gameClient.addMessageHandler(this::handleServerMessage);
    }

    private void handleServerMessage(String message) {
        System.out.println("Client received message: " + message);
        
        if (message.startsWith("ROOM_CREATED:")) {
            String roomCode = message.split(":")[1];
            showRoomCreatedAlert(roomCode);
        } else if (message.startsWith("ROOM_JOINED:")) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Room Joined");
                alert.setHeaderText("Connected to Game Room");
                alert.setContentText("Successfully joined the room! Waiting for game to start...");
                alert.showAndWait();
            });
        } else if (message.startsWith("JOIN_FAILED:")) {
            String errorMessage = message.split(":")[1];
            showAlert(Alert.AlertType.ERROR, "Join Failed", errorMessage);
            // Enable the room joining button again
            Platform.runLater(() -> joinRoomButton.setDisable(false));
        } else if (message.startsWith("CONNECTION_SUCCESS:")) {
            // This is a new message sent after both players are connected
            System.out.println("Received CONNECTION_SUCCESS signal");
            startBattleGame();
        } else if (message.startsWith("GAME_START")) {
            System.out.println("Received GAME_START signal");
            startBattleGame();
        } else if (message.startsWith("CONNECTION_ERROR:") || message.startsWith("CONNECTION_FAILED:")) {
            String errorMsg = message.contains("CONNECTION_ERROR:") ? 
                    message.substring("CONNECTION_ERROR:".length()) : 
                    message.substring("CONNECTION_FAILED:".length());
            showAlert(Alert.AlertType.ERROR, "Connection Error", "Error: " + errorMsg);
            // Re-enable buttons after error
            Platform.runLater(() -> {
                createRoomButton.setDisable(false);
                joinRoomButton.setDisable(false);
            });
        }
    }

    @FXML
    private void handleCreateRoom() {
        String serverIp = roomCodeField.getText().trim();
        
        if (serverIp.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Server IP Required");
            alert.setContentText("Please enter a valid IP address to start server.");
            alert.showAndWait();
            return;
        }
        
        // Disable the button to prevent multiple clicks
        createRoomButton.setDisable(true);
        
        // Show connecting dialog
        Alert connectingAlert = new Alert(Alert.AlertType.INFORMATION);
        connectingAlert.setTitle("Connecting");
        connectingAlert.setHeaderText("Connecting to server...");
        connectingAlert.setContentText("Attempting to connect to server at " + serverIp);
        
        // Show the alert without waiting
        Platform.runLater(() -> connectingAlert.show());
        
        // Generate a random room code
        String roomCode = String.format("%04d", random.nextInt(10000));
        
        // Connect to the server in a background thread
        new Thread(() -> {
            boolean connected = gameClient.connect(serverIp);
            
            // Close the connecting dialog
            Platform.runLater(() -> connectingAlert.close());
            
            if (connected) {
                // Create a new room
                gameClient.createRoom(roomCode);
                System.out.println("Room creation request sent with code: " + roomCode);
            } else {
                // Re-enable the button if connection fails
                Platform.runLater(() -> createRoomButton.setDisable(false));
                showAlert(Alert.AlertType.ERROR, "Connection Failed", "Could not connect to the server at " + serverIp);
            }
        }).start();
    }
    
    private void showRoomCreatedAlert(String roomCode) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Room Created");
            alert.setHeaderText("Multiplayer Room");
            alert.setContentText("Room Code: " + roomCode +
                    "\nShare this code with another player to start the game!" +
                    "\n\nWaiting for opponent to join...");
            alert.showAndWait();
        });
    }

    @FXML
    private void handleJoinRoom() {
        String serverIp = roomCodeField.getText().trim();

        if (serverIp.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Server IP Required");
            alert.setContentText("Please enter the server's IP address.");
            alert.showAndWait();
            return;
        }

        // Disable the button to prevent multiple clicks
        joinRoomButton.setDisable(true);
        
        // Create a custom dialog to get the room code
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Enter Room Code");
        dialog.setHeaderText("Enter the 4-digit room code");
        
        // Set the button types
        ButtonType joinButtonType = new ButtonType("Join", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(joinButtonType, ButtonType.CANCEL);
        
        // Create the room code field
        TextField roomCodeField = new TextField();
        roomCodeField.setPromptText("Room Code (e.g. 1234)");
        
        // Create layout for dialog
        VBox content = new VBox(10);
        content.getChildren().add(new Label("Room Code:"));
        content.getChildren().add(roomCodeField);
        dialog.getDialogPane().setContent(content);
        
        // Request focus on the room code field
        Platform.runLater(roomCodeField::requestFocus);
        
        // Convert the result to a room code when the join button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == joinButtonType) {
                return roomCodeField.getText();
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            // User cancelled, re-enable the button
            joinRoomButton.setDisable(false);
            return;
        }
        
        String roomCode = result.get().trim();
        
        // Validate room code
        if (roomCode.length() != 4 || !roomCode.matches("\\d+")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Room Code");
            alert.setHeaderText("Invalid Format");
            alert.setContentText("Room code must be a 4-digit number.");
            alert.showAndWait();
            joinRoomButton.setDisable(false);
            return;
        }
        
        // Show connecting dialog
        Alert connectingAlert = new Alert(Alert.AlertType.INFORMATION);
        connectingAlert.setTitle("Connecting");
        connectingAlert.setHeaderText("Connecting to server...");
        connectingAlert.setContentText("Attempting to connect to " + serverIp + " and join room " + roomCode);
        
        // Show the alert without waiting
        Platform.runLater(() -> connectingAlert.show());
        
        // Connect to server in background thread
        final String finalRoomCode = roomCode;
        new Thread(() -> {
            boolean connected = gameClient.connect(serverIp);
            
            // Close the connecting dialog
            Platform.runLater(() -> connectingAlert.close());
            
            if (connected) {
                // Join the specified room
                System.out.println("Connected to server, joining room: " + finalRoomCode);
                gameClient.joinRoom(finalRoomCode);
                
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Joining Room");
                    alert.setHeaderText("Connecting...");
                    alert.setContentText("Attempting to join room: " + finalRoomCode +
                            "\n\nConnecting to opponent...");
                    alert.showAndWait();
                });
            } else {
                // Re-enable button if connection fails
                Platform.runLater(() -> joinRoomButton.setDisable(false));
                showAlert(Alert.AlertType.ERROR, "Connection Failed", "Could not connect to the server at " + serverIp);
            }
        }).start();
    }

    private void startBattleGame() {
        try {
            System.out.println("Starting battle game...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("BattleGame.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 800);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            BattleGameController controller = loader.getController();
            controller.setGameClient(gameClient);
            
            // Make sure gameMenuController is set properly
            if (gameMenuController != null) {
                controller.setGameMenuController(gameMenuController);
                
                // Explicitly set the username from gameMenuController
                String username = gameMenuController.getUsername();
                System.out.println("Setting username in BattleGameController: " + username);
                controller.setUsername(username);
            } else {
                // Try to get username from SceneManager
                String username = SceneManager.getInstance().getCurrentUsername();
                System.out.println("GameMenuController is null, using username from SceneManager: " + username);
                controller.setUsername(username);
            }

            Platform.runLater(() -> {
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                System.out.println("Switched to battle game scene");
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Game Load Error", "Failed to load the game: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        // Disconnect from the server if connected
        if (gameClient != null && gameClient.isConnected()) {
            gameClient.disconnect();
        }
        
        // Navigate back to the GameMenu scene
        //loadScene("GameMenu.fxml");
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    @FXML
    private void handleMainMenu() {
        // Disconnect from the server if connected
        if (gameClient != null && gameClient.isConnected()) {
            gameClient.disconnect();
        }
        
        // Navigate back to the GameMenu scene
        //loadScene("GameMenu.fxml");
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    private void loadScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 800);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            // If navigating back to GameMenu, ensure the GameMenuController instance is passed
            if (fxmlFile.equals("GameMenu.fxml")) {
                // If returning to GameMenu, the GameMenuController should already exist
                // and its state (coins) should be updated from other minigames/features.
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}