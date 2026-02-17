# CyberHeist Multiplayer Game

## Overview
CyberHeist is a multiplayer cyber battle game where two players can connect and engage in hacking battles. Players can use various attack and defense tools to reduce their opponent's health and protect their own base.

## How to Play Multiplayer

### Setup (Two Players on Different Computers)
1. **Player 1 (Host):**
   - Run the server first: Execute `StartServer.bat` (Windows) or `StartServer.sh` (Linux/Mac)
   - The server will display its IP address in the console window
   - Launch the game by running CyberHeistMain
   - Go to the "Attack" section from the main menu
   - Enter the server's IP address (shown in the server console)
   - Click "CREATE GAME"
   - Note the 4-digit room code that appears

2. **Player 2 (Client):**
   - Launch the game by running CyberHeistMain
   - Go to the "Attack" section from the main menu
   - Enter the server's IP address (same as Player 1 entered)
   - Click "JOIN GAME"
   - Enter the 4-digit room code provided by Player 1

3. **Both Players:**
   - Once connected, click the "READY" button to start the game
   - When both players are ready, the game will begin automatically

### Gameplay
- Each player has a base with 100% health
- Players have limited Attack XP (20) and Defense XP (15)
- Choose your attack or defense type from the dropdown menus
- Click "LAUNCH ATTACK" to attack your opponent's base
- Click "ACTIVATE DEFENSE" to protect your base
- Each action consumes 1 XP point from the respective category
- The game continues until one player's base health reaches 0%
- The winner receives 30 Diamonds as a reward

### Attack Tools
- Brute Force: Medium damage (3000 coins)
- Phishing Mail: Low damage (2000 coins)
- DDos: High damage (5000 coins)
- KeySniffer: Low damage (1500 coins)
- Injector: High damage (4000 coins)
- Code Virus: Very high damage (8000 coins)

### Defense Tools
- FireWall Pro: Strong defense (3000 coins)
- Encryption Vault: Medium defense (2500 coins)
- EDS: Medium defense (2000 coins)
- Antivirus: Very strong defense (7000 coins)
- Patch Manager: Basic defense (1500 coins)

## Building and Running from Source

### Prerequisites
- Java JDK 11 or higher
- Maven

### Build Instructions
1. Clone the repository
2. Navigate to the project directory
3. Run `mvn clean package`
4. The compiled files will be in the `target` directory

### Running
1. Start the server: `java -cp target/classes com.cyberheist.server.CyberHeistServer`
2. In a new terminal, run the client: `java -cp target/classes com.cyberheist.CyberHeistMain`

## Game Controls
- Select attack/defense types using the dropdown menus
- Click the respective buttons to perform actions
- The game message area shows the history of actions and their results
- Click "QUIT GAME" to exit the match and return to the main menu

Enjoy the cyber battles! 