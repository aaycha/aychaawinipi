package com.gestion.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité pour repas détaillé avec ingrédients, calories, allergènes
 */
public class RepasDetaille {
    
    private Long id;
    private String nom;
    private String description;
    private BigDecimal prix;
    private Integer calories;
    private String typeRepas; // PETIT_DEJEUNER, DEJEUNER, DINER, SNACK
    private LocalDate date;
    private Long participantId;
    private Long evenementId;
    
    // Ingrédients et allergènes
    private List<String> ingredients = new ArrayList<>();
    private List<String> allergenes = new ArrayList<>(); // GLUTEN, LAIT, OEUFS, POISSON, etc.
    private boolean vegetarien = false;
    private boolean vegan = false;
    private boolean sansGluten = false;
    private boolean halal = false;
    
    // Métadonnées
    private boolean actif = true;
    private String imageUrl;
    private String notes;
    
    public RepasDetaille() {}
    
    public RepasDetaille(String nom, String description, BigDecimal prix, Integer calories) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.calories = calories;
    }
    
    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }
    
    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }
    
    public String getTypeRepas() { return typeRepas; }
    public void setTypeRepas(String typeRepas) { this.typeRepas = typeRepas; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }
    
    public Long getEvenementId() { return evenementId; }
    public void setEvenementId(Long evenementId) { this.evenementId = evenementId; }
    
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    
    public List<String> getAllergenes() { return allergenes; }
    public void setAllergenes(List<String> allergenes) { this.allergenes = allergenes; }
    
    public boolean isVegetarien() { return vegetarien; }
    public void setVegetarien(boolean vegetarien) { this.vegetarien = vegetarien; }
    
    public boolean isVegan() { return vegan; }
    public void setVegan(boolean vegan) { this.vegan = vegan; }
    
    public boolean isSansGluten() { return sansGluten; }
    public void setSansGluten(boolean sansGluten) { this.sansGluten = sansGluten; }
    
    public boolean isHalal() { return halal; }
    public void setHalal(boolean halal) { this.halal = halal; }
    
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getIngredientsAsString() {
        return String.join(", ", ingredients);
    }
    
    public String getAllergenesAsString() {
        return String.join(", ", allergenes);
    }
}
