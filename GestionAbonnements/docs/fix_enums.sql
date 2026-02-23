-- =====================================================
-- Correctif pour les ENUMs de la table ABONNEMENTS
-- Ajout du type 'EVENEMENT_PASS'
-- =====================================================

USE lamma_db;

-- Mise à jour du type pour inclure EVENEMENT_PASS
ALTER TABLE abonnements 
MODIFY COLUMN type ENUM('MENSUEL', 'ANNUEL', 'PREMIUM', 'EVENEMENT_PASS') NOT NULL;

-- Mise à jour du statut pour s'assurer qu'il est cohérent (optionnel mais recommandé)
ALTER TABLE abonnements 
MODIFY COLUMN statut ENUM('ACTIF', 'EXPIRE', 'SUSPENDU', 'EN_ATTENTE') NOT NULL DEFAULT 'ACTIF';

SELECT 'Migration des ENUMs terminée avec succès!' as Message;
