package com.gestion.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité Menu - Représente un menu dans un restaurant
 */
public class Menu {

    private Long id;
    private Long restaurantId;
    private String restaurantNom; // Pour affichage

    private String nom;
    private String description;
    private BigDecimal prix;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private boolean actif = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Menu() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Validation complète
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        if (nom == null || nom.trim().isEmpty()) {
            result.addError("nom", "Le nom du menu est obligatoire. Ex. : Menu du Jour, Menu Ramadan.");
        } else if (nom.length() > 100) {
            result.addError("nom", "Le nom ne doit pas dépasser 100 caractères.");
        }

        if (restaurantId == null) {
            result.addError("restaurant", "Vous devez sélectionner un restaurant pour ce menu.");
        }

        if (prix == null) {
            result.addError("prix", "Le prix est obligatoire. Saisissez un montant en € (ex. 15.00).");
        } else if (prix.compareTo(BigDecimal.ZERO) < 0) {
            result.addError("prix", "Le prix ne peut pas être négatif.");
        } else if (prix.compareTo(new BigDecimal("999999.99")) > 0) {
            result.addError("prix", "Le prix maximum autorisé est 999 999,99 €.");
        }

        if (description == null || description.trim().isEmpty()) {
            result.addError("description", "La description est obligatoire (max 500 caractères).");
        } else if (description.length() > 500) {
            result.addError("description", "La description ne doit pas dépasser 500 caractères.");
        }

        if (dateDebut == null) {
            result.addError("dates", "La date de début est obligatoire.");
        }
        if (dateFin == null) {
            result.addError("dates", "La date de fin est obligatoire.");
        }
        if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
            result.addError("dates", "La date de début doit être antérieure à la date de fin.");
        }

        return result;
    }

    // Met à jour les timestamps
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ================= GETTERS / SETTERS =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public String getRestaurantNom() { return restaurantNom; }
    public void setRestaurantNom(String restaurantNom) { this.restaurantNom = restaurantNom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return nom + (prix != null ? " (" + prix + " €)" : "");
    }
}
