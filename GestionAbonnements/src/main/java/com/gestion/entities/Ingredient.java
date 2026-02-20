package com.gestion.entities;

import java.math.BigDecimal;

/**
 * Entité pour un ingrédient individuel (base, protéine, légume, sauce, topping)
 */
public class Ingredient {
    public enum Categorie {
        BASE, PROTEINE, LEGUME, SAUCE, TOPPING, EXTRA
    }

    private Long id;
    private String nom;
    private Categorie categorie;
    private BigDecimal prixSupplement;
    private Integer calories;
    private String iconUrl;
    private boolean actif = true;

    public Ingredient() {
    }

    public Ingredient(String nom, Categorie categorie, BigDecimal prixSupplement, Integer calories) {
        this.nom = nom;
        this.categorie = categorie;
        this.prixSupplement = prixSupplement;
        this.calories = calories;
    }

    // Getters / Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public BigDecimal getPrixSupplement() {
        return prixSupplement;
    }

    public void setPrixSupplement(BigDecimal prixSupplement) {
        this.prixSupplement = prixSupplement;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Ingredient that = (Ingredient) o;
        if (id != null && that.id != null)
            return id.equals(that.id);
        return nom != null ? nom.equals(that.nom) : that.nom == null;
    }

    @Override
    public int hashCode() {
        if (id != null)
            return id.hashCode();
        return nom != null ? nom.hashCode() : 0;
    }

    @Override
    public String toString() {
        return nom + " (+" + prixSupplement + " €)";
    }
}
