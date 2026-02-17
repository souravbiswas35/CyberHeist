package com.cyberheist;

import com.cyberheist.DatabaseConnection;
import com.cyberheist.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class TicTacToeController implements Initializable {

    @FXML private Button btn00, btn01, btn02;
    @FXML private Button btn10, btn11, btn12;
    @FXML private Button btn20, btn21, btn22;
    @FXML private Label statusLabel;
    @FXML private Button resetButton;
    @FXML private Button backButton;
    @FXML private Button mainMenuButton;
    @FXML private GridPane gameGrid;

    private Button[] buttons;
    private String[] board = new String[9];
    private boolean isPlayerTurn = true;
    private boolean gameEnded = false;
    private String username;

    private static final int WIN_REWARD  = 500;
    private static final int LOSS_REWARD = 25;

    /** Injected by SceneManager */
    public void setUsername(String username) {
        this.username = username;
        if (username == null || username.isEmpty()) {
            disableAll();
            statusLabel.setText("Please log in to play.");
        } else {
            resetGame();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buttons = new Button[]{ btn00, btn01, btn02,
                btn10, btn11, btn12,
                btn20, btn21, btn22 };
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            board[i] = "";
            buttons[i].setOnAction(e -> handlePlayerMove(idx));
        }
        resetButton.setOnAction(e -> handleReset());
        backButton.setOnAction(e -> handleBack());
        mainMenuButton.setOnAction(e -> handleMainMenu());
        disableAll();  // until setUsername runs
    }

    private void handlePlayerMove(int idx) {
        if (gameEnded || !board[idx].isEmpty() || !isPlayerTurn || username == null) return;
        board[idx] = "X";
        buttons[idx].setText("X");
        if (checkWin("X")) {
            gameEnded = true;
            statusLabel.setText("You Win! +" + WIN_REWARD + " Coins");
            awardCoins(WIN_REWARD);
            updateGameHistory("TicTacToe", "win", WIN_REWARD);
            return;
        }
        if (isBoardFull()) {
            gameEnded = true;
            statusLabel.setText("Draw! +" + LOSS_REWARD + " Coins");
            awardCoins(LOSS_REWARD);
            updateGameHistory("TicTacToe", "draw", LOSS_REWARD);
            return;
        }
        isPlayerTurn = false;
        statusLabel.setText("AI Turn...");
        aiMove();
    }

    private void aiMove() {
        int bestScore = Integer.MIN_VALUE;
        int move = -1;
        for (int i = 0; i < 9; i++) {
            if (board[i].isEmpty()) {
                board[i] = "O";
                int score = minimax(false);
                board[i] = "";
                if (score > bestScore) {
                    bestScore = score;
                    move = i;
                }
            }
        }
        if (move != -1) {
            board[move] = "O";
            buttons[move].setText("O");
        }
        if (checkWin("O")) {
            gameEnded = true;
            statusLabel.setText("AI Wins! +" + LOSS_REWARD + " Coins");
            awardCoins(LOSS_REWARD);
            updateGameHistory("TicTacToe", "loss", LOSS_REWARD);
        } else if (isBoardFull()) {
            gameEnded = true;
            statusLabel.setText("Draw! +" + LOSS_REWARD + " Coins");
            awardCoins(LOSS_REWARD);
            updateGameHistory("TicTacToe", "draw", LOSS_REWARD);
        } else {
            isPlayerTurn = true;
            statusLabel.setText("Your Turn (X)");
        }
    }

    private int minimax(boolean isMaximizing) {
        if (checkWin("O")) return 10;
        if (checkWin("X")) return -10;
        if (isBoardFull()) return 0;

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < 9; i++) {
                if (board[i].isEmpty()) {
                    board[i] = "O";
                    int eval = minimax(false);
                    board[i] = "";
                    maxEval = Math.max(eval, maxEval);
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 0; i < 9; i++) {
                if (board[i].isEmpty()) {
                    board[i] = "X";
                    int eval = minimax(true);
                    board[i] = "";
                    minEval = Math.min(eval, minEval);
                }
            }
            return minEval;
        }
    }


    private boolean checkWin(String p) {
        int[][] lines = {
                {0,1,2},{3,4,5},{6,7,8},
                {0,3,6},{1,4,7},{2,5,8},
                {0,4,8},{2,4,6}
        };
        for (int[] L : lines) {
            if (board[L[0]].equals(p) &&
                    board[L[1]].equals(p) &&
                    board[L[2]].equals(p)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBoardFull() {
        for (String s : board) if (s.isEmpty()) return false;
        return true;
    }

    /** Called by FXML onAction="#handleReset" */
    @FXML
    private void handleReset() {
        resetGame();
    }

    /** Called by FXML onAction="#handleBack" */
    @FXML
    private void handleBack() {
        SceneManager.getInstance().switchToScene("MiniGames.fxml");
    }

    /** Called by FXML onAction="#handleMainMenu" */
    @FXML
    private void handleMainMenu() {
        SceneManager.getInstance().switchToScene("GameMenu.fxml");
    }

    private void resetGame() {
        for (int i = 0; i < 9; i++) {
            board[i] = "";
            buttons[i].setText("");
            buttons[i].setDisable(username == null || username.isEmpty());
        }
        isPlayerTurn = true;
        gameEnded = false;
        statusLabel.setText(
                (username == null || username.isEmpty())
                        ? "Please log in to play."
                        : "Your Turn (X)"
        );
        resetButton.setDisable(username == null || username.isEmpty());
    }

    private void disableAll() {
        for (Button b : buttons) b.setDisable(true);
        resetButton.setDisable(true);
    }

    /** Update user coins in database */
    private void awardCoins(int amount) {
        if (username == null) return;
        String sql = "UPDATE users SET coins = coins + ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update coins.").showAndWait();
        }
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
            stmt.setString(5, "1 game");

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
}
