package com.gestion.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entité unifiée pour la restauration : Menu, Option, Repas, Restriction, Presence.
 * Le champ {@code type} détermine quels attributs sont utilisés.
 */
public class Restauration {

    public enum TypeRestauration {
        MENU, OPTION, REPAS, RESTRICTION, PRESENCE
    }

    private Long id;
    private TypeRestauration type;

    // Commun
    private Long participantId;
    private Long evenementId;
    private boolean actif = true;

    // Menu / Option
    private String nom;
    private Long optionRestaurationId;
    private String libelle;
    private String typeEvenement;

    // Repas
    private String nomRepas;
    private BigDecimal prix;
    private LocalDate date;

    // Restriction
    private String restrictionLibelle;
    private String restrictionDescription;

    // Presence
    private LocalDate datePresence;
    private boolean abonnementActif;

    public Restauration() {}

    public Restauration(TypeRestauration type) {
        this.type = type;
    }

    // ================= FACTORIES STREAM =================

    public static Restauration menu(String nom, Long optionRestaurationId, boolean actif) {
        Restauration r = new Restauration(TypeRestauration.MENU);
        r.setNom(nom);
        r.setOptionRestaurationId(optionRestaurationId);
        r.setActif(actif);
        return r;
    }

    public static Restauration option(String libelle, String typeEvenement, boolean actif) {
        Restauration r = new Restauration(TypeRestauration.OPTION);
        r.setLibelle(libelle);
        r.setTypeEvenement(typeEvenement);
        r.setActif(actif);
        return r;
    }

    public static Restauration repas(String nomRepas, BigDecimal prix, LocalDate date, Long participantId) {
        Restauration r = new Restauration(TypeRestauration.REPAS);
        r.setNomRepas(nomRepas);
        r.setPrix(prix);
        r.setDate(date);
        r.setParticipantId(participantId);
        return r;
    }

    public static Restauration restriction(String libelle, String description, boolean actif) {
        Restauration r = new Restauration(TypeRestauration.RESTRICTION);
        r.setRestrictionLibelle(libelle);
        r.setRestrictionDescription(description);
        r.setActif(actif);
        return r;
    }

    public static Restauration presence(Long participantId, LocalDate date, boolean abonnementActif) {
        Restauration r = new Restauration(TypeRestauration.PRESENCE);
        r.setParticipantId(participantId);
        r.setDatePresence(date);
        r.setAbonnementActif(abonnementActif);
        return r;
    }

    // ================= GETTERS / SETTERS =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TypeRestauration getType() { return type; }
    public void setType(TypeRestauration type) { this.type = type; }

    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public Long getEvenementId() { return evenementId; }
    public void setEvenementId(Long evenementId) { this.evenementId = evenementId; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public Long getOptionRestaurationId() { return optionRestaurationId; }
    public void setOptionRestaurationId(Long optionRestaurationId) { this.optionRestaurationId = optionRestaurationId; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getTypeEvenement() { return typeEvenement; }
    public void setTypeEvenement(String typeEvenement) { this.typeEvenement = typeEvenement; }

    public String getNomRepas() { return nomRepas; }
    public void setNomRepas(String nomRepas) { this.nomRepas = nomRepas; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getRestrictionLibelle() { return restrictionLibelle; }
    public void setRestrictionLibelle(String restrictionLibelle) { this.restrictionLibelle = restrictionLibelle; }

    public String getRestrictionDescription() { return restrictionDescription; }
    public void setRestrictionDescription(String restrictionDescription) { this.restrictionDescription = restrictionDescription; }

    public LocalDate getDatePresence() { return datePresence; }
    public void setDatePresence(LocalDate datePresence) { this.datePresence = datePresence; }

    public boolean isAbonnementActif() { return abonnementActif; }
    public void setAbonnementActif(boolean abonnementActif) { this.abonnementActif = abonnementActif; }
}
