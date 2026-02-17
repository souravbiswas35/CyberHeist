-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 29, 2025 at 03:11 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `game`
--

-- --------------------------------------------------------

--
-- Table structure for table `game_history`
--

CREATE TABLE `game_history` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `game_type` varchar(100) DEFAULT NULL,
  `result` varchar(10) DEFAULT NULL,
  `score` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `duration` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tools`
--

CREATE TABLE `tools` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `category` enum('attacking','defensive') NOT NULL,
  `base_cost` int(11) NOT NULL,
  `max_level` tinyint(4) NOT NULL DEFAULT 10,
  `description` text DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tools`
--

INSERT INTO `tools` (`id`, `name`, `category`, `base_cost`, `max_level`, `description`, `image_url`, `created_at`) VALUES
(1, 'Brute Force', 'attacking', 3000, 10, 'Attempt to crack passwords by trying many combinations.', NULL, '2025-06-27 16:06:14'),
(2, 'Phishing Mail', 'attacking', 2000, 10, 'Send deceptive emails to trick targets into revealing info.', NULL, '2025-06-27 16:06:14'),
(3, 'DDoS', 'attacking', 5000, 10, 'Overwhelm a system with traffic to disrupt service.', NULL, '2025-06-27 16:06:14'),
(4, 'Keysniffer', 'attacking', 1500, 10, 'Capture keystrokes to steal sensitive information.', NULL, '2025-06-27 16:06:14'),
(5, 'Injector', 'attacking', 4000, 10, 'Inject malicious code into processes or systems.', NULL, '2025-06-27 16:06:14'),
(6, 'Code Virus', 'attacking', 8000, 10, 'Deploy self-replicating malicious software.', NULL, '2025-06-27 16:06:14'),
(7, 'Firewall Pro', 'defensive', 3000, 10, 'Blocks unauthorized network access and malicious traffic.', NULL, '2025-06-27 16:08:39'),
(8, 'Encryption Vault', 'defensive', 2500, 10, 'Securely stores sensitive data using strong encryption.', NULL, '2025-06-27 16:08:39'),
(9, 'EDS', 'defensive', 2000, 10, 'Detects and responds to cyber threats in real-time.', NULL, '2025-06-27 16:08:39'),
(10, 'Antivirus', 'defensive', 7000, 10, 'Scans, detects, and removes malicious software.', NULL, '2025-06-27 16:08:39'),
(11, 'Patch Manager', 'defensive', 1500, 10, 'Applies security updates to fix vulnerabilities.', NULL, '2025-06-27 16:08:39');

-- --------------------------------------------------------

--
-- Table structure for table `tool_upgrade_costs`
--

CREATE TABLE `tool_upgrade_costs` (
  `id` int(11) NOT NULL,
  `tool_id` int(11) NOT NULL,
  `from_level` tinyint(4) NOT NULL,
  `to_level` tinyint(4) NOT NULL,
  `coins_cost` int(11) NOT NULL,
  `diamonds_cost` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) NOT NULL,
  `coins` int(11) DEFAULT 5000,
  `diamonds` int(11) DEFAULT 20,
  `level` int(11) DEFAULT 1,
  `experience` int(11) DEFAULT 0,
  `max_experience` int(11) DEFAULT 1000,
  `total_games` int(11) DEFAULT 0,
  `wins` int(11) DEFAULT 0,
  `losses` int(11) DEFAULT 0,
  `total_score` int(11) DEFAULT 0,
  `playtime_hours` int(11) DEFAULT 0,
  `rank` varchar(50) DEFAULT 'Newbie',
  `profile_pic_url` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `name`, `username`, `password`, `email`, `coins`, `diamonds`, `level`, `experience`, `max_experience`, `total_games`, `wins`, `losses`, `total_score`, `playtime_hours`, `rank`, `profile_pic_url`) VALUES
(1, 'Sourav', 's', '123', 's@gmail.com', 275489, 20, 1, 11, 1000, 5, 0, 0, 0, 0, 'Newbie', 'C:\\Users\\User\\OneDrive\\Documents\\erd_updated_2.png'),
(2, 'sas', 'a', '123', 'a@gmail.com', 5000, 20, 1, 0, 1000, 0, 0, 0, 0, 0, 'Newbie', NULL),
(4, 'Shuvo', 'shuvo', '1234', 'si@gmail.com', 5000, 20, 1, 0, 1000, 0, 0, 0, 0, 0, 'Newbie', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `user_tools`
--

CREATE TABLE `user_tools` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `tool_id` int(11) NOT NULL,
  `level` tinyint(4) NOT NULL DEFAULT 1,
  `purchased_at` datetime DEFAULT current_timestamp(),
  `last_upgraded_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_tools`
--

INSERT INTO `user_tools` (`id`, `username`, `tool_id`, `level`, `purchased_at`, `last_upgraded_at`) VALUES
(2, 's', 2, 1, '2025-06-27 18:09:40', NULL),
(3, 's', 3, 1, '2025-06-27 18:13:41', NULL),
(4, 's', 4, 1, '2025-06-27 18:15:06', NULL),
(5, 's', 6, 1, '2025-06-27 18:17:55', NULL),
(6, 's', 1, 1, '2025-06-27 18:27:42', NULL),
(7, 's', 7, 1, '2025-06-27 18:28:05', NULL),
(8, 's', 8, 1, '2025-06-27 18:28:13', NULL),
(9, 's', 9, 1, '2025-06-27 18:28:21', NULL),
(10, 's', 11, 1, '2025-06-27 18:28:28', NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `game_history`
--
ALTER TABLE `game_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `username` (`username`);

--
-- Indexes for table `tools`
--
ALTER TABLE `tools`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`);

--
-- Indexes for table `tool_upgrade_costs`
--
ALTER TABLE `tool_upgrade_costs`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `tool_id` (`tool_id`,`from_level`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `user_tools`
--
ALTER TABLE `user_tools`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`,`tool_id`),
  ADD KEY `tool_id` (`tool_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `game_history`
--
ALTER TABLE `game_history`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `tools`
--
ALTER TABLE `tools`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `tool_upgrade_costs`
--
ALTER TABLE `tool_upgrade_costs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `user_tools`
--
ALTER TABLE `user_tools`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `game_history`
--
ALTER TABLE `game_history`
  ADD CONSTRAINT `game_history_ibfk_1` FOREIGN KEY (`username`) REFERENCES `users` (`username`);

--
-- Constraints for table `tool_upgrade_costs`
--
ALTER TABLE `tool_upgrade_costs`
  ADD CONSTRAINT `tool_upgrade_costs_ibfk_1` FOREIGN KEY (`tool_id`) REFERENCES `tools` (`id`);

--
-- Constraints for table `user_tools`
--
ALTER TABLE `user_tools`
  ADD CONSTRAINT `user_tools_ibfk_1` FOREIGN KEY (`username`) REFERENCES `users` (`username`),
  ADD CONSTRAINT `user_tools_ibfk_2` FOREIGN KEY (`tool_id`) REFERENCES `tools` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
