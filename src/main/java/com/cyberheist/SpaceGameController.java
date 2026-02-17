package com.cyberheist;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.HashSet;
import java.util.Set;

public class SpaceGameController implements Initializable {

    @FXML
    private Canvas gameCanvas;

    @FXML
    private Label scoreLabel;

    @FXML
    private Label livesLabel;

    @FXML
    private Label gameOverLabel;

    @FXML
    private Label levelLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label fpsLabel;

    @FXML
    private Label enemiesLabel;

    @FXML
    private Label difficultyLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button backButton;

    @FXML
    private Button menuButton;

    @FXML
    private Button pauseButton;

    @FXML
    private Button quickPauseButton;

    @FXML
    private Button resumeMenuButton;

    @FXML
    private VBox pauseOverlay;

    @FXML
    private VBox gameOverOverlay;

    @FXML
    private VBox menuOverlay;

    @FXML
    private VBox loadingOverlay;

    private GraphicsContext gc;
    private AnimationTimer gameLoop;
    private Timeline enemySpawnTimer;
    private Timeline asteroidSpawnTimer;
    private Random random = new Random();

    // Game state
    private boolean gameRunning = false;
    private boolean gamePaused = false;
    private int score = 0;
    private int lives = 3;
    private int level = 1;
    private long gameStartTime;
    private long pausedDuration = 0;
    private long pauseStartTime = 0;
    private int enemyCount = 0;
    private String currentStatus = "Ready";
    
    // User data
    private String username;
    private boolean isAuthenticated = false;
    
    // Reference to GameMenuController
    private GameMenuController gameMenuController;

    // Performance tracking
    private long lastFpsUpdateTime = 0;
    private int frameCounter = 0;
    private int currentFps = 0;
    private static final long FPS_UPDATE_INTERVAL_NS = 1_000_000_000L;

    // Player
    private Player player;

    // Game objects
    private List<Bullet> bullets;
    private List<Enemy> enemies;
    private List<Star> stars;
    private List<Planet> planets;
    private List<Asteroid> asteroids;
    private List<Particle> particles;

    // Fixed input handling with Set for multiple key presses
    private Set<KeyCode> pressedKeys = new HashSet<>();

    // Animation frame counter for effects
    private long frameCount = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing SpaceGameController...");

        gc = gameCanvas.getGraphicsContext2D();

        // Initialize game objects
        player = new Player(400, 500);
        bullets = new ArrayList<>();
        enemies = new ArrayList<>();
        stars = new ArrayList<>();
        planets = new ArrayList<>();
        asteroids = new ArrayList<>();
        particles = new ArrayList<>();

        // Create background stars with different sizes and colors
        for (int i = 0; i < 150; i++) {
            stars.add(new Star(random.nextDouble() * 800, random.nextDouble() * 600));
        }

        // Create distant planets
        for (int i = 0; i < 3; i++) {
            planets.add(new Planet(random.nextDouble() * 800, random.nextDouble() * 300));
        }

        // Make canvas focusable for key events and request focus
        gameCanvas.setFocusTraversable(true);

        // Use Platform.runLater to ensure the scene is fully loaded before requesting focus
        Platform.runLater(() -> {
            gameCanvas.requestFocus();
            System.out.println("Focus requested for canvas");
            
            // Check if user is authenticated
            checkAuthentication();
        });

        // Show loading overlay initially
        showOverlay(loadingOverlay);
        statusLabel.setText("Status: Initializing...");

        // Simulate a loading time before starting the game
        Timeline loadingTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            startGame();
            hideAllOverlays();
            statusLabel.setText("Status: Fighting");
        }));
        loadingTimeline.play();
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

    @FXML
    private void handleBackButton(ActionEvent event) {
        System.out.println("Back button clicked");

        // Stop all game activities before changing scene
        stopGameActivities();


        SceneManager.getInstance().switchToScene("MiniGames.fxml");
    }

    @FXML
    private void handleMeenuButton(){
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    // Optional: Helper method to show error alerts
    private void showErrorAlert(String title, String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Could not show error alert: " + e.getMessage());
        }
    }

    private void startGame() {
        System.out.println("Starting game...");
        gameRunning = true;
        gamePaused = false;
        score = 0;
        lives = 5;
        level = 1;
        enemyCount = 0;
        gameStartTime = System.currentTimeMillis();
        pausedDuration = 0;
        pauseStartTime = 0;
        hideAllOverlays();
        updateUI();

        // Ensure player is reset to initial position
        player.x = 400;
        player.y = 500;
        player.lastShot = 0;

        // Clear all existing game objects
        bullets.clear();
        enemies.clear();
        asteroids.clear();
        particles.clear();
        pressedKeys.clear(); // Clear pressed keys

        // Enemy spawn timer
        if (enemySpawnTimer != null) enemySpawnTimer.stop();
        enemySpawnTimer = new Timeline(new KeyFrame(Duration.seconds(2.0), e -> spawnEnemy()));
        enemySpawnTimer.setCycleCount(Timeline.INDEFINITE);
        enemySpawnTimer.play();

        // Asteroid spawn timer
        if (asteroidSpawnTimer != null) asteroidSpawnTimer.stop();
        asteroidSpawnTimer = new Timeline(new KeyFrame(Duration.seconds(3.0), e -> spawnAsteroid()));
        asteroidSpawnTimer.setCycleCount(Timeline.INDEFINITE);
        asteroidSpawnTimer.play();

        // Main game loop
        if (gameLoop != null) gameLoop.stop();
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gameRunning && !gamePaused) {
                    frameCount++;
                    frameCounter++;
                    updateFPS(now);
                    update();
                    render();
                }
            }
        };
        gameLoop.start();

        // Request focus again after starting the game
        Platform.runLater(() -> gameCanvas.requestFocus());
    }

    private void stopGameActivities() {
        System.out.println("Stopping game activities...");
        if (gameLoop != null) {
            gameLoop.stop();
        }
        if (enemySpawnTimer != null) {
            enemySpawnTimer.stop();
        }
        if (asteroidSpawnTimer != null) {
            asteroidSpawnTimer.stop();
        }
        gameRunning = false;
    }

    private void updateFPS(long now) {
        if (now - lastFpsUpdateTime >= FPS_UPDATE_INTERVAL_NS) {
            currentFps = frameCounter;
            fpsLabel.setText("FPS: " + currentFps);
            frameCounter = 0;
            lastFpsUpdateTime = now;
        }
    }

    private void hideAllOverlays() {
        if (pauseOverlay != null) pauseOverlay.setVisible(false);
        if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
        if (menuOverlay != null) menuOverlay.setVisible(false);
        if (loadingOverlay != null) loadingOverlay.setVisible(false);
    }

    private void showOverlay(VBox overlay) {
        hideAllOverlays();
        if (overlay != null) {
            overlay.setVisible(true);
        }
    }

    private void update() {
        // Update player using Set-based key tracking
        boolean leftPressed = pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A);
        boolean rightPressed = pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D);
        boolean upPressed = pressedKeys.contains(KeyCode.UP) || pressedKeys.contains(KeyCode.W);
        boolean downPressed = pressedKeys.contains(KeyCode.DOWN) || pressedKeys.contains(KeyCode.S);
        boolean spacePressed = pressedKeys.contains(KeyCode.SPACE);

        if (leftPressed && player.x > 0) {
            player.x -= player.speed;
        }
        if (rightPressed && player.x < gameCanvas.getWidth() - player.width) {
            player.x += player.speed;
        }
        if (upPressed && player.y > 0) {
            player.y -= player.speed;
        }
        if (downPressed && player.y < gameCanvas.getHeight() - player.height) {
            player.y += player.speed;
        }

        // Player shooting
        if (spacePressed && System.currentTimeMillis() - player.lastShot > 150) {
            // Create twin laser beams
            bullets.add(new Bullet(player.x + 8, player.y, -8, BulletType.PLAYER_LASER));
            bullets.add(new Bullet(player.x + player.width - 8, player.y, -8, BulletType.PLAYER_LASER));
            player.lastShot = System.currentTimeMillis();

            // Add muzzle flash particles
            for (int i = 0; i < 3; i++) {
                particles.add(new Particle(player.x + player.width/2, player.y, ParticleType.MUZZLE_FLASH));
            }
        }

        // Update bullets
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.y += bullet.speed;
            bullet.update();
            if (bullet.y < -20 || bullet.y > gameCanvas.getHeight() + 20) {
                bulletIterator.remove();
            }
        }

        // Update enemies
        Iterator<Enemy> enemyIterator = enemies.iterator();
        enemyCount = 0;
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            enemy.y += enemy.speed;
            enemy.update();
            enemyCount++;

            // Enemy shooting
            if (random.nextDouble() < 0.005 * (1 + (level - 1) * 0.1)) {
                bullets.add(new Bullet(enemy.x + enemy.width/2, enemy.y + enemy.height, 4, BulletType.ENEMY_PLASMA));
            }

            if (enemy.y > gameCanvas.getHeight() + 20) {
                enemyIterator.remove();
                lives--;
                if (lives <= 0) {
                    gameOver();
                }
            }
        }

        // Update asteroids
        Iterator<Asteroid> asteroidIterator = asteroids.iterator();
        while (asteroidIterator.hasNext()) {
            Asteroid asteroid = asteroidIterator.next();
            asteroid.y += asteroid.speed;
            asteroid.rotation += asteroid.rotationSpeed;
            if (asteroid.y > gameCanvas.getHeight() + 20) {
                asteroidIterator.remove();
            }
        }

        // Update stars
        for (Star star : stars) {
            star.y += star.speed;
            star.twinkle += 0.1;
            if (star.y > gameCanvas.getHeight()) {
                star.y = -5;
                star.x = random.nextDouble() * gameCanvas.getWidth();
            }
        }

        // Update planets
        for (Planet planet : planets) {
            planet.y += 0.1;
            if (planet.y > gameCanvas.getHeight() + 50) {
                planet.y = -100;
                planet.x = random.nextDouble() * gameCanvas.getWidth();
            }
        }

        // Update particles
        Iterator<Particle> particleIterator = particles.iterator();
        while (particleIterator.hasNext()) {
            Particle particle = particleIterator.next();
            particle.update();
            if (particle.isDead()) {
                particleIterator.remove();
            }
        }

        // Collision detection
        checkCollisions();

        // Level progression
        if (score >= level * 100 && enemies.isEmpty() && asteroids.isEmpty()) {
            level++;
            currentStatus = "Level " + level + " incoming!";

            if (enemySpawnTimer != null) {
                enemySpawnTimer.setRate(enemySpawnTimer.getRate() * 1.1);
            }
            player.speed += 0.5;
            if (player.speed > 10) player.speed = 10;

            showOverlay(loadingOverlay);
            Timeline levelTransition = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                hideAllOverlays();
                currentStatus = "Fighting";
            }));
            levelTransition.play();
        }

        updateUI();
    }

    private void checkCollisions() {
        // Bullet-Enemy collisions
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            if (bullet.type == BulletType.PLAYER_LASER) {
                Iterator<Enemy> enemyIterator = enemies.iterator();
                while (enemyIterator.hasNext()) {
                    Enemy enemy = enemyIterator.next();
                    if (isColliding(bullet.x, bullet.y, bullet.width, bullet.height,
                            enemy.x, enemy.y, enemy.width, enemy.height)) {
                        bulletIterator.remove();
                        enemyIterator.remove();
                        score += 10;

                        // Create explosion particles
                        for (int i = 0; i < 10; i++) {
                            particles.add(new Particle(enemy.x + enemy.width/2, enemy.y + enemy.height/2, ParticleType.EXPLOSION));
                        }
                        break;
                    }
                }
            }
        }

        // Bullet-Asteroid collisions
        bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            if (bullet.type == BulletType.PLAYER_LASER) {
                Iterator<Asteroid> asteroidIterator = asteroids.iterator();
                while (asteroidIterator.hasNext()) {
                    Asteroid asteroid = asteroidIterator.next();
                    if (isColliding(bullet.x, bullet.y, bullet.width, bullet.height,
                            asteroid.x, asteroid.y, asteroid.size, asteroid.size)) {
                        bulletIterator.remove();
                        asteroidIterator.remove();
                        score += 15;

                        // Create debris particles
                        for (int i = 0; i < 8; i++) {
                            particles.add(new Particle(asteroid.x + asteroid.size/2, asteroid.y + asteroid.size/2, ParticleType.DEBRIS));
                        }
                        break;
                    }
                }
            }
        }

        // Player-Enemy collisions
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (isColliding(player.x, player.y, player.width, player.height,
                    enemy.x, enemy.y, enemy.width, enemy.height)) {
                enemyIterator.remove();
                lives--;

                // Create damage particles
                for (int i = 0; i < 6; i++) {
                    particles.add(new Particle(player.x + player.width/2, player.y + player.height/2, ParticleType.DAMAGE));
                }

                if (lives <= 0) {
                    gameOver();
                }
                break;
            }
        }

        // Player-Enemy bullet collisions
        bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            if (bullet.type == BulletType.ENEMY_PLASMA) {
                if (isColliding(bullet.x, bullet.y, bullet.width, bullet.height,
                        player.x, player.y, player.width, player.height)) {
                    bulletIterator.remove();
                    lives--;

                    // Create damage particles
                    for (int i = 0; i < 6; i++) {
                        particles.add(new Particle(player.x + player.width/2, player.y + player.height/2, ParticleType.DAMAGE));
                    }

                    if (lives <= 0) {
                        gameOver();
                    }
                    break;
                }
            }
        }
    }

    private boolean isColliding(double x1, double y1, double w1, double h1,
                                double x2, double y2, double w2, double h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    private void render() {
        // Clear canvas with deep space gradient
        RadialGradient spaceGradient = new RadialGradient(0, 0, gameCanvas.getWidth()/2, gameCanvas.getHeight()/2, 500, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#000611")),
                new Stop(1, Color.web("#000000"))
        );
        gc.setFill(spaceGradient);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw distant planets
        for (Planet planet : planets) {
            drawPlanet(planet);
        }

        // Draw stars with twinkling effect
        for (Star star : stars) {
            drawStar(star);
        }

        // Draw asteroids
        for (Asteroid asteroid : asteroids) {
            drawAsteroid(asteroid);
        }

        // Draw player spaceship
        drawPlayer(player);

        // Draw bullets
        for (Bullet bullet : bullets) {
            drawBullet(bullet);
        }

        // Draw enemies
        for (Enemy enemy : enemies) {
            drawEnemy(enemy);
        }

        // Draw particles
        for (Particle particle : particles) {
            drawParticle(particle);
        }
    }

    private void drawPlayer(Player player) {
        gc.save();
        gc.translate(player.x + player.width/2, player.y + player.height/2);

        // Engine glow
        RadialGradient engineGlow = new RadialGradient(0, 0, 0, 15, 20, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00ccff")),
                new Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(engineGlow);
        gc.fillOval(-10, 10, 20, 15);

        // Main body
        gc.setFill(Color.web("#c0c0c0"));
        gc.fillPolygon(new double[]{0, -8, -5, -12, -12, -5, 5, 12, 12, 5, 8},
                new double[]{-15, -5, 0, 5, 10, 12, 12, 10, 5, 0, -5}, 11);

        // Cockpit
        RadialGradient cockpit = new RadialGradient(0, 0, 0, -5, 8, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4080ff")),
                new Stop(1, Color.web("#001133"))
        );
        gc.setFill(cockpit);
        gc.fillOval(-4, -10, 8, 8);

        // Wing details
        gc.setFill(Color.web("#808080"));
        gc.fillRect(-12, 3, 4, 2);
        gc.fillRect(8, 3, 4, 2);

        gc.restore();
    }

    private void drawEnemy(Enemy enemy) {
        gc.save();
        gc.translate(enemy.x + enemy.width/2, enemy.y + enemy.height/2);

        // Alien ship design
        gc.setFill(Color.web("#ff4500"));
        gc.fillOval(-15, -8, 30, 16);

        // Dark center
        gc.setFill(Color.web("#660000"));
        gc.fillOval(-10, -5, 20, 10);

        // Glowing spots
        double glowIntensity = Math.sin(enemy.animationTime * 0.1) * 0.3 + 0.7;
        gc.setFill(Color.web("#ffaa00").deriveColor(0, 1, glowIntensity, 1));
        gc.fillOval(-8, -3, 6, 6);
        gc.fillOval(2, -3, 6, 6);

        // Wing spikes
        gc.setFill(Color.web("#cc3300"));
        gc.fillPolygon(new double[]{-15, -20, -15}, new double[]{-4, 0, 4}, 3);
        gc.fillPolygon(new double[]{15, 20, 15}, new double[]{-4, 0, 4}, 3);

        gc.restore();
    }

    private void drawBullet(Bullet bullet) {
        gc.save();

        if (bullet.type == BulletType.PLAYER_LASER) {
            // Blue laser beam
            RadialGradient laser = new RadialGradient(0, 0, 0, 0, 3, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#00ffff")),
                    new Stop(1, Color.web("#0080ff"))
            );
            gc.setFill(laser);
            gc.fillRect(bullet.x - 1, bullet.y, 3, bullet.height + 5);

            // Core beam
            gc.setFill(Color.WHITE);
            gc.fillRect(bullet.x, bullet.y, 1, bullet.height + 5);
        } else {
            // Enemy plasma
            RadialGradient plasma = new RadialGradient(0, 0, bullet.x + bullet.width/2, bullet.y + bullet.height/2,
                    bullet.width, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#ff6600")),
                    new Stop(1, Color.web("#ff0000"))
            );
            gc.setFill(plasma);
            gc.fillOval(bullet.x - 2, bullet.y, bullet.width + 4, bullet.height + 2);
        }

        gc.restore();
    }

    private void drawStar(Star star) {
        double brightness = Math.sin(star.twinkle) * 0.3 + 0.7;
        Color starColor = star.color.deriveColor(0, 1, brightness, 1);

        gc.setFill(starColor);
        if (star.type == StarType.LARGE) {
            // Draw cross pattern for large stars
            gc.fillRect(star.x - 1, star.y - star.size/2, 2, star.size);
            gc.fillRect(star.x - star.size/2, star.y - 1, star.size, 2);
            gc.fillOval(star.x - 1, star.y - 1, 2, 2);
        } else {
            gc.fillOval(star.x, star.y, star.size, star.size);
        }
    }

    private void drawPlanet(Planet planet) {
        gc.save();

        RadialGradient planetGradient = new RadialGradient(0, 0, planet.x + planet.size/2, planet.y + planet.size/2,
                planet.size/2, false, CycleMethod.NO_CYCLE,
                new Stop(0, planet.color.deriveColor(0, 1, 1.2, 0.8)),
                new Stop(1, planet.color.deriveColor(0, 1, 0.3, 0.8))
        );

        gc.setFill(planetGradient);
        gc.fillOval(planet.x, planet.y, planet.size, planet.size);

        gc.restore();
    }

    private void drawAsteroid(Asteroid asteroid) {
        gc.save();
        gc.translate(asteroid.x + asteroid.size/2, asteroid.y + asteroid.size/2);
        gc.rotate(asteroid.rotation);

        // Irregular asteroid shape
        gc.setFill(Color.web("#8B4513"));
        double[] xPoints = {-asteroid.size/2, -asteroid.size/3, 0, asteroid.size/3, asteroid.size/2, asteroid.size/3, 0, -asteroid.size/3};
        double[] yPoints = {-asteroid.size/3, -asteroid.size/2, -asteroid.size/3, -asteroid.size/2, 0, asteroid.size/2, asteroid.size/3, asteroid.size/2};
        gc.fillPolygon(xPoints, yPoints, 8);

        // Highlight edges
        gc.setFill(Color.web("#CD853F"));
        gc.fillPolygon(new double[]{-asteroid.size/2, -asteroid.size/3, 0},
                new double[]{-asteroid.size/3, -asteroid.size/2, -asteroid.size/3}, 3);

        gc.restore();
    }

    private void drawParticle(Particle particle) {
        gc.save();

        Color particleColor = particle.color.deriveColor(0, 1, 1, particle.alpha);
        gc.setFill(particleColor);

        if (particle.type == ParticleType.EXPLOSION) {
            gc.fillOval(particle.x - particle.size/2, particle.y - particle.size/2, particle.size, particle.size);
        } else {
            gc.fillRect(particle.x, particle.y, particle.size, particle.size);
        }

        gc.restore();
    }

    private void spawnEnemy() {
        if (gameRunning && !gamePaused) {
            double x = random.nextDouble() * (gameCanvas.getWidth() - 30);
            enemies.add(new Enemy(x, -30));
        }
    }

    private void spawnAsteroid() {
        if (gameRunning && !gamePaused && random.nextDouble() < 0.7) {
            double x = random.nextDouble() * (gameCanvas.getWidth() - 40);
            asteroids.add(new Asteroid(x, -40));
        }
    }

    private void updateUI() {
        if (scoreLabel != null) scoreLabel.setText(String.valueOf(score));
        if (livesLabel != null) livesLabel.setText(String.valueOf(lives));
        if (levelLabel != null) levelLabel.setText(String.valueOf(level));
        if (enemiesLabel != null) enemiesLabel.setText("Enemies: " + enemyCount);
        if (statusLabel != null) statusLabel.setText("Status: " + currentStatus);
        if (fpsLabel != null) fpsLabel.setText("FPS: " + currentFps);

        // Update time
        if (gameRunning && !gamePaused) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = (currentTime - gameStartTime - pausedDuration) / 1000;
            int minutes = (int) (elapsedTime / 60);
            int seconds = (int) (elapsedTime % 60);
            if (timeLabel != null) timeLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }

        // Update difficulty
        String difficulty = "Easy";
        if (level >= 3) difficulty = "Hard";
        else if (level >= 2) difficulty = "Normal";
        if (difficultyLabel != null) difficultyLabel.setText("Difficulty: " + difficulty);
    }

    private void gameOver() {
        System.out.println("Game Over!");
        gameRunning = false;
        gamePaused = false;
        currentStatus = "Game Over";
        if (gameOverLabel != null) gameOverLabel.setText("Final Score: " + score);
        showOverlay(gameOverOverlay);
        updateUI();
        stopGameActivities();
        
        // If player is authenticated, update the game history and add rewards
        if (isAuthenticated) {
            boolean isWin = score > 1000;
            int earnedCoins = score / 10; // Convert score to coins
            
            updateGameHistory("SpaceGame", isWin ? "win" : "loss", score);
            
            // Add coins to user's account
            if (earnedCoins > 0) {
                updateUserCoins(earnedCoins);
            }
        }
    }
    
    private void updateGameHistory(String gameType, String result, int score) {
        if (!isAuthenticated || username == null || username.isEmpty()) {
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Insert game history record
            String insertQuery = "INSERT INTO game_history (username, game_type, result, score, date, duration) " +
                                "VALUES (?, ?, ?, ?, ?, ?)";
            
            // Calculate game duration in seconds
            long durationMs = System.currentTimeMillis() - gameStartTime - pausedDuration;
            String durationStr = (durationMs / 1000) + " seconds";
            
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, gameType);
            insertStmt.setString(3, result);
            insertStmt.setInt(4, score);
            insertStmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            insertStmt.setString(6, durationStr);
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
            
            // Display message to user
            System.out.println("Added " + coinsToAdd + " coins to " + username);
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to update user coins: " + e.getMessage());
        }
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        System.out.println("Key pressed: " + event.getCode());
        KeyCode code = event.getCode();

        // Add to pressed keys set
        pressedKeys.add(code);

        // Global key actions (always active)
        switch (code) {
            case R:
                if (!gameRunning || (gameOverOverlay != null && gameOverOverlay.isVisible())) {
                    restartGame();
                }
                break;
            case P:
            case ESCAPE:
                if (gameOverOverlay == null || !gameOverOverlay.isVisible()) {
                    if (loadingOverlay == null || !loadingOverlay.isVisible()) {
                        handlePauseButton();
                    }
                }
                break;
            case M:
                if (loadingOverlay == null || !loadingOverlay.isVisible()) {
                    handleMenuButton();
                }
                break;
            default:
                break;
        }
        event.consume(); // Prevent event from bubbling up
    }

    @FXML
    private void handleKeyReleased(KeyEvent event) {
        System.out.println("Key released: " + event.getCode());
        // Remove from pressed keys set
        pressedKeys.remove(event.getCode());
        event.consume();
    }

    @FXML
    private void handlePauseButton() {
        System.out.println("Pause button clicked");
        if (gameRunning) {
            if (gamePaused) {
                resumeGame();
            } else {
                pauseGame();
            }
        }
    }

    @FXML
    private void handleResumeButton() {
        System.out.println("Resume button clicked");
        resumeGame();
    }

    @FXML
    private void handleMenuButton() {
        System.out.println("Menu button clicked");
        if (menuOverlay.isVisible()) {
            // If menu is showing, hide it and resume if game was running
            hideAllOverlays();
            if (gameRunning && !gamePaused) {
                // Game was running, don't change pause state
            } else if (gameRunning && gamePaused) {
                // Game was paused, keep it paused
                showOverlay(pauseOverlay);
            }
        } else {
            // Show menu
            if (gameRunning && !gamePaused) {
                pauseGame();
            }
            showOverlay(menuOverlay);

            // Enable/disable resume button based on game state
            if (resumeMenuButton != null) {
                resumeMenuButton.setDisable(!gameRunning);
            }
        }
    }

    @FXML
    private void restartGame() {
        System.out.println("Restart game clicked");
        stopGameActivities();

        // Reset game state
        score = 0;
        lives = 3;
        level = 1;
        enemyCount = 0;
        gameStartTime = System.currentTimeMillis();
        pausedDuration = 0;
        pauseStartTime = 0;
        currentStatus = "Restarting...";

        // Clear all game objects
        bullets.clear();
        enemies.clear();
        asteroids.clear();
        particles.clear();
        pressedKeys.clear();

        // Show loading overlay
        showOverlay(loadingOverlay);
        updateUI();

        // Start game after brief loading
        Timeline restartTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            startGame();
        }));
        restartTimeline.play();
    }

    @FXML
    private void handleSettingsButton() {
        System.out.println("Settings button clicked");
        // TODO: Implement settings functionality
        currentStatus = "Settings not implemented yet";
        updateUI();
    }

    @FXML
    private void handleHighScoresButton() {
        System.out.println("High scores button clicked");
        // TODO: Implement high scores functionality
        currentStatus = "High scores not implemented yet";
        updateUI();
    }

    @FXML
    private void handleHelpButton() {
        System.out.println("Help button clicked");
        // TODO: Implement help functionality
        currentStatus = "Help not implemented yet";
        updateUI();
    }

    @FXML
    private void handleMuteButton() {
        System.out.println("Mute button clicked");
        // TODO: Implement sound muting functionality
        currentStatus = "Sound toggle not implemented yet";
        updateUI();
    }

    @FXML
    private void handleFullscreenButton() {
        System.out.println("Fullscreen button clicked");
        try {
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            stage.setFullScreen(!stage.isFullScreen());
            currentStatus = stage.isFullScreen() ? "Fullscreen mode" : "Windowed mode";
            updateUI();
        } catch (Exception e) {
            System.err.println("Error toggling fullscreen: " + e.getMessage());
            currentStatus = "Fullscreen toggle failed";
            updateUI();
        }
    }

    private void pauseGame() {
        if (gameRunning && !gamePaused) {
            gamePaused = true;
            pauseStartTime = System.currentTimeMillis();
            currentStatus = "Paused";
            showOverlay(pauseOverlay);
            updateUI();
            System.out.println("Game paused");
        }
    }

    private void resumeGame() {
        if (gameRunning && gamePaused) {
            gamePaused = false;
            if (pauseStartTime > 0) {
                pausedDuration += System.currentTimeMillis() - pauseStartTime;
                pauseStartTime = 0;
            }
            currentStatus = "Fighting";
            hideAllOverlays();
            updateUI();

            // Request focus back to canvas
            Platform.runLater(() -> gameCanvas.requestFocus());
            System.out.println("Game resumed");
        }
    }

    // Inner classes for game objects
    public static class Player {
        public double x, y;
        public double width = 24, height = 30;
        public double speed = 6;
        public long lastShot = 0;

        public Player(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class Bullet {
        public double x, y;
        public double width = 2, height = 10;
        public double speed;
        public BulletType type;
        public double life = 1.0;

        public Bullet(double x, double y, double speed, BulletType type) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.type = type;
        }

        public void update() {
            life -= 0.01;
        }
    }

    public static class Enemy {
        public double x, y;
        public double width = 30, height = 20;
        public double speed = 2;
        public long animationTime = System.currentTimeMillis();

        public Enemy(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = 1.5 + Math.random() * 2;
        }

        public void update() {
            animationTime += 16; // Approximate frame time
        }
    }

    public static class Star {
        public double x, y;
        public double size;
        public double speed;
        public double twinkle = 0;
        public Color color;
        public StarType type;

        public Star(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = 0.5 + Math.random() * 2;
            this.type = Math.random() < 0.1 ? StarType.LARGE : StarType.SMALL;
            this.size = type == StarType.LARGE ? 3 + Math.random() * 2 : 1 + Math.random();

            // Different star colors
            double colorChoice = Math.random();
            if (colorChoice < 0.6) {
                this.color = Color.WHITE;
            } else if (colorChoice < 0.8) {
                this.color = Color.LIGHTBLUE;
            } else {
                this.color = Color.LIGHTYELLOW;
            }
        }
    }

    public static class Planet {
        public double x, y;
        public double size;
        public Color color;

        public Planet(double x, double y) {
            this.x = x;
            this.y = y;
            this.size = 40 + Math.random() * 60;

            // Different planet colors
            Color[] planetColors = {
                    Color.web("#FF6B6B"), Color.web("#4ECDC4"), Color.web("#45B7D1"),
                    Color.web("#96CEB4"), Color.web("#FFEAA7"), Color.web("#DDA0DD")
            };
            this.color = planetColors[(int)(Math.random() * planetColors.length)];
        }
    }

    public static class Asteroid {
        public double x, y;
        public double size;
        public double speed;
        public double rotation = 0;
        public double rotationSpeed;

        public Asteroid(double x, double y) {
            this.x = x;
            this.y = y;
            this.size = 15 + Math.random() * 25;
            this.speed = 1 + Math.random() * 3;
            this.rotationSpeed = (Math.random() - 0.5) * 4;
        }
    }

    public static class Particle {
        public double x, y;
        public double vx, vy;
        public double size;
        public Color color;
        public double alpha = 1.0;
        public double life = 1.0;
        public ParticleType type;

        public Particle(double x, double y, ParticleType type) {
            this.x = x;
            this.y = y;
            this.type = type;

            switch (type) {
                case EXPLOSION:
                    this.vx = (Math.random() - 0.5) * 8;
                    this.vy = (Math.random() - 0.5) * 8;
                    this.size = 2 + Math.random() * 4;
                    this.color = Math.random() < 0.5 ? Color.ORANGE : Color.RED;
                    break;
                case MUZZLE_FLASH:
                    this.vx = (Math.random() - 0.5) * 4;
                    this.vy = -Math.random() * 6 - 2;
                    this.size = 1 + Math.random() * 2;
                    this.color = Color.CYAN;
                    break;
                case DEBRIS:
                    this.vx = (Math.random() - 0.5) * 6;
                    this.vy = (Math.random() - 0.5) * 6;
                    this.size = 1 + Math.random() * 3;
                    this.color = Color.BROWN;
                    break;
                case DAMAGE:
                    this.vx = (Math.random() - 0.5) * 10;
                    this.vy = (Math.random() - 0.5) * 10;
                    this.size = 2 + Math.random() * 3;
                    this.color = Color.RED;
                    break;
            }
        }

        public void update() {
            x += vx;
            y += vy;
            life -= 0.02;
            alpha = Math.max(0, life);
            size *= 0.98;
        }

        public boolean isDead() {
            return life <= 0 || size < 0.5;
        }
    }

    // Enums
    public enum BulletType {
        PLAYER_LASER, ENEMY_PLASMA
    }

    public enum StarType {
        SMALL, LARGE
    }

    public enum ParticleType {
        EXPLOSION, MUZZLE_FLASH, DEBRIS, DAMAGE
    }
}