package com.gestion.entities;

public class EventStats {
    private final String nomEvenement;
    private final double totalMontant;
    private final int nbAssociations;

    public EventStats(String nomEvenement, double totalMontant, int nbAssociations) {
        this.nomEvenement = nomEvenement;
        this.totalMontant = totalMontant;
        this.nbAssociations = nbAssociations;
    }

    public String getNomEvenement() {
        return nomEvenement;
    }

    public double getTotalMontant() {
        return totalMontant;
    }

    public int getNbAssociations() {
        return nbAssociations;
    }
}

