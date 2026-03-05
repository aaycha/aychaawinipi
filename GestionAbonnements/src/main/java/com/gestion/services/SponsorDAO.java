package com.gestion.services;

import com.gestion.entities.Sponsor;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SponsorDAO {

    // Ajouter un sponsor
    public void ajouter(Sponsor sponsor) throws SQLException {
        String query = "INSERT INTO Sponsor (nom, telephone, email, logo, statut) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, sponsor.getNom());
            pstmt.setString(2, sponsor.getTelephone());
            pstmt.setString(3, sponsor.getEmail());
            pstmt.setString(4, sponsor.getLogo());
            pstmt.setBoolean(5, sponsor.isStatut());

            pstmt.executeUpdate();

            // Récupérer l'ID généré
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                sponsor.setId(rs.getInt(1));
            }
        }
    }

    // Modifier un sponsor
    public void modifier(Sponsor sponsor) throws SQLException {
        String query = "UPDATE Sponsor SET nom = ?, telephone = ?, email = ?, logo = ?, statut = ? WHERE id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, sponsor.getNom());
            pstmt.setString(2, sponsor.getTelephone());
            pstmt.setString(3, sponsor.getEmail());
            pstmt.setString(4, sponsor.getLogo());
            pstmt.setBoolean(5, sponsor.isStatut());
            pstmt.setInt(6, sponsor.getId());

            pstmt.executeUpdate();
        }
    }

    // Supprimer un sponsor
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM Sponsor WHERE id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Récupérer tous les sponsors
    public List<Sponsor> listerTous() throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();
        String query = "SELECT * FROM Sponsor ORDER BY nom";

        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                sponsors.add(new Sponsor(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("telephone"),
                        rs.getString("email"),
                        rs.getString("logo"),
                        rs.getBoolean("statut")));
            }
        }
        return sponsors;
    }

    // Récupérer les sponsors actifs uniquement
    public List<Sponsor> listerActifs() throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();
        String query = "SELECT * FROM Sponsor WHERE statut = true ORDER BY nom";

        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                sponsors.add(new Sponsor(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("telephone"),
                        rs.getString("email"),
                        rs.getString("logo"),
                        rs.getBoolean("statut")));
            }
        }
        return sponsors;
    }

    // Récupérer un sponsor par ID
    public Sponsor getById(int id) throws SQLException {
        String query = "SELECT * FROM Sponsor WHERE id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Sponsor(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("telephone"),
                        rs.getString("email"),
                        rs.getString("logo"),
                        rs.getBoolean("statut"));
            }
        }
        return null;
    }

    // Récupérer un sponsor par email
    public Sponsor getByEmail(String email) throws SQLException {
        String query = "SELECT * FROM Sponsor WHERE email = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Sponsor(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("telephone"),
                        rs.getString("email"),
                        rs.getString("logo"),
                        rs.getBoolean("statut"));
            }
        }
        return null;
    }

    // Vérifier si un sponsor a des événements associés
    public boolean hasAssociatedEvents(int sponsorId) throws SQLException {
        String query = "SELECT COUNT(*) FROM EventSponsor WHERE sponsor_id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, sponsorId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // Compter le nombre total de sponsors
    public int compter() throws SQLException {
        String query = "SELECT COUNT(*) FROM Sponsor";
        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}