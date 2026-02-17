package com.cyberheist.Tools;

import com.cyberheist.DatabaseConnection;
import com.cyberheist.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class InjectorController implements Initializable {

    @FXML private ImageView toolImage;
    @FXML private Label toolDescription;
    @FXML private Label costLabel;
    @FXML private Button buyButton;
    @FXML private Button backButton;

    private String username;
    private int userCoins;
    private int toolId;
    private int toolCost;

    /** Called by SceneManager to inject the logged-in username */
    public void setUsername(String username) {
        this.username = username;
        loadUserCoins();
        loadToolData();

        updateUI();
        System.out.println("Pishing -"+username); //it loads later
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set image and description for the Phishing Mail tool
        // As per your instruction from previous controllers, this line remains unchanged with the file: path.
        toolImage.setImage(new Image("file:D:/JavaFx Projects/CyberHeist/img/Injector.jpg"));
        toolDescription.setText("Phishing Mail: Craft a fake email to trick your target into revealing passwords or clicking malicious links.\n" +
                "Silent and sneaky—relies on human error rather than system flaws.");
    }

    /** Load this tool's ID and cost from the tools table */
    private void loadToolData() {
        String sql = "SELECT id, base_cost FROM tools WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Injector");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                toolId = rs.getInt("id");
                toolCost = rs.getInt("base_cost");
                System.out.println(toolCost);
                costLabel.setText("Cost: " + toolCost + " coins");
                System.out.println("tools found"+" "+toolId); // for debugging it doesnt get print
            } else {
                showAlert(Alert.AlertType.ERROR, "Tool Not Found", "Injector tool not found in database.");
                toolId = -1;
                buyButton.setDisable(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load tool data.");
            toolId = -1;
            buyButton.setDisable(true);
        }
    }

    /** Load current user's coin balance from the users table */
    private void loadUserCoins() {
        if (username == null) {
            showAlert(Alert.AlertType.ERROR, "Not Logged In", "Please log in to purchase tools.");
            buyButton.setDisable(true);
            return;
        }
        String sql = "SELECT coins FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userCoins = rs.getInt("coins");
                System.out.println(userCoins);
            } else {
                userCoins = 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load your coin balance.");
            buyButton.setDisable(true);
        }
    }

    /** Enable or disable buy button based on state */
    private void updateUI() {
        boolean owns = false;
        if (toolId > 0 && username != null) {
            String checkSql = "SELECT 1 FROM user_tools WHERE username = ? AND tool_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, username);
                ps.setInt(2, toolId);
                ResultSet rs = ps.executeQuery();
                owns = rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (owns) {
            buyButton.setText("Already Purchased");
            buyButton.setDisable(true);
        } else {
            buyButton.setText("BUY");
            buyButton.setDisable(toolCost > userCoins);
        }
    }

    @FXML
    private void handleBuyTool() {
        if (username == null) {
            showAlert(Alert.AlertType.ERROR, "Not Logged In", "Please log in to purchase tools.");
            return;
        }
        if (toolId < 0) {
            showAlert(Alert.AlertType.ERROR, "Tool Error", "Tool information unavailable.");
            return;
        }
        // Check if already owned
        String checkSql = "SELECT 1 FROM user_tools WHERE username = ? AND tool_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, username);
            checkPs.setInt(2, toolId);
            if (checkPs.executeQuery().next()) {
                showAlert(Alert.AlertType.INFORMATION, "Already Owned", "You already have this tool.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not check ownership.");
            return;
        }

        // Check coin balance
        if (userCoins < toolCost) {
            showAlert(Alert.AlertType.ERROR, "Insufficient Funds",
                    "You need " + toolCost + " coins to buy this tool. You have " + userCoins + ".");
            return;
        }

        // Perform purchase transaction
        String updateCoinsSql = "UPDATE users SET coins = coins - ? WHERE username = ?";
        String insertUserToolSql = "INSERT INTO user_tools (username, tool_id) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement updatePs = null;
        PreparedStatement insertPs = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            updatePs = conn.prepareStatement(updateCoinsSql);
            updatePs.setInt(1, toolCost);
            updatePs.setString(2, username);
            updatePs.executeUpdate();

            insertPs = conn.prepareStatement(insertUserToolSql);
            insertPs.setString(1, username);
            insertPs.setInt(2, toolId);
            insertPs.executeUpdate();

            conn.commit();
            userCoins -= toolCost;
            showAlert(Alert.AlertType.INFORMATION, "Purchase Successful",
                    "You have purchased Injector for " + toolCost + " coins!");
            updateUI();

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Purchase Failed", "An error occurred during purchase.");
        } finally {
            try { if (updatePs != null) updatePs.close(); } catch (SQLException ignore) {}
            try { if (insertPs != null) insertPs.close(); } catch (SQLException ignore) {}
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException ignore) {}
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.getInstance().switchToScene("Tools.fxml");
    }

    /** Helper to show alerts */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
