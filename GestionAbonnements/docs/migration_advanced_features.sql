-- =====================================================
-- Migration pour les Fonctionnalités Avancées (v2.0)
-- Inclut : Fidélité, Notifications, Codes Promo, Tickets Repas, Stocks
-- =====================================================

USE lamma_db;

-- 1. Mise à jour de la table USERS
ALTER TABLE users ADD COLUMN IF NOT EXISTS loyalty_points INT DEFAULT 0;

-- 2. Mise à jour de la table PARTICIPATIONS
ALTER TABLE participations 
ADD COLUMN IF NOT EXISTS meal_option VARCHAR(50) DEFAULT 'SANS_REPAS',
ADD COLUMN IF NOT EXISTS points_earned INT DEFAULT 0;

-- 3. Mise à jour de la table INGREDIENTS
ALTER TABLE ingredients 
ADD COLUMN IF NOT EXISTS stock_quantite INT DEFAULT 100,
ADD COLUMN IF NOT EXISTS stock_seuil_alerte INT DEFAULT 10;

-- 4. Table des CODES PROMO
CREATE TABLE IF NOT EXISTS promo_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_percentage INT NOT NULL,
    expiration_date DATE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    usage_limit INT DEFAULT 0,
    current_usage INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 5. Table des TICKETS REPAS
CREATE TABLE IF NOT EXISTS meal_tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    participation_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    qr_code VARCHAR(100) UNIQUE NOT NULL,
    time_slot DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (participation_id) REFERENCES participations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. Table des NOTIFICATIONS (Structure Alignée)
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    type ENUM('INFO', 'SUCCESS', 'WARNING', 'ERROR') NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. Données de Test Initiales
INSERT IGNORE INTO promo_codes (code, discount_percentage, expiration_date, usage_limit) 
VALUES ('WELCOME10', 10, '2026-12-31', 100);

-- Affichage de succès
SELECT 'Migration terminée avec succès!' as Message;
