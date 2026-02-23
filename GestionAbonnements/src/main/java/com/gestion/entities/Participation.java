package com.gestion.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité représentant une participation à un événement
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Participation {

    public enum TypeParticipation {
        SIMPLE("Simple"),
        HEBERGEMENT("Avec hébergement"),
        GROUPE("Groupe");

        private final String label;

        TypeParticipation(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum StatutParticipation {
        EN_ATTENTE("En attente"),
        CONFIRME("Confirmé"),
        ANNULE("Annulé"),
        EN_LISTE_ATTENTE("En liste d'attente");

        private final String label;

        StatutParticipation(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum ContexteSocial {
        COUPLE("Couple"),
        AMIS("Amis"),
        FAMILLE("Famille"),
        SOLO("Solo"),
        PROFESSIONNEL("Professionnel");

        private final String label;

        ContexteSocial(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum MealOption {
        SANS_REPAS("Sans repas"),
        AVEC_REPAS("Avec repas"),
        AVEC_MENU("Avec menu complet"),
        COMPOSITION_SUR_PLACE("Composition sur place");

        private final String label;

        MealOption(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private Long id;
    private Long userId;
    private Long evenementId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime dateInscription;
    private TypeParticipation type;
    private StatutParticipation statut;
    private int hebergementNuits;
    private ContexteSocial contexteSocial;
    private String badgeAssocie;
    private int nbAdultes;
    private int nbEnfants;
    private int nbChiens;
    private int totalParticipants;
    private String typeAbonnementChoisi;
    private BigDecimal montantCalcule;
    private String devise = "EUR";
    private String commentaire;
    private String besoinsSpeciaux;
    private MealOption mealOption;
    private int pointsEarned;
    private Long abonnementId;

    public Participation() {
    }

    public Participation(Long userId, Long evenementId, TypeParticipation type, ContexteSocial contexteSocial) {
        this.userId = userId;
        this.evenementId = evenementId;
        this.type = type;
        this.contexteSocial = contexteSocial;
        this.dateInscription = LocalDateTime.now();
        this.statut = StatutParticipation.EN_ATTENTE;
        this.hebergementNuits = type == TypeParticipation.HEBERGEMENT ? 1 : 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(Long evenementId) {
        this.evenementId = evenementId;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public TypeParticipation getType() {
        return type;
    }

    public void setType(TypeParticipation type) {
        this.type = type;
    }

    public StatutParticipation getStatut() {
        return statut;
    }

    public void setStatut(StatutParticipation statut) {
        this.statut = statut;
    }

    public int getHebergementNuits() {
        return hebergementNuits;
    }

    public void setHebergementNuits(int hebergementNuits) {
        this.hebergementNuits = hebergementNuits;
    }

    public ContexteSocial getContexteSocial() {
        return contexteSocial;
    }

    public void setContexteSocial(ContexteSocial contexteSocial) {
        this.contexteSocial = contexteSocial;
    }

    public String getBadgeAssocie() {
        return badgeAssocie;
    }

    public void setBadgeAssocie(String badgeAssocie) {
        this.badgeAssocie = badgeAssocie;
    }

    public int getNbAdultes() {
        return nbAdultes;
    }

    public void setNbAdultes(int nbAdultes) {
        this.nbAdultes = nbAdultes;
    }

    public int getNbEnfants() {
        return nbEnfants;
    }

    public void setNbEnfants(int nbEnfants) {
        this.nbEnfants = nbEnfants;
    }

    public int getNbChiens() {
        return nbChiens;
    }

    public void setNbChiens(int nbChiens) {
        this.nbChiens = nbChiens;
    }

    public int getTotalParticipants() {
        return totalParticipants;
    }

    public void setTotalParticipants(int totalParticipants) {
        this.totalParticipants = totalParticipants;
    }

    public String getTypeAbonnementChoisi() {
        return typeAbonnementChoisi;
    }

    public void setTypeAbonnementChoisi(String typeAbonnementChoisi) {
        this.typeAbonnementChoisi = typeAbonnementChoisi;
    }

    public BigDecimal getMontantCalcule() {
        return montantCalcule;
    }

    public void setMontantCalcule(BigDecimal montantCalcule) {
        this.montantCalcule = montantCalcule;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public String getBesoinsSpeciaux() {
        return besoinsSpeciaux;
    }

    public void setBesoinsSpeciaux(String besoinsSpeciaux) {
        this.besoinsSpeciaux = besoinsSpeciaux;
    }

    public MealOption getMealOption() {
        return mealOption;
    }

    public void setMealOption(MealOption mealOption) {
        this.mealOption = mealOption;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public Long getAbonnementId() {
        return abonnementId;
    }

    public void setAbonnementId(Long abonnementId) {
        this.abonnementId = abonnementId;
    }

    public void confirmer() {
        this.statut = StatutParticipation.CONFIRME;
        this.badgeAssocie = attribuerBadge();
    }

    public void annuler() {
        this.statut = StatutParticipation.ANNULE;
    }

    private String attribuerBadge() {
        if (contexteSocial == null)
            return "Explorateur_Standard";
        switch (contexteSocial) {
            case COUPLE:
                return "Romantique_Aventure";
            case AMIS:
                return "Esprit_Equipe";
            case FAMILLE:
                return "Famille_Unie";
            case SOLO:
                return "Explorateur_Solitaire";
            case PROFESSIONNEL:
                return "Pro_Leadership";
            default:
                return "Explorateur_Standard";
        }
    }
}
