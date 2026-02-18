package com.gestion.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité Repas (Plat) - Représente un plat dans le système
 */
public class Repas {

    public enum TypePlat {
        VIANDE("Viande"),
        POISSON("Poisson"),
        VEGETARIEN("Végétarien"),
        VEGETALIEN("Végétalien"),
        AUTRE("Autre");

        private final String label;

        TypePlat(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Categorie {
        ENTREE("Entrée"),
        PLAT_PRINCIPAL("Plat principal"),
        DESSERT("Dessert"),
        BOISSON("Boisson"),
        ACCOMPAGNEMENT("Accompagnement");

        private final String label;

        Categorie(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private Long id;
    private Long restaurantId;
    private String restaurantNom; // Pour affichage
    private Long menuId;
    private String menuNom; // Pour affichage

    private String nom;
    private String description;
    private BigDecimal prix;
    private Categorie categorie;
    private TypePlat typePlat;
    private Integer tempsPreparation; // en minutes
    private String imageUrl;
    private boolean disponible = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Repas() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Validation complète
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        if (nom == null || nom.trim().isEmpty()) {
            result.addError("nom", "Le nom du plat est obligatoire. Saisissez par ex. : Mloukhia, Pizza 4 fromages.");
        } else if (nom.length() > 120) {
            result.addError("nom", "Le nom ne doit pas dépasser 120 caractères.");
        }

        if (restaurantId == null) {
            result.addError("restaurant", "Le restaurant est obligatoire pour créer le plat.");
        }
        if (menuId == null) {
            result.addError("menu", "Le menu est obligatoire pour créer le plat.");
        }

        if (prix == null) {
            result.addError("prix", "Le prix est obligatoire (ex. 12.50).");
        } else if (prix.compareTo(BigDecimal.ZERO) < 0) {
            result.addError("prix", "Le prix ne peut pas être négatif. Saisissez un nombre ≥ 0 (ex. 12.50).");
        } else if (prix.compareTo(new BigDecimal("999999.99")) > 0) {
            result.addError("prix", "Le prix est trop élevé. Maximum 999 999,99 €.");
        }

        if (categorie == null) {
            result.addError("categorie", "La catégorie est obligatoire.");
        }

        if (typePlat == null) {
            result.addError("typePlat", "Le type de plat est obligatoire.");
        }

        if (tempsPreparation == null) {
            result.addError("tempsPreparation", "Le temps de préparation est obligatoire (0 à 1440 minutes).");
        } else if (tempsPreparation < 0) {
            result.addError("tempsPreparation", "Le temps de préparation ne peut pas être négatif.");
        } else if (tempsPreparation > 1440) {
            result.addError("tempsPreparation", "Le temps de préparation ne peut pas dépasser 1440 minutes.");
        }

        if (description == null || description.trim().isEmpty()) {
            result.addError("description", "La description est obligatoire (max 1000 caractères).");
        } else if (description.length() > 1000) {
            result.addError("description", "La description ne doit pas dépasser 1000 caractères.");
        }

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            result.addError("imageUrl", "L'URL de l'image est obligatoire.");
        } else if (imageUrl.length() > 255) {
            result.addError("imageUrl", "L'URL de l'image ne doit pas dépasser 255 caractères.");
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

    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }

    public String getMenuNom() { return menuNom; }
    public void setMenuNom(String menuNom) { this.menuNom = menuNom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }

    public TypePlat getTypePlat() { return typePlat; }
    public void setTypePlat(TypePlat typePlat) { this.typePlat = typePlat; }

    public Integer getTempsPreparation() { return tempsPreparation; }
    public void setTempsPreparation(Integer tempsPreparation) { this.tempsPreparation = tempsPreparation; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return nom + (prix != null ? " (" + prix + " €)" : "");
    }
}
