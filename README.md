<div align="center">

# 🎮 CyberHeist

**CyberHeist is an educational game that teaches cybersecurity concepts and terminology through interactive gameplay.**

---

</div>

## 🎮 Project Overview

**CyberHeist** is a **JavaFX cyber-battle game** with:

- 🎨 **Cyberpunk UI** (neon/glow theme via `styles.css`)  
- 👤 **User accounts** backed by MySQL  
- 💰 **Coin/diamond economy** for tools and upgrades  
- 🎯 **Multiple mini-games** (Snake, Tetris, Tic-Tac-Toe, Bubble Shooter, Space Game, Word Game)  
- ⚔️ **Multiplayer Attack Mode** with socket-based server-client communication  
- 🛠️ **Tool purchasing and upgrade system**  
- 📊 **Player profile & stats tracking**

Players log in or sign up, then navigate the **Hacker Command Center** to:

- 🚀 Launch multiplayer battles  
- 🎮 Play training mini-games to earn coins/diamonds  
- 🛒 Browse and upgrade hacking tools  
- 📈 View detailed stats and game history

---

## 🖥️ Features

### ⚔️ Multiplayer Battle

- 🌐 **Server port:** 8888 (start server via `StartServer.bat` / `StartServer.sh`)  
- 🔢 **Room system:** 4-digit room codes to join  
- ❤️ **Base health, attack/defense XP, timers**  
- 🎯 **Tool selection and battle animations**  
- 🤖 **AI-supported mode, rematches, and rewards**  

### 🎮 Mini-Games

- 🐍 **Snake:** Grid-based, score-to-coins conversion  
- 🧱 **Tetris:** Piece rotation, line clears, levels, score-to-coins  
- ❌ **Tic-Tac-Toe:** Minimax AI opponent  
- 🎯 **Bubble Shooter:** Match-3 mechanics, coins for progress  
- 🚀 **Space Game:** Arcade shooter, FPS tracking, coins  
- 📝 **Word Game:** "Password Cracker," letter-guessing with hints  

### 💾 Database & Economy

All game progression is stored in MySQL (`game.sql`):

- 👥 **users:** Login credentials, coins, diamonds, level, XP, stats, profile picture  
- 🛠️ **tools:** Attack/defense catalog, base costs, descriptions  
- 📦 **user_tools:** Tool ownership & upgrade levels  
- 📜 **game_history:** Game results, timestamps, durations  

### 🎨 UI

- 🖼️ **FXML screens** for login, signup, menu, attack/join, battle, tools, upgrades, profile, and mini-games  
- 🌈 **Cyberpunk neon/glow styling** via `styles.css`  
- 🎬 **Visual assets:** backgrounds, tool images, animations

---

## ⚙️ Project Structure

```
CyberHeist/
├─ 📁 src/main/java/com/cyberheist/
│  ├─ 🎮 Controllers (All game & tool controllers)
│  ├─ 🌐 network/ (GameClient.java)
│  ├─ 🖥️ server/ (CyberHeistServer.java)
│  └─ 🚀 CyberHeistMain.java
├─ 📁 src/main/resources/com/cyberheist/
│  ├─ 📄 FXML/ (UI screens)
│  ├─ 🛠️ Tool/ (Tool screens)
│  ├─ 🎨 assets/ (images, gifs)
│  └─ 💅 styles.css
├─ 🗄️ game.sql (Database schema & initial data)
├─ 🖥️ StartServer.bat / StartServer.sh
├─ 📦 pom.xml (Maven config)
└─ 🚫 .gitignore
```

---

## 🛠️ Dependencies

- ☕ **Java 21**  
- 🖼️ **JavaFX 23**  
- 📦 **Maven project**  
- 🗄️ **MySQL Connector JAR:** `mysql-connector-j-8.0.33.jar` (add to `pom.xml` or project library)  

**Maven dependency for MySQL Connector:**

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

---

## 🗄️ Database Setup

1. **Install MySQL or XAMPP**

2. **Create a new database:**

```sql
CREATE DATABASE cyberheist;
```

3. **Import `game.sql`:**

```bash
mysql -u root -p cyberheist < game.sql
```

4. **Update database connection in `DatabaseConnection.java`:**

```java
private static final String URL = "jdbc:mysql://localhost:3306/cyberheist";
private static final String USER = "root";
private static final String PASSWORD = "";
```

5. **Make sure `mysql-connector-java-8.0.33.jar` is in your project libraries**

---

## 🚀 How to Run

1. **Clone the repo:**

```bash
git clone https://github.com/souravbiswas35/CyberHeist.git
cd CyberHeist
```

2. **Build with Maven:**

```bash
mvn clean install
```

3. **Start server** (for multiplayer mode):

   - **Windows:** `StartServer.bat`
   - **Linux/macOS:** `StartServer.sh`

4. **Run the JavaFX application** (`CyberHeistMain.java`)

5. **Play and learn** cybersecurity concepts interactively! 🎉

---

## 🎯 Learn & Play

**CyberHeist teaches:**

- 🛡️ **Cybersecurity tools:** Antivirus, Brute Force, DDoS, Encryption, Firewalls, etc.
- ⚠️ **Cybersecurity terms:** Phishing, Injection, Keylogging, Patch Management, IDS
- 🧠 **Strategy & logic skills:** Mini-games and attack/defense battles
- 💾 **Database handling & in-game economy**

---

## 📌 Notes

- ⚠️ **Multiplayer requires** both host and client to use the same server IP
- 📊 **Mini-game scores** are automatically logged in `game_history`
- 🔴 **Always ensure MySQL server is running** before launching the app
