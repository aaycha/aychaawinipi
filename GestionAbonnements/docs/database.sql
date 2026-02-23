-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1:3306
-- Généré le : lun. 23 fév. 2026 à 20:20
-- Version du serveur : 8.4.7
-- Version de PHP : 8.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

--
-- Base de données : `lama`
--

-- --------------------------------------------------------

--
-- Structure de la table `abonnements`
--

DROP TABLE IF EXISTS `abonnements`;
CREATE TABLE IF NOT EXISTS `abonnements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `evenement_id` bigint DEFAULT NULL,
  `type` enum('MENSUEL','ANNUEL','PREMIUM','EVENEMENT_PASS') COLLATE utf8mb4_unicode_ci NOT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date NOT NULL,
  `prix` decimal(10,2) NOT NULL,
  `statut` enum('ACTIF','EXPIRE','SUSPENDU','EN_ATTENTE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIF',
  `avantages` json DEFAULT NULL,
  `auto_renew` tinyint(1) DEFAULT '0',
  `points_accumules` int DEFAULT '0',
  `churn_score` double DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_abonnements_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_abonnements_event` FOREIGN KEY (`evenement_id`) REFERENCES `evenement` (`id_event`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `activite_logs`
--

DROP TABLE IF EXISTS `activite_logs`;
CREATE TABLE IF NOT EXISTS `activite_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `action` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entite` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `entite_id` bigint DEFAULT NULL,
  `details` json DEFAULT NULL,
  `date_action` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `composition_menu`
--

DROP TABLE IF EXISTS `composition_menu`;
CREATE TABLE IF NOT EXISTS `composition_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_id` bigint NOT NULL,
  `repas_id` bigint NOT NULL,
  `ordre` int NOT NULL DEFAULT '1',
  `type_repas` varchar(50) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `participant_id` bigint DEFAULT NULL,
  `evenement_id` bigint DEFAULT NULL,
  `actif` tinyint(1) DEFAULT '1',
  `notes` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_menu` (`menu_id`),
  KEY `idx_repas` (`repas_id`),
  KEY `idx_participant` (`participant_id`),
  KEY `idx_evenement` (`evenement_id`),
  KEY `idx_date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `evenement`
--

DROP TABLE IF EXISTS `evenement`;
CREATE TABLE IF NOT EXISTS `evenement` (
  `id_event` bigint NOT NULL AUTO_INCREMENT,
  `titre` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `date_debut` datetime NOT NULL,
  `date_fin` datetime DEFAULT NULL,
  `lieu` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_event`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `ingredients`
--

DROP TABLE IF EXISTS `ingredients`;
CREATE TABLE IF NOT EXISTS `ingredients` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `categorie` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `prix_supplement` decimal(10,2) DEFAULT '0.00',
  `calories` int DEFAULT '0',
  `icon_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actif` tinyint(1) DEFAULT '1',
  ` stock_quantite` int NOT NULL DEFAULT '50',
  `stock_seuil_alerte` int NOT NULL DEFAULT '10',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `meal_tickets`
--

DROP TABLE IF EXISTS `meal_tickets`;
CREATE TABLE IF NOT EXISTS `meal_tickets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `participation_id` bigint NOT NULL,
  `user_id` int NOT NULL,
  `qr_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `time_slot` datetime NOT NULL,
  `used` tinyint(1) DEFAULT '0',
  `used_at` datetime DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `qr_code` (`qr_code`),
  KEY `participation_id` (`participation_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `menus_admin`
--

DROP TABLE IF EXISTS `menus_admin`;
CREATE TABLE IF NOT EXISTS `menus_admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `restaurant_id` bigint NOT NULL,
  `restaurant_nom` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `prix` decimal(10,2) NOT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date NOT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `menu_proposition`
--

DROP TABLE IF EXISTS `menu_proposition`;
CREATE TABLE IF NOT EXISTS `menu_proposition` (
  `id` int NOT NULL AUTO_INCREMENT,
  `menu_id` bigint NOT NULL,
  `option_restauration_id` bigint DEFAULT NULL,
  `actif` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `option_restauration_id` (`option_restauration_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `type` enum('INFO','SUCCESS','WARNING','ERROR') COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- --------------------------------------------------------

--
-- Structure de la table `participations`
--

DROP TABLE IF EXISTS `participations`;
CREATE TABLE IF NOT EXISTS `participations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `evenement_id` bigint NOT NULL,
  `date_inscription` datetime NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `statut` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hebergement_nuits` int DEFAULT '0',
  `contexte_social` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `badge_associe` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nb_adultes` int NOT NULL DEFAULT '1',
  `nb_enfants` int NOT NULL DEFAULT '0',
  `nb_chiens` int NOT NULL DEFAULT '0',
  `total_participants` int NOT NULL DEFAULT '1',
  `type_abonnement` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `montant_calcule` decimal(10,2) NOT NULL,
  `devise` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'TND',
  `commentaire` text COLLATE utf8mb4_unicode_ci,
  `besoins_speciaux` text COLLATE utf8mb4_unicode_ci,
  `abonnement_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `evenement_id` (`evenement_id`),
  KEY `abonnement_id` (`abonnement_id`),
  CONSTRAINT `fk_participations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_participations_event` FOREIGN KEY (`evenement_id`) REFERENCES `evenement` (`id_event`) ON DELETE CASCADE,
  CONSTRAINT `fk_participations_abonnement` FOREIGN KEY (`abonnement_id`) REFERENCES `abonnements` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

DROP TABLE IF EXISTS `users`;
CREATE TABLE IF NOT EXISTS `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('USER','ADMIN') COLLATE utf8mb4_unicode_ci DEFAULT 'USER',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `motorized` enum('YES','NO') COLLATE utf8mb4_unicode_ci DEFAULT 'NO',
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `loyalty_point` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_email` (`email`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `utilisateurs`
--

DROP TABLE IF EXISTS `utilisateurs`;
CREATE TABLE IF NOT EXISTS `utilisateurs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `telephone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date_naissance` date DEFAULT NULL,
  `adresse` text COLLATE utf8mb4_unicode_ci,
  `ville` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `code_postal` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pays` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'Tunisie',
  `preferences` json DEFAULT NULL,
  `statut` enum('ACTIF','INACTIF','SUSPENDU') COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIF',
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

COMMIT;
