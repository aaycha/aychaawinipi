package com.gestion.entities;

import java.sql.Timestamp;

public class MessageChat {
    private int id;
    private String contenu;
    private Timestamp dateEnvoi;
    private int idGroupe;
    private int idUser; // NEW
    private String typeMessage; // TEXT, IMAGE, PDF, AUDIO, VIDEO, LOCATION
    private String fichierPath;
    private Double latitude;
    private Double longitude;

    public MessageChat() {
    }

    // Pour INSERT
    public MessageChat(String contenu, int idGroupe, int idUser) {
        this.contenu = contenu != null ? contenu : "";
        this.idGroupe = idGroupe;
        this.idUser = idUser;
        this.typeMessage = "TEXT";
    }

    // Pour SELECT
    public MessageChat(int id, String contenu, Timestamp dateEnvoi, int idGroupe, int idUser) {
        this.id = id;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.idGroupe = idGroupe;
        this.idUser = idUser;
        this.typeMessage = "TEXT";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Timestamp getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(Timestamp dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public int getIdGroupe() {
        return idGroupe;
    }

    public void setIdGroupe(int idGroupe) {
        this.idGroupe = idGroupe;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getTypeMessage() {
        return typeMessage;
    }

    public void setTypeMessage(String typeMessage) {
        this.typeMessage = typeMessage;
    }

    public String getFichierPath() {
        return fichierPath;
    }

    public void setFichierPath(String fichierPath) {
        this.fichierPath = fichierPath;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "MessageChat{" +
                "id=" + id +
                ", contenu='" + contenu + '\'' +
                ", dateEnvoi=" + dateEnvoi +
                ", idGroupe=" + idGroupe +
                ", idUser=" + idUser +
                '}';
    }
}