package com.cyberheist;

import com.cyberheist.network.GameClient;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BattleGameController {
    @FXML private Rectangle playerHealthBar;
    @FXML private Rectangle opponentHealthBar;
    @FXML private Label playerHealthLabel;
    @FXML private Label opponentHealthLabel;
    @FXML private Label statusLabel;
    @FXML private Label playerXpLabel;
    @FXML private Label attackXpLabel;
    @FXML private Label defenseXpLabel;
    @FXML private Label opponentStatusLabel;
    @FXML private Label timerLabel;
    @FXML private ImageView playerBase;
    @FXML private ImageView opponentBase;
    @FXML private ImageView battleAnimation;
    @FXML private ComboBox<String> attackTypeComboBox;
    @FXML private ComboBox<String> defenseTypeComboBox;
    @FXML private Button attackButton;
    @FXML private Button defenseButton;
    @FXML private Button readyButton;
    @FXML private Button quitButton;
    @FXML private Button backButton;
    @FXML private Button rematchButton;
    @FXML private Button playAIButton;
    @FXML private TextArea gameMessageArea;

    private GameClient gameClient;
    private GameMenuController gameMenuController;
    private int attackXp = 20;
    private int defenseXp = 15;
    private int playerHealth = 100;
    private int opponentHealth = 100;
    private boolean gameStarted = false;
    private boolean isReady = false;
    private boolean opponentReady = false;
    private boolean isDefenseActive = false;
    private boolean opponentDefenseActive = false;
    private boolean playingAgainstAI = false;
    private long defenseActivationTime = 0;
    private int gameTimeSeconds = 60; // Changed from 120 to 60 (1 minute)
    private Timeline gameTimer;
    private String username;
    private Map<String, Integer> userToolLevels = new HashMap<>();

    // Tool usage limits
    private Map<String, Integer> attackToolUsage = new HashMap<>();
    private Map<String, Integer> defenseToolUsage = new HashMap<>();
    private final int MAX_TOOL_USAGE = 2;

    // Maps for attack and defense tools
    private Map<String, Integer> attackTools = new HashMap<>();
    private Map<String, Integer> defenseTools = new HashMap<>();

    @FXML
    private void initialize() {
        // Initialize attack tools with their cost
        attackTools.put("Brute Force", 3000);
        attackTools.put("Phishing Mail", 2000);
        attackTools.put("DDos", 5000);
        attackTools.put("KeySniffer", 1500);
        attackTools.put("Injector", 4000);
        attackTools.put("Code Virus", 8000);

        // Initialize defense tools with their cost
        defenseTools.put("FireWall Pro", 3000);
        defenseTools.put("Encryption Vault", 2500);
        defenseTools.put("EDS", 2000);
        defenseTools.put("Antivirus", 7000);
        defenseTools.put("Patch Manager", 1500);

        // Initialize tool usage counters
        for (String tool : attackTools.keySet()) {
            attackToolUsage.put(tool, 0);
        }

        for (String tool : defenseTools.keySet()) {
            defenseToolUsage.put(tool, 0);
        }

        // Populate combo boxes
        attackTypeComboBox.getItems().addAll(attackTools.keySet());
        defenseTypeComboBox.getItems().addAll(defenseTools.keySet());

        // Default selections
        attackTypeComboBox.getSelectionModel().selectFirst();
        defenseTypeComboBox.getSelectionModel().selectFirst();

        // Update XP labels
        updateXpLabels();

        // Set initial state
        disableGameControls();

        // Initialize the rematch button
        rematchButton.setDisable(true);
        rematchButton.setVisible(false);

        // Initialize back button
        backButton.setOnAction(event -> handleBack());

        // Initialize play against AI button
        playAIButton.setOnAction(event -> startAIGame());

        // Load images
        try {
            playerBase.setImage(new Image(getClass().getResourceAsStream("assets/player_base.png")));
            opponentBase.setImage(new Image(getClass().getResourceAsStream("assets/enemy_base.png")));
        } catch (Exception e) {
            System.err.println("Failed to load images: " + e.getMessage());
            // Use placeholder images if the actual images aren't available
            playerBase.setImage(null);
            opponentBase.setImage(null);
        }

        // Set initial timer display
        timerLabel.setText(formatTime(gameTimeSeconds));
    }

    public void setGameClient(GameClient client) {
        this.gameClient = client;
        setupClientMessageHandlers();
    }

    public void setGameMenuController(GameMenuController controller) {
        this.gameMenuController = controller;
        if (gameMenuController != null) {
            this.username = controller.getUsername();
            loadUserToolLevels();
            System.out.println("Username set from GameMenuController: " + this.username);
        } else {
            System.out.println("WARNING: GameMenuController is null in setGameMenuController");
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        System.out.println("The username of the game player is :::"+"  "+ username);

        // If username is set directly through this method (from SceneManager), make sure to load user tools
        if (username != null && !username.isEmpty()) {
            loadUserToolLevels();
        }
    }

    private void loadUserToolLevels() {
        if (username == null || username.isEmpty()) {
            System.out.println("WARNING: Cannot load user tool levels - username is null or empty");

            // Try to get username from SceneManager if it's not set yet
            String sceneManagerUsername = SceneManager.getInstance().getCurrentUsername();
            if (sceneManagerUsername != null && !sceneManagerUsername.isEmpty()) {
                this.username = sceneManagerUsername;
                System.out.println("Retrieved username from SceneManager: " + this.username);
            } else {
                System.out.println("Username not available from SceneManager either");
                return;
            }
        } else {
            System.out.println("Loading user tool levels for username: " + username);
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT t.name, ut.level FROM user_tools ut " +
                    "JOIN tools t ON ut.tool_id = t.id " +
                    "WHERE ut.username = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String toolName = rs.getString("name");
                int level = rs.getInt("level");
                userToolLevels.put(toolName, level);
                System.out.println("Loaded tool: " + toolName + " at level " + level);
            }

            System.out.println("Loaded " + userToolLevels.size() + " tools for user: " + username);
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to load user tool levels: " + e.getMessage());
        }
    }

    private void setupClientMessageHandlers() {
        // Clear existing handlers and add new ones
        if (gameClient != null) {
            gameClient.clearMessageHandlers();

            gameClient.addMessageHandler(message -> {
                if (message.startsWith("WAITING:")) {
                    updateStatus(message.substring("WAITING:".length()));
                } else if (message.startsWith("OPPONENT_READY:")) {
                    updateStatus("Opponent is ready!");
                    opponentReady = true;
                    checkBothPlayersReady();
                } else if (message.startsWith("GAME_STARTED:")) {
                    startGame();
                } else if (message.startsWith("CONNECTION_SUCCESS:")) {
                    String result = message.substring("CONNECTION_SUCCESS:".length());
                    addGameMessage(result);
                    updateStatus(result);
                } else if (message.startsWith("CONNECTION_ERROR:")) {
                    String error = message.substring("CONNECTION_ERROR:".length());
                    addGameMessage("Connection error: " + error);
                    updateStatus("Connection error! " + error);
                    handleConnectionError();
                } else if (message.startsWith("RECONNECTED:")) {
                    String result = message.substring("RECONNECTED:".length());
                    addGameMessage(result);
                    updateStatus("Reconnected to server!");
                } else if (message.startsWith("CONNECTION_FAILED:")) {
                    String error = message.substring("CONNECTION_FAILED:".length());
                    addGameMessage("Connection failed: " + error);
                    updateStatus("Connection failed!");
                    handleConnectionError();
                } else if (message.startsWith("ATTACK_RESULT:")) {
                    String result = message.substring("ATTACK_RESULT:".length());
                    addGameMessage(result);
                    playAttackAnimation();
                } else if (message.startsWith("UNDER_ATTACK:")) {
                    String result = message.substring("UNDER_ATTACK:".length());
                    addGameMessage("Enemy attack: " + result);
                    handleIncomingAttack(result);
                } else if (message.startsWith("DEFENSE_ACTIVATED:")) {
                    String result = message.substring("DEFENSE_ACTIVATED:".length());
                    addGameMessage(result);
                    activateDefense();
                } else if (message.startsWith("OPPONENT_DEFENSE:")) {
                    addGameMessage("Opponent activated defense");
                    updateOpponentStatus("DEFENSE ACTIVE");
                    opponentDefenseActive = true;
                } else if (message.startsWith("HEALTH_UPDATE:")) {
                    String[] parts = message.substring("HEALTH_UPDATE:".length()).split(":");
                    if (parts.length == 2) {
                        int myHealth = Integer.parseInt(parts[0]);
                        int theirHealth = Integer.parseInt(parts[1]);
                        updateHealthBars(myHealth, theirHealth);
                    }
                } else if (message.startsWith("GAME_OVER:")) {
                    String result = message.substring("GAME_OVER:".length());
                    handleGameOver(result);
                } else if (message.startsWith("OPPONENT_LEFT:")) {
                    handleOpponentLeft();
                } else if (message.startsWith("REMATCH_REQUEST:")) {
                    handleRematchRequest();
                } else if (message.startsWith("REMATCH_ACCEPTED:")) {
                    handleRematchAccepted();
                } else if (message.startsWith("REMATCH_DENIED:")) {
                    handleRematchDenied();
                } else if (message.startsWith("READY_FOR_REMATCH:")) {
                    // New message to handle both players ready for rematch
                    handleBothPlayersReadyForRematch();
                }
            });
        }
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    private void updateOpponentStatus(String status) {
        Platform.runLater(() -> opponentStatusLabel.setText(status));
    }

    private void addGameMessage(String message) {
        Platform.runLater(() -> {
            // TextArea doesn't support HTML formatting, so we'll use a prefix to indicate color
            if (message.contains("received damage") || message.contains("Enemy attack")) {
                // Red text for incoming attacks
                gameMessageArea.appendText("❌ " + message + "\n");
            } else if (message.contains("dealt damage") || message.contains("launched a") || message.contains("attack")) {
                // Green text for outgoing attacks
                gameMessageArea.appendText("✓ " + message + "\n");
            } else if (message.contains("defense")) {
                // Blue text for defense messages
                gameMessageArea.appendText("🛡️ " + message + "\n");
            } else {
                // Default for other messages
                gameMessageArea.appendText(message + "\n");
            }

            // Auto-scroll to bottom
            gameMessageArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void updateXpLabels() {
        Platform.runLater(() -> {
            attackXpLabel.setText("Attack XP: " + attackXp);
            defenseXpLabel.setText("Defense XP: " + defenseXp);
            playerXpLabel.setText("ATTACK XP: " + attackXp + " | DEFENSE XP: " + defenseXp);
        });
    }

    private void updateHealthBars(int playerHealth, int opponentHealth) {
        Platform.runLater(() -> {
            this.playerHealth = playerHealth;
            this.opponentHealth = opponentHealth;

            // Update player health bar
            double playerHealthPercent = (double) playerHealth / 100.0;
            playerHealthBar.setWidth(400 * playerHealthPercent);
            playerHealthLabel.setText(playerHealth + "%");

            // Update opponent health bar
            double opponentHealthPercent = (double) opponentHealth / 100.0;
            opponentHealthBar.setWidth(400 * opponentHealthPercent);
            opponentHealthLabel.setText(opponentHealth + "%");

            // Check for game over
            if (playerHealth <= 0 || opponentHealth <= 0) {
                endGame();
            }
        });
    }

    private void disableGameControls() {
        Platform.runLater(() -> {
            attackButton.setDisable(true);
            defenseButton.setDisable(true);
            attackTypeComboBox.setDisable(true);
            defenseTypeComboBox.setDisable(true);
            rematchButton.setDisable(true);
            rematchButton.setVisible(false);
        });
    }

    private void enableGameControls() {
        Platform.runLater(() -> {
            attackButton.setDisable(false);
            defenseButton.setDisable(false);
            attackTypeComboBox.setDisable(false);
            defenseTypeComboBox.setDisable(false);
            readyButton.setDisable(true);
            playAIButton.setDisable(true);
        });
    }

    private void checkBothPlayersReady() {
        if (isReady && opponentReady) {
            updateStatus("Both players ready! Game starting...");
        }
    }

    private void startGame() {
        Platform.runLater(() -> {
            gameStarted = true;
            enableGameControls();
            updateStatus("Game started! Your turn to attack or defend.");
            addGameMessage("Game has started! Use your attack and defense abilities wisely.");

            // Start the game timer
            startGameTimer();
        });
    }

    private void playAttackAnimation() {
        Platform.runLater(() -> {
            try {
                battleAnimation.setImage(new Image(getClass().getResourceAsStream("assets/attack_animation.gif")));
                battleAnimation.setVisible(true);

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.seconds(2), event -> battleAnimation.setVisible(false))
                );
                timeline.play();
            } catch (Exception e) {
                System.err.println("Failed to play attack animation: " + e.getMessage());
            }
        });
    }

    private void playUnderAttackAnimation() {
        Platform.runLater(() -> {
            try {
                battleAnimation.setImage(new Image(getClass().getResourceAsStream("assets/under_attack_animation.gif")));
                battleAnimation.setVisible(true);

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.seconds(2), event -> battleAnimation.setVisible(false))
                );
                timeline.play();
            } catch (Exception e) {
                System.err.println("Failed to play under attack animation: " + e.getMessage());
            }
        });
    }

    private void playDefenseAnimation() {
        Platform.runLater(() -> {
            try {
                battleAnimation.setImage(new Image(getClass().getResourceAsStream("assets/defense_animation.gif")));
                battleAnimation.setVisible(true);

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.seconds(2), event -> battleAnimation.setVisible(false))
                );
                timeline.play();
            } catch (Exception e) {
                System.err.println("Failed to play defense animation: " + e.getMessage());
            }
        });
    }

    private void handleGameOver(String result) {
        Platform.runLater(() -> {
            gameStarted = false;
            disableGameControls();
            stopGameTimer();

            int diamondsWon = 0;

            System.out.println("Game over with result: " + result);
            System.out.println("Current username: " + username);

            if (result.contains("won")) {
                // Extra reward for winning before time runs out
                if (gameTimeSeconds > 0) {
                    diamondsWon = 30; // 20 for winning + 10 extra for before time
                    updateStatus("You won! +" + diamondsWon + " Diamonds (Including time bonus!)");
                } else {
                    diamondsWon = 20;
                    updateStatus("You won! +" + diamondsWon + " Diamonds");
                }

                // Double-check that the username is available
                if (username == null || username.isEmpty()) {
                    System.err.println("WARNING: Username is null or empty in handleGameOver, cannot update diamonds");

                    // Try to get username from SceneManager
                    String sceneManagerUsername = SceneManager.getInstance().getCurrentUsername();
                    if (sceneManagerUsername != null && !sceneManagerUsername.isEmpty()) {
                        username = sceneManagerUsername;
                        System.out.println("Retrieved username from SceneManager: " + username);
                    }
                }

                // Update diamonds in the database directly
                if (username != null && !username.isEmpty()) {
                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String query = "UPDATE users SET diamonds = diamonds + ? WHERE username = ?";
                        PreparedStatement stmt = conn.prepareStatement(query);
                        stmt.setInt(1, diamondsWon);
                        stmt.setString(2, username);
                        int rowsUpdated = stmt.executeUpdate();

                        System.out.println("Direct database update - Diamonds added: " + diamondsWon +
                                " for user: " + username +
                                " - Rows updated: " + rowsUpdated);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        System.err.println("Error updating diamonds in database: " + e.getMessage());
                    }
                }

                // Update diamonds through the game menu controller if available
                if (gameMenuController != null) {
                    System.out.println("Updating diamonds through GameMenuController: +" + diamondsWon);
                    gameMenuController.addDiamonds(diamondsWon);
                } else {
                    System.out.println("WARNING: GameMenuController is null, cannot update diamonds in UI");
                }

                // Update game history in database
                updateGameHistory("BattleGame", "win", playerHealth);
            } else {
                updateStatus("You lost!");
                // Update game history in database
                updateGameHistory("BattleGame", "loss", playerHealth);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText(result);
            alert.showAndWait();

            // Enable rematch button
            rematchButton.setDisable(false);
            rematchButton.setVisible(true);
        });
    }
    @FXML
    private void handleExitToAttack(){
        SceneManager.getInstance().switchToScene("Attack.fxml");
    }

    private void handleOpponentLeft() {
        Platform.runLater(() -> {
            gameStarted = false;
            disableGameControls();
            stopGameTimer();

            updateStatus("Opponent left the game. You win by default! +20 Diamonds");

            // Update diamonds in the database directly when opponent leaves
            if (username != null && !username.isEmpty()) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String query = "UPDATE users SET diamonds = diamonds + ? WHERE username = ?";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setInt(1, 20);
                    stmt.setString(2, username);
                    int rowsUpdated = stmt.executeUpdate();

                    System.out.println("Direct database update - Diamonds added: 20" +
                            " for user: " + username +
                            " - Rows updated: " + rowsUpdated);
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Error updating diamonds in database: " + e.getMessage());
                }
            }

            // Update diamonds through the game menu controller if available
            if (gameMenuController != null) {
                gameMenuController.addDiamonds(20);
            }

            // Update game history in database
            updateGameHistory("BattleGame", "win", playerHealth);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Opponent Left");
            alert.setHeaderText(null);
            alert.setContentText("Your opponent has left the game. You win by default!");
            alert.showAndWait();
        });
    }

    @FXML
    private void handleReady() {
        if (!isReady) {
            isReady = true;
            readyButton.setText("WAITING...");
            readyButton.setDisable(true);
            updateStatus("You are ready! Waiting for opponent...");

            if (gameClient != null && !playingAgainstAI) {
                System.out.println("Sending ready signal to server");
                gameClient.sendReady();

                // Add a small delay and check if game hasn't started yet
                new Thread(() -> {
                    try {
                        // Wait 5 seconds and check if we're still not in game
                        Thread.sleep(5000);
                        if (!gameStarted) {
                            Platform.runLater(() -> {
                                System.out.println("Game didn't start after ready signal, checking connection");
                                if (gameClient.isConnected()) {
                                    System.out.println("Client still connected, sending ready signal again");
                                    gameClient.sendReady(); // Try sending ready signal again
                                } else {
                                    System.out.println("Client disconnected after ready signal");
                                    handleConnectionError();
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

            } else if (playingAgainstAI) {
                // Simulate AI getting ready after a short delay
                Timeline timeline = new Timeline(new KeyFrame(
                        Duration.seconds(1.5),
                        event -> {
                            opponentReady = true;
                            updateStatus("AI opponent is ready!");
                            checkBothPlayersReady();
                            startGame();
                        }
                ));
                timeline.play();
            }

            if (opponentReady) {
                checkBothPlayersReady();
            }
        }
    }

    @FXML
    private void handleAttack() {
        if (!gameStarted || attackXp <= 0) {
            return;
        }

        String selectedValue = attackTypeComboBox.getValue();
        if (selectedValue == null) return;

        // Extract the actual tool name from the combo box value (removes the usage count part)
        String attackType = selectedValue;
        if (selectedValue.contains("(")) {
            attackType = selectedValue.substring(0, selectedValue.lastIndexOf("(")).trim();
        }

        if (attackType != null) {
            // Check if tool usage limit is reached
            int usageCount = attackToolUsage.getOrDefault(attackType, 0);
            if (usageCount >= MAX_TOOL_USAGE) {
                addGameMessage("You've reached the usage limit for " + attackType + "!");
                return;
            }

            // Increment usage counter
            attackToolUsage.put(attackType, usageCount + 1);

            attackXp--;
            updateXpLabels();

            if (gameClient != null && !playingAgainstAI) {
                // Convert attack type for server communication (no spaces)
                String serverAttackType = attackType.replace(" ", "_").toUpperCase();
                gameClient.sendAttack(serverAttackType);
            } else if (playingAgainstAI) {
                // Calculate damage based on attack tool value
                int damage = calculateDamage(attackType);

                // Apply defense reduction if AI has defense active
                if (opponentDefenseActive) {
                    damage = (int) (damage * 0.6); // 40% damage reduction
                    addGameMessage("AI's defense reduced your attack damage!");
                }

                // Update AI's health
                opponentHealth -= damage;
                if (opponentHealth < 0) opponentHealth = 0;
                updateHealthBars(playerHealth, opponentHealth);

                addGameMessage("You dealt " + damage + " damage with " + attackType + "!");
                playAttackAnimation();

                // AI response after a short delay
                Timeline timeline = new Timeline(new KeyFrame(
                        Duration.seconds(1.2),
                        event -> handleAIAction()
                ));
                timeline.play();
            }

            addGameMessage("You launched a " + attackType + " attack!");

            // Disable attack button temporarily
            attackButton.setDisable(true);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(1), event -> attackButton.setDisable(attackXp <= 0))
            );
            timeline.play();

            // Update combo box to show usage count
            updateAttackComboBoxItems();
        }
    }

    @FXML
    private void handleDefense() {
        if (!gameStarted || defenseXp <= 0) {
            return;
        }

        String selectedValue = defenseTypeComboBox.getValue();
        if (selectedValue == null) return;

        // Extract the actual tool name from the combo box value (removes the usage count part)
        String defenseType = selectedValue;
        if (selectedValue.contains("(")) {
            defenseType = selectedValue.substring(0, selectedValue.lastIndexOf("(")).trim();
        }

        if (defenseType != null) {
            // Check if tool usage limit is reached
            int usageCount = defenseToolUsage.getOrDefault(defenseType, 0);
            if (usageCount >= MAX_TOOL_USAGE) {
                addGameMessage("You've reached the usage limit for " + defenseType + "!");
                return;
            }

            // Increment usage counter
            defenseToolUsage.put(defenseType, usageCount + 1);

            defenseXp--;
            updateXpLabels();

            if (gameClient != null && !playingAgainstAI) {
                gameClient.sendDefense();
            } else if (playingAgainstAI) {
                activateDefense();
                addGameMessage("You activated " + defenseType + " defense!");
            }

            addGameMessage("You activated " + defenseType + " defense!");

            // Disable defense button temporarily
            defenseButton.setDisable(true);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(1), event -> defenseButton.setDisable(defenseXp <= 0))
            );
            timeline.play();

            // Update combo box to show usage count
            updateDefenseComboBoxItems();
        }
    }

    private void activateDefense() {
        isDefenseActive = true;
        defenseActivationTime = System.currentTimeMillis();
        playDefenseAnimation();

        // Schedule the deactivation after 5 seconds
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(5),
                event -> {
                    isDefenseActive = false;
                    addGameMessage("Your defense has expired!");
                }
        ));
        timeline.play();
    }

    private void handleIncomingAttack(String attackInfo) {
        playUnderAttackAnimation();

        // Extract damage from the attack info
        String damageStr = attackInfo.replaceAll("\\D+", "");
        int incomingDamage;

        try {
            incomingDamage = Integer.parseInt(damageStr);
        } catch (NumberFormatException e) {
            incomingDamage = 10; // Default if we can't parse
        }

        // Apply defense reduction if active
        if (isDefenseActive) {
            int originalDamage = incomingDamage;
            incomingDamage = (int) (incomingDamage * 0.6); // 40% damage reduction
            addGameMessage("Your defense reduced incoming damage from " + originalDamage + " to " + incomingDamage + "!");
        }

        // Update player health
        playerHealth -= incomingDamage;
        if (playerHealth < 0) playerHealth = 0;

        // If playing against AI, we need to update the health display manually
        if (playingAgainstAI) {
            updateHealthBars(playerHealth, opponentHealth);
        }
    }

    @FXML
    private void handleQuit() {
        Alert confirmQuit = new Alert(Alert.AlertType.CONFIRMATION);
        confirmQuit.setTitle("Confirm Quit");
        confirmQuit.setHeaderText(null);
        confirmQuit.setContentText("Are you sure you want to quit the game? You will lose if you quit.");

        confirmQuit.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                quitGame();
            }
        });
    }

    @FXML
    private void handleBack() {
        // First check if game is in progress
        if (gameStarted) {
            Alert confirmQuit = new Alert(Alert.AlertType.CONFIRMATION);
            confirmQuit.setTitle("Confirm Exit");
            confirmQuit.setHeaderText(null);
            confirmQuit.setContentText("Game in progress. Quitting will count as a loss. Continue?");

            Optional<ButtonType> result = confirmQuit.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.OK) {
                return; // User canceled
            }
        }

        // Stop game timer and disconnect
        stopGameTimer();
        if (gameClient != null && !playingAgainstAI) {
            gameClient.disconnect();
        }

        // Navigate back to game menu
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    @FXML
    private void handleRematch() {
        if (gameClient != null && !playingAgainstAI) {
            // Reset the rematch controls first
            rematchButton.setText("REMATCH REQUESTED");
            rematchButton.setDisable(true);

            // Send rematch request to the server
            if (gameClient.isConnected()) {
                System.out.println("Sending rematch request");
                gameClient.sendAttack("REMATCH_REQUEST");
                updateStatus("Rematch requested. Waiting for opponent response...");
            } else {
                addGameMessage("Cannot request rematch: not connected to server");
                rematchButton.setText("REMATCH");
                rematchButton.setDisable(false);

                // Show reconnection dialog
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText("Not connected to server");
                alert.setContentText("Would you like to return to the Attack screen to reconnect?");

                ButtonType returnButton = new ButtonType("Return to Attack Screen");
                ButtonType cancelButton = new ButtonType("Stay Here", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(returnButton, cancelButton);
                alert.showAndWait().ifPresent(type -> {
                    if (type == returnButton) {
                        handleExitToAttack();
                    }
                });
            }
        } else if (playingAgainstAI) {
            // Immediately restart game against AI
            resetGameState();
            startAIGame();
        }
    }

    private void handleRematchRequest() {
        Platform.runLater(() -> {
            Alert rematchRequest = new Alert(Alert.AlertType.CONFIRMATION);
            rematchRequest.setTitle("Rematch Request");
            rematchRequest.setHeaderText(null);
            rematchRequest.setContentText("Your opponent wants a rematch. Accept?");

            rematchRequest.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (gameClient != null && gameClient.isConnected()) {
                        System.out.println("Accepting rematch request");
                        gameClient.sendAttack("REMATCH_ACCEPTED");
                        resetGameState();
                        isReady = true;
                        // Send ready for rematch to establish both players are ready
                        gameClient.sendAttack("READY_FOR_REMATCH");
                    } else {
                        addGameMessage("Cannot accept rematch: not connected to server");
                    }
                } else {
                    if (gameClient != null && gameClient.isConnected()) {
                        System.out.println("Declining rematch request");
                        gameClient.sendAttack("REMATCH_DENIED");
                    }
                }
            });
        });
    }

    private void handleRematchAccepted() {
        Platform.runLater(() -> {
            addGameMessage("Rematch accepted! Starting new game...");
            resetGameState();
            isReady = true;

            // Send ready for rematch to establish both players are ready
            if (gameClient != null && gameClient.isConnected()) {
                System.out.println("Sending READY_FOR_REMATCH signal");
                gameClient.sendAttack("READY_FOR_REMATCH");
            } else {
                addGameMessage("Connection error during rematch");
            }
        });
    }

    private void handleRematchDenied() {
        Platform.runLater(() -> {
            addGameMessage("Opponent declined rematch.");
            rematchButton.setText("REMATCH");
            rematchButton.setDisable(false);
        });
    }

    private void handleBothPlayersReadyForRematch() {
        // Only start game when both players have sent READY_FOR_REMATCH
        if (isReady) {
            Platform.runLater(() -> {
                System.out.println("Both players ready for rematch, starting new game");
                startGame();
            });
        }
    }

    private void resetGameState() {
        Platform.runLater(() -> {
            System.out.println("Resetting game state");
            playerHealth = 100;
            opponentHealth = 100;
            attackXp = 20;
            defenseXp = 15;
            gameTimeSeconds = 60;
            isDefenseActive = false;
            opponentDefenseActive = false;
            isReady = false;
            opponentReady = false;
            gameStarted = false;

            // Reset tool usage counters
            for (String tool : attackTools.keySet()) {
                attackToolUsage.put(tool, 0);
            }

            for (String tool : defenseTools.keySet()) {
                defenseToolUsage.put(tool, 0);
            }

            // Update UI
            updateHealthBars(playerHealth, opponentHealth);
            updateXpLabels();
            timerLabel.setText(formatTime(gameTimeSeconds));

            gameMessageArea.clear();
            addGameMessage("New game started. Get ready!");

            readyButton.setDisable(false);
            readyButton.setText("READY");

            // Hide rematch button, show ready button
            rematchButton.setVisible(false);
            readyButton.setVisible(true);

            disableGameControls();

            // Reset combo boxes
            updateAttackComboBoxItems();
            updateDefenseComboBoxItems();
        });
    }

    private void startAIGame() {
        playingAgainstAI = true;
        resetGameState();
        updateStatus("Starting game against AI...");

        // Hide the ready button in AI mode - player is automatically ready
        readyButton.setText("WAITING...");
        isReady = true;

        // Simulate AI getting ready after a short delay
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(1.5),
                event -> {
                    opponentReady = true;
                    updateStatus("AI opponent is ready!");
                    startGame();
                }
        ));
        timeline.play();
    }

    private void handleAIAction() {
        if (!gameStarted) return;

        // AI is now more aggressive with higher attack chance (85% vs previous 75%)
        if (Math.random() < 0.85) { // 85% chance to attack
            // AI now strategically chooses more powerful attacks more often
            String[] attackTypes = {"Brute Force", "Phishing Mail", "DDos", "KeySniffer", "Injector", "Code Virus"};
            int[] weights = {2, 2, 3, 2, 3, 5}; // Higher weights for stronger attacks

            // Weighted random selection
            int totalWeight = 0;
            for (int weight : weights) totalWeight += weight;

            int randomWeight = new Random().nextInt(totalWeight);
            int currentWeight = 0;
            String attackType = attackTypes[0]; // Default

            for (int i = 0; i < attackTypes.length; i++) {
                currentWeight += weights[i];
                if (randomWeight < currentWeight) {
                    attackType = attackTypes[i];
                    break;
                }
            }

            int damage = calculateAIDamage(attackType);

            // Apply defense reduction if player has defense active
            if (isDefenseActive) {
                damage = (int) (damage * 0.6);
                addGameMessage("Your defense reduced the AI's attack damage!");
            }

            // Update player's health
            playerHealth -= damage;
            if (playerHealth < 0) playerHealth = 0;
            updateHealthBars(playerHealth, opponentHealth);

            addGameMessage("AI attacks with " + attackType + " and deals " + damage + " damage!");
            playUnderAttackAnimation();

        } else { // 15% chance to defend
            opponentDefenseActive = true;
            updateOpponentStatus("DEFENSE ACTIVE");
            addGameMessage("AI activated defense!");

            // Schedule defense deactivation
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(5),
                    event -> {
                        opponentDefenseActive = false;
                        updateOpponentStatus("Waiting...");
                        addGameMessage("AI's defense has expired!");
                    }
            ));
            timeline.play();
        }

        // AI sometimes does a second action quickly if player's health is high
        if (playerHealth > 70 && Math.random() < 0.3) {
            // Schedule another attack after a short delay
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(2.5),
                    event -> handleAIAction()
            ));
            timeline.play();
        }
    }

    private int calculateDamage(String attackType) {
        // Base damage for different attack types - reduced by around 30%
        int baseDamage;

        switch(attackType) {
            case "Brute Force": baseDamage = 10; break; // was 15
            case "Phishing Mail": baseDamage = 7; break; // was 10
            case "DDos": baseDamage = 14; break; // was 20
            case "KeySniffer": baseDamage = 6; break; // was 8
            case "Injector": baseDamage = 12; break; // was 18
            case "Code Virus": baseDamage = 18; break; // was 25
            default: baseDamage = 4; // was 5
        }

        // Apply tool level multiplier if available (8% increase per level instead of 10%)
        Integer toolLevel = userToolLevels.get(attackType);
        if (toolLevel != null && toolLevel > 1) {
            baseDamage = (int)(baseDamage * (1 + (toolLevel - 1) * 0.08));
        }

        // Apply tool value multiplier (higher cost = more damage) - slightly reduced factor
        Integer toolValue = attackTools.get(attackType);
        if (toolValue != null) {
            double valueFactor = 1.0 + (toolValue / 12000.0); // was 10000.0
            baseDamage = (int)(baseDamage * valueFactor);
        }

        // Add some randomness (±15%) - reduced from ±20%
        double randomFactor = 0.85 + (Math.random() * 0.3); // 0.85 to 1.15
        baseDamage = (int)(baseDamage * randomFactor);

        return baseDamage;
    }

    private int calculateAIDamage(String attackType) {
        // Increased AI damage by about 20% from the player's damage calculation
        int baseDamage;

        switch(attackType) {
            case "Brute Force": baseDamage = 12; break; // was 15, then 10
            case "Phishing Mail": baseDamage = 9; break; // was 10, then 7
            case "DDos": baseDamage = 17; break; // was 20, then 14
            case "KeySniffer": baseDamage = 8; break; // was 8, then 6
            case "Injector": baseDamage = 15; break; // was 18, then 12
            case "Code Virus": baseDamage = 22; break; // was 25, then 18
            default: baseDamage = 5; // was 5, then 4
        }

        // AI gets better accuracy (less randomness)
        double randomFactor = 0.90 + (Math.random() * 0.25); // 0.90 to 1.15
        baseDamage = (int)(baseDamage * randomFactor);

        return baseDamage;
    }

    private void quitGame() {
        // Disconnect from the server
        if (gameClient != null && !playingAgainstAI) {
            gameClient.disconnect();
        }

        // Stop the game timer
        stopGameTimer();

        // Return to the main menu
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameMenu.fxml"));
//            Parent root = loader.load();
//            Scene scene = new Scene(root, 1024, 800);
//            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
//
//            Stage stage = (Stage) quitButton.getScene().getWindow();
//            stage.setScene(scene);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    private void startGameTimer() {
        if (gameTimer != null) {
            gameTimer.stop();
        }

        gameTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    gameTimeSeconds--;
                    timerLabel.setText(formatTime(gameTimeSeconds));

                    if (gameTimeSeconds <= 0) {
                        endGameByTimeout();
                    }
                })
        );

        gameTimer.setCycleCount(gameTimeSeconds);
        gameTimer.play();
    }

    private void stopGameTimer() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private void endGameByTimeout() {
        gameStarted = false;
        stopGameTimer();

        // Determine winner based on health
        if (playerHealth > opponentHealth) {
            handleGameOver("Time's up! You won with more health remaining!");

            // Update game history
            updateGameHistory("BattleGame", "win", playerHealth);

            // No need to award diamonds here as they're handled in handleGameOver
        } else if (opponentHealth > playerHealth) {
            handleGameOver("Time's up! You lost with less health remaining.");

            // Update game history
            updateGameHistory("BattleGame", "loss", playerHealth);
        } else {
            handleGameOver("Time's up! The game ended in a draw.");

            // Update game history
            updateGameHistory("BattleGame", "draw", playerHealth);

            // No need to award diamonds here as they're handled in handleGameOver
        }
    }

    private void endGame() {
        gameStarted = false;
        stopGameTimer();

        // Game ended due to health reaching zero
        if (playerHealth <= 0 && opponentHealth > 0) {
            handleGameOver("You lost! Your base was destroyed.");
            updateGameHistory("BattleGame", "loss", 0);
        } else if (opponentHealth <= 0 && playerHealth > 0) {
            String message = "You won! You destroyed the enemy base!";
            int diamondsWon = gameTimeSeconds > 0 ? 30 : 20;

            handleGameOver(message);
            updateGameHistory("BattleGame", "win", playerHealth);

            // No need to award diamonds here as they're handled in handleGameOver
        } else {
            // Both reached zero at the same time - unlikely but possible
            handleGameOver("Draw! Both bases were destroyed simultaneously.");
            updateGameHistory("BattleGame", "draw", 0);

            // No need to award diamonds here as they're handled in handleGameOver
        }
    }

    private void updateGameHistory(String gameType, String result, int score) {
        System.out.println("Updating game history - Game: " + gameType + ", Result: " + result + ", Score: " + score);

        if (username == null || username.isEmpty()) {
            System.err.println("ERROR: Cannot update game history - username is null or empty");

            // Try to get username from SceneManager as a fallback
            String sceneManagerUsername = SceneManager.getInstance().getCurrentUsername();
            if (sceneManagerUsername != null && !sceneManagerUsername.isEmpty()) {
                username = sceneManagerUsername;
                System.out.println("Retrieved username from SceneManager: " + username);
            } else {
                System.err.println("CRITICAL ERROR: No username available from any source, game history update aborted");
                return;
            }
        }

        System.out.println("Updating game history for user: " + username);

        try (Connection conn = DatabaseConnection.getConnection()) {
            // First check if database connection is valid
            if (conn == null) {
                System.err.println("CRITICAL ERROR: Database connection is null");
                return;
            }

            // Insert game history record
            String insertQuery = "INSERT INTO game_history (username, game_type, result, score, date, duration) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, gameType);
            insertStmt.setString(3, result);
            insertStmt.setInt(4, score);
            insertStmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            insertStmt.setString(6, (120 - gameTimeSeconds) + " seconds");
            int rowsInserted = insertStmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Game history record inserted successfully");
            } else {
                System.err.println("Failed to insert game history record - no rows affected");
            }

            // Update user statistics
            StringBuilder updateQuery = new StringBuilder("UPDATE users SET total_games = total_games + 1");

            // Add win or loss increment based on result
            if (result.equalsIgnoreCase("win")) {
                updateQuery.append(", wins = wins + 1");
            } else if (result.equalsIgnoreCase("loss")) {
                updateQuery.append(", losses = losses + 1");
            }

            // Add score update
            updateQuery.append(", total_score = total_score + ? WHERE username = ?");

            PreparedStatement updateStmt = conn.prepareStatement(updateQuery.toString());
            updateStmt.setInt(1, score);
            updateStmt.setString(2, username);
            int rowsUpdated = updateStmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Player stats updated successfully for: " + username +
                        ", Result: " + result +
                        ", Score: " + score);
            } else {
                System.err.println("Failed to update player stats - no rows affected");

                // If update failed, check if user exists
                String checkUserQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery);
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    int userCount = rs.getInt(1);
                    System.out.println("User check: found " + userCount + " users with username: " + username);
                }
            }

            // Add coins for playing (win or lose)
            int coinsEarned = result.equalsIgnoreCase("win") ? 500 : 100;
            String coinQuery = "UPDATE users SET coins = coins + ? WHERE username = ?";

            PreparedStatement coinStmt = conn.prepareStatement(coinQuery);
            coinStmt.setInt(1, coinsEarned);
            coinStmt.setString(2, username);
            int coinRows = coinStmt.executeUpdate();

            if (coinRows > 0) {
                System.out.println("Coins updated in database: +" + coinsEarned + " for " + username);
            } else {
                System.err.println("Failed to update coins in database - no rows affected");
            }

            // No longer updating diamonds here as they are already handled in the specific game outcome methods

            if (gameMenuController != null) {
                gameMenuController.addCoins(coinsEarned);
                // Diamonds are now handled directly in the game outcome methods (handleGameOver, endGame, etc.)
                // to avoid duplicate rewards
            } else {
                System.out.println("WARNING: GameMenuController is null, cannot update UI");
            }

            addGameMessage("Earned " + coinsEarned + " coins!");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database error in updateGameHistory: " + e.getMessage());
        }
    }

    private void updateAttackComboBoxItems() {
        String currentSelection = attackTypeComboBox.getValue();
        attackTypeComboBox.getItems().clear();

        for (String tool : attackTools.keySet()) {
            int usageCount = attackToolUsage.getOrDefault(tool, 0);
            String displayText = tool + " (" + usageCount + "/" + MAX_TOOL_USAGE + ")";
            attackTypeComboBox.getItems().add(displayText);

            // If this was previously selected, select it again
            if (tool.equals(currentSelection)) {
                attackTypeComboBox.setValue(displayText);
            }
        }

        // If nothing was selected, select the first item
        if (attackTypeComboBox.getValue() == null && !attackTypeComboBox.getItems().isEmpty()) {
            attackTypeComboBox.getSelectionModel().selectFirst();
        }
    }

    private void updateDefenseComboBoxItems() {
        String currentSelection = defenseTypeComboBox.getValue();
        defenseTypeComboBox.getItems().clear();

        for (String tool : defenseTools.keySet()) {
            int usageCount = defenseToolUsage.getOrDefault(tool, 0);
            String displayText = tool + " (" + usageCount + "/" + MAX_TOOL_USAGE + ")";
            defenseTypeComboBox.getItems().add(displayText);

            // If this was previously selected, select it again
            if (tool.equals(currentSelection)) {
                defenseTypeComboBox.setValue(displayText);
            }
        }

        // If nothing was selected, select the first item
        if (defenseTypeComboBox.getValue() == null && !defenseTypeComboBox.getItems().isEmpty()) {
            defenseTypeComboBox.getSelectionModel().selectFirst();
        }
    }

    private void handleConnectionError() {
        Platform.runLater(() -> {
            gameStarted = false;
            disableGameControls();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText("Lost connection to server");
            alert.setContentText("Would you like to return to the Attack screen to reconnect?");

            ButtonType returnButton = new ButtonType("Return to Attack Screen");
            ButtonType cancelButton = new ButtonType("Stay Here", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(returnButton, cancelButton);
            alert.showAndWait().ifPresent(type -> {
                if (type == returnButton) {
                    handleExitToAttack();
                }
            });
        });
    }
}