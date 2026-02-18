-- =====================================================
-- Base de données LAMMA - Gestion Abonnements et Participations
-- Version : 1.0.0 - COMPLET ET CORRIGÉ
-- Auteur : Système de Gestion
-- =====================================================

-- Création de la base de données
-- NOTE: Si vous utilisez une base existante avec un autre nom, modifiez le nom ici
-- et dans MyConnection.java pour correspondre
CREATE DATABASE IF NOT EXISTS lamma_db3 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE lamma_db3;

-- =====================================================
-- TABLE DES UTILISATEURS
-- =====================================================
CREATE TABLE IF NOT EXISTS utilisateurs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    telephone VARCHAR(20),
    date_naissance DATE,
    adresse TEXT,
    ville VARCHAR(100),
    code_postal VARCHAR(10),
    pays VARCHAR(50) DEFAULT 'Tunisie',
    preferences JSON,
    statut ENUM('ACTIF', 'INACTIF', 'SUSPENDU') DEFAULT 'ACTIF',
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_mise_a_jour TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_email (email),
    INDEX idx_statut (statut),
    INDEX idx_ville (ville)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE DES ÉVÉNEMENTS
-- =====================================================
CREATE TABLE IF NOT EXISTS evenement (
    id_event INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT DEFAULT NULL,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME DEFAULT NULL,
    lieu VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    
    INDEX idx_titre (titre),
    INDEX idx_dates (date_debut, date_fin),
    INDEX idx_lieu (lieu),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- TABLE DES ABONNEMENTS
-- =====================================================
CREATE TABLE IF NOT EXISTS abonnements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('MENSUEL', 'ANNUEL', 'PREMIUM') NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    prix DECIMAL(10,2) NOT NULL CHECK (prix > 0),
    statut ENUM('ACTIF', 'EXPIRE', 'SUSPENDU', 'EN_ATTENTE') NOT NULL DEFAULT 'ACTIF',
    avantages JSON,
    auto_renew BOOLEAN DEFAULT FALSE,
    points_accumules INT DEFAULT 0 CHECK (points_accumules >= 0),
    churn_score FLOAT DEFAULT 0.0 CHECK (churn_score >= 0 AND churn_score <= 1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_statut (statut),
    INDEX idx_dates (date_debut, date_fin),
    INDEX idx_auto_renew (auto_renew),
    INDEX idx_churn_score (churn_score),
    
    -- Contraintes métier
    CONSTRAINT chk_date_fin CHECK (date_fin > date_debut),
    CONSTRAINT chk_duree_minimale CHECK (DATEDIFF(date_fin, date_debut) >= 30)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE DES PARTICIPATIONS
-- =====================================================
CREATE TABLE IF NOT EXISTS participations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    evenement_id BIGINT NOT NULL,
    date_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type ENUM('SIMPLE', 'HEBERGEMENT', 'GROUPE') NOT NULL DEFAULT 'SIMPLE',
    statut ENUM('EN_ATTENTE', 'CONFIRME', 'ANNULE', 'EN_LISTE_ATTENTE') NOT NULL DEFAULT 'EN_ATTENTE',
    hebergement_nuits INT DEFAULT 0 CHECK (hebergement_nuits >= 0),
    contexte_social ENUM('COUPLE', 'AMIS', 'FAMILLE', 'SOLO', 'PROFESSIONNEL'),
    badge_associe VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    FOREIGN KEY (evenement_id) REFERENCES evenement(id_event) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_evenement_id (evenement_id),
    INDEX idx_statut (statut),
    INDEX idx_type (type),
    INDEX idx_contexte_social (contexte_social),
    INDEX idx_date_inscription (date_inscription),
    INDEX idx_badge (badge_associe),
    
    -- Contrainte d'unicité : un utilisateur ne peut participer qu'une fois par événement
    UNIQUE KEY uk_user_evenement (user_id, evenement_id),
    
    -- Contrainte métier
    CONSTRAINT chk_hebergement_type CHECK (
        (type = 'HEBERGEMENT' AND hebergement_nuits > 0) OR 
        (type != 'HEBERGEMENT' AND hebergement_nuits = 0)
    )
) ENGINE=InnoDB;

-- =====================================================
-- TABLE DES TICKETS
-- =====================================================
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    participation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type ENUM('TICKET', 'BADGE', 'PASS') NOT NULL,
    code_unique VARCHAR(100) UNIQUE NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    lieu VARCHAR(200),
    statut ENUM('VALIDE', 'UTILISE', 'EXPIRE', 'ANNULE') NOT NULL DEFAULT 'VALIDE',
    format ENUM('NUMERIQUE', 'PHYSIQUE', 'HYBRIDE') NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_expiration TIMESTAMP,
    qr_code VARCHAR(200),
    informations_supplementaires TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (participation_id) REFERENCES participations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    INDEX idx_participation_id (participation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_statut (statut),
    INDEX idx_format (format),
    INDEX idx_code_unique (code_unique),
    INDEX idx_date_creation (date_creation),
    INDEX idx_date_expiration (date_expiration),
    INDEX idx_coordonnees (latitude, longitude),
    INDEX idx_lieu (lieu)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE DES RECOMMANDATIONS
-- =====================================================
CREATE TABLE IF NOT EXISTS recommandations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    evenement_suggere_id BIGINT NOT NULL,
    score FLOAT NOT NULL CHECK (score >= 0 AND score <= 1),
    raison TEXT,
    algorithme_used ENUM('COLLABORATIVE', 'CONTENT_BASED', 'NLP', 'HYBRIDE', 'ML_TENSORFLOW', 'CLUSTERING') NOT NULL,
    equipement_bundle JSON,
    source_scraped VARCHAR(500),
    date_generation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_expiration TIMESTAMP NOT NULL,
    est_utilisee BOOLEAN DEFAULT FALSE,
    interaction_count INT DEFAULT 0,
    conversion_score FLOAT DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    FOREIGN KEY (evenement_suggere_id) REFERENCES evenement(id_event) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_evenement_suggere_id (evenement_suggere_id),
    INDEX idx_score (score),
    INDEX idx_algorithme (algorithme_used),
    INDEX idx_date_generation (date_generation),
    INDEX idx_date_expiration (date_expiration),
    INDEX idx_est_utilisee (est_utilisee),
    INDEX idx_conversion_score (conversion_score),
    
    -- Contrainte métier
    CONSTRAINT chk_date_expiration CHECK (date_expiration > date_generation)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE DES PAIEMENTS
-- =====================================================
CREATE TABLE IF NOT EXISTS paiements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    abonnement_id BIGINT,
    participation_id BIGINT,
    montant DECIMAL(10,2) NOT NULL CHECK (montant > 0),
    devise VARCHAR(3) DEFAULT 'TND',
    methode ENUM('CARTE', 'VIREMENT', 'ESPECE', 'STRIPE', 'PAYPAL') NOT NULL,
    statut ENUM('EN_ATTENTE', 'CONFIRME', 'ECHOUE', 'REMBOURSE') NOT NULL DEFAULT 'EN_ATTENTE',
    transaction_id VARCHAR(100),
    date_paiement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_confirmation TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    FOREIGN KEY (abonnement_id) REFERENCES abonnements(id) ON DELETE SET NULL,
    FOREIGN KEY (participation_id) REFERENCES participations(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_abonnement_id (abonnement_id),
    INDEX idx_participation_id (participation_id),
    INDEX idx_statut (statut),
    INDEX idx_date_paiement (date_paiement),
    INDEX idx_montant (montant),
    
    -- Contrainte : soit abonnement soit participation, pas les deux
    CONSTRAINT chk_type_paiement CHECK (
        (abonnement_id IS NOT NULL AND participation_id IS NULL) OR 
        (abonnement_id IS NULL AND participation_id IS NOT NULL) OR
        (abonnement_id IS NULL AND participation_id IS NULL)
    )
) ENGINE=InnoDB;

-- =====================================================
-- TABLE DES NOTIFICATIONS
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('INFO', 'SUCCESS', 'WARNING', 'ERROR', 'PROMOTION') NOT NULL,
    titre VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    url_action VARCHAR(500),
    est_lue BOOLEAN DEFAULT FALSE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_lecture TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_est_lue (est_lue),
    INDEX idx_date_creation (date_creation)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE DES LOGS ACTIVITÉS
-- =====================================================
CREATE TABLE IF NOT EXISTS activite_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entite VARCHAR(50) NOT NULL, -- 'ABONNEMENT', 'PARTICIPATION', 'RECOMMANDATION'
    entite_id BIGINT,
    details JSON,
    adresse_ip VARCHAR(45),
    user_agent TEXT,
    date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_entite (entite),
    INDEX idx_date_action (date_action)
) ENGINE=InnoDB;

-- =====================================================
-- TABLES RESTAURATION
-- =====================================================

CREATE TABLE IF NOT EXISTS option_restauration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    libelle VARCHAR(100) NOT NULL,
    type_evenement VARCHAR(30),
    actif BOOLEAN DEFAULT TRUE,
    INDEX idx_type_evenement (type_evenement)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS menu_proposition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(150) NOT NULL,
    option_restauration_id BIGINT,
    actif BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (option_restauration_id) REFERENCES option_restauration(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS restriction_alimentaire (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    libelle VARCHAR(100) NOT NULL,
    description TEXT,
    actif BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS participant_restauration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    participant_id BIGINT NOT NULL,
    evenement_id BIGINT NOT NULL,
    besoin_libelle VARCHAR(200),
    besoin_description TEXT,
    restriction_libelle VARCHAR(150),
    restriction_description TEXT,
    niveau_gravite VARCHAR(20),
    menu_proposition_id BIGINT,
    date_limite_modification DATE,
    annule BOOLEAN DEFAULT FALSE,
    INDEX idx_participant (participant_id),
    INDEX idx_evenement (evenement_id),
    FOREIGN KEY (menu_proposition_id) REFERENCES menu_proposition(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS repas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nom_repas VARCHAR(150) NOT NULL,
    prix DECIMAL(10,2) NOT NULL,
    date DATE NOT NULL,
    participant_id BIGINT NOT NULL,
    INDEX idx_participant (participant_id),
    INDEX idx_date (date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS presence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    participant_id BIGINT NOT NULL,
    date DATE NOT NULL,
    abonnement_actif BOOLEAN DEFAULT TRUE,
    INDEX idx_participant (participant_id),
    INDEX idx_date (date)
) ENGINE=InnoDB;

-- =====================================================
-- VUES UTILES (CORRIGÉES)
-- =====================================================

-- Vue des statistiques des abonnements
CREATE OR REPLACE VIEW v_stat_abonnements AS
SELECT 
    type,
    statut,
    COUNT(*) as nombre,
    AVG(prix) as prix_moyen,
    SUM(prix) as revenu_total,
    AVG(points_accumules) as points_moyens,
    AVG(churn_score) as churn_moyen
FROM abonnements 
GROUP BY type, statut;

-- Vue des participations par événement (CORRIGÉE)
CREATE OR REPLACE VIEW v_participations_evenement AS
SELECT 
    e.id_event as evenement_id,
    e.titre,
    e.type,
    e.date_debut,
    e.lieu,
    COUNT(p.id) as nb_inscrits,
    SUM(CASE WHEN p.statut = 'CONFIRME' THEN 1 ELSE 0 END) as nb_confirmes,
    SUM(CASE WHEN p.statut = 'EN_LISTE_ATTENTE' THEN 1 ELSE 0 END) as nb_liste_attente
FROM evenement e
LEFT JOIN participations p ON e.id_event = p.evenement_id
GROUP BY e.id_event, e.titre, e.type, e.date_debut, e.lieu;

-- Vue des recommandations performantes
CREATE OR REPLACE VIEW v_performance_recommandations AS
SELECT 
    algorithme_used,
    COUNT(*) as nb_recommandations,
    AVG(score) as score_moyen,
    SUM(CASE WHEN est_utilisee THEN 1 ELSE 0 END) as nb_utilisees,
    ROUND(SUM(CASE WHEN est_utilisee THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as taux_conversion,
    AVG(conversion_score) as conversion_moyenne
FROM recommandations 
WHERE date_generation >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY algorithme_used;

-- =====================================================
-- TRIGGERS (CORRIGÉS)
-- =====================================================

-- Trigger pour logger les activités importantes
DROP TRIGGER IF EXISTS tr_log_abonnement_creation;
DELIMITER //
CREATE TRIGGER tr_log_abonnement_creation
AFTER INSERT ON abonnements
FOR EACH ROW
BEGIN
    INSERT INTO activite_logs (user_id, action, entite, entite_id, details)
    VALUES (NEW.user_id, 'CREATION', 'ABONNEMENT', NEW.id, 
            JSON_OBJECT('type', NEW.type, 'prix', NEW.prix));
END//
DELIMITER ;

-- =====================================================
-- DONNÉES DE TEST
-- =====================================================

-- Insertion d'utilisateurs de test
INSERT IGNORE INTO utilisateurs (id, nom, email, ville, telephone) VALUES
(1, 'Admin Système', 'admin@lamma.com', 'Tunis', '+21612345678'),
(2, 'Mohamed Ben Ali', 'mohamed.benali@email.com', 'Sfax', '+21698765432'),
(3, 'Sonia Trabelsi', 'sonia.trabelsi@email.com', 'Sousse', '+21611111111'),
(4, 'Karim Mejri', 'karim.mejri@email.com', 'Monastir', '+21622222222'),
(5, 'Leila Khaled', 'leila.khaled@email.com', 'Nabeul', '+21633333333');

-- Insertion d'événements de test
INSERT IGNORE INTO evenement (id_event, titre, description, date_debut, date_fin, lieu, type) VALUES
(1, 'Camping Ain Draham', 'Week-end nature en montagne', '2026-03-15 16:00:00', '2026-03-17 18:00:00', 'Ain Draham', 'CAMPING'),
(2, 'Randonnée Zaghouan', 'Randonnée pédestre dans les montagnes', '2026-04-10 08:00:00', '2026-04-10 18:00:00', 'Zaghouan', 'RANDONNEE'),
(3, 'Soirée Saint Valentin', 'Soirée romantique pour couples', '2026-02-14 19:00:00', '2026-02-15 00:00:00', 'Tunis', 'SOIREE'),
(4, 'Fuego Beach Party', 'Soirée plage avec musique', '2026-07-20 20:00:00', '2026-07-21 02:00:00', 'Sousse', 'SOIREE'),
(5, 'Camping Djerba', 'Week-end détente à Djerba', '2026-05-01 14:00:00', '2026-05-03 12:00:00', 'Djerba', 'CAMPING');

-- Insertion d'abonnements de test
INSERT IGNORE INTO abonnements (user_id, type, date_debut, date_fin, prix, statut, auto_renew, points_accumules) VALUES
(2, 'MENSUEL', '2024-01-01', '2025-12-31', 29.99, 'ACTIF', TRUE, 250),
(3, 'ANNUEL', '2024-01-01', '2025-12-31', 299.99, 'ACTIF', TRUE, 500),
(4, 'PREMIUM', '2024-02-15', '2025-12-31', 499.99, 'ACTIF', TRUE, 750),
(5, 'MENSUEL', '2024-03-01', '2025-12-31', 29.99, 'SUSPENDU', FALSE, 120);

-- Insertion de participations de test
INSERT IGNORE INTO participations (user_id, evenement_id, type, statut, hebergement_nuits, contexte_social, badge_associe) VALUES
(2, 1, 'HEBERGEMENT', 'CONFIRME', 2, 'COUPLE', 'Romantique_Aventure'),
(3, 2, 'GROUPE', 'CONFIRME', 0, 'AMIS', 'Esprit_Equipe'),
(4, 3, 'SIMPLE', 'EN_LISTE_ATTENTE', 0, 'SOLO', 'Explorateur_Solitaire'),
(5, 4, 'HEBERGEMENT', 'CONFIRME', 1, 'FAMILLE', 'Famille_Unie'),
(2, 5, 'SIMPLE', 'CONFIRME', 0, 'COUPLE', 'Romantique_Aventure');

-- Insertion de tickets de test
INSERT IGNORE INTO tickets (participation_id, user_id, type, code_unique, latitude, longitude, lieu, statut, format, qr_code) VALUES
(1, 2, 'TICKET', 'TKT-1700000000-1', 36.8065, 10.1815, 'Parc National Belvédère', 'VALIDE', 'NUMERIQUE', 'QR-TKT-1700000000-1-36.8065-10.1815'),
(2, 3, 'BADGE', 'TKT-1700000001-2', 36.4029, 10.1425, 'Montagne Zaghouan', 'VALIDE', 'PHYSIQUE', 'QR-TKT-1700000001-2-36.4029-10.1425'),
(4, 5, 'PASS', 'TKT-1700000002-4', 33.8080, 10.8530, 'Resort Djerba', 'VALIDE', 'HYBRIDE', 'QR-TKT-1700000002-4-33.8080-10.8530');

-- Insertion de recommandations de test
INSERT IGNORE INTO recommandations (user_id, evenement_suggere_id, score, raison, algorithme_used, equipement_bundle, date_expiration) VALUES
(2, 4, 0.92, 'Parfait pour couple avec spa et détente (score IA: 92%)', 'NLP', 
 '{"pour_couple": {"tente": "2p_confort_queen", "matelas": "epais_luxe"}}', 
 DATE_ADD(NOW(), INTERVAL 7 DAY)),
(3, 1, 0.85, 'Recommandé basé sur vos préférences et celles d''utilisateurs similaires (score: 85%)', 'COLLABORATIVE', 
 '{"pour_couple": {"tente": "couple_matelas_queen", "loisir": "spa_portable"}}', 
 DATE_ADD(NOW(), INTERVAL 7 DAY)),
(4, 2, 0.78, 'Idéal pour groupe d''amis avec activités extérieures (score: 78%)', 'HYBRIDE', 
 '{"pour_amis": {"tente": "5p_spacieuse", "extras": "barbecue_vin"}}', 
 DATE_ADD(NOW(), INTERVAL 7 DAY));

-- =====================================================
-- DONNÉES DE TEST RESTAURATION
-- =====================================================

-- Insertion d'options de restauration
INSERT IGNORE INTO option_restauration (id, libelle, type_evenement, actif) VALUES
(1, 'Menu Standard', 'SOIREE', TRUE),
(2, 'Menu Végétarien', 'SOIREE', TRUE),
(3, 'Menu Halal', 'SOIREE', TRUE),
(4, 'Pique-nique Randonnée', 'RANDONNEE', TRUE),
(5, 'Petit-déjeuner Camping', 'CAMPING', TRUE),
(6, 'Déjeuner Camping', 'CAMPING', TRUE),
(7, 'Dîner Camping', 'CAMPING', TRUE),
(8, 'Menu Premium', 'SOIREE', TRUE);

-- Insertion de menus de proposition
INSERT IGNORE INTO menu_proposition (id, nom, option_restauration_id, actif) VALUES
(1, 'Menu Standard - Entrée + Plat + Dessert', 1, TRUE),
(2, 'Menu Végétarien - Salade + Plat végétarien + Dessert', 2, TRUE),
(3, 'Menu Halal - Entrée + Plat halal + Dessert', 3, TRUE),
(4, 'Pique-nique Randonnée - Sandwich + Fruit + Boisson', 4, TRUE),
(5, 'Petit-déjeuner Camping - Café + Croissant + Jus', 5, TRUE),
(6, 'Déjeuner Camping - Grillades + Salade + Pain', 6, TRUE),
(7, 'Dîner Camping - Couscous + Salade', 7, TRUE),
(8, 'Menu Premium - Entrée + Plat premium + Dessert + Vin', 8, TRUE);

-- Insertion de restrictions alimentaires
INSERT IGNORE INTO restriction_alimentaire (id, libelle, description, actif) VALUES
(1, 'Sans gluten', 'Régime sans gluten pour intolérance', TRUE),
(2, 'Sans lactose', 'Régime sans produits laitiers', TRUE),
(3, 'Végétarien', 'Pas de viande ni de poisson', TRUE),
(4, 'Végétalien', 'Pas de produits d''origine animale', TRUE),
(5, 'Halal', 'Conforme aux règles halal', TRUE),
(6, 'Sans arachides', 'Allergie aux arachides', TRUE),
(7, 'Sans fruits de mer', 'Allergie aux fruits de mer', TRUE);

-- Insertion de besoins de restauration pour participants
INSERT IGNORE INTO participant_restauration (id, participant_id, evenement_id, besoin_libelle, restriction_libelle, niveau_gravite, menu_proposition_id, date_limite_modification, annule) VALUES
(1, 2, 1, 'Besoin d''aide pour le service', 'Végétarien', 'MODEREE', 2, '2026-03-10', FALSE),
(2, 3, 2, 'Pas de besoin particulier', NULL, NULL, 4, '2026-04-05', FALSE),
(3, 4, 3, 'Besoin de chaise haute', 'Sans gluten', 'LEGERE', 1, '2026-02-10', FALSE),
(4, 5, 4, 'Pas de besoin particulier', 'Halal', 'MODEREE', 3, '2026-07-15', FALSE);

-- Insertion de repas
INSERT IGNORE INTO repas (id, nom_repas, prix, date, participant_id) VALUES
(1, 'Déjeuner Camping', 25.00, '2026-03-16', 2),
(2, 'Dîner Camping', 30.00, '2026-03-16', 2),
(3, 'Petit-déjeuner Camping', 15.00, '2026-03-17', 2),
(4, 'Pique-nique Randonnée', 20.00, '2026-04-10', 3);

-- Insertion de présences
INSERT IGNORE INTO presence (id, participant_id, date, abonnement_actif) VALUES
(1, 2, '2026-03-15', TRUE),
(2, 2, '2026-03-16', TRUE),
(3, 2, '2026-03-17', TRUE),
(4, 3, '2026-04-10', TRUE),
(5, 4, '2026-02-14', FALSE),
(6, 5, '2026-07-20', TRUE);

-- =====================================================
-- PROCÉDURES STOCKÉES
-- =====================================================

DROP PROCEDURE IF EXISTS sp_calculer_churn_score;
DELIMITER //
CREATE PROCEDURE sp_calculer_churn_score(IN p_user_id BIGINT)
BEGIN
    DECLARE v_nb_participations INT DEFAULT 0;
    DECLARE v_derniere_participation DATE;
    DECLARE v_score FLOAT DEFAULT 0.0;
    
    -- Compter les participations des 6 derniers mois
    SELECT COUNT(*), MAX(DATE(p.date_inscription))
    INTO v_nb_participations, v_derniere_participation
    FROM participations p
    WHERE p.user_id = p_user_id 
    AND p.date_inscription >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
    AND p.statut = 'CONFIRME';
    
    -- Calculer le score de churn
    IF v_nb_participations < 3 THEN
        SET v_score = 0.8;
    ELSEIF v_nb_participations < 5 THEN
        SET v_score = 0.5;
    ELSE
        SET v_score = 0.2;
    END IF;
    
    -- Ajuster selon la date de dernière participation
    IF v_derniere_participation < DATE_SUB(NOW(), INTERVAL 3 MONTH) THEN
        SET v_score = LEAST(1.0, v_score + 0.2);
    END IF;
    
    -- Mettre à jour le score
    UPDATE abonnements 
    SET churn_score = v_score 
    WHERE user_id = p_user_id AND statut = 'ACTIF';
    
    SELECT v_score as churn_score_calculé;
END//
DELIMITER ;

-- =====================================================
-- FIN DU SCRIPT
-- =====================================================

-- Affichage des statistiques initiales
SELECT 'Base de données LAMMA initialisée avec succès!' as message;
SELECT COUNT(*) as nb_utilisateurs FROM utilisateurs;
SELECT COUNT(*) as nb_evenements FROM evenement;
SELECT COUNT(*) as nb_abonnements FROM abonnements;
SELECT COUNT(*) as nb_participations FROM participations;
SELECT COUNT(*) as nb_recommandations FROM recommandations;
SELECT COUNT(*) as nb_options_restauration FROM option_restauration;
SELECT COUNT(*) as nb_menus FROM menu_proposition;
SELECT COUNT(*) as nb_restrictions FROM restriction_alimentaire;
