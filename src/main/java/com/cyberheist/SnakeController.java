package com.cyberheist;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.geometry.VPos;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.sql.*;

public class SnakeController {

    @FXML
    private Canvas gameCanvas;

    @FXML
    private Label scoreLabel;

    @FXML
    private Button startButton;

    @FXML
    private Button backButton;

    @FXML
    private Button mainMenuButton;

    private GraphicsContext gc;
    private List<Point> snake;
    private Point food;
    private int direction = 0; // 0=right, 1=down, 2=left, 3=up
    private boolean gameRunning = false;
    private int score = 0;
    private AnimationTimer gameLoop;
    private Random random = new Random();

    // Board and Cell Dimensions as specified
    private final int BOARD_WIDTH = 25;  // Units wide
    private final int BOARD_HEIGHT = 25; // Units high
    private final int CELL_SIZE = 19;    // Pixels per unit
    private final int BOARD_X = 40;     // X-offset for board drawing on canvas
    private final int BOARD_Y = 30;     // Y-offset for board drawing on canvas

    // Calculate canvas pixel dimensions to contain the board + offsets
    private final int CANVAS_DISPLAY_WIDTH = BOARD_X * 2 + BOARD_WIDTH * CELL_SIZE;
    private final int CANVAS_DISPLAY_HEIGHT = BOARD_Y * 2 + BOARD_HEIGHT * CELL_SIZE;

    private String username;

    // Inner class to represent points on the board
    private class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @FXML
    private void initialize() {
        gc = gameCanvas.getGraphicsContext2D();

        // Set canvas dimensions dynamically based on calculated display size
        gameCanvas.setWidth(CANVAS_DISPLAY_WIDTH);
        gameCanvas.setHeight(CANVAS_DISPLAY_HEIGHT);

        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();

        gameCanvas.setOnKeyPressed(e -> {
            if (!gameRunning) return;

            KeyCode key = e.getCode();
            switch (key) {
                case UP:    if (direction != 1) direction = 3; break;
                case DOWN:  if (direction != 3) direction = 1; break;
                case LEFT:  if (direction != 0) direction = 2; break;
                case RIGHT: if (direction != 2) direction = 0; break;

                case W:     if (direction != 1) direction = 3; break;
                case S:     if (direction != 3) direction = 1; break;
                case A:     if (direction != 0) direction = 2; break;
                case D:     if (direction != 2) direction = 0; break;
            }
        });

        // Get the username of the logged-in user from SceneManager
        username = SceneManager.getInstance().getCurrentUsername();
        if (username == null || username.isEmpty()) {
            showAlert("Error", "User not logged in.");
            return;
        }

        initGame();
    }

    private void initGame() {
        snake = new ArrayList<>();
        // Start snake at a central position within the new board dimensions
        snake.add(new Point(BOARD_WIDTH / 2 - 2, BOARD_HEIGHT / 2));
        snake.add(new Point(BOARD_WIDTH / 2 - 1, BOARD_HEIGHT / 2));
        snake.add(new Point(BOARD_WIDTH / 2, BOARD_HEIGHT / 2));

        spawnFood();
        score = 0;
        scoreLabel.setText("Score: 0");
        direction = 0; // Initial direction (right)

        draw();
    }

    private void spawnFood() {
        int x = random.nextInt(BOARD_WIDTH);
        int y = random.nextInt(BOARD_HEIGHT);
        food = new Point(x, y);

        // Make sure food doesn't spawn on snake
        for (Point p : snake) {
            if (p.x == food.x && p.y == food.y) {
                spawnFood(); // Recurse if food spawns on snake
                return;
            }
        }
    }

    @FXML
    private void handleStart() {
        if (gameRunning) {
            gameLoop.stop();
            gameRunning = false;
            startButton.setText("Start Game");
            return;
        }

        initGame();
        gameRunning = true;
        startButton.setText("Stop Game");
        gameCanvas.requestFocus();

        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 150_000_000) { // 150ms delay
                    update();
                    draw();
                    lastUpdate = now;
                }
            }
        };
        gameLoop.start();
    }

    private void update() {
        if (!gameRunning) return;

        Point head = snake.get(snake.size() - 1);
        Point newHead = new Point(head.x, head.y);

        switch (direction) {
            case 0: newHead.x++; break; // right
            case 1: newHead.y++; break; // down
            case 2: newHead.x--; break; // left
            case 3: newHead.y--; break; // up
        }

        // Check wall collision (using BOARD_WIDTH and BOARD_HEIGHT)
        if (newHead.x < 0 || newHead.x >= BOARD_WIDTH || newHead.y < 0 || newHead.y >= BOARD_HEIGHT) {
            gameOver();
            return;
        }

        // Check self collision
        for (Point p : snake) {
            if (p.x == newHead.x && p.y == newHead.y) {
                gameOver();
                return;
            }
        }

        snake.add(newHead);

        // Check food collision
        if (newHead.x == food.x && newHead.y == food.y) {
            score += 10;
            scoreLabel.setText("Score: " + score);
            spawnFood();
        } else {
            snake.remove(0);
        }
    }

    private void draw() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw the board grid and border
        drawBoardGrid();

        // Draw snake
        gc.setFill(Color.LIME);
        for (Point p : snake) {
            gc.fillRect(BOARD_X + p.x * CELL_SIZE, BOARD_Y + p.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        // Draw food
        gc.setFill(Color.RED);
        gc.fillRect(BOARD_X + food.x * CELL_SIZE, BOARD_Y + food.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
    }

    private void drawBoardGrid() {
        gc.setFill(Color.web("#222222"));
        gc.fillRect(BOARD_X, BOARD_Y, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);

        gc.setStroke(Color.DARKSLATEGRAY);
        gc.setLineWidth(1);

        for (int x = 0; x <= BOARD_WIDTH; x++) {
            gc.strokeLine(BOARD_X + x * CELL_SIZE, BOARD_Y,
                    BOARD_X + x * CELL_SIZE, BOARD_Y + BOARD_HEIGHT * CELL_SIZE);
        }

        for (int y = 0; y <= BOARD_HEIGHT; y++) {
            gc.strokeLine(BOARD_X, BOARD_Y + y * CELL_SIZE,
                    BOARD_X + BOARD_WIDTH * CELL_SIZE, BOARD_Y + y * CELL_SIZE);
        }

        gc.setStroke(Color.CYAN);
        gc.setLineWidth(3);
        gc.strokeRect(BOARD_X, BOARD_Y, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);
    }

    private void gameOver() {
        gameRunning = false;
        if (gameLoop != null) {
            gameLoop.stop();
        }

        int earnedCoins = score / 2; // 5 coins per 10 points

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Courier New", 24));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        double boardCenterX = BOARD_X + (BOARD_WIDTH * CELL_SIZE) / 2;
        double boardCenterY = BOARD_Y + (BOARD_HEIGHT * CELL_SIZE) / 2;

        gc.fillText("Game Over! Score: " + score, boardCenterX, boardCenterY - 20);
        gc.fillText("You earned " + earnedCoins + " coins!", boardCenterX, boardCenterY + 20);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("Final Score: " + score);
        alert.setContentText("You earned " + earnedCoins + " coins!");
        alert.showAndWait();

        // Add coins to the player's account
        updateCoinsInDatabase(earnedCoins);

        // Add game history record
        updateGameHistory("Snake", score > 50 ? "win" : "loss", score);

        // Reset the game
        startButton.setText("Start Game");
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
            stmt.setString(5, (score / 10) + " food items");

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

    private void updateCoinsInDatabase(int earnedCoins) {
        String updateQuery = "UPDATE users SET coins = coins + ? WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {

            preparedStatement.setInt(1, earnedCoins);
            preparedStatement.setString(2, username);  // Use the logged-in username

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Coins updated successfully!");
            } else {
                showAlert("Error", "Failed to update coins in the database.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update coins.");
        }
    }

    @FXML
    private void handleBack() {
        if (gameLoop != null) gameLoop.stop();
        SceneManager.getInstance().switchToScene("MiniGames.fxml");
    }

    @FXML
    private void handleMainMenu() {
        if (gameLoop != null) gameLoop.stop();
        SceneManager.getInstance().switchToScene("MainMenu.fxml");
    }

    private void loadScene(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Scene scene = new Scene(root, 1024, 800);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
