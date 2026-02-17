package com.cyberheist.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CyberHeistServer {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private boolean running = false;
    private Map<String, GameSession> gameSessions = new ConcurrentHashMap<>();
    private Map<String, ClientHandler> waitingPlayers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        CyberHeistServer server = new CyberHeistServer();
        server.start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("CyberHeist Server started on port " + PORT);
            System.out.println("Server IP: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Waiting for players to connect...");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                new Thread(clientHandler).start();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    // Print active rooms for debugging
    public void printRoomStatus() {
        System.out.println("---- ROOM STATUS ----");
        System.out.println("Active game sessions: " + gameSessions.size());
        for (Map.Entry<String, GameSession> entry : gameSessions.entrySet()) {
            GameSession session = entry.getValue();
            System.out.println("Room " + entry.getKey() + ": " 
                + (session.player1 != null ? "Player1 (Ready: " + session.player1Ready + ")" : "No Player1") 
                + " | " 
                + (session.player2 != null ? "Player2 (Ready: " + session.player2Ready + ")" : "No Player2"));
        }
        System.out.println("Waiting players: " + waitingPlayers.size());
        for (Map.Entry<String, ClientHandler> entry : waitingPlayers.entrySet()) {
            System.out.println("Room " + entry.getKey() + " is waiting for player 2");
        }
        System.out.println("--------------------");
    }

    public void createGameSession(String roomCode, ClientHandler host) {
        // Remove any existing session with this code (should not happen)
        if (gameSessions.containsKey(roomCode)) {
            System.out.println("Warning: Overwriting existing room with code " + roomCode);
            gameSessions.remove(roomCode);
            waitingPlayers.remove(roomCode);
        }
        
        GameSession session = new GameSession(roomCode, host);
        gameSessions.put(roomCode, session);
        waitingPlayers.put(roomCode, host);
        host.roomCode = roomCode; // Make sure the client handler knows its room code
        System.out.println("Game session created with room code: " + roomCode);
        printRoomStatus();
    }

    public boolean joinGameSession(String roomCode, ClientHandler player) {
        System.out.println("Attempting to join room: " + roomCode);
        if (!gameSessions.containsKey(roomCode)) {
            System.out.println("Failed join attempt: Room " + roomCode + " not found");
            printRoomStatus();
            return false;
        }

        GameSession session = gameSessions.get(roomCode);
        ClientHandler host = waitingPlayers.get(roomCode);
        
        if (session.isFull()) {
            System.out.println("Failed join attempt: Room " + roomCode + " is full");
            return false;
        }

        session.addPlayer(player);
        player.roomCode = roomCode; // Make sure the client handler knows its room code
        waitingPlayers.remove(roomCode);
        
        System.out.println("Player successfully joined room: " + roomCode);
        
        // Don't start the game immediately, wait for ready signals
        // Notify both players that the connection is successful
        host.sendMessage("CONNECTION_SUCCESS:Player joined your room");
        player.sendMessage("CONNECTION_SUCCESS:Connected to host");
        
        // Also send the ROOM_JOINED message to ensure client handling works
        player.sendMessage("ROOM_JOINED:" + roomCode);
        
        System.out.println("Player joined session " + roomCode);
        printRoomStatus();
        return true;
    }

    public void handlePlayerAction(String roomCode, ClientHandler player, String action) {
        if (!gameSessions.containsKey(roomCode)) {
            System.out.println("Error: Player action in non-existent room: " + roomCode);
            return;
        }

        GameSession session = gameSessions.get(roomCode);
        session.processAction(player, action);
    }

    public void removePlayerFromSession(String roomCode, ClientHandler player) {
        if (gameSessions.containsKey(roomCode)) {
            GameSession session = gameSessions.get(roomCode);
            session.removePlayer(player);
            
            if (session.isEmpty()) {
                gameSessions.remove(roomCode);
                System.out.println("Game session removed: " + roomCode);
            } else {
                // Notify other player that opponent left
                session.notifyOpponentLeft(player);
            }
        }
        
        // Also remove from waiting players if this was a host waiting
        if (waitingPlayers.containsKey(roomCode) && waitingPlayers.get(roomCode) == player) {
            waitingPlayers.remove(roomCode);
            System.out.println("Removed waiting player for room: " + roomCode);
        }
        
        printRoomStatus();
    }

    // Inner class to handle individual client connections
    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private CyberHeistServer server;
        public String roomCode = null; // Changed to public for easier access

        public ClientHandler(Socket socket, CyberHeistServer server) {
            this.clientSocket = socket;
            this.server = server;
            try {
                this.out = new PrintWriter(clientSocket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                System.err.println("Error setting up client handler: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from client: " + inputLine);
                    processCommand(inputLine);
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                if (roomCode != null) {
                    server.removePlayerFromSession(roomCode, this);
                }
                try {
                    clientSocket.close();
                    System.out.println("Client disconnected");
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void processCommand(String command) {
            String[] parts = command.split(":", 2);
            if (parts.length < 2) {
                System.out.println("Invalid command format: " + command);
                return;
            }
            
            String cmd = parts[0];
            String data = parts[1];
            
            switch (cmd) {
                case "CREATE_ROOM":
                    roomCode = data;
                    server.createGameSession(roomCode, this);
                    sendMessage("ROOM_CREATED:" + roomCode);
                    break;
                case "JOIN_ROOM":
                    // Check if client is already in a room
                    if (roomCode != null && server.gameSessions.containsKey(roomCode)) {
                        System.out.println("Warning: Client attempting to join room " + data + " but is already in room " + roomCode);
                        sendMessage("JOIN_FAILED:You are already in a room");
                        return;
                    }
                    
                    roomCode = data;
                    boolean joined = server.joinGameSession(roomCode, this);
                    if (joined) {
                        sendMessage("ROOM_JOINED:" + roomCode);
                    } else {
                        // Reset the roomCode if join failed
                        roomCode = null;
                        sendMessage("JOIN_FAILED:Room not found or full");
                    }
                    break;
                case "PLAYER_ACTION":
                    if (roomCode != null) {
                        server.handlePlayerAction(roomCode, this, data);
                    } else {
                        System.out.println("Warning: Player action received but player not in a room");
                    }
                    break;
                case "PLAYER_READY":
                    if (roomCode != null) {
                        System.out.println("Player in room " + roomCode + " is ready");
                        server.handlePlayerAction(roomCode, this, "READY");
                    } else {
                        System.out.println("Warning: Player ready signal but player not in a room");
                    }
                    break;
                default:
                    System.out.println("Unknown command: " + cmd);
                    break;
            }
        }

        public void sendMessage(String message) {
            System.out.println("Sending to client [room:" + roomCode + "]: " + message);
            try {
                out.println(message);
                out.flush(); // Ensure message is sent immediately
            } catch (Exception e) {
                System.err.println("Error sending message to client: " + e.getMessage());
            }
        }
    }

    // Inner class to manage game sessions
    class GameSession {
        private String roomCode;
        private ClientHandler player1;
        private ClientHandler player2;
        private boolean player1Ready = false;
        private boolean player2Ready = false;
        
        private int player1Health = 100;
        private int player2Health = 100;

        public GameSession(String roomCode, ClientHandler host) {
            this.roomCode = roomCode;
            this.player1 = host;
        }

        public boolean isFull() {
            return player1 != null && player2 != null;
        }
        
        public boolean isEmpty() {
            return player1 == null && player2 == null;
        }

        public void addPlayer(ClientHandler player) {
            if (player1 == null) {
                player1 = player;
            } else if (player2 == null) {
                player2 = player;
            }
            System.out.println("Added player to room " + roomCode + ", now has " + 
                (player1 != null ? "1" : "0") + " + " + (player2 != null ? "1" : "0") + " players");
        }

        public void removePlayer(ClientHandler player) {
            if (player == player1) {
                player1 = null;
                player1Ready = false;
                System.out.println("Player 1 removed from room " + roomCode);
            } else if (player == player2) {
                player2 = null;
                player2Ready = false;
                System.out.println("Player 2 removed from room " + roomCode);
            }
        }

        public void processAction(ClientHandler player, String action) {
            if (action.equals("READY")) {
                synchronized(this) {
                    // First ensure the player is actually part of this game session
                    if (player != player1 && player != player2) {
                        System.out.println("Warning: Ready signal from player not in this session");
                        return;
                    }
                    
                    if (player == player1) {
                        player1Ready = true;
                        System.out.println("Player 1 in room " + roomCode + " is ready");
                        player1.sendMessage("WAITING:Waiting for opponent to be ready");
                        if (player2 != null) {
                            player2.sendMessage("OPPONENT_READY:Your opponent is ready");
                        }
                    } else if (player == player2) {
                        player2Ready = true;
                        System.out.println("Player 2 in room " + roomCode + " is ready");
                        player2.sendMessage("WAITING:Waiting for opponent to be ready");
                        if (player1 != null) {
                            player1.sendMessage("OPPONENT_READY:Your opponent is ready");
                        }
                    }
                    
                    // Check if both players are ready and both connections are active
                    if (player1Ready && player2Ready && player1 != null && player2 != null) {
                        System.out.println("Both players ready in room " + roomCode + ", starting game");
                        try {
                            Thread.sleep(1000); // Give a slightly longer delay for safety
                            startGame();
                        } catch (InterruptedException e) {
                            System.err.println("Sleep interrupted during game start: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Not starting game yet: P1 Ready: " + player1Ready + 
                            ", P2 Ready: " + player2Ready + 
                            ", P1 null: " + (player1 == null) + 
                            ", P2 null: " + (player2 == null));
                    }
                }
            } else if (action.startsWith("ATTACK:")) {
                String attackType = action.split(":")[1];
                int damage = calculateDamage(attackType);
                
                if (player == player1) {
                    player2Health -= damage;
                    player1.sendMessage("ATTACK_RESULT:You dealt " + damage + " damage");
                    player2.sendMessage("UNDER_ATTACK:You received " + damage + " damage");
                    
                    // Update health status for both players
                    updateHealthStatus();
                    
                    // Check if game is over
                    if (player2Health <= 0) {
                        endGame(player1, player2);
                    }
                } else if (player == player2) {
                    player1Health -= damage;
                    player2.sendMessage("ATTACK_RESULT:You dealt " + damage + " damage");
                    player1.sendMessage("UNDER_ATTACK:You received " + damage + " damage");
                    
                    // Update health status for both players
                    updateHealthStatus();
                    
                    // Check if game is over
                    if (player1Health <= 0) {
                        endGame(player2, player1);
                    }
                }
            } else if (action.equals("DEFENSE")) {
                if (player == player1) {
                    player1.sendMessage("DEFENSE_ACTIVATED:Your defense is active");
                    player2.sendMessage("OPPONENT_DEFENSE:Opponent activated defense");
                } else if (player == player2) {
                    player2.sendMessage("DEFENSE_ACTIVATED:Your defense is active");
                    player1.sendMessage("OPPONENT_DEFENSE:Opponent activated defense");
                }
            } else if (action.equals("REMATCH_REQUEST")) {
                System.out.println("Rematch requested in room " + roomCode);
                if (player == player1 && player2 != null) {
                    player2.sendMessage("REMATCH_REQUEST:Opponent wants a rematch");
                } else if (player == player2 && player1 != null) {
                    player1.sendMessage("REMATCH_REQUEST:Opponent wants a rematch");
                }
            } else if (action.equals("REMATCH_ACCEPTED")) {
                System.out.println("Rematch accepted in room " + roomCode);
                if (player == player1 && player2 != null) {
                    player2.sendMessage("REMATCH_ACCEPTED:Opponent accepted rematch");
                } else if (player == player2 && player1 != null) {
                    player1.sendMessage("REMATCH_ACCEPTED:Opponent accepted rematch");
                }
                
                // Reset game state for rematch
                resetForRematch();
            } else if (action.equals("REMATCH_DENIED")) {
                System.out.println("Rematch declined in room " + roomCode);
                if (player == player1 && player2 != null) {
                    player2.sendMessage("REMATCH_DENIED:Opponent declined rematch");
                } else if (player == player2 && player1 != null) {
                    player1.sendMessage("REMATCH_DENIED:Opponent declined rematch");
                }
            } else if (action.equals("READY_FOR_REMATCH")) {
                if (player == player1) {
                    player1Ready = true;
                    System.out.println("Player 1 ready for rematch in room " + roomCode);
                    if (player2Ready && player2 != null) {
                        player1.sendMessage("READY_FOR_REMATCH:Both players ready");
                        player2.sendMessage("READY_FOR_REMATCH:Both players ready");
                    }
                } else if (player == player2) {
                    player2Ready = true;
                    System.out.println("Player 2 ready for rematch in room " + roomCode);
                    if (player1Ready && player1 != null) {
                        player1.sendMessage("READY_FOR_REMATCH:Both players ready");
                        player2.sendMessage("READY_FOR_REMATCH:Both players ready");
                    }
                }
            }
        }
        
        private int calculateDamage(String attackType) {
            // Basic damage for different attack types
            switch(attackType) {
                case "BRUTE_FORCE": return 15;
                case "PHISHING": return 10;
                case "DDOS": return 20;
                case "KEY_SNIFFER": return 8;
                case "INJECTOR": return 18;
                case "CODE_VIRUS": return 25;
                default: return 5;
            }
        }
        
        private void updateHealthStatus() {
            player1.sendMessage("HEALTH_UPDATE:" + player1Health + ":" + player2Health);
            player2.sendMessage("HEALTH_UPDATE:" + player2Health + ":" + player1Health);
        }
        
        private void startGame() {
            // Make sure both clients are still connected before starting
            if (player1 != null && player2 != null) {
                try {
                    // Give a small delay before starting to ensure messages are processed
                    Thread.sleep(500);
                    System.out.println("Sending GAME_STARTED messages to both players in room " + roomCode);
                    
                    // Send multiple times with delay to ensure reliability
                    for (int i = 0; i < 3; i++) {
                        player1.sendMessage("GAME_STARTED:Game has started");
                        player2.sendMessage("GAME_STARTED:Game has started");
                        System.out.println("Sent game started messages to both players (attempt " + (i+1) + ")");
                        if (i < 2) Thread.sleep(200); // Small delay between retries
                    }
                } catch (InterruptedException e) {
                    System.err.println("Error during game start delay: " + e.getMessage());
                }
            } else {
                System.out.println("Cannot start game, at least one player is null");
            }
        }
        
        private void endGame(ClientHandler winner, ClientHandler loser) {
            winner.sendMessage("GAME_OVER:You won! +30 Diamonds");
            loser.sendMessage("GAME_OVER:You lost!");
        }

        public void notifyOpponentLeft(ClientHandler player) {
            if (player == player1 && player2 != null) {
                player2.sendMessage("OPPONENT_LEFT:Your opponent has left the game");
            } else if (player == player2 && player1 != null) {
                player1.sendMessage("OPPONENT_LEFT:Your opponent has left the game");
            }
        }

        private void resetForRematch() {
            System.out.println("Resetting game state for rematch in room " + roomCode);
            player1Ready = false;
            player2Ready = false;
            player1Health = 100;
            player2Health = 100;
        }
    }
} 