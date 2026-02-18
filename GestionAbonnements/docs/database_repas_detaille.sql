-- Script SQL pour créer les tables nécessaires pour les repas détaillés et compositions de menus

-- Table pour repas détaillés
CREATE TABLE IF NOT EXISTS repas_detaille (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    prix DECIMAL(10, 2) NOT NULL,
    calories INT,
    type_repas VARCHAR(50),
    date DATE,
    participant_id BIGINT,
    evenement_id BIGINT,
    ingredients TEXT,
    allergenes TEXT,
    vegetarien BOOLEAN DEFAULT FALSE,
    vegan BOOLEAN DEFAULT FALSE,
    sans_gluten BOOLEAN DEFAULT FALSE,
    halal BOOLEAN DEFAULT FALSE,
    actif BOOLEAN DEFAULT TRUE,
    image_url VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_participant (participant_id),
    INDEX idx_evenement (evenement_id),
    INDEX idx_date (date),
    INDEX idx_type_repas (type_repas),
    INDEX idx_actif (actif)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table pour compositions de menus
CREATE TABLE IF NOT EXISTS composition_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    repas_id BIGINT NOT NULL,
    ordre INT NOT NULL DEFAULT 1,
    type_repas VARCHAR(50),
    date DATE,
    participant_id BIGINT,
    evenement_id BIGINT,
    actif BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_menu (menu_id),
    INDEX idx_repas (repas_id),
    INDEX idx_participant (participant_id),
    INDEX idx_evenement (evenement_id),
    INDEX idx_date (date),
    FOREIGN KEY (menu_id) REFERENCES menu_proposition(id) ON DELETE CASCADE,
    FOREIGN KEY (repas_id) REFERENCES repas_detaille(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Données d'exemple
INSERT INTO repas_detaille (nom, description, prix, calories, type_repas, vegetarien, vegan, sans_gluten, actif) VALUES
('Salade César', 'Salade fraîche avec poulet grillé, parmesan et croûtons', 12.50, 350, 'DEJEUNER', FALSE, FALSE, FALSE, TRUE),
('Risotto aux champignons', 'Risotto crémeux aux champignons de saison', 14.00, 420, 'DINER', TRUE, FALSE, FALSE, TRUE),
('Bowl végétarien', 'Bowl coloré avec légumes grillés, quinoa et sauce tahini', 13.00, 380, 'DEJEUNER', TRUE, TRUE, TRUE, TRUE),
('Poulet rôti', 'Poulet rôti avec légumes de saison et pommes de terre', 16.50, 550, 'DINER', FALSE, FALSE, TRUE, TRUE),
('Petit-déjeuner continental', 'Croissants, confiture, fruits frais et café', 8.50, 280, 'PETIT_DEJEUNER', TRUE, FALSE, FALSE, TRUE);
