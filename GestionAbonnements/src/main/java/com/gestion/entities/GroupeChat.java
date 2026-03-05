package com.gestion.entities;

import java.sql.Timestamp;

public class GroupeChat {
    private int id;
    private String nom;
    private String description;
    private String type; // PUBLIC / PRIVATE / ...
    private Timestamp dateCreation;
    private int idCreateur=1; // NEW

    public GroupeChat() {}

    public GroupeChat(String nom, String description, String type, int idCreateur) {
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.idCreateur = idCreateur;
    }

    public GroupeChat(int id, String nom, String description, String type, Timestamp dateCreation, int idCreateur) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.dateCreation = dateCreation;
        this.idCreateur = idCreateur;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }

    public int getIdCreateur() { return idCreateur; }
    public void setIdCreateur(int idCreateur) { this.idCreateur = idCreateur; }

    @Override
    public String toString() {
        return "GroupeChat{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", dateCreation=" + dateCreation +
                ", idCreateur=" + idCreateur +
                '}';
    }
}