package com.gestion.controllers;

import com.gestion.entities.ParticipantRestauration;
import com.gestion.entities.Restauration;
import com.gestion.interfaces.RestaurationService;
import com.gestion.services.RestaurationServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur Restauration – utilise l'entité unifiée Restauration et l'API Stream.
 */
public class RestaurationController {

    private final RestaurationService service = new RestaurationServiceImpl();

    // ================= MENU =================

    public Restauration createMenu(String nom, Long optionRestaurationId, boolean actif) {
        return service.create(Restauration.menu(nom, optionRestaurationId, actif));
    }

    public List<Restauration> getMenusActifs() {
        return service.findMenusActifs();
    }

    public List<Restauration> getMenusByOption(Long optionId) {
        return service.findMenusActifs().stream()
                .filter(m -> optionId != null && optionId.equals(m.getOptionRestaurationId()))
                .collect(Collectors.toList());
    }

    public Restauration updateMenu(Restauration menu) {
        menu.setType(Restauration.TypeRestauration.MENU);
        return service.update(menu);
    }

    public boolean deleteMenu(Long id) {
        return service.delete(id, Restauration.TypeRestauration.MENU);
    }

    // ================= OPTION =================

    public Restauration createOption(String libelle, String typeEvenement, boolean actif) {
        return service.create(Restauration.option(libelle, typeEvenement, actif));
    }

    public List<Restauration> getOptionsByType(String type) {
        return service.findOptionsByTypeEvenement(type);
    }

    public List<Restauration> getAllOptions() {
        return service.findAll(Restauration.TypeRestauration.OPTION).stream()
                .filter(Restauration::isActif)
                .collect(Collectors.toList());
    }

    // ================= REPAS =================

    public Restauration createRepas(String nomRepas, BigDecimal prix, LocalDate date, Long participantId) {
        return service.create(Restauration.repas(nomRepas, prix, date, participantId));
    }

    public List<Restauration> getRepasByParticipant(Long participantId) {
        return service.findRepasByParticipantId(participantId);
    }

    public List<Restauration> getRepasByDate(LocalDate date) {
        return service.findRepasByDate(date);
    }

    public boolean hasRepasForDay(Long participantId, LocalDate date) {
        return service.hasRepasForParticipantAndDate(participantId, date);
    }

    public Restauration updateRepas(Restauration repas) {
        repas.setType(Restauration.TypeRestauration.REPAS);
        return service.update(repas);
    }

    public boolean deleteRepas(Long id) {
        return service.delete(id, Restauration.TypeRestauration.REPAS);
    }

    // ================= RESTRICTION =================

    public Restauration createRestriction(String libelle, String description, boolean actif) {
        return service.create(Restauration.restriction(libelle, description, actif));
    }

    public List<Restauration> getRestrictionsActives() {
        return service.findRestrictionsActives();
    }

    // ================= PRESENCE =================

    public Restauration createPresence(Long participantId, LocalDate date, boolean abonnementActif) {
        return service.create(Restauration.presence(participantId, date, abonnementActif));
    }

    public List<Restauration> getPresenceByParticipant(Long participantId) {
        return service.findPresencesByParticipantId(participantId);
    }

    public boolean hasPresenceForDay(Long participantId, LocalDate date) {
        return service.findAllPresences().stream()
                .anyMatch(p -> participantId.equals(p.getParticipantId()) && date.equals(p.getDatePresence()));
    }

    public boolean isAbonnementActif(Long participantId) {
        return service.findPresencesByParticipantId(participantId).stream()
                .anyMatch(Restauration::isAbonnementActif);
    }

    // ================= BESOIN PARTICIPANT =================

    public ParticipantRestauration createBesoin(ParticipantRestauration besoin) {
        return service.createBesoin(besoin);
    }

    public List<ParticipantRestauration> getBesoinsByParticipantId(Long participantId) {
        return service.findBesoinsByParticipantId(participantId);
    }
}
