package com.cyberheist;

import javafx.event.ActionEvent; // Added for ActionEvent
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage; // Import Stage

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.sql.SQLException;

public class WordGameController {

    @FXML private Label scoreLabel;
    @FXML private Label wordLabel;
    @FXML private Label hintLabel;
    @FXML private TextField guessField;
    @FXML private Button guessButton;
    @FXML private Button skipButton;
    @FXML private Label attemptsLabel;
    @FXML private Label statusLabel;

    @FXML private Label wrongGuessesLabel;  // Added
    @FXML private Label usedLettersLabel;   // Added

    @FXML private Button backButton;
    @FXML private Button mainMenuButton;

    private int score = 0;
    private String currentWord;
    private String currentHint;
    private StringBuilder displayedWord;
    private Set<Character> usedLetters;
    private int attemptsLeft;
    private final int MAX_ATTEMPTS = 3;
    private Random random = new Random();

    private GameMenuController gameMenuController;
    private Stage primaryStage;

    // Injected by SceneManager for user authentication
    private String username;

    public void setGameMenuController(GameMenuController controller) {
        this.gameMenuController = controller;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setUsername(String username) {
        this.username = username;
        if (username == null || username.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Not Logged In", "Please log in to play Word Game and earn coins.");
            // Still allow playing but won't earn coins
        }
    }

    private final List<WordData> wordList = Arrays.asList(
            new WordData("JAVA", "A popular object-oriented programming language", "PROGRAMMING"),
            new WordData("PYTHON", "A high-level, interpreted programming language", "PROGRAMMING"),
            new WordData("FIREWALL", "A network security system", "SECURITY"),
            new WordData("ENCRYPT", "To convert information into a code", "SECURITY"),
            new WordData("VIRUS", "A type of malicious software", "MALWARE"),
            new WordData("PHISHING", "Fraudulent attempt to obtain sensitive information", "CYBERCRIME"),
            new WordData("HACKER", "An expert in computer systems", "PEOPLE"),
            new WordData("ALGORITHM", "A set of rules to be followed in problem-solving", "COMPUTING"),
            new WordData("NETWORK", "A group of interconnected computers", "COMPUTING"),
            new WordData("SERVER", "A computer program or device that provides a service", "COMPUTING")
    );

    @FXML
    private void initialize() {
        updateScoreDisplay();
        setupNewGame();
    }

    private void setupNewGame() {
        WordData selectedWordData = wordList.get(random.nextInt(wordList.size()));
        currentWord = selectedWordData.word.toUpperCase();
        currentHint = selectedWordData.hint;

        hintLabel.setText("Hint: " + currentHint);

        displayedWord = new StringBuilder(currentWord.length());
        for (int i = 0; i < currentWord.length(); i++) {
            displayedWord.append("_");
        }
        wordLabel.setText(formatDisplayedWord(displayedWord.toString()));

        usedLetters = new HashSet<>();
        attemptsLeft = MAX_ATTEMPTS;

        updateAttemptsDisplay();
        updateWrongGuessesDisplay();
        updateUsedLettersDisplay();

        statusLabel.setText("Decrypt the word to earn coins!");
        guessField.setText("");
        guessField.setDisable(false);
        guessButton.setDisable(false);
        skipButton.setDisable(false);
    }

    private String formatDisplayedWord(String word) {
        StringBuilder formatted = new StringBuilder();
        for (char c : word.toCharArray()) {
            formatted.append(c).append(" ");
        }
        return formatted.toString().trim();
    }

    @FXML
    private void handleGuess() {
        String guess = guessField.getText().trim().toUpperCase();
        guessField.setText("");

        if (guess.isEmpty() || guess.length() > 1 || !Character.isLetter(guess.charAt(0))) {
            showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please enter a single letter.");
            return;
        }

        char guessedChar = guess.charAt(0);

        if (usedLetters.contains(guessedChar)) {
            statusLabel.setText("You already guessed '" + guessedChar + "'. Try another letter.");
            return;
        }

        usedLetters.add(guessedChar);
        updateUsedLettersDisplay();

        boolean correctGuess = false;
        for (int i = 0; i < currentWord.length(); i++) {
            if (currentWord.charAt(i) == guessedChar) {
                displayedWord.setCharAt(i, guessedChar);
                correctGuess = true;
            }
        }
        wordLabel.setText(formatDisplayedWord(displayedWord.toString()));

        if (correctGuess) {
            if (displayedWord.toString().equals(currentWord)) {
                gameEnd(true);
            } else {
                statusLabel.setText("Correct guess! Keep going.");
            }
        } else {
            attemptsLeft--;
            updateAttemptsDisplay();
            updateWrongGuessesDisplay();
            if (attemptsLeft <= 0) {
                gameEnd(false);
            } else {
                statusLabel.setText("Wrong guess! Attempts left: " + attemptsLeft);
            }
        }
    }

    @FXML
    private void handleSkip() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Skip Word");
        confirm.setHeaderText("Are you sure you want to skip this word?");
        confirm.setContentText("You will not earn coins for this word.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                gameEnd(false);
            }
        });
    }

    private void gameEnd(boolean win) {
        guessField.setDisable(true);
        guessButton.setDisable(true);
        skipButton.setDisable(true);

        int earnedCoins = 0;
        if (win) {
            earnedCoins = 100;
            score += earnedCoins;
            statusLabel.setText("Congratulations! You decrypted the word: " + currentWord);

            // Only show coin earning message if user is logged in
            if (username != null && !username.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Word Decrypted!", "You earned " + earnedCoins + " coins!");
                awardCoins(earnedCoins);
                updateGameHistory("WordGame", "win", earnedCoins);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Word Decrypted!", "Great job! (Login to earn coins)");
            }
        } else {
            statusLabel.setText("Decryption failed! The word was: " + currentWord);

            if (username != null && !username.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Decryption Failed!", "The word was: " + currentWord + "\nYou earned 0 coins.");
                updateGameHistory("WordGame", "loss", 0);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Decryption Failed!", "The word was: " + currentWord + "\n(Login to earn coins)");
            }
        }
        updateScoreDisplay();

        // Legacy support for GameMenuController if still used
        if (gameMenuController != null) {
            gameMenuController.addCoins(earnedCoins);
        }
    }

    /**
     * Awards coins to the logged-in user by updating the database
     */
    private void awardCoins(int amount) {
        if (username == null || username.isEmpty()) {
            System.out.println("No user logged in, coins not awarded");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE users SET coins = coins + ? WHERE username = ?")) {
            ps.setInt(1, amount);
            ps.setString(2, username);
            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Awarded " + amount + " coins to user: " + username);
            } else {
                System.out.println("Failed to award coins - user not found: " + username);
                showAlert(Alert.AlertType.ERROR, "Error", "Could not update coins for user.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update coins.");
        }
    }

    private void updateScoreDisplay() {
        scoreLabel.setText("Score: " + score);
    }

    private void updateAttemptsDisplay() {
        attemptsLabel.setText("Attempts: " + attemptsLeft);
    }

    private void updateWrongGuessesDisplay() {
        int wrongGuesses = MAX_ATTEMPTS - attemptsLeft;
        wrongGuessesLabel.setText(wrongGuesses + "/" + MAX_ATTEMPTS);
    }

    private void updateUsedLettersDisplay() {
        StringBuilder sb = new StringBuilder();
        for (char c : usedLetters) {
            sb.append(c).append(" ");
        }
        usedLettersLabel.setText(sb.toString().trim());
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        SceneManager.getInstance().switchToScene("MiniGames.fxml");
    }

    @FXML
    private void handleMainMenu() {
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    private void loadScene(String fxmlFile) {
        try {
            if (primaryStage == null) {
                primaryStage = (Stage) backButton.getScene().getWindow();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 800);

            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            if (fxmlFile.equals("MiniGames.fxml")) {
                //MiniGamesController nextController = loader.getController();
                // nextController.setPrimaryStage(primaryStage);
                // nextController.setGameMenuController(gameMenuController);
                SceneManager.getInstance().switchToScene("MiniGames.fxml");
            } else if (fxmlFile.equals("GameMenu.fxml")) {
                GameMenuController nextController = loader.getController();
                nextController.setPrimaryStage(primaryStage);
                nextController.setGameMenuController(gameMenuController);
            }

            primaryStage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading scene: " + fxmlFile);
        }
    }

    public void handleSolve(ActionEvent actionEvent) {
    }

    private void updateGameHistory(String gameType, String result, int score) {
        if (username == null || username.isEmpty()) {
            System.out.println("No username available, cannot update game history");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "INSERT INTO game_history (username, game_type, result, score, date, duration) " +
                    "VALUES (?, ?, ?, ?, NOW(), ?)";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, gameType);
            stmt.setString(3, result);
            stmt.setInt(4, score);
            stmt.setString(5, attemptsLeft + "/" + MAX_ATTEMPTS + " attempts");

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Game history recorded successfully for " + gameType);
            } else {
                System.out.println("Failed to insert game history record");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Database error in updateGameHistory: " + e.getMessage());
        }
    }

    private static class WordData {
        String word;
        String hint;
        String category;

        WordData(String word, String hint, String category) {
            this.word = word;
            this.hint = hint;
            this.category = category;
        }
    }
}