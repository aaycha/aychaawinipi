package com.gestion.interfaces;

import com.gestion.entities.ParticipantRestauration;
import com.gestion.entities.Restauration;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service global de gestion Restauration (entité unifiée).
 * Toutes les opérations utilisent l'API Stream.
 */
public interface RestaurationService {

    // =====================================================
    // 🔹 GESTION RESTAURATION UNIFIÉE (Stream)
    // =====================================================

    Restauration create(Restauration r);
    Optional<Restauration> findById(Long id, Restauration.TypeRestauration type);
    List<Restauration> findAll(Restauration.TypeRestauration type);

    /**
     * Met à jour une entité restauration existante (menu, repas, ...).
     * L'implémentation actuelle supporte principalement MENU et REPAS.
     */
    Restauration update(Restauration r);

    /**
     * Supprime une entité restauration par id et type.
     * Retourne true si au moins une ligne a été supprimée.
     */
    boolean delete(Long id, Restauration.TypeRestauration type);

    // Menus actifs
    List<Restauration> findMenusActifs();

    // Options par type événement
    List<Restauration> findOptionsByTypeEvenement(String typeEvenement);

    // Repas
    List<Restauration> findRepasByParticipantId(Long participantId);
    List<Restauration> findRepasByDate(LocalDate date);
    boolean hasRepasForParticipantAndDate(Long participantId, LocalDate date);

    // Restrictions actives
    List<Restauration> findRestrictionsActives();

    // Présences
    List<Restauration> findAllPresences();
    List<Restauration> findPresencesByParticipantId(Long participantId);

    // =====================================================
    // 🔹 GESTION BESOIN PARTICIPATION (ParticipantRestauration)
    // =====================================================

    ParticipantRestauration createBesoin(ParticipantRestauration besoin);
    Optional<ParticipantRestauration> findBesoinById(Long id);
    List<ParticipantRestauration> findBesoinsByParticipantId(Long participantId);
    ParticipantRestauration updateBesoin(ParticipantRestauration besoin);
    boolean deleteBesoin(Long id);

    // Règle métier : modification autorisée avant J-3
    boolean peutModifierChoixRepas(Long id);
}
