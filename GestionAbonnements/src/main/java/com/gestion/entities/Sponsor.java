package com.gestion.entities;

public class Sponsor {
    private int id;
    private String nom;
    private String telephone;
    private String email;
    private String logo;
    private boolean statut;

    public Sponsor() {}

    public Sponsor(int id, String nom, String telephone, String email, String logo, boolean statut) {
        this.id = id;
        this.nom = nom;
        this.telephone = telephone;
        this.email = email;
        this.logo = logo;
        this.statut = statut;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public boolean isStatut() { return statut; }
    public void setStatut(boolean statut) { this.statut = statut; }

    @Override
    public String toString() {
        return nom;
    }
}