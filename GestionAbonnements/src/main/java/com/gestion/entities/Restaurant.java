package com.gestion.entities;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Entité Restaurant - Représente un restaurant dans le système
 */
public class Restaurant {

    private Long id;

    @NotBlank(message = "Le nom du restaurant est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String nom;

    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    private String adresse;

    @Pattern(regexp = "^[0-9\\s\\+\\-\\(\\)]+$", message = "Le téléphone n'est pas valide")
    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    private String telephone;

    @Email(message = "L'email n'est pas valide")
    @Size(max = 100, message = "L'email ne doit pas dépasser 100 caractères")
    private String email;

    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;

    @Size(max = 255, message = "L'URL de l'image ne doit pas dépasser 255 caractères")
    private String imageUrl;

    private LocalDateTime dateCreation;

    private boolean actif = true;

    public Restaurant() {
        this.dateCreation = LocalDateTime.now();
    }

    public Restaurant(String nom) {
        this();
        this.nom = nom;
    }

    // Validation complète
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        if (nom == null || nom.trim().isEmpty()) {
            result.addError("nom", "Le nom du restaurant est obligatoire. Ex. : La Table du Marché.");
        } else if (nom.length() > 100) {
            result.addError("nom", "Le nom ne doit pas dépasser 100 caractères.");
        }

        if (adresse == null || adresse.trim().isEmpty()) {
            result.addError("adresse", "L'adresse est obligatoire.");
        } else if (adresse.length() > 255) {
            result.addError("adresse", "L'adresse ne doit pas dépasser 255 caractères.");
        }

        if (telephone == null || telephone.trim().isEmpty()) {
            result.addError("telephone", "Le téléphone est obligatoire. Ex. : +33 1 23 45 67 89.");
        } else if (telephone.length() > 20) {
            result.addError("telephone", "Le téléphone ne doit pas dépasser 20 caractères.");
        } else if (!telephone.matches("^[0-9\\s\\+\\-\\(\\)]+$")) {
            result.addError("telephone", "Le téléphone doit contenir uniquement chiffres, espaces, + - ( ).");
        }

        if (email == null || email.trim().isEmpty()) {
            result.addError("email", "L'email est obligatoire. Ex. : contact@restaurant.com.");
        } else if (email.length() > 100) {
            result.addError("email", "L'email ne doit pas dépasser 100 caractères.");
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            result.addError("email", "Format d'email invalide. Ex. : contact@restaurant.com.");
        }

        if (description == null || description.trim().isEmpty()) {
            result.addError("description", "La description est obligatoire.");
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

    // ================= GETTERS / SETTERS =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    @Override
    public String toString() {
        return nom;
    }
}
