-- Script pour corriger les contraintes de clé étrangère dans EventSponsor
-- Problème: La contrainte ON DELETE CASCADE empêche l'ajout de nouveaux enregistrements
-- Solution: Changer à ON DELETE SET NULL

-- 1. Afficher la structure actuelle
DESCRIBE EventSponsor;

-- 2. Supprimer la contrainte existante (elle doit être nommée fk_eventsponsor_sponsor)
ALTER TABLE EventSponsor DROP FOREIGN KEY fk_eventsponsor_sponsor;

-- 3. Ajouter la nouvelle contrainte avec ON DELETE SET NULL
ALTER TABLE EventSponsor
ADD CONSTRAINT fk_eventsponsor_sponsor
FOREIGN KEY (sponsor_id) REFERENCES Sponsor (id)
ON DELETE SET NULL;

-- 4. Vérifier la contrainte a été bien modifiée
SHOW CREATE TABLE EventSponsor;

-- EXPLICATION:
-- ON DELETE CASCADE : Supprime tous les EventSponsor quand un Sponsor est supprimé (PROBLÉMATIQUE)
-- ON DELETE SET NULL : Met sponsor_id à NULL quand un Sponsor est supprimé (ACCEPTABLE)
-- ON DELETE RESTRICT : Refuse la suppression d'un Sponsor s'il a des EventSponsor (IDÉAL)

-- Si vous préférez RESTRICT (plus sûr) :
-- ALTER TABLE EventSponsor
-- ADD CONSTRAINT fk_eventsponsor_sponsor
-- FOREIGN KEY (sponsor_id) REFERENCES Sponsor (id)
-- ON DELETE RESTRICT;

