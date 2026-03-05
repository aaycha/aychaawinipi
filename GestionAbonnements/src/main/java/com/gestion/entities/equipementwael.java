package com.gestion.entities;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class equipementwael  {
    private Long id;
    private String nom;
    private String description;
    private String categorie;
    private String type; // VENTE / LOCATION
    private BigDecimal prix;
    private String ville;
    private Timestamp dateAjout;
    private String caracteristiques; // JSON: {"places":"4","taille":"GRANDE"} pour tente, etc.
    private Integer nombreVues; // pour statistique "équipement le plus affiché"

    public equipementwael() {}

    // Constructeur pour INSERT
    public equipementwael(String nom, String description, String categorie, String type, BigDecimal prix, String ville) {
        this.nom = nom;
        this.description = description;
        this.categorie = categorie;
        this.type = type;
        this.prix = prix;
        this.ville = ville;
    }

    // Constructeur complet pour SELECT
    public equipementwael(Long id, String nom, String description, String categorie, String type, BigDecimal prix, String ville, Timestamp dateAjout) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.categorie = categorie;
        this.type = type;
        this.prix = prix;
        this.ville = ville;
        this.dateAjout = dateAjout;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public Timestamp getDateAjout() { return dateAjout; }
    public void setDateAjout(Timestamp dateAjout) { this.dateAjout = dateAjout; }

    public String getCaracteristiques() { return caracteristiques; }
    public void setCaracteristiques(String caracteristiques) { this.caracteristiques = caracteristiques; }

    public Integer getNombreVues() { return nombreVues; }
    public void setNombreVues(Integer nombreVues) { this.nombreVues = nombreVues; }

    @Override
    public String toString() {
        return "Equipement{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", categorie='" + categorie + '\'' +
                ", type='" + type + '\'' +
                ", prix=" + prix +
                ", ville='" + ville + '\'' +
                ", dateAjout=" + dateAjout +
                '}';
    }
}
