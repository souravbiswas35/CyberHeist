package com.cyberheist.network;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class GameClient {
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private Thread listenThread;
    private ConcurrentLinkedQueue<Consumer<String>> messageHandlers = new ConcurrentLinkedQueue<>();
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds timeout
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;

    public GameClient() {
        this.serverPort = 8888;
    }

    public boolean connect(String serverAddress) {
        this.serverAddress = serverAddress;
        try {
            // Set connection timeout
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverAddress, serverPort), CONNECTION_TIMEOUT);
            
            // Create input/output streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Set connection status
            connected = true;
            reconnectAttempts = 0;
            
            // Start listening for messages from the server
            listenThread = new Thread(this::listenForMessages);
            listenThread.setDaemon(true);
            listenThread.start();
            
            System.out.println("Connected to server at " + serverAddress);
            return true;
        } catch (SocketTimeoutException e) {
            System.err.println("Connection timed out: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }
    
    public boolean reconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            System.err.println("Maximum reconnection attempts reached.");
            return false;
        }
        
        reconnectAttempts++;
        System.out.println("Attempting to reconnect... (Attempt " + reconnectAttempts + ")");
        
        // Close existing connection
        disconnect();
        
        // Wait before reconnecting
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Attempt to reconnect
        return connect(serverAddress);
    }

    public void disconnect() {
        connected = false;
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void addMessageHandler(Consumer<String> handler) {
        messageHandlers.add(handler);
    }

    public void clearMessageHandlers() {
        messageHandlers.clear();
    }

    private void listenForMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                final String receivedMessage = message;
                // Process the message on the JavaFX thread
                Platform.runLater(() -> {
                    System.out.println("Received from server: " + receivedMessage);
                    
                    // Special handling for game started messages - prioritize processing these
                    if (receivedMessage.startsWith("GAME_STARTED:")) {
                        System.out.println("IMPORTANT: Processing game started message!");
                        for (Consumer<String> handler : messageHandlers) {
                            handler.accept(receivedMessage);
                        }
                        return;
                    }
                    
                    // Process all other messages
                    for (Consumer<String> handler : messageHandlers) {
                        handler.accept(receivedMessage);
                    }
                });
            }
        } catch (SocketException e) {
            if (connected) {
                System.err.println("Socket connection lost: " + e.getMessage());
                // Attempt to reconnect automatically
                Platform.runLater(() -> {
                    for (Consumer<String> handler : messageHandlers) {
                        handler.accept("CONNECTION_ERROR:Connection lost to server");
                    }
                    
                    // Only try to reconnect if we were previously connected
                    if (reconnect()) {
                        for (Consumer<String> handler : messageHandlers) {
                            handler.accept("RECONNECTED:Connection reestablished");
                        }
                    } else {
                        for (Consumer<String> handler : messageHandlers) {
                            handler.accept("CONNECTION_FAILED:Failed to reconnect to server");
                        }
                    }
                });
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("Error receiving message: " + e.getMessage());
                disconnect();
                
                Platform.runLater(() -> {
                    for (Consumer<String> handler : messageHandlers) {
                        handler.accept("CONNECTION_ERROR:" + e.getMessage());
                    }
                });
            }
        }
    }

    public void createRoom(String roomCode) {
        if (isConnected()) {
            sendMessage("CREATE_ROOM:" + roomCode);
        } else {
            System.err.println("Cannot create room: not connected to server");
            Platform.runLater(() -> {
                for (Consumer<String> handler : messageHandlers) {
                    handler.accept("CONNECTION_ERROR:Not connected to server");
                }
            });
        }
    }

    public void joinRoom(String roomCode) {
        if (isConnected()) {
            sendMessage("JOIN_ROOM:" + roomCode);
        } else {
            System.err.println("Cannot join room: not connected to server");
            Platform.runLater(() -> {
                for (Consumer<String> handler : messageHandlers) {
                    handler.accept("CONNECTION_ERROR:Not connected to server");
                }
            });
        }
    }

    public void sendReady() {
        if (isConnected()) {
            System.out.println("Sending PLAYER_READY signal");
            sendMessage("PLAYER_READY:");
        } else {
            System.err.println("Cannot send ready: not connected to server");
            Platform.runLater(() -> {
                for (Consumer<String> handler : messageHandlers) {
                    handler.accept("CONNECTION_ERROR:Not connected to server");
                }
            });
        }
    }

    public void sendAttack(String attackType) {
        if (isConnected()) {
            sendMessage("PLAYER_ACTION:ATTACK:" + attackType);
        } else {
            System.err.println("Cannot send attack: not connected to server");
            Platform.runLater(() -> {
                for (Consumer<String> handler : messageHandlers) {
                    handler.accept("CONNECTION_ERROR:Not connected to server");
                }
            });
        }
    }

    public void sendDefense() {
        if (isConnected()) {
            sendMessage("PLAYER_ACTION:DEFENSE");
        } else {
            System.err.println("Cannot send defense: not connected to server");
            Platform.runLater(() -> {
                for (Consumer<String> handler : messageHandlers) {
                    handler.accept("CONNECTION_ERROR:Not connected to server");
                }
            });
        }
    }

    private void sendMessage(String message) {
        if (isConnected() && out != null) {
            System.out.println("Sending to server: " + message);
            out.println(message);
        }
    }
} 