package com.gestion.entities;

public class EventSponsor {
    private int id;
    private int eventId;
    private int sponsorId;
    private String niveau; // GOLD, SILVER, BRONZE, PARTENAIRE
    private double montant;
    private String dateAssociation;

    // Pour l'affichage (jointure)
    private String nomEvenement;
    private String nomSponsor;

    public EventSponsor() {}

    public EventSponsor(int id, int eventId, int sponsorId, String niveau, double montant, String dateAssociation) {
        this.id = id;
        this.eventId = eventId;
        this.sponsorId = sponsorId;
        this.niveau = niveau;
        this.montant = montant;
        this.dateAssociation = dateAssociation;
    }

    // Constructeur avec noms pour l'affichage
    public EventSponsor(int id, String nomEvenement, String nomSponsor, String niveau, double montant) {
        this.id = id;
        this.nomEvenement = nomEvenement;
        this.nomSponsor = nomSponsor;
        this.niveau = niveau;
        this.montant = montant;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public int getSponsorId() { return sponsorId; }
    public void setSponsorId(int sponsorId) { this.sponsorId = sponsorId; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getDateAssociation() { return dateAssociation; }
    public void setDateAssociation(String dateAssociation) { this.dateAssociation = dateAssociation; }

    public String getNomEvenement() { return nomEvenement; }
    public void setNomEvenement(String nomEvenement) { this.nomEvenement = nomEvenement; }

    public String getNomSponsor() { return nomSponsor; }
    public void setNomSponsor(String nomSponsor) { this.nomSponsor = nomSponsor; }
}