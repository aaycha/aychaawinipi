package com.gestion.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Gestion complète restauration + besoins spécifiques d’un participant
 * pour un événement donné.
 */
public class ParticipantRestauration {

    private Long id;

    // --- Lien principal ---
    private Long participantId;
    private Long evenementId;

    // --- Besoin spécifique ---
    private String besoinLibelle;          // ex: accès PMR, aide découpe
    private String besoinDescription;

    // --- Restriction alimentaire ---
    private String restrictionLibelle;     // ex: sans gluten
    private String restrictionDescription;
    private String niveauGravite;          // LEGERE, MODEREE, SEVERE
    private boolean restrictionActive = true;

    // --- Choix repas ---
    private Long menuPropositionId;
    private LocalDateTime dateChoix;
    private LocalDate dateLimiteModification;
    private String commentaire;

    // --- Etat ---
    private boolean annule = false;

    public ParticipantRestauration() {}

    // ================= GETTERS / SETTERS =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public Long getEvenementId() { return evenementId; }
    public void setEvenementId(Long evenementId) { this.evenementId = evenementId; }

    public String getBesoinLibelle() { return besoinLibelle; }
    public void setBesoinLibelle(String besoinLibelle) { this.besoinLibelle = besoinLibelle; }

    public String getBesoinDescription() { return besoinDescription; }
    public void setBesoinDescription(String besoinDescription) { this.besoinDescription = besoinDescription; }

    public String getRestrictionLibelle() { return restrictionLibelle; }
    public void setRestrictionLibelle(String restrictionLibelle) { this.restrictionLibelle = restrictionLibelle; }

    public String getRestrictionDescription() { return restrictionDescription; }
    public void setRestrictionDescription(String restrictionDescription) { this.restrictionDescription = restrictionDescription; }

    public String getNiveauGravite() { return niveauGravite; }
    public void setNiveauGravite(String niveauGravite) { this.niveauGravite = niveauGravite; }

    public boolean isRestrictionActive() { return restrictionActive; }
    public void setRestrictionActive(boolean restrictionActive) { this.restrictionActive = restrictionActive; }

    public Long getMenuPropositionId() { return menuPropositionId; }
    public void setMenuPropositionId(Long menuPropositionId) { this.menuPropositionId = menuPropositionId; }

    public LocalDateTime getDateChoix() { return dateChoix; }
    public void setDateChoix(LocalDateTime dateChoix) { this.dateChoix = dateChoix; }

    public LocalDate getDateLimiteModification() { return dateLimiteModification; }
    public void setDateLimiteModification(LocalDate dateLimiteModification) { this.dateLimiteModification = dateLimiteModification; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public boolean isAnnule() { return annule; }
    public void setAnnule(boolean annule) { this.annule = annule; }
}
