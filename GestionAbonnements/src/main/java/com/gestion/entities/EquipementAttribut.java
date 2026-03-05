package com.gestion.entities;

/**
 * Attribut spécifique par type d'équipement (Matelas: épaisseur, Lunettes: UV, etc.)
 * Stocké dans la table equipement_attributs.
 */
public class EquipementAttribut {
    private int id;
    private long equipementId;
    private String nomAttribut;
    private String valeur;
    private String description; // nouvelle colonne pour expliquer l'attribut

    public EquipementAttribut() {}

    public EquipementAttribut(long equipementId, String nomAttribut, String valeur) {
        this.equipementId = equipementId;
        this.nomAttribut = nomAttribut;
        this.valeur = valeur;
    }

    public EquipementAttribut(int id, long equipementId, String nomAttribut, String valeur) {
        this.id = id;
        this.equipementId = equipementId;
        this.nomAttribut = nomAttribut;
        this.valeur = valeur;
    }

    public EquipementAttribut(int id, long equipementId, String nomAttribut, String valeur, String description) {
        this.id = id;
        this.equipementId = equipementId;
        this.nomAttribut = nomAttribut;
        this.valeur = valeur;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public long getEquipementId() { return equipementId; }
    public void setEquipementId(long equipementId) { this.equipementId = equipementId; }
    public String getNomAttribut() { return nomAttribut; }
    public void setNomAttribut(String nomAttribut) { this.nomAttribut = nomAttribut; }
    public String getValeur() { return valeur; }
    public void setValeur(String valeur) { this.valeur = valeur; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
