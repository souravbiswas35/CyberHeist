package com.cyberheist;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class ToolsController {

    @FXML private VBox attackingToolsBox;
    @FXML private VBox defensiveToolsBox;
    @FXML private Button backButton;
    @FXML private Button mainMenuButton;
    @FXML private Label coinsLabel;
    @FXML private Label diamondsLabel;

    private String username;
    private int coins;
    private int diamonds;

    // Map tool names to their FXML paths (relative to com/cyberheist)
    private final Map<String,String> toolFxmlMap = new HashMap<>();

    private static class ToolData {
        String name, description;
        int coinCost;
        ToolData(String name, String description, int coinCost) {
            this.name = name;
            this.description = description;
            this.coinCost = coinCost;
        }
    }

    private final ToolData[] attackingToolsData = {
            new ToolData("Brute Force",   "Attempt to crack passwords by trying many combinations.", 3000),
            new ToolData("Phishing Mail", "Send deceptive emails to trick targets into revealing info.", 2000),
            new ToolData("DDoS",          "Overwhelm a system with traffic to disrupt service.", 5000),
            new ToolData("Keysniffer",    "Capture keystrokes to steal sensitive information.", 1500),
            new ToolData("Injector",      "Inject malicious code into processes or systems.", 4000),
            new ToolData("Code Virus",    "Deploy self-replicating malicious software.", 8000)
    };

    private final ToolData[] defensiveToolsData = {
            new ToolData("Firewall Pro",     "Blocks unauthorized network access and malicious traffic.", 3000),
            new ToolData("Encryption Vault", "Securely stores sensitive data using strong encryption.", 2500),
            new ToolData("EDS",              "Detects and responds to cyber threats in real-time.", 2000),
            new ToolData("Antivirus",        "Scans, detects, and removes malicious software.", 7000),
            new ToolData("Patch Manager",    "Applies security updates to fix vulnerabilities.", 1500)
    };

    /** Called by SceneManager to inject the logged-in username. */
    public void setUsername(String username) {
        this.username = username;
        loadUserData();       // loads coins & diamonds and calls updateDisplay()
    }

    @FXML
    private void initialize() {
        // Build FXML mappings (no leading slash)
        toolFxmlMap.put("Brute Force",   "Tool/bruteforce.fxml");
        toolFxmlMap.put("Phishing Mail", "Tool/PishingTool.fxml");
        toolFxmlMap.put("DDoS",          "Tool/DdosTool.fxml");
        toolFxmlMap.put("Keysniffer",    "Tool/KeysnifferTool.fxml");
        toolFxmlMap.put("Injector",      "Tool/InjectorTool.fxml");
        toolFxmlMap.put("Code Virus",    "Tool/CodeVirus.fxml");
        toolFxmlMap.put("Firewall Pro",  "Tool/Firewall.fxml");
        toolFxmlMap.put("Encryption Vault","Tool/EncryptionVault.fxml");
        toolFxmlMap.put("EDS",           "Tool/EDS.fxml");
        toolFxmlMap.put("Antivirus",     "Tool/Antivirus.fxml");
        toolFxmlMap.put("Patch Manager", "Tool/PatchManager.fxml");

        // Populate the UI
        for (ToolData t: attackingToolsData) {
            attackingToolsBox.getChildren().add(createToolContainer(t,"attacking"));
        }
        for (ToolData t: defensiveToolsData) {
            defensiveToolsBox.getChildren().add(createToolContainer(t,"defensive"));
        }

        // In case setUsername ran before initialize, update labels
        updateDisplay();
    }

    private VBox createToolContainer(ToolData tool, String type) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().addAll("tool-item-container", type+"-tool-item");
        box.setPadding(new Insets(10));

        Label name = new Label(tool.name);
        name.getStyleClass().add("tool-name");

        Label desc = new Label(tool.description);
        desc.getStyleClass().add("tool-description");
        desc.setWrapText(true);

        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER_LEFT);
        Label cost = new Label("Cost: " + tool.coinCost + " Coins");
        cost.getStyleClass().add("tool-cost");

        Button access = new Button("ACCESS");
        access.getStyleClass().addAll("tool-action-button", type+"-action-button");

        // *** ALWAYS go through SceneManager ***
        access.setOnAction(e -> {
            String fxml = toolFxmlMap.get(tool.name);
            if (fxml != null) {
                System.out.println("Switching to: " + fxml + " for user "+username);
                SceneManager.getInstance().switchToScene(fxml);
            } else {
                showAlert("Navigation Error",
                        "Missing FXML for " + tool.name,
                        Alert.AlertType.ERROR);
            }
        });

        bottom.getChildren().addAll(cost, access);
        box.getChildren().addAll(name, desc, bottom);
        return box;
    }

    /** Load coins & diamonds from DB */
    private void loadUserData() {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT coins, diamonds FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                coins    = rs.getInt("coins");
                diamonds = rs.getInt("diamonds");
            } else {
                coins = diamonds = 0;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Error", "Failed to load user data.",
                    Alert.AlertType.ERROR);
        }
        updateDisplay();
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

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
