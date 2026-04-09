<div align="center">

![CyberHeist Typing SVG](https://readme-typing-svg.demolab.com?font=Orbitron&weight=900&size=42&duration=3000&pause=1000&color=00FFFF&center=true&vCenter=true&width=600&lines=🎮+CYBERHEIST;HACK+THE+SYSTEM;LEARN+%26+DOMINATE)
<br/>

![CyberHeist Banner](https://capsule-render.vercel.app/api?type=waving&color=0:0d0d0d,50:0a1628,100:00ffff&height=200&section=header&text=CyberHeist&fontSize=72&fontColor=00ffff&fontAlignY=38&desc=Hack.+Learn.+Dominate.&descAlignY=60&descColor=ff00ff&animation=fadeIn)

<br/>

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![JavaFX](https://img.shields.io/badge/JavaFX-23-0096C7?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org)
[![MySQL](https://img.shields.io/badge/MySQL-Database-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com)
[![License](https://img.shields.io/badge/License-MIT-00FF41?style=for-the-badge)](LICENSE)
[![Stars](https://img.shields.io/github/stars/souravbiswas35/CyberHeist?style=for-the-badge&color=FFD700&logo=github)](https://github.com/souravbiswas35/CyberHeist/stargazers)

<br/>

> **`// An educational cybersecurity game where you learn by hacking //`**

---

</div>

## 🌐 Project Overview

**CyberHeist** is a **JavaFX cyber-battle game** built with a full cyberpunk aesthetic, letting players fight, learn, and level up in a neon-drenched hacker universe.

```
╔══════════════════════════════════════════════════════════╗
║  🎨  Cyberpunk UI     →  neon/glow theme via styles.css  ║
║  👤  User Accounts    →  backed by MySQL                 ║
║  💰  Economy          →  coins & diamonds system         ║
║  🎯  Mini-Games       →  6 unique training modules       ║
║  ⚔️  Multiplayer      →  socket-based real-time battles  ║
║  🛠️  Tools & Upgrades →  strategic hacking arsenal       ║
║  📊  Player Profiles  →  full stats & history tracking   ║
╚══════════════════════════════════════════════════════════╝
```

Players **log in or sign up**, then navigate the **Hacker Command Center** to:

- 🚀 Launch multiplayer battles against real opponents
- 🎮 Play training mini-games to earn coins & diamonds
- 🛒 Browse, buy, and upgrade hacking tools
- 📈 View detailed stats and battle history

---

## ⚡ Features

<table>
<tr>
<td width="50%">

### ⚔️ Multiplayer Battle
```
🌐 Server Port  → 8888
🔢 Room Codes   → 4-digit join system
❤️ Base Health  → attack/defense XP
🎯 Tool Select  → animated battles
🤖 AI Mode      → rematch & rewards
```
Start server via `StartServer.bat` / `StartServer.sh`

</td>
<td width="50%">

### 💾 Database & Economy
```
👥 users        → credentials, coins, XP, stats
🛠️ tools        → attack/defense catalog
📦 user_tools   → ownership & upgrade levels
📜 game_history → results, timestamps, durations
```
All progression stored in **MySQL** via `game.sql`

</td>
</tr>
</table>

---

### 🎮 Mini-Games Arsenal

| Game | Description | Reward |
|------|-------------|--------|
| 🐍 **Snake** | Grid-based classic, score-to-coins | 💰 Coins |
| 🧱 **Tetris** | Piece rotation, line clears, leveling | 💰 Coins |
| ❌ **Tic-Tac-Toe** | Minimax AI opponent | 💰 Coins |
| 🎯 **Bubble Shooter** | Match-3 mechanics | 💰 Coins |
| 🚀 **Space Game** | Arcade shooter with FPS tracking | 💰 Coins |
| 📝 **Word Game** | "Password Cracker" letter-guessing | 💰 Coins |

---

### 🎨 UI & Design

- 🖼️ **FXML Screens** — login, signup, menu, battle, tools, upgrades, profile & mini-games
- 🌈 **Cyberpunk Neon Glow** — full theme via `styles.css`
- 🎬 **Visual Assets** — backgrounds, tool images, custom animations

---

## 📁 Project Structure

```
CyberHeist/
│
├─ 📁 src/main/java/com/cyberheist/
│  ├─ 🎮 Controllers/          # All game & tool controllers
│  ├─ 🌐 network/              # GameClient.java
│  ├─ 🖥️ server/               # CyberHeistServer.java
│  └─ 🚀 CyberHeistMain.java   # Entry point
│
├─ 📁 src/main/resources/com/cyberheist/
│  ├─ 📄 FXML/                 # UI screens
│  ├─ 🛠️ Tool/                 # Tool screens
│  ├─ 🎨 assets/               # images, gifs
│  └─ 💅 styles.css            # Global cyberpunk theme
│
├─ 🗄️ game.sql                 # Database schema & seed data
├─ 🖥️ StartServer.bat          # Windows server launcher
├─ 🐧 StartServer.sh           # Linux/macOS server launcher
├─ 📦 pom.xml                  # Maven configuration
└─ 🚫 .gitignore
```

---

## 🛠️ Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| ☕ Java | `21` | Core runtime |
| 🖼️ JavaFX | `23` | UI framework |
| 📦 Maven | latest | Build & dependency management |
| 🗄️ MySQL Connector | `8.0.33` | Database connectivity |

**Maven dependency for MySQL:**
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

---

## 🗄️ Database Setup

**Step 1** — Install MySQL or XAMPP

**Step 2** — Create the database:
```sql
CREATE DATABASE cyberheist;
```

**Step 3** — Import schema:
```bash
mysql -u root -p cyberheist < game.sql
```

**Step 4** — Update `DatabaseConnection.java`:
```java
private static final String URL      = "jdbc:mysql://localhost:3306/cyberheist";
private static final String USER     = "root";
private static final String PASSWORD = "";
```

**Step 5** — Add `mysql-connector-java-8.0.33.jar` to project libraries ✅

---

## 🚀 How to Run

```bash
# 1. Clone the repository
git clone https://github.com/souravbiswas35/CyberHeist.git
cd CyberHeist

# 2. Build with Maven
mvn clean install

# 3. Start multiplayer server
./StartServer.sh          # Linux/macOS
StartServer.bat           # Windows

# 4. Run the application
# Launch CyberHeistMain.java from your IDE or via Maven JavaFX plugin
```

---

## 🧠 What You'll Learn

```
🛡️  Cybersecurity Tools   →  Antivirus, Brute Force, DDoS, Encryption, Firewalls
⚠️  Cybersecurity Terms   →  Phishing, Injection, Keylogging, Patch Mgmt, IDS
🧩  Strategy & Logic      →  Mini-games + attack/defense battle mechanics
💾  Database Handling     →  In-game economy & progression systems
```

---

## 📌 Important Notes

> ⚠️ **Multiplayer** requires both host and client to connect to the **same server IP**

> 📊 **Mini-game scores** are automatically logged to `game_history`

> 🔴 **Always ensure MySQL** is running before launching the app

---

## 👥 Team

> *This project was built as part of an initiative to gamify cybersecurity education — making complex concepts approachable through interactive gameplay.*

<div align="center">

| 👨‍💻 Name | 🔗 GitHub Profile |
|-----------|-------------------|
| **Sourav Biswas** | [![GitHub](https://img.shields.io/badge/GitHub-%40souravbiswas35-181717?style=for-the-badge&logo=github&logoColor=white&labelColor=0d1117&color=00ffff)](https://github.com/souravbiswas35) |
| **Saidul Islam Shuvo** | [![GitHub](https://img.shields.io/badge/GitHub-%40shuivva-181717?style=for-the-badge&logo=github&logoColor=white&labelColor=0d1117&color=ff00ff)](https://github.com/shuivva) |
| **Sakibul Alam** | [![GitHub](https://img.shields.io/badge/GitHub-%40SakibulAlam001-181717?style=for-the-badge&logo=github&logoColor=white&labelColor=0d1117&color=00ff41)](https://github.com/SakibulAlam001) |

</div>

---

<div align="center">

![Footer](https://capsule-render.vercel.app/api?type=waving&color=0:00ffff,50:0a1628,100:0d0d0d&height=120&section=footer&animation=fadeIn)

**⭐ Star this repo if CyberHeist helped you learn something new!**

[![GitHub Repo](https://img.shields.io/badge/🔗%20View%20on%20GitHub-CyberHeist-00ffff?style=for-the-badge&logo=github&logoColor=black)](https://github.com/souravbiswas35/CyberHeist)

*Made with 💻 + ☕ + 🌙 by the CyberHeist Team*

</div>
