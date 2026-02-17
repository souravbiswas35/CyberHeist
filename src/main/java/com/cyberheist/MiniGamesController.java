package com.cyberheist;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class MiniGamesController {

    @FXML private Button ticTacToeButton;
    @FXML private Button snakeButton;
    @FXML private Button bubbleShooterButton;
    @FXML private Button tetrisButton;
    @FXML private Button wordGameButton;
    @FXML private Button randomGameButton;
    @FXML private Button backButton;
    @FXML private Button mainMenuButton;

    // Tracks the logged-in user
    private String username;

    // SceneManager will call this to inject the logged-in username
    public void setUsername(String username) {
        this.username = username;
        System.out.println("Mini Games"+username);
        // Optionally, enable buttons now that user is known
        boolean loggedIn = username != null && !username.isEmpty();
        ticTacToeButton.setDisable(!loggedIn);
        snakeButton.setDisable(!loggedIn);
        bubbleShooterButton.setDisable(!loggedIn);
        tetrisButton.setDisable(!loggedIn);
        wordGameButton.setDisable(!loggedIn);
        randomGameButton.setDisable(!loggedIn);
    }

    @FXML
    private void handleTicTacToe() {
        if (!checkLoggedIn()) return;
        System.out.println("handle tic"+username);
        SceneManager.getInstance().switchToScene("TicTacToe.fxml");
    }

    @FXML
    private void handleSnake() {
        if (!checkLoggedIn()) return;
        SceneManager.getInstance().switchToScene("Snake.fxml");
    }

    @FXML
    private void handleBubbleShooter() {
        if (!checkLoggedIn()) return;
        SceneManager.getInstance().switchToScene("BubbleShooter.fxml");
    }

    @FXML
    private void handleTetris() {
        if (!checkLoggedIn()) return;
        SceneManager.getInstance().switchToScene("Tetris.fxml");
    }

    @FXML
    private void handleWordGame() {
        if (!checkLoggedIn()) return;
        SceneManager.getInstance().switchToScene("WordGame.fxml");
    }

    @FXML
    private void handleSpaceGame() {
        if (!checkLoggedIn()) return;
        SceneManager.getInstance().switchToScene("SpaceGame.fxml");
    }

    @FXML
    private void handleRandomGame() {
        if (!checkLoggedIn()) return;
        String[] games = {
                "MiniGames/TicTacToe.fxml",
                "MiniGames/Snake.fxml",
                "MiniGames/BubbleShooter.fxml",
                "MiniGames/Tetris.fxml",
                "MiniGames/WordGame.fxml",
                "MiniGames/SpaceGame.fxml"
        };
        int idx = (int) (Math.random() * games.length);
        SceneManager.getInstance().switchToScene(games[idx]);
    }

    @FXML
    private void handleBack() {
        if (!checkLoggedIn()) return;
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    @FXML
    private void handleMainMenu() {
        if (!checkLoggedIn()) return;
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    /**
     * Verifies that a user is logged in before navigation.
     * Shows an alert if not logged in.
     */
    private boolean checkLoggedIn() {
        if (username == null || username.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please log in to access mini-games.");
            alert.showAndWait();
            return false;
        }
        return true;
    }
}
