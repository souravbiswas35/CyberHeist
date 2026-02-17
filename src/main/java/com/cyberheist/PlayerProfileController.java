package com.cyberheist;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlayerProfileController {
    private String username;

    @FXML private Label usernameLabel;
    @FXML private ImageView profileImageView;
    @FXML private Button changeProfilePicButton;
    @FXML private Label levelLabel;
    @FXML private ProgressBar experienceBar;
    @FXML private Label experienceLabel;

    // Statistics
    @FXML private Label totalGamesLabel;
    @FXML private Label winsLabel;
    @FXML private Label lossesLabel;
    @FXML private Label winRateLabel;
    @FXML private Label totalScoreLabel;
    @FXML private Label averageScoreLabel;
    @FXML private Label playtimeLabel;
    @FXML private Label rankLabel;

    // Game History Table
    @FXML private TableView<GameHistory> gameHistoryTable;
    @FXML private TableColumn<GameHistory, String> gameTypeColumn;
    @FXML private TableColumn<GameHistory, String> resultColumn;
    @FXML private TableColumn<GameHistory, Integer> scoreColumn;
    @FXML private TableColumn<GameHistory, String> dateColumn;
    @FXML private TableColumn<GameHistory, String> durationColumn;

    @FXML private Button backButton;
    @FXML private Button editProfileButton;

    // Player Data
    private PlayerData playerData;
    private ObservableList<GameHistory> gameHistoryList;

    // Database connection
    private Connection connection;

    public void setUsername(String username) {
        System.out.println("=== SetUsername called ===");
        System.out.println("Received username: '" + username + "'");

        this.username = username;
        System.out.println("Username set to: '" + this.username + "'");

        // Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            // Check if FXML components are initialized
            if (usernameLabel == null) {
                System.out.println("WARNING: FXML components not yet initialized!");
                return;
            }

            // Load player data from database
            loadPlayerData();

            // Setup the UI components after data is loaded
            if (playerData != null) {
                System.out.println("Player data loaded successfully, setting up UI components");
                setupProfileDisplay();
                setupStatistics();
                loadGameHistoryData();
                System.out.println("UI setup completed");
            } else {
                System.out.println("Player data is null, showing error");
                showAlert("Error", "Failed to load player data from the database.");
            }
        });
    }


    @FXML
    private void initialize() {
        System.out.println("Initialize method called");

        // Setup table columns first
        setupGameHistoryTableColumns();

        // If username was set before initialization, load data now
        if (username != null && !username.trim().isEmpty()) {
            System.out.println("Username already set, loading data now");
            loadPlayerData();
            if (playerData != null) {
                setupProfileDisplay();
                setupStatistics();
                loadGameHistoryData();
            }
        }

        System.out.println("Initialize completed - FXML components ready");
    }

    private void setupGameHistoryTableColumns() {
        // Check if table components are available
        if (gameTypeColumn == null) {
            System.out.println("Table columns not yet initialized");
            return;
        }

        // Set up table column cell value factories
        gameTypeColumn.setCellValueFactory(new PropertyValueFactory<>("gameType"));
        resultColumn.setCellValueFactory(new PropertyValueFactory<>("result"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));

        // Style the result column based on win/loss
        resultColumn.setCellFactory(column -> new TableCell<GameHistory, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Since we're already converting to uppercase in loadGameHistoryData
                    if (item.contains("WIN")) {
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    } else if (item.contains("LOSS")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (item.contains("DRAW")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Add tooltips to column headers for clarity
        gameTypeColumn.setText("Game");
        resultColumn.setText("Result");
        scoreColumn.setText("Score");
        dateColumn.setText("Date & Time");
        durationColumn.setText("Duration");
    }

    private void loadPlayerData() {
        System.out.println("Loading player data for username: " + username);

        if (username == null || username.trim().isEmpty()) {
            System.out.println("Username is null or empty, cannot load player data");
            showAlert("Error", "Username is not set. Cannot load player data.");
            return;
        }

        // First, let's test if we can connect to the database
        try (Connection testConnection = DatabaseConnection.getConnection()) {
            System.out.println("Database connection successful!");
            System.out.println("Connection URL: " + testConnection.getMetaData().getURL());
            System.out.println("Database name: " + testConnection.getCatalog());
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
            showAlert("Error", "Failed to connect to database: " + e.getMessage());
            return;
        }

        // Let's also check what tables exist
        try (Connection connection = DatabaseConnection.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            System.out.println("Available tables:");
            while (tables.next()) {
                System.out.println("- " + tables.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            System.out.println("Failed to get table information: " + e.getMessage());
        }

        String query = "SELECT * FROM users WHERE username = ?";
        System.out.println("Executing query: " + query + " with username: '" + username + "'");

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            // Let's also check how many total users exist
            try (PreparedStatement countStatement = connection.prepareStatement("SELECT COUNT(*) as total FROM users")) {
                ResultSet countResult = countStatement.executeQuery();
                if (countResult.next()) {
                    System.out.println("Total users in database: " + countResult.getInt("total"));
                }
            }

            // Let's see all usernames in the database
            try (PreparedStatement allUsersStatement = connection.prepareStatement("SELECT username FROM users LIMIT 10")) {
                ResultSet allUsersResult = allUsersStatement.executeQuery();
                System.out.println("Existing usernames in database:");
                while (allUsersResult.next()) {
                    System.out.println("- '" + allUsersResult.getString("username") + "'");
                }
            }

            if (resultSet.next()) {
                System.out.println("Player data found in database for username: " + username);
                playerData = new PlayerData();
                playerData.setUsername(resultSet.getString("username"));
                playerData.setLevel(resultSet.getInt("level"));
                playerData.setExperience(resultSet.getInt("experience"));
                playerData.setMaxExperience(resultSet.getInt("max_experience"));
                playerData.setTotalGames(resultSet.getInt("total_games"));
                playerData.setWins(resultSet.getInt("wins"));
                playerData.setLosses(resultSet.getInt("losses"));
                playerData.setTotalScore(resultSet.getInt("total_score"));
                playerData.setPlaytimeHours(resultSet.getInt("playtime_hours"));
                playerData.setRank(resultSet.getString("rank"));
                playerData.setProfilePicUrl(resultSet.getString("profile_pic_url"));

                System.out.println("Loaded player data - Level: " + playerData.getLevel() +
                        ", Total Games: " + playerData.getTotalGames() +
                        ", Wins: " + playerData.getWins());
            } else {
                System.out.println("No player data found for username: '" + username + "'");
                System.out.println("Make sure this exact username exists in your database");
                showAlert("Error", "No player found with username: " + username);
            }
        } catch (SQLException e) {
            System.out.println("SQL Exception occurred: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load player data: " + e.getMessage());
        }
    }

    private void setupProfileDisplay() {
        System.out.println("Setting up profile display...");

        // Check if UI components are available
        if (usernameLabel == null) {
            System.out.println("UI components not available yet");
            return;
        }

        if (playerData != null) {
            System.out.println("Updating profile display with data:");
            System.out.println("- Username: " + playerData.getUsername());
            System.out.println("- Level: " + playerData.getLevel());
            System.out.println("- Experience: " + playerData.getExperience() + "/" + playerData.getMaxExperience());

            usernameLabel.setText(playerData.getUsername());
            levelLabel.setText("Level " + playerData.getLevel());

            // Setup experience bar
            if (playerData.getMaxExperience() > 0) {
                double progress = (double) playerData.getExperience() / playerData.getMaxExperience();
                experienceBar.setProgress(progress);
                experienceLabel.setText(playerData.getExperience() + " / " + playerData.getMaxExperience() + " XP");
                System.out.println("Experience bar set to: " + (progress * 100) + "%");
            } else {
                experienceBar.setProgress(0);
                experienceLabel.setText("0 / 0 XP");
            }

            // Load profile picture
            try {
                String profilePicUrl = playerData.getProfilePicUrl();
                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    Image image = new Image("file:" + profilePicUrl);
                    profileImageView.setImage(image);
                    System.out.println("Profile image loaded from: " + profilePicUrl);
                } else {
                    // Default image if profile pic is not available
                    Image defaultImage = new Image(getClass().getResourceAsStream("/com/cyberheist/images/default_avatar.png"));
                    profileImageView.setImage(defaultImage);
                    System.out.println("Using default profile image");
                }
            } catch (Exception e) {
                System.out.println("Error loading profile image, using default: " + e.getMessage());
                try {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/com/cyberheist/images/default_avatar.png"));
                    profileImageView.setImage(defaultImage);
                } catch (Exception ex) {
                    System.out.println("Default avatar image not found");
                }
            }
        } else {
            System.out.println("Player data is null, cannot setup profile display");
        }
    }

    private void setupStatistics() {
        System.out.println("Setting up statistics...");

        // Check if UI components are available
        if (totalGamesLabel == null) {
            System.out.println("Statistics labels not available yet");
            return;
        }

        if (playerData != null) {
            System.out.println("Updating statistics with data:");
            System.out.println("- Total Games: " + playerData.getTotalGames());
            System.out.println("- Wins: " + playerData.getWins());
            System.out.println("- Losses: " + playerData.getLosses());
            System.out.println("- Total Score: " + playerData.getTotalScore());

            totalGamesLabel.setText(String.valueOf(playerData.getTotalGames()));

            // Add color to wins and losses
            winsLabel.setText(String.valueOf(playerData.getWins()));
            winsLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

            lossesLabel.setText(String.valueOf(playerData.getLosses()));
            lossesLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

            // Handle division by zero for win rate and add color based on win rate
            if (playerData.getTotalGames() > 0) {
                double winRate = (double) playerData.getWins() / playerData.getTotalGames() * 100;
                winRateLabel.setText(String.format("%.1f%%", winRate));
                System.out.println("- Win Rate: " + String.format("%.1f%%", winRate));

                // Color the win rate label based on percentage
                if (winRate >= 70.0) {
                    winRateLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Green for good
                } else if (winRate >= 40.0) {
                    winRateLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;"); // Orange for average
                } else {
                    winRateLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Red for poor
                }
            } else {
                winRateLabel.setText("0.0%");
                winRateLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-weight: normal;"); // Gray for no games
            }

            // Format total score with thousands separators
            totalScoreLabel.setText(String.format("%,d", playerData.getTotalScore()));

            // Handle division by zero for average score
            if (playerData.getTotalGames() > 0) {
                int averageScore = playerData.getTotalScore() / playerData.getTotalGames();
                averageScoreLabel.setText(String.format("%,d", averageScore));
                System.out.println("- Average Score: " + averageScore);
            } else {
                averageScoreLabel.setText("0");
            }

            playtimeLabel.setText(playerData.getPlaytimeHours() + " hours");

            // Format the rank with better styling
            rankLabel.setText(playerData.getRank());
            if (playerData.getRank() != null) {
                String rank = playerData.getRank().toUpperCase();
                rankLabel.setText(rank);

                // Style rank based on level
                if (rank.contains("GOLD") || rank.contains("PLATINUM") || rank.contains("DIAMOND")) {
                    rankLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;"); // Gold color
                } else if (rank.contains("SILVER")) {
                    rankLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;"); // Silver color
                } else if (rank.contains("BRONZE")) {
                    rankLabel.setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold;"); // Bronze color
                }
            }

            System.out.println("Statistics setup completed successfully");
        } else {
            System.out.println("Player data is null, cannot setup statistics");
        }
    }

    private void loadGameHistoryData() {
        System.out.println("Loading game history data...");

        if (gameHistoryTable == null) {
            System.out.println("Game history table not available yet");
            return;
        }

        gameHistoryList = FXCollections.observableArrayList();

        // Modified query to get the most recent 10 games with proper ordering
        String query = "SELECT * FROM game_history WHERE username = ? ORDER BY date DESC LIMIT 10";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            int count = 0;
            while (resultSet.next()) {
                String gameType = resultSet.getString("game_type");
                String result = resultSet.getString("result");

                // Format the result to be more user-friendly (uppercase for emphasis)
                String formattedResult = result.toUpperCase();

                // Format game type to be more user-friendly
                String formattedGameType = gameType;
                switch (gameType) {
                    case "BattleGame":
                        formattedGameType = "Cyber Battle";
                        break;
                    case "SpaceGame":
                        formattedGameType = "Space Shooter";
                        break;
                    case "BubbleShooter":
                        formattedGameType = "Bubble Shooter";
                        break;
                    case "Tetris":
                        formattedGameType = "Tetris";
                        break;
                    case "Snake":
                        formattedGameType = "Snake";
                        break;
                    case "TicTacToe":
                        formattedGameType = "Tic-Tac-Toe";
                        break;
                    case "WordGame":
                        formattedGameType = "Word Decoder";
                        break;
                }

                // Format the date in a more readable way
                String dateStr = resultSet.getString("date");
                String formattedDate = dateStr;
                try {
                    // Parse the date from database format
                    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr, inputFormatter);

                    // Format it for display
                    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                    formattedDate = dateTime.format(outputFormatter);
                } catch (Exception e) {
                    System.out.println("Date parsing error: " + e.getMessage());
                    // Keep original format if parsing fails
                }

                GameHistory gameHistory = new GameHistory(
                        formattedGameType,
                        formattedResult,
                        resultSet.getInt("score"),
                        formattedDate,
                        resultSet.getString("duration")
                );
                gameHistoryList.add(gameHistory);
                count++;
            }

            gameHistoryTable.setItems(gameHistoryList);
            System.out.println("Loaded " + count + " game history records");

            // Refresh the table view to show the new data
            gameHistoryTable.refresh();

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error loading game history: " + e.getMessage());
            showAlert("Error", "Failed to load game history data.");
        }
    }

    @FXML
    private void handleChangeProfilePic() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) changeProfilePicButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Save the selected file to a folder (you can customize this path)
                String profilePicPath = selectedFile.getAbsolutePath();

                // Update the profile picture in the UI
                Image newImage = new Image(selectedFile.toURI().toString());
                profileImageView.setImage(newImage);

                // Update the profile picture URL in the database
                updateProfilePicInDatabase(profilePicPath);

            } catch (Exception e) {
                showAlert("Error", "Could not load the selected image.");
            }
        }
    }

    private void updateProfilePicInDatabase(String profilePicPath) {
        String query = "UPDATE users SET profile_pic_url = ? WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, profilePicPath);
            preparedStatement.setString(2, username); // Use username instead of playerData.getUsername()

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Profile picture updated successfully!");
                // Update the playerData object as well
                if (playerData != null) {
                    playerData.setProfilePicUrl(profilePicPath);
                }
            } else {
                showAlert("Error", "Failed to update profile picture.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update profile picture.");
        }
    }

    @FXML
    private void handleEditProfile() {
        if (playerData == null) {
            showAlert("Error", "No player data available.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(playerData.getUsername());
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Edit your profile information");
        dialog.setContentText("Username:");

        dialog.showAndWait().ifPresent(newUsername -> {
            if (!newUsername.trim().isEmpty() && !newUsername.trim().equals(playerData.getUsername())) {
                // Update the username in the database first
                if (updateUsernameInDatabase(newUsername.trim())) {
                    // If database update successful, update the UI and local data
                    String oldUsername = this.username;
                    this.username = newUsername.trim();

                    playerData.setUsername(newUsername.trim());
                    usernameLabel.setText(newUsername.trim());

                    System.out.println("Username changed from " + oldUsername + " to " + newUsername.trim());
                }
            }
        });
    }

    private boolean updateUsernameInDatabase(String newUsername) {
        String query = "UPDATE users SET username = ? WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, newUsername);
            preparedStatement.setString(2, username); // Use current username

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Username updated successfully!");
                return true;
            } else {
                showAlert("Error", "Failed to update username.");
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update username: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void handleBack() {
        // loadScene("GameMenu.fxml");
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    private void loadScene(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/cyberheist/" + fxmlFile));
            Scene scene = new Scene(root, 1024, 800);
            scene.getStylesheets().add(getClass().getResource("/com/cyberheist/styles.css").toExternalForm());

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not load the requested page.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner classes for data models
    public static class PlayerData {
        private String username;
        private int level;
        private int experience;
        private int maxExperience;
        private int totalGames;
        private int wins;
        private int losses;
        private int totalScore;
        private int playtimeHours;
        private String rank;
        private String profilePicUrl;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public int getExperience() { return experience; }
        public void setExperience(int experience) { this.experience = experience; }
        public int getMaxExperience() { return maxExperience; }
        public void setMaxExperience(int maxExperience) { this.maxExperience = maxExperience; }
        public int getTotalGames() { return totalGames; }
        public void setTotalGames(int totalGames) {
            this.totalGames = totalGames;
            System.out.println("Total games set to: " + totalGames);
        }
        public int getWins() { return wins; }
        public void setWins(int wins) { this.wins = wins; }
        public int getLosses() { return losses; }
        public void setLosses(int losses) { this.losses = losses; }
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        public int getPlaytimeHours() { return playtimeHours; }
        public void setPlaytimeHours(int playtimeHours) { this.playtimeHours = playtimeHours; }
        public String getRank() { return rank; }
        public void setRank(String rank) { this.rank = rank; }
        public String getProfilePicUrl() { return profilePicUrl; }
        public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }
    }

    public static class GameHistory {
        private String gameType;
        private String result;
        private int score;
        private String date;
        private String duration;

        public GameHistory(String gameType, String result, int score, String date, String duration) {
            this.gameType = gameType;
            this.result = result;
            this.score = score;
            this.date = date;
            this.duration = duration;
        }

        // Getters
        public String getGameType() { return gameType; }
        public String getResult() { return result; }
        public int getScore() { return score; }
        public String getDate() { return date; }
        public String getDuration() { return duration; }
    }
}