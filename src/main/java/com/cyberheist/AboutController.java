package com.cyberheist;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class AboutController {

    @FXML
    private TextArea gameDescription;

    @FXML
    private TextArea authorsDescription;

    @FXML
    private Button backButton;

    @FXML
    private void initialize() {
        // Set the game description and authors information
        gameDescription.setText("Cyber Heist is an online hacking simulation game where players learn the art of hacking, defense techniques and cyber warfare.\n\n" +
                "Players can use various tools to attack and defend, earn coins and upgrade their arsenal as they progress through different levels.\n\n" +
                "The game offers a dynamic and immersive experience, challenging players to use their skills to navigate complex hacking scenarios.");

        authorsDescription.setText("Game Developed by:\n\n" +
                "Sakibul Alam - Lead Developer\n" +
                "Saidul Islam - Game Designer\n" +
                "Sourav Biswas - Programmer\n\n" +
                "Special thanks to the entire development team for their hard work and dedication.");

        // Add background colors to TextAreas using inline CSS
        gameDescription.setStyle("-fx-control-inner-background: #2c3e50; -fx-text-fill: #ecf0f1; -fx-font-size: 14px;");
        authorsDescription.setStyle("-fx-control-inner-background: #34495e; -fx-text-fill: #ecf0f1; -fx-font-size: 14px;");

        // Make TextAreas non-editable (optional)
        gameDescription.setEditable(false);
        authorsDescription.setEditable(false);
    }

    @FXML
    private void handleBack() {
        // Go back to the Tools.fxml page
        loadScene("GameMenu.fxml");
    }

    private void loadScene(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/cyberheist/" + fxmlFile));
            Scene scene = new Scene(root, 1024, 800);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}