package com.cyberheist;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UpgradeController {

    @FXML
    private VBox attackUpgradesBox;

    @FXML
    private VBox defenseUpgradesBox;

    @FXML
    private Button backButton;

    @FXML
    private Button mainMenuButton;

    @FXML
    private Label coinsLabel;

    @FXML
    private Label diamondsLabel;

    private int coins = 5000; // Starting coins for testing
    private int diamonds = 20; // Starting diamonds for testing
    private String username; // Current user's username

    // Upgrade levels (1-10)
    private Map<String, Integer> upgradeLevels = new HashMap<>();
    private Map<Integer, String> toolIdToName = new HashMap<>(); // Map tool ID to name for DB operations
    private Map<String, Integer> toolNameToId = new HashMap<>(); // Map tool name to ID for DB operations
    private static final int MAX_LEVEL = 10;
    private static final int MIN_LEVEL = 1;
    private GameMenuController gameMenuController;

    public void setGameMenuController(GameMenuController gameMenuController) {
        this.gameMenuController = gameMenuController;
        if (gameMenuController != null) {
            this.username = gameMenuController.getUsername();
            this.coins = gameMenuController.getCoins();
            this.diamonds = gameMenuController.getDiamonds();
            loadUserToolData();
            updateDisplay();
        }
    }
    
    public void setUsername(String username) {
        this.username = username;
        loadUserToolData();
    }

    // Upgrade data class
    private static class UpgradeData {
        String name;
        String description;
        int baseCoinCost;
        int baseDiamondCost;
        int id; // Tool ID from database

        UpgradeData(String name, String description, int baseCoinCost, int baseDiamondCost) {
            this.name = name;
            this.description = description;
            this.baseCoinCost = baseCoinCost;
            this.baseDiamondCost = baseDiamondCost;
        }
        
        UpgradeData(int id, String name, String description, int baseCoinCost, int baseDiamondCost) {
            this(name, description, baseCoinCost, baseDiamondCost);
            this.id = id;
        }
    }

    // Attack upgrades
    private UpgradeData[] attackUpgrades = {
            new UpgradeData("Brute Force", "Attempt to crack passwords by trying many combinations.", 1000, 2),
            new UpgradeData("Phishing Mail", "Send deceptive emails to trick targets into revealing info. ", 1500, 3),
            new UpgradeData("DDoS", "Increases chance of critical hits", 2000, 4),
            new UpgradeData("Keysniffer", "Bypasses enemy defenses", 2500, 5),
            new UpgradeData("Injector", "Attack multiple targets simultaneously", 3000, 6),
            new UpgradeData("Code Virus", "Reduces detection probability", 1800, 3)
    };

    // Defense upgrades
    private UpgradeData[] defenseUpgrades = {
            new UpgradeData("FireWall Pro", "Increases resistance to attacks", 1200, 2),
            new UpgradeData("Encryption Vault", "Faster health/system recovery", 1600, 3),
            new UpgradeData("EDS", "Automatically retaliates when attacked", 2200, 4),
            new UpgradeData("Anti Virus", "Reduces incoming damage", 2800, 5),
            new UpgradeData("Patch Manager", "Temporary invulnerability periods", 3500, 7),
            new UpgradeData("Alert System", "Early warning of incoming attacks", 1400, 2)
    };

    @FXML
    private void initialize() {
        loadAllToolsFromDatabase();
        initializeUpgradeLevels();
        setupUpgrades();
        updateDisplay();
    }
    
    private void loadAllToolsFromDatabase() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Load all tools from the database
            String query = "SELECT id, name, category, base_cost, description FROM tools";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            // Clear existing upgrade data
            attackUpgrades = new UpgradeData[0];
            defenseUpgrades = new UpgradeData[0];
            
            // Temporary lists to hold tools from the database
            Map<String, UpgradeData> attackMap = new HashMap<>();
            Map<String, UpgradeData> defenseMap = new HashMap<>();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String category = rs.getString("category");
                int baseCost = rs.getInt("base_cost");
                String description = rs.getString("description");
                
                // Store the mapping of tool ID to name and vice versa
                toolIdToName.put(id, name);
                toolNameToId.put(name, id);
                
                // Create the appropriate upgrade data
                UpgradeData data = new UpgradeData(
                    id, 
                    name, 
                    description, 
                    baseCost, 
                    baseCost / 1000 + 1 // Simple formula to derive diamond cost from coin cost
                );
                
                // Add to the appropriate map based on category
                if ("attacking".equals(category)) {
                    attackMap.put(name, data);
                } else if ("defensive".equals(category)) {
                    defenseMap.put(name, data);
                }
            }
            
            // Convert maps to arrays
            attackUpgrades = attackMap.values().toArray(new UpgradeData[0]);
            defenseUpgrades = defenseMap.values().toArray(new UpgradeData[0]);
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to load tools from database: " + e.getMessage());
            
            // Show error alert
            showAlert("Database Error", "Failed to load tools", 
                     "There was an error loading tools from the database. Using default values instead.", 
                     Alert.AlertType.ERROR);
        }
    }
    
    private void loadUserToolData() {
        if (username == null || username.isEmpty()) {
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Load user's coins and diamonds
            String userQuery = "SELECT coins, diamonds FROM users WHERE username = ?";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            userStmt.setString(1, username);
            ResultSet userRs = userStmt.executeQuery();
            
            if (userRs.next()) {
                coins = userRs.getInt("coins");
                diamonds = userRs.getInt("diamonds");
            }
            
            // Load user's tools and their levels
            String toolsQuery = "SELECT t.id, t.name, ut.level FROM user_tools ut " +
                               "JOIN tools t ON ut.tool_id = t.id " +
                               "WHERE ut.username = ?";
            
            PreparedStatement toolsStmt = conn.prepareStatement(toolsQuery);
            toolsStmt.setString(1, username);
            ResultSet toolsRs = toolsStmt.executeQuery();
            
            // Clear existing upgrade levels
            upgradeLevels.clear();
            
            while (toolsRs.next()) {
                String toolName = toolsRs.getString("name");
                int level = toolsRs.getInt("level");
                upgradeLevels.put(toolName, level);
            }
            
            // Refresh the UI
            setupUpgrades();
            updateDisplay();
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to load user tool data: " + e.getMessage());
            
            // Show error alert
            showAlert("Database Error", "Failed to load user data", 
                     "There was an error loading your tools and upgrades.", 
                     Alert.AlertType.ERROR);
        }
    }

    private void initializeUpgradeLevels() {
        // Initialize all upgrades to level 1
        for (UpgradeData upgrade : attackUpgrades) {
            if (!upgradeLevels.containsKey(upgrade.name)) {
                upgradeLevels.put(upgrade.name, MIN_LEVEL);
            }
        }
        for (UpgradeData upgrade : defenseUpgrades) {
            if (!upgradeLevels.containsKey(upgrade.name)) {
                upgradeLevels.put(upgrade.name, MIN_LEVEL);
            }
        }
    }

    private void setupUpgrades() {
        // Clear existing containers
        attackUpgradesBox.getChildren().clear();
        defenseUpgradesBox.getChildren().clear();
        
        // Setup attack upgrades
        for (UpgradeData upgrade : attackUpgrades) {
            VBox upgradeContainer = createUpgradeContainer(upgrade, "attack");
            attackUpgradesBox.getChildren().add(upgradeContainer);
        }

        // Setup defense upgrades
        for (UpgradeData upgrade : defenseUpgrades) {
            VBox upgradeContainer = createUpgradeContainer(upgrade, "defense");
            defenseUpgradesBox.getChildren().add(upgradeContainer);
        }
    }

    private VBox createUpgradeContainer(UpgradeData upgrade, String type) {
        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("upgrade-container");
        container.getStyleClass().add(type + "-upgrade");
        container.setPadding(new Insets(15));

        // Header with name and level
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(upgrade.name);
        nameLabel.getStyleClass().add("upgrade-name");

        int level = upgradeLevels.getOrDefault(upgrade.name, MIN_LEVEL);
        Label levelLabel = new Label("Level " + level + "/" + MAX_LEVEL);
        levelLabel.getStyleClass().add("upgrade-level");

        header.getChildren().addAll(nameLabel, levelLabel);

        // Description
        Label descLabel = new Label(upgrade.description);
        descLabel.getStyleClass().add("upgrade-description");
        descLabel.setWrapText(true);

        // Progress bar
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress((double) level / MAX_LEVEL);
        progressBar.setPrefWidth(200);
        progressBar.getStyleClass().add("upgrade-progress");

        // Cost and upgrade button
        HBox bottomRow = new HBox(15);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox costInfo = new VBox(2);
        costInfo.setAlignment(Pos.CENTER_LEFT);

        if (level < MAX_LEVEL) {
            int nextCoinCost = calculateCoinCost(upgrade.baseCoinCost, level);
            int nextDiamondCost = calculateDiamondCost(upgrade.baseDiamondCost, level);

            Label coinCostLabel = new Label("Coins: " + nextCoinCost);
            coinCostLabel.getStyleClass().add("cost-label");

            Label diamondCostLabel = new Label("Diamonds: " + nextDiamondCost);
            diamondCostLabel.getStyleClass().add("cost-label");

            costInfo.getChildren().addAll(coinCostLabel, diamondCostLabel);
        } else {
            Label maxLevelLabel = new Label("MAX LEVEL REACHED");
            maxLevelLabel.getStyleClass().add("max-level-label");
            costInfo.getChildren().add(maxLevelLabel);
        }

        Button upgradeButton = new Button();
        upgradeButton.getStyleClass().add("upgrade-button");
        upgradeButton.getStyleClass().add(type + "-button");

        if (level < MAX_LEVEL) {
            upgradeButton.setText("UPGRADE");
            upgradeButton.setOnAction(e -> performUpgrade(upgrade));
        } else {
            upgradeButton.setText("MAXED");
            upgradeButton.setDisable(true);
        }

        bottomRow.getChildren().addAll(costInfo, upgradeButton);

        container.getChildren().addAll(header, descLabel, progressBar, bottomRow);

        // Store references for easy updating
        container.setUserData(new Object[]{levelLabel, progressBar, costInfo, upgradeButton});

        return container;
    }

    private int calculateCoinCost(int baseCost, int currentLevel) {
        // Cost increases exponentially: baseCost * (1.5 ^ (currentLevel - 1))
        return (int) (baseCost * Math.pow(1.5, currentLevel - 1));
    }

    private int calculateDiamondCost(int baseCost, int currentLevel) {
        // Diamond cost increases more gradually: baseCost + (currentLevel - 1)
        return baseCost + (currentLevel - 1);
    }

    private void performUpgrade(UpgradeData upgrade) {
        int currentLevel = upgradeLevels.getOrDefault(upgrade.name, MIN_LEVEL);

        if (currentLevel >= MAX_LEVEL) {
            showAlert("Max Level", "Upgrade Maxed",
                    upgrade.name + " is already at maximum level!", Alert.AlertType.WARNING);
            return;
        }
        
        // First check if the user already owns this tool
        boolean userOwnsTool = checkIfUserOwnsTool(upgrade);
        
        // If user doesn't own the tool, ask if they want to purchase it
        if (!userOwnsTool) {
            Alert purchaseAlert = new Alert(Alert.AlertType.CONFIRMATION);
            purchaseAlert.setTitle("Purchase Required");
            purchaseAlert.setHeaderText("You don't own this tool yet");
            purchaseAlert.setContentText("Would you like to purchase " + upgrade.name + " for " + upgrade.baseCoinCost + " coins?");
            
            javafx.scene.control.ButtonType result = purchaseAlert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL);
            
            if (result != javafx.scene.control.ButtonType.OK) {
                return; // User canceled purchase
            }
            
            // Check if user has enough coins to purchase the tool
            if (coins < upgrade.baseCoinCost) {
                showAlert("Insufficient Coins", "Cannot Purchase Tool",
                        "You need " + upgrade.baseCoinCost + " coins to purchase this tool.", Alert.AlertType.ERROR);
                return;
            }
            
            // Purchase the tool
            if (!purchaseTool(upgrade)) {
                return; // Purchase failed
            }
            
            // Set the current level to 1 since they just purchased it
            currentLevel = 1;
            upgradeLevels.put(upgrade.name, currentLevel);
            refreshUpgradeContainer(upgrade);
            return; // Exit after purchase - don't immediately upgrade
        }

        int coinCost = calculateCoinCost(upgrade.baseCoinCost, currentLevel);
        int diamondCost = calculateDiamondCost(upgrade.baseDiamondCost, currentLevel);

        if (coins >= coinCost && diamonds >= diamondCost) {
            // Try to upgrade in the database first
            if (!upgradeToolInDatabase(upgrade, currentLevel + 1, coinCost, diamondCost)) {
                return; // Database update failed
            }
            
            // Deduct costs
            coins -= coinCost;
            diamonds -= diamondCost;

            // Increase level
            upgradeLevels.put(upgrade.name, currentLevel + 1);

            // Update display
            updateDisplay();
            refreshUpgradeContainer(upgrade);

            showAlert("Upgrade Successful", "Level Up!",
                    upgrade.name + " upgraded to level " + (currentLevel + 1) + "!\n" +
                            "Cost: " + coinCost + " coins, " + diamondCost + " diamonds");
            
            // Update GameMenuController
            if (gameMenuController != null) {
                gameMenuController.addCoins(-coinCost);
                gameMenuController.addDiamonds(-diamondCost);
            }
        } else {
            String message = "Insufficient resources!\n";
            message += "Required: " + coinCost + " coins, " + diamondCost + " diamonds\n";
            message += "You have: " + coins + " coins, " + diamonds + " diamonds";

            showAlert("Insufficient Resources", "Cannot Upgrade", message, Alert.AlertType.ERROR);
        }
    }
    
    private boolean checkIfUserOwnsTool(UpgradeData upgrade) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM user_tools WHERE username = ? AND tool_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setInt(2, upgrade.id);
            ResultSet rs = stmt.executeQuery();
            
            return rs.next(); // If there's a result, the user owns the tool
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database error checking tool ownership: " + e.getMessage());
            return false;
        }
    }
    
    private boolean purchaseTool(UpgradeData upgrade) {
        if (username == null || username.isEmpty()) {
            showAlert("Error", "Not Logged In", "You must be logged in to purchase tools.", Alert.AlertType.ERROR);
            return false;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Start a transaction
            conn.setAutoCommit(false);
            
            try {
                // First deduct coins from user
                String updateUserQuery = "UPDATE users SET coins = coins - ? WHERE username = ?";
                PreparedStatement updateUserStmt = conn.prepareStatement(updateUserQuery);
                updateUserStmt.setInt(1, upgrade.baseCoinCost);
                updateUserStmt.setString(2, username);
                int userRowsAffected = updateUserStmt.executeUpdate();
                
                if (userRowsAffected != 1) {
                    throw new SQLException("Failed to update user coins");
                }
                
                // Then add the tool to user_tools
                String insertToolQuery = "INSERT INTO user_tools (username, tool_id, level) VALUES (?, ?, 1)";
                PreparedStatement insertToolStmt = conn.prepareStatement(insertToolQuery);
                insertToolStmt.setString(1, username);
                insertToolStmt.setInt(2, upgrade.id);
                int toolRowsAffected = insertToolStmt.executeUpdate();
                
                if (toolRowsAffected != 1) {
                    throw new SQLException("Failed to add tool to user");
                }
                
                // Commit the transaction
                conn.commit();
                
                // Update local values
                coins -= upgrade.baseCoinCost;
                updateDisplay();
                
                if (gameMenuController != null) {
                    gameMenuController.addCoins(-upgrade.baseCoinCost);
                }
                
                showAlert("Purchase Successful", "Tool Acquired", 
                        "You have successfully purchased " + upgrade.name + "!");
                
                return true;
                
            } catch (SQLException e) {
                // Something went wrong, rollback
                conn.rollback();
                e.printStackTrace();
                
                showAlert("Purchase Failed", "Database Error", 
                         "Failed to purchase tool: " + e.getMessage(), Alert.AlertType.ERROR);
                
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            
            showAlert("Database Error", "Connection Failed", 
                     "Could not connect to the database: " + e.getMessage(), Alert.AlertType.ERROR);
            
            return false;
        }
    }
    
    private boolean upgradeToolInDatabase(UpgradeData upgrade, int newLevel, int coinCost, int diamondCost) {
        if (username == null || username.isEmpty()) {
            showAlert("Error", "Not Logged In", "You must be logged in to upgrade tools.", Alert.AlertType.ERROR);
            return false;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Start a transaction
            conn.setAutoCommit(false);
            
            try {
                // First deduct coins and diamonds from user
                String updateUserQuery = "UPDATE users SET coins = coins - ?, diamonds = diamonds - ? WHERE username = ?";
                PreparedStatement updateUserStmt = conn.prepareStatement(updateUserQuery);
                updateUserStmt.setInt(1, coinCost);
                updateUserStmt.setInt(2, diamondCost);
                updateUserStmt.setString(3, username);
                int userRowsAffected = updateUserStmt.executeUpdate();
                
                if (userRowsAffected != 1) {
                    throw new SQLException("Failed to update user resources");
                }
                
                // Then update the tool level
                String updateToolQuery = "UPDATE user_tools SET level = ?, last_upgraded_at = CURRENT_TIMESTAMP WHERE username = ? AND tool_id = ?";
                PreparedStatement updateToolStmt = conn.prepareStatement(updateToolQuery);
                updateToolStmt.setInt(1, newLevel);
                updateToolStmt.setString(2, username);
                updateToolStmt.setInt(3, upgrade.id);
                int toolRowsAffected = updateToolStmt.executeUpdate();
                
                if (toolRowsAffected != 1) {
                    throw new SQLException("Failed to update tool level");
                }
                
                // Commit the transaction
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                // Something went wrong, rollback
                conn.rollback();
                e.printStackTrace();
                
                showAlert("Upgrade Failed", "Database Error", 
                         "Failed to upgrade tool: " + e.getMessage(), Alert.AlertType.ERROR);
                
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            
            showAlert("Database Error", "Connection Failed", 
                     "Could not connect to the database: " + e.getMessage(), Alert.AlertType.ERROR);
            
            return false;
        }
    }

    private void refreshUpgradeContainer(UpgradeData upgrade) {
        VBox container = findUpgradeContainer(upgrade.name);
        if (container != null) {
            Object[] components = (Object[]) container.getUserData();
            Label levelLabel = (Label) components[0];
            ProgressBar progressBar = (ProgressBar) components[1];
            VBox costInfo = (VBox) components[2];
            Button upgradeButton = (Button) components[3];

            int currentLevel = upgradeLevels.get(upgrade.name);
            
            // Update level label and progress bar
            levelLabel.setText("Level " + currentLevel + "/" + MAX_LEVEL);
            progressBar.setProgress((double) currentLevel / MAX_LEVEL);
            
            costInfo.getChildren().clear();
            
            if (currentLevel < MAX_LEVEL) {
                int nextCoinCost = calculateCoinCost(upgrade.baseCoinCost, currentLevel);
                int nextDiamondCost = calculateDiamondCost(upgrade.baseDiamondCost, currentLevel);

                Label coinCostLabel = new Label("Coins: " + nextCoinCost);
                coinCostLabel.getStyleClass().add("cost-label");

                Label diamondCostLabel = new Label("Diamonds: " + nextDiamondCost);
                diamondCostLabel.getStyleClass().add("cost-label");

                costInfo.getChildren().addAll(coinCostLabel, diamondCostLabel);
                
                upgradeButton.setText("UPGRADE");
                upgradeButton.setDisable(false);
            } else {
                Label maxLevelLabel = new Label("MAX LEVEL REACHED");
                maxLevelLabel.getStyleClass().add("max-level-label");
                costInfo.getChildren().add(maxLevelLabel);
                
                upgradeButton.setText("MAXED");
                upgradeButton.setDisable(true);
            }
        }
    }

    private VBox findUpgradeContainer(String upgradeName) {
        // Search in attack upgrades
        for (javafx.scene.Node node : attackUpgradesBox.getChildren()) {
            if (node instanceof VBox) {
                VBox container = (VBox) node;
                HBox header = (HBox) container.getChildren().get(0);
                Label nameLabel = (Label) header.getChildren().get(0);
                if (nameLabel.getText().equals(upgradeName)) {
                    return container;
                }
            }
        }
        
        // Search in defense upgrades
        for (javafx.scene.Node node : defenseUpgradesBox.getChildren()) {
            if (node instanceof VBox) {
                VBox container = (VBox) node;
                HBox header = (HBox) container.getChildren().get(0);
                Label nameLabel = (Label) header.getChildren().get(0);
                if (nameLabel.getText().equals(upgradeName)) {
                    return container;
                }
            }
        }
        
        return null;
    }

    private void showAlert(String title, String header, String content) {
        showAlert(title, header, content, Alert.AlertType.INFORMATION);
    }
    
    private void showAlert(String title, String header, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(header);
                alert.setContentText(content);
                alert.showAndWait();
            } catch (Exception e) {
                System.err.println("Failed to show alert: " + e.getMessage());
            }
        });
    }

    private void updateDisplay() {
        coinsLabel.setText("Coins: " + coins);
        diamondsLabel.setText("Diamonds: " + diamonds);
    }

    @FXML
    private void handleBack() {
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    @FXML
    private void handleMainMenu() {
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    private void loadScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 800);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            Stage stage = (Stage) mainMenuButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}