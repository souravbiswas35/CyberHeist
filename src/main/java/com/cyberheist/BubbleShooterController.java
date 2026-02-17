package com.cyberheist;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BubbleShooterController {
    @FXML private Canvas gameCanvas;
    @FXML private Label scoreLabel;
    @FXML private Label levelLabel;
    @FXML private Label bubblesLeftLabel;
    @FXML private Label coinsLabel;
    @FXML private ProgressBar levelProgressBar;
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button restartButton;
    @FXML private Button backButton;
    @FXML private Button mainMenuButton;
    @FXML private Button swapButton;  // NEW: Swap button

    private GraphicsContext gc;
    private List<Bubble> bubbles;
    private List<Bubble> fallingBubbles;
    private List<Particle> particles;
    private Bubble shooterBubble;
    private Bubble nextBubble;
    private boolean gameRunning = false;
    private boolean gamePaused = false;
    private boolean gameOver = false;
    private int score = 0;
    private int level = 1;
    private int bubblesLeft = 50;
    private int coins = 80;
    private int totalBubblesCleared = 0;
    private int initialBubbleCount = 0;
    private AnimationTimer gameLoop;
    private Random random = new Random();
    private double mouseX = 400, mouseY = 300;
    private double shooterX = 400, shooterY = 460;
    
    // User data
    private String username;
    private boolean isAuthenticated = false;
    
    // Reference to GameMenuController
    private GameMenuController gameMenuController;

    // NEW: Swap functionality variables
    private boolean canSwap = true;
    private int swapsRemaining = 10;

    // Game constants
    private final double BUBBLE_RADIUS = 16;
    private final double BUBBLE_DIAMETER = 32;
    private final int ROWS = 12;
    private final int COLS = 25;
    private final double ROW_HEIGHT = 28;
    private final double SHOT_SPEED = 10;
    private final int MAX_SWAPS_PER_LEVEL = 10;  // NEW: Max swaps per level
    private final Color[] BUBBLE_COLORS = {
            Color.web("#FF5733"), // Vibrant Red
            Color.web("#FFBD33"), // Golden Yellow
            Color.web("#33FF57"), // Bright Green
            Color.web("#33A1FF"), // Bright Blue
            Color.web("#A133FF"), // Purple
            Color.web("#FF33A1"), // Hot Pink
            Color.web("#33FFDD"), // Turquoise
            Color.web("#FFC300") // Light Yellow
    };

    private class Bubble {
        double x, y, vx, vy;
        Color color;
        boolean moving, marked;
        int row, col;
        double scale = 1.0;
        boolean popping = false;

        Bubble(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }

        Bubble(double x, double y, Color color, int row, int col) {
            this(x, y, color);
            this.row = row;
            this.col = col;
        }

        void setVelocity(double vx, double vy) {
            this.vx = vx;
            this.vy = vy;
            this.moving = true;
        }

        void update() {
            if (moving) {
                x += vx;
                y += vy;
            }
            if (popping && scale > 0) {
                scale -= 0.05;
            }
        }

        boolean collidesWith(Bubble other) {
            if (other == null) return false;
            double dx = x - other.x;
            double dy = y - other.y;
            return Math.sqrt(dx * dx + dy * dy) < BUBBLE_DIAMETER - 2;
        }

        void draw(GraphicsContext gc) {
            if (scale <= 0) return;

            double drawRadius = BUBBLE_RADIUS * scale;
            double drawDiameter = drawRadius * 2;

            // Create gradient for 3D effect with better contrast
            RadialGradient gradient = new RadialGradient(
                    0, 0, 0.3, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
                    new Stop(0, color.brighter().brighter()), // Brightest part
                    new Stop(0.7, color), // Base color
                    new Stop(1.0, color.darker()) // Darkest part
            );

            gc.setFill(gradient);
            gc.fillOval(x - drawRadius, y - drawRadius, drawDiameter, drawDiameter);

            // Highlight for a nice glowing effect
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.4));
            gc.fillOval(x - drawRadius + 4, y - drawRadius + 4, drawRadius, drawRadius);

            // Border for better definition
            gc.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.8));
            gc.setLineWidth(1.5);
            gc.strokeOval(x - drawRadius, y - drawRadius, drawDiameter, drawDiameter);
        }
    }

    private class Particle {
        double x, y, vx, vy;
        Color color;
        double life = 1.0;
        double size;

        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (random.nextDouble() - 0.5) * 8;
            this.vy = (random.nextDouble() - 0.5) * 8 - 2;
            this.size = random.nextDouble() * 4 + 2;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.2; // gravity
            life -= 0.02;
        }

        void draw(GraphicsContext gc) {
            if (life <= 0) return;
            gc.setFill(color.deriveColor(0, 1, 1, life));
            gc.fillOval(x - size/2, y - size/2, size, size);
        }
    }

    @FXML
    private void initialize() {
        gc = gameCanvas.getGraphicsContext2D();
        gameCanvas.setFocusTraversable(true);
        gameCanvas.setOnMouseMoved(this::handleMouseMove);
        gameCanvas.setOnMouseClicked(this::handleMouseClick);

        // NEW: Add keyboard support for swap (Spacebar)
        gameCanvas.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("SPACE")) {
                handleSwap();
            }
        });

        bubbles = new ArrayList<>();
        fallingBubbles = new ArrayList<>();
        particles = new ArrayList<>();

        initGame();
    }
    
    // Method to set the game menu controller
    public void setGameMenuController(GameMenuController controller) {
        this.gameMenuController = controller;
        if (controller != null) {
            this.username = controller.getUsername();
            checkAuthentication();
        }
    }

    // Method to set username and check authentication
    public void setUsername(String username) {
        this.username = username;
        checkAuthentication();
    }
    
    // Check if user is authenticated
    private void checkAuthentication() {
        if (username != null && !username.isEmpty()) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT * FROM users WHERE username = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    isAuthenticated = true;
                    System.out.println("User authenticated: " + username);
                } else {
                    isAuthenticated = false;
                    System.out.println("User not authenticated");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Database error: " + e.getMessage());
                isAuthenticated = false;
            }
        } else {
            isAuthenticated = false;
        }
    }

    private void initGame() {
        bubbles.clear();
        fallingBubbles.clear();
        particles.clear();
        score = 0;
        level = 1;
        bubblesLeft = 50;
        totalBubblesCleared = 0;
        gameOver = false;
        gamePaused = false;
        swapsRemaining = MAX_SWAPS_PER_LEVEL;  // NEW: Reset swaps
        canSwap = true;  // NEW: Enable swapping

        updateLabels();
        createInitialBubbles();
        createShooterBubble();
        createNextBubble();
        updateSwapButton();  // NEW: Update swap button
        draw();
    }

    private void createInitialBubbles() {
        bubbles.clear();
        initialBubbleCount = 0; // Reset for each new game

        int maxRows = Math.max(10, 6 + level/3); // Original logic for max rows

        // A 2D array to temporarily store bubbles for easy neighbor access during generation
        Bubble[][] grid = new Bubble[ROWS][COLS];

        int bubbleCount = 0;
        for (int row = 0; row < maxRows; row++) {
            int colsInRow = (row % 2 == 0) ? 24 : 23;
            // Calculate initial X offset to center the row on the canvas
            double initialXOffset = (gameCanvas.getWidth() - (colsInRow * BUBBLE_DIAMETER)) * 0.5; // Keeping this for consistency

            for (int col = 0; col < colsInRow; col++) {
                // Original: if (random.nextDouble() > 0.8) continue; // Random gaps
                // For "easier" patterns, let's make fewer gaps, e.g., 0.9 for 90% chance of a bubble
                if (random.nextDouble() > 0.9) continue; // High chance of placing a bubble

                double x = col * BUBBLE_DIAMETER + BUBBLE_RADIUS + 16;
                if (row % 2 == 1) {
                    x += BUBBLE_RADIUS;
                }
                double y = row * ROW_HEIGHT + BUBBLE_RADIUS + 40;

                // --- MODIFICATION START ---
                // Try to make bubbles of similar color together
                Color chosenColor;
                List<Color> neighborColors = new ArrayList<>();

                // Check top-left neighbor
                if (row > 0 && col > 0) { // Adjusted for grid bounds
                    if (row % 2 == 0) { // Even row, check top-left
                        if (grid[row - 1][col - 1] != null) neighborColors.add(grid[row - 1][col - 1].color);
                    } else { // Odd row, top-left is directly above
                        if (grid[row - 1][col] != null) neighborColors.add(grid[row - 1][col].color);
                    }
                }
                // Check top-right neighbor
                if (row > 0 && col < colsInRow - 1) { // Adjusted for grid bounds
                    if (row % 2 == 0) { // Even row, check top-right
                        if (grid[row - 1][col] != null) neighborColors.add(grid[row - 1][col].color);
                    } else { // Odd row, top-right is directly above + 1 col
                        if (grid[row - 1][col + 1] != null) neighborColors.add(grid[row - 1][col + 1].color);
                    }
                }
                // Check left neighbor (in the same row)
                if (col > 0 && grid[row][col - 1] != null) {
                    neighborColors.add(grid[row][col - 1].color);
                }

                // If there are neighboring colors, pick one of them with a high probability
                // Otherwise, pick a random color
                if (!neighborColors.isEmpty() && random.nextDouble() < 0.4) { // 40% chance to match a neighbor
                    chosenColor = neighborColors.get(random.nextInt(neighborColors.size()));
                } else {
                    int colorCount = Math.min(5 + level / 2, BUBBLE_COLORS.length);
                    chosenColor = BUBBLE_COLORS[random.nextInt(colorCount)];
                }
                // --- MODIFICATION END ---

                Bubble newBubble = new Bubble(x, y, chosenColor, row, col);
                bubbles.add(newBubble);
                grid[row][col] = newBubble; // Store in temporary grid
                bubbleCount++;
            }
        }
        initialBubbleCount = bubbleCount;
        updateProgressBar();
    }

    private void createShooterBubble() {
        if (nextBubble != null) {
            shooterBubble = new Bubble(shooterX, shooterY, nextBubble.color);
        } else {
            int colorCount = Math.min(5 + level / 2, BUBBLE_COLORS.length);
            Color color = BUBBLE_COLORS[random.nextInt(colorCount)];
            shooterBubble = new Bubble(shooterX, shooterY, color);
        }
        createNextBubble();
    }

    private void createNextBubble() {
        int colorCount = Math.min(5 + level / 2, BUBBLE_COLORS.length);
        Color color = BUBBLE_COLORS[random.nextInt(colorCount)];
        nextBubble = new Bubble(shooterX + 80, shooterY, color);
        nextBubble.scale = 0.7;
    }

    // NEW: Swap functionality
    @FXML
    private void handleSwap() {
        if (!gameRunning || gamePaused || gameOver || !canSwap ||
                shooterBubble.moving || swapsRemaining <= 0) {
            return;
        }

        // Swap the colors of shooter and next bubbles
        Color tempColor = shooterBubble.color;
        shooterBubble.color = nextBubble.color;
        nextBubble.color = tempColor;

        // Decrease swap count
        swapsRemaining--;

        // If no more swaps, disable until next level
        if (swapsRemaining <= 0) {
            canSwap = false;
        }

        updateSwapButton();
        draw();
    }

    // NEW: Update swap button state and text
    private void updateSwapButton() {
        if (swapButton != null) {
            swapButton.setText("SWAP (" + swapsRemaining + ")");
            swapButton.setDisable(!canSwap || swapsRemaining <= 0 || !gameRunning || gamePaused || gameOver);
        }
    }

    private void handleMouseMove(MouseEvent e) {
        if (!gameRunning || gamePaused) return;
        mouseX = e.getX();
        mouseY = e.getY();
        draw();
    }

    private void handleMouseClick(MouseEvent e) {
        if (!gameRunning || gamePaused || gameOver || shooterBubble.moving) return;
        shoot();
    }

    private void shoot() {
        if (bubblesLeft <= 0) {
            gameOver();
            return;
        }

        double dx = mouseX - shooterBubble.x;
        double dy = mouseY - shooterBubble.y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0 && dy < -60) { // Only shoot upward
            shooterBubble.setVelocity((dx / distance) * SHOT_SPEED, (dy / distance) * SHOT_SPEED);
            bubblesLeft--;
            updateLabels();
        }
    }

    @FXML
    private void handleStart() {
        if (gameOver) {
            initGame();
        }

        gameRunning = true;
        gamePaused = false;
        startButton.setText("PLAYING");
        startButton.setDisable(true);
        pauseButton.setDisable(false);
        updateSwapButton(); // NEW: Enable swap button

        if (gameLoop != null) gameLoop.stop();

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                draw();
            }
        };
        gameLoop.start();
    }

    @FXML
    private void handlePause() {
        if (!gameRunning) return;

        gamePaused = !gamePaused;
        if (gamePaused) {
            pauseButton.setText("RESUME");
            startButton.setDisable(false);
            startButton.setText("RESUME");
        } else {
            pauseButton.setText("PAUSE");
            startButton.setDisable(true);
            startButton.setText("PLAYING");
        }
        updateSwapButton(); // NEW: Update swap button state
    }

    @FXML
    private void handleRestart() {
        if (gameLoop != null) gameLoop.stop();
        initGame();
        startButton.setText("START GAME");
        startButton.setDisable(false);
        pauseButton.setText("PAUSE");
        pauseButton.setDisable(true);
        gameRunning = false;
        gamePaused = false;
        updateSwapButton(); // NEW: Update swap button
    }

    private void update() {
        if (!gameRunning || gamePaused || gameOver) return;

        // Update shooter bubble
        if (shooterBubble.moving) {
            shooterBubble.update();

            // Wall collision
            if (shooterBubble.x <= BUBBLE_RADIUS || shooterBubble.x >= gameCanvas.getWidth() - BUBBLE_RADIUS) {
                shooterBubble.vx = -shooterBubble.vx;
            }

            // Ceiling collision
            if (shooterBubble.y <= BUBBLE_RADIUS + 40) {
                attachBubbleToGrid(shooterBubble);
                createShooterBubble();
            }

            // Collision with other bubbles
            for (Bubble bubble : bubbles) {
                if (shooterBubble.collidesWith(bubble)) {
                    attachBubbleToGrid(shooterBubble);
                    createShooterBubble();
                    break;
                }
            }
        }

        // Update falling bubbles
        fallingBubbles.removeIf(bubble -> {
            bubble.update();
            return bubble.y > gameCanvas.getHeight();
        });

        // Update particles
        particles.removeIf(particle -> {
            particle.update();
            return particle.life <= 0;
        });

        // Update popping bubbles
        bubbles.removeIf(bubble -> bubble.popping && bubble.scale <= 0);

        // Check win condition
        if (bubbles.isEmpty()) {
            levelComplete();
        }

        // Check lose condition
        if (bubblesLeft <= 0 && !shooterBubble.moving) {
            gameOver();
        }

        // Check if bubbles reached danger zone
        for (Bubble bubble : bubbles) {
            if (bubble.y > 420) {
                gameOver();
                break;
            }
        }
    }

    private void attachBubbleToGrid(Bubble bubble) {
        double minDistance = Double.MAX_VALUE;
        int bestRow = 0, bestCol = 0;

        for (int row = 0; row < ROWS; row++) {
            int colsInRow = (row % 2 == 0) ? 24 : 23;
            for (int col = 0; col < colsInRow; col++) {
                double x = col * BUBBLE_DIAMETER + BUBBLE_RADIUS + 16;
                if (row % 2 == 1) {
                    x += BUBBLE_RADIUS;
                }
                double y = row * ROW_HEIGHT + BUBBLE_RADIUS + 40;

                if (isPositionEmpty(x, y)) {
                    double distance = Math.sqrt(Math.pow(bubble.x - x, 2) + Math.pow(bubble.y - y, 2));
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestRow = row;
                        bestCol = col;
                    }
                }
            }
        }

        // Position the bubble
        double x = bestCol * BUBBLE_DIAMETER + BUBBLE_RADIUS + 16;
        if (bestRow % 2 == 1) {
            x += BUBBLE_RADIUS;
        }
        double y = bestRow * ROW_HEIGHT + BUBBLE_RADIUS + 40;

        Bubble newBubble = new Bubble(x, y, bubble.color, bestRow, bestCol);
        bubbles.add(newBubble);
        bubble.moving = false;

        // Check for matches
        checkMatches(newBubble);
        checkFloatingBubbles();
    }

    private boolean isPositionEmpty(double x, double y) {
        for (Bubble existing : bubbles) {
            if (Math.abs(existing.x - x) < BUBBLE_RADIUS && Math.abs(existing.y - y) < BUBBLE_RADIUS) {
                return false;
            }
        }
        return true;
    }

    private void checkMatches(Bubble bubble) {
        List<Bubble> matches = new ArrayList<>();
        findMatches(bubble, matches);

        if (matches.size() >= 3) {
            // Create explosion effect
            for (Bubble match : matches) {
                createExplosion(match.x, match.y, match.color);
                match.popping = true;
            }

            bubbles.removeAll(matches);
            totalBubblesCleared += matches.size();

            // Update score with combo bonus
            int baseScore = matches.size() * 10;
            int comboBonus = (matches.size() - 3) * 5;
            score += (baseScore + comboBonus) * level;

            updateLabels();
            updateProgressBar();
        }
    }

    private void createExplosion(double x, double y, Color color) {
        for (int i = 0; i < 8; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void findMatches(Bubble bubble, List<Bubble> matches) {
        if (matches.contains(bubble)) return;
        matches.add(bubble);

        for (Bubble other : bubbles) {
            if (other != bubble && !matches.contains(other) &&
                    bubble.color.equals(other.color) &&
                    Math.sqrt(Math.pow(bubble.x - other.x, 2) + Math.pow(bubble.y - other.y, 2)) < BUBBLE_DIAMETER + 5) {
                findMatches(other, matches);
            }
        }
    }

    private void checkFloatingBubbles() {
        bubbles.forEach(b -> b.marked = false);

        // Mark connected bubbles from top
        bubbles.stream()
                .filter(b -> b.y < 80)
                .forEach(this::markConnected);

        // Remove floating bubbles
        Iterator<Bubble> iterator = bubbles.iterator();
        while (iterator.hasNext()) {
            Bubble bubble = iterator.next();
            if (!bubble.marked) {
                iterator.remove();
                bubble.setVelocity((random.nextDouble() - 0.5) * 6, random.nextDouble() * 3 + 1);
                fallingBubbles.add(bubble);
                score += 5 * level;
                totalBubblesCleared++;
                createExplosion(bubble.x, bubble.y, bubble.color);
            }
        }
        updateLabels();
        updateProgressBar();
    }

    private void markConnected(Bubble bubble) {
        if (bubble.marked) return;
        bubble.marked = true;

        bubbles.stream()
                .filter(other -> other != bubble && !other.marked)
                .filter(other -> Math.sqrt(Math.pow(bubble.x - other.x, 2) + Math.pow(bubble.y - other.y, 2)) < BUBBLE_DIAMETER + 5)
                .forEach(this::markConnected);
    }

    private void levelComplete() {
        level++;
        bubblesLeft += 50;
        totalBubblesCleared = 0;
        swapsRemaining = MAX_SWAPS_PER_LEVEL; // Reset swaps for new level
        canSwap = true;
        
        // Award coins for level completion
        int levelCoins = level * 50;
        coins += levelCoins;
        
        // Update user coins in database if authenticated
        if (isAuthenticated) {
            updateUserCoins(levelCoins);
        }
        
        updateLabels();
        updateSwapButton();
        createInitialBubbles();
        showMessage("Level " + level + " Complete!\n+" + levelCoins + " coins");
    }

    private void gameOver() {
        gameRunning = false;
        gameOver = true;
        
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        // Calculate final score and earned coins
        int finalCoins = score / 50;
        
        // Update user data if authenticated
        if (isAuthenticated) {
            // Record game history
            updateGameHistory("BubbleShooter", score > 1000 ? "win" : "loss", score);
            
            // Add coins to user's account
            if (finalCoins > 0) {
                updateUserCoins(finalCoins);
            }
        }
        
        showGameOverScreen();
    }
    
    private void updateGameHistory(String gameType, String result, int score) {
        if (!isAuthenticated || username == null || username.isEmpty()) {
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Insert game history record
            String insertQuery = "INSERT INTO game_history (username, game_type, result, score, date, duration) " +
                                "VALUES (?, ?, ?, ?, ?, ?)";
            
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, gameType);
            insertStmt.setString(3, result);
            insertStmt.setInt(4, score);
            insertStmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            insertStmt.setString(6, "N/A"); // No specific duration tracking in this game
            insertStmt.executeUpdate();
            
            // Update user statistics
            String updateQuery = "UPDATE users SET ";
            
            if (result.equals("win")) {
                updateQuery += "wins = wins + 1, ";
            } else if (result.equals("loss")) {
                updateQuery += "losses = losses + 1, ";
            }
            
            updateQuery += "total_games = total_games + 1, total_score = total_score + ? WHERE username = ?";
            
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setInt(1, score);
            updateStmt.setString(2, username);
            updateStmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to update game history: " + e.getMessage());
        }
    }
    
    private void updateUserCoins(int coinsToAdd) {
        if (!isAuthenticated || username == null || username.isEmpty()) {
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Update user's coins
            String updateQuery = "UPDATE users SET coins = coins + ? WHERE username = ?";
            
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setInt(1, coinsToAdd);
            updateStmt.setString(2, username);
            updateStmt.executeUpdate();
            
            // Update the game menu controller if available
            if (gameMenuController != null) {
                gameMenuController.addCoins(coinsToAdd);
            }
            
            // Display message to player
            System.out.println("Added " + coinsToAdd + " coins to " + username);
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to update user coins: " + e.getMessage());
        }
    }

    private void showMessage(String message) {
        gc.setFill(Color.web("#000000", 0.7));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        gc.fillText(message, 
                (gameCanvas.getWidth() - gc.getFont().getSize() * message.length() * 0.5) / 2, 
                gameCanvas.getHeight() / 2);
    }

    private void showGameOverScreen() {
        gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.9));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        gc.fillText("GAME OVER", gameCanvas.getWidth()/2 - 100, gameCanvas.getHeight()/2 - 60);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        gc.fillText("Final Score: " + score, gameCanvas.getWidth()/2 - 80, gameCanvas.getHeight()/2 - 20);
        gc.fillText("Level Reached: " + level, gameCanvas.getWidth()/2 - 90, gameCanvas.getHeight()/2 + 10);
        gc.fillText("Coins Earned: " + Math.max(1, score / 100), gameCanvas.getWidth()/2 - 95, gameCanvas.getHeight()/2 + 40);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.fillText("Click 'NEW GAME' to play again!", gameCanvas.getWidth()/2 - 120, gameCanvas.getHeight()/2 + 80);
    }

    private void updateLabels() {
        scoreLabel.setText(String.valueOf(score));
        levelLabel.setText(String.valueOf(level));
        bubblesLeftLabel.setText(String.valueOf(bubblesLeft));
        coinsLabel.setText(String.valueOf(coins));
    }

    private void updateProgressBar() {
        if (initialBubbleCount > 0) {
            double progress = (double) totalBubblesCleared / initialBubbleCount;
            levelProgressBar.setProgress(Math.min(1.0, progress));
        }
    }

    private void draw() {
        // Clear with gradient background
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw animated background
        drawAnimatedBackground();

        // Draw danger zone indicator
        if (bubbles.stream().anyMatch(b -> b.y > 380)) {
            gc.setFill(Color.RED.deriveColor(0, 1, 1, 0.2));
            gc.fillRect(0, 400, gameCanvas.getWidth(), 50);
            gc.setFill(Color.RED);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            gc.fillText("DANGER ZONE!", 20, 425);
        }

        // Draw game elements
        bubbles.forEach(bubble -> bubble.draw(gc));
        fallingBubbles.forEach(bubble -> bubble.draw(gc));
        particles.forEach(particle -> particle.draw(gc));

        if (shooterBubble != null) {
            shooterBubble.draw(gc);
        }

        if (nextBubble != null) {
            nextBubble.draw(gc);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            gc.fillText("NEXT", nextBubble.x - 12, nextBubble.y + 30);
        }

        // NEW: Draw swap instructions
        if (gameRunning && !gamePaused && !gameOver && swapsRemaining > 0) {
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.7));
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            gc.fillText("Press SPACE or SWAP button to swap bubbles", 20, gameCanvas.getHeight() - 60);
            gc.fillText("Swaps remaining: " + swapsRemaining, 20, gameCanvas.getHeight() - 40);
        }

        // Draw aiming line
        if (!gameOver && !gamePaused && shooterBubble != null && !shooterBubble.moving && mouseY < shooterY - 50) {
            gc.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.6));
            gc.setLineWidth(3);
            gc.strokeLine(shooterBubble.x, shooterBubble.y, mouseX, mouseY);

            // Draw trajectory dots
            double dx = mouseX - shooterBubble.x;
            double dy = mouseY - shooterBubble.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance > 0) {
                double stepX = (dx / distance) * 30;
                double stepY = (dy / distance) * 30;
                gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.4));
                for (int i = 1; i <= 8; i++) {
                    double dotX = shooterBubble.x + stepX * i;
                    double dotY = shooterBubble.y + stepY * i;
                    if (dotY > 40) {
                        gc.fillOval(dotX - 2, dotY - 2, 4, 4);
                    }
                }
            }

            // *** STYLIZED BORDER CODE ***
            // Outer glow
            gc.setStroke(Color.web("#00ff41").deriveColor(0, 1, 1, 0.2));
            gc.setLineWidth(12);
            gc.strokeRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

            // Main border
            gc.setStroke(Color.web("#00ff41"));
            gc.setLineWidth(4);
            gc.strokeRect(2, 2, gameCanvas.getWidth() - 4, gameCanvas.getHeight() - 4);

            // Inner highlight
            gc.setStroke(Color.web("#00d4aa"));
            gc.setLineWidth(2);
            gc.strokeRect(4, 4, gameCanvas.getWidth() - 8, gameCanvas.getHeight() - 8);

            // Corner accents
            gc.setFill(Color.web("#00ff41"));
            double cornerSize = 20;
            // Top-left corner
            gc.fillRect(0, 0, cornerSize, 4);
            gc.fillRect(0, 0, 4, cornerSize);
            // Top-right corner
            gc.fillRect(gameCanvas.getWidth() - cornerSize, 0, cornerSize, 4);
            gc.fillRect(gameCanvas.getWidth() - 4, 0, 4, cornerSize);
            // Bottom-left corner
            gc.fillRect(0, gameCanvas.getHeight() - 4, cornerSize, 4);
            gc.fillRect(0, gameCanvas.getHeight() - cornerSize, 4, cornerSize);
            // Bottom-right corner
            gc.fillRect(gameCanvas.getWidth() - cornerSize, gameCanvas.getHeight() - 4, cornerSize, 4);
            gc.fillRect(gameCanvas.getWidth() - 4, gameCanvas.getHeight() - cornerSize, 4, cornerSize);
            // *** END STYLIZED BORDER CODE ***
        }

        // Draw shooter platform
        drawShooterPlatform();

        // Draw pause overlay
        if (gamePaused) {
            gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.7));
            gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            gc.fillText("PAUSED", gameCanvas.getWidth()/2 - 80, gameCanvas.getHeight()/2);
        }
    }

    private void drawAnimatedBackground() {
        // Animated grid pattern
        gc.setStroke(Color.web("#16213e").deriveColor(0, 1, 1, 0.3));
        gc.setLineWidth(1);
        long time = System.currentTimeMillis() / 50;

        for (int i = 0; i < gameCanvas.getWidth(); i += 40) {
            double offset = Math.sin(time * 0.01 + i * 0.01) * 2;
            gc.strokeLine(i, 0, i, gameCanvas.getHeight());
        }
        for (int i = 0; i < gameCanvas.getHeight(); i += 30) {
            double offset = Math.cos(time * 0.01 + i * 0.01) * 2;
            gc.strokeLine(0, i + offset, gameCanvas.getWidth(), i + offset);
        }

        // Floating particles in background
        gc.setFill(Color.web("#0f3460").deriveColor(0, 1, 1, 0.2));
        for (int i = 0; i < 20; i++) {
            double x = (Math.sin(time * 0.001 + i) * 100 + gameCanvas.getWidth() / 2 + i * 40) % gameCanvas.getWidth();
            double y = (Math.cos(time * 0.002 + i) * 50 + gameCanvas.getHeight() / 2 + i * 30) % gameCanvas.getHeight();
            gc.fillOval(x, y, 3, 3);
        }
    }

    private void drawShooterPlatform() {
        // Platform base
        gc.setFill(Color.web("#2d3748"));
        gc.fillRoundRect(shooterX - 50, shooterY + 20, 100, 25, 10, 10);

        // Platform highlight
        gc.setFill(Color.web("#4a5568"));
        gc.fillRoundRect(shooterX - 45, shooterY + 22, 90, 6, 5, 5);

        // Cannon barrel
        if (shooterBubble != null && !shooterBubble.moving) {
            double angle = Math.atan2(mouseY - shooterY, mouseX - shooterX);
            if (mouseY < shooterY - 20) { // Only show when aiming upward
                gc.save();
                gc.translate(shooterX, shooterY);
                gc.rotate(Math.toDegrees(angle));

                gc.setFill(Color.web("#718096"));
                gc.fillRoundRect(-5, -8, 40, 16, 8, 8);
                gc.setFill(Color.web("#a0aec0"));
                gc.fillRoundRect(-3, -6, 35, 12, 6, 6);

                gc.restore();
            }
        }

        // Platform border
        gc.setStroke(Color.web("#e2e8f0"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(shooterX - 50, shooterY + 20, 100, 25, 10, 10);
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
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}