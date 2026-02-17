package com.cyberheist;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;
import java.util.ResourceBundle;

public class TetrisController implements Initializable {

    @FXML private Canvas gameCanvas;
    @FXML private Label scoreLabel;
    @FXML private Label levelLabel;
    @FXML private Label linesLabel;
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button restartButton;
    @FXML private Button backButton;
    @FXML private Button mainMenuButton;

    private GraphicsContext gc;
    private boolean gameRunning = false;
    private boolean gamePaused = false;
    private AnimationTimer gameLoop;

    private static final int BOARD_WIDTH = 15;
    private static final int BOARD_HEIGHT = 22;
    private static final int CELL_SIZE = 22;
    private static final int COINS_PER_POINT = 1;

    private final int[][] board = new int[BOARD_HEIGHT][BOARD_WIDTH];
    private int score = 0;
    private int level = 1;
    private int linesCleared = 0;
    private long lastDropTime = 0;
    private long dropInterval = 500_000_000; // nanoseconds

    private Piece currentPiece;
    private int pieceX, pieceY;

    // Injected by SceneManager
    private String username;

    public void setUsername(String username) {
        this.username = username;
        if (username == null || username.isEmpty()) {
            showAlert("Not Logged In", "Please log in to play Tetris.");
            //disableGame();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gc = gameCanvas.getGraphicsContext2D();

        // Ensure canvas can receive key events - FIXED
        gameCanvas.setFocusTraversable(true);

        // Set up key handler on the canvas parent's scene when available
        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> handleKey(e.getCode()));
            }
        });

        // Also set directly on canvas as backup
        gameCanvas.setOnKeyPressed(e -> handleKey(e.getCode()));

        startButton.setOnAction(e -> handleStart());
        pauseButton.setOnAction(e -> handlePause());
        restartButton.setOnAction(e -> handleRestart());
        backButton.setOnAction(e -> handleBack());
        mainMenuButton.setOnAction(e -> handleMainMenu());

        initGame();
    }

    private void handleKey(KeyCode key) {
        if (!gameRunning || gamePaused || currentPiece == null) return;

        System.out.println("Key pressed: " + key); // Debug output

        switch (key) {
            // Move left: A or left arrow
            case A:
            case LEFT:
                tryMove(currentPiece, pieceX - 1, pieceY);
                break;

            // Move right: D or right arrow
            case D:
            case RIGHT:
                tryMove(currentPiece, pieceX + 1, pieceY);
                break;

            // Soft drop down: S or down arrow
            case S:
            case DOWN:
                if (tick()) {
                    score += 1;
                    updateLabels();
                }
                break;

            // Rotate: W or Space or up arrow
            case W:
            case SPACE:
            case UP:
                tryMove(currentPiece.rotated(), pieceX, pieceY);
                break;

            default:
                return;
        }
        draw();
    }

    /**
     * Attempts to move the current piece to (x,y) if no collision.
     */
    private void tryMove(Piece p, int x, int y) {
        if (!collision(p, x, y)) {
            currentPiece = p;
            pieceX = x;
            pieceY = y;
            System.out.println("Piece moved to: " + x + ", " + y); // Debug output
        } else {
            System.out.println("Move blocked to: " + x + ", " + y); // Debug output
        }
    }

    private void initGame() {
        clearBoard();
        score = linesCleared = 0;
        level = 1;
        dropInterval = 500_000_000;

        spawnPiece();
        updateLabels();
        draw();

        if (gameLoop != null) gameLoop.stop();
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameRunning || gamePaused) return;
                if (now - lastDropTime >= dropInterval) {
                    tick();
                    lastDropTime = now;
                    updateLabels();
                    draw();
                }
            }
        };
    }

    @FXML
    private void handleStart() {
        if (username == null || username.isEmpty()) {
            showAlert("Not Logged In", "Please log in to play Tetris.");
            return;
        }

        gameRunning = !gameRunning;
        if (gameRunning) {
            // Request focus when starting the game
            gameCanvas.requestFocus();
            gameLoop.start();
            startButton.setText("Stop");
            lastDropTime = System.nanoTime(); // Reset drop timer
        } else {
            gameLoop.stop();
            startButton.setText("Start");
        }
    }

    @FXML
    private void handlePause() {
        if (!gameRunning) return;
        gamePaused = !gamePaused;
        pauseButton.setText(gamePaused ? "Resume" : "Pause");
        if (!gamePaused) {
            gameCanvas.requestFocus(); // Restore focus when resuming
            lastDropTime = System.nanoTime(); // Reset drop timer
        }
    }

    @FXML
    private void handleRestart() {
        // Stop current game
        gameRunning = false;
        gamePaused = false;
        if (gameLoop != null) gameLoop.stop();

        // Reset button states
        startButton.setText("Start");
        pauseButton.setText("Pause");

        // Reinitialize the game
        initGame();

        // Request focus for keyboard input
        gameCanvas.requestFocus();

        System.out.println("Game restarted");
    }

    @FXML private void handleBack() {
        if (gameLoop != null) gameLoop.stop();
        SceneManager.getInstance().switchToScene("MiniGames.fxml");
    }

    @FXML private void handleMainMenu() {
        if (gameLoop != null) gameLoop.stop();
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    /** Drop the piece one cell; return true if successful drop. */
    private boolean tick() {
        if (currentPiece == null) return false;

        if (collision(currentPiece, pieceX, pieceY + 1)) {
            lockPiece();
            return false;
        }
        pieceY++;
        return true;
    }

    private void spawnPiece() {
        currentPiece = Piece.randomPiece();
        pieceX = BOARD_WIDTH/2 - currentPiece.width()/2;
        pieceY = 0;

        System.out.println("New piece spawned at: " + pieceX + ", " + pieceY); // Debug output

        if (collision(currentPiece, pieceX, pieceY)) {
            // Game over
            gameOver();
        }
    }

    private boolean collision(Piece p, int x, int y) {
        if (p == null) return true;

        int[][] shape = p.shape();
        for (int r = 0; r < p.height(); r++) {
            for (int c = 0; c < p.width(); c++) {
                if (shape[r][c] != 0) {
                    int by = y + r, bx = x + c;
                    if (bx < 0 || bx >= BOARD_WIDTH || by >= BOARD_HEIGHT) return true;
                    if (by >= 0 && board[by][bx] != 0) return true;
                }
            }
        }
        return false;
    }

    private void lockPiece() {
        if (currentPiece == null) return;

        int[][] shape = currentPiece.shape();
        for (int r = 0; r < currentPiece.height(); r++) {
            for (int c = 0; c < currentPiece.width(); c++) {
                if (shape[r][c] != 0) {
                    board[pieceY + r][pieceX + c] = currentPiece.type()+1;
                }
            }
        }
        clearLines();
        spawnPiece();
    }

    private void clearLines() {
        int lines = 0;
        for (int r = BOARD_HEIGHT - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < BOARD_WIDTH; c++) {
                if (board[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                lines++;
                // Shift all rows above down
                for (int rr = r; rr > 0; rr--) {
                    System.arraycopy(board[rr-1], 0, board[rr], 0, BOARD_WIDTH);
                }
                // Clear top row
                java.util.Arrays.fill(board[0], 0);
                r++; // Check the same row again since we shifted everything down
            }
        }
        if (lines > 0) {
            linesCleared += lines;
            score += lines * 100;
            level = 1 + linesCleared/10;
            dropInterval = Math.max(100_000_000, 500_000_000 - (level-1)*40_000_000);
        }
    }

    private void gameOver() {
        gameRunning = false;
        if (gameLoop != null) gameLoop.stop();
        int coins = score * COINS_PER_POINT;
        showAlert("Game Over", "Score: " + score + "\nCoins earned: " + coins);
        awardCoins(coins);

        // Add game history record
        updateGameHistory("Tetris", score > 500 ? "win" : "loss", score);

        SceneManager.getInstance().switchToScene("MiniGames.fxml");
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
            stmt.setString(5, level + " levels");

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

    private void clearBoard() {
        for (int[] row : board) {
            java.util.Arrays.fill(row, 0);
        }
    }

    private void updateLabels() {
        scoreLabel.setText("Score: " + score);
        levelLabel.setText("Level: " + level);
        linesLabel.setText("Lines: " + linesCleared);
    }

    private void draw() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw locked blocks
        for (int r = 0; r < BOARD_HEIGHT; r++) {
            for (int c = 0; c < BOARD_WIDTH; c++) {
                int val = board[r][c];
                if (val != 0) {
                    gc.setFill(Piece.PIECE_COLORS[val-1]);
                    gc.fillRect(c*CELL_SIZE, r*CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    // Add border for better visibility
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(1);
                    gc.strokeRect(c*CELL_SIZE, r*CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // Draw falling piece
        if (currentPiece != null) {
            gc.setFill(Piece.PIECE_COLORS[currentPiece.type()]);
            int[][] shape = currentPiece.shape();
            for (int r = 0; r < currentPiece.height(); r++) {
                for (int c = 0; c < currentPiece.width(); c++) {
                    if (shape[r][c] != 0) {
                        gc.fillRect((pieceX+c)*CELL_SIZE, (pieceY+r)*CELL_SIZE, CELL_SIZE, CELL_SIZE);

                        // Add border for better visibility
                        gc.setStroke(Color.WHITE);
                        gc.setLineWidth(1);
                        gc.strokeRect((pieceX+c)*CELL_SIZE, (pieceY+r)*CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    }
                }
            }
        }
    }

    private void awardCoins(int amount) {
        if (username == null) return;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE users SET coins = coins + ? WHERE username = ?")) {
            ps.setInt(1, amount);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not update coins.");
        }
    }

    /** Helper to show alerts from this controller */
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}