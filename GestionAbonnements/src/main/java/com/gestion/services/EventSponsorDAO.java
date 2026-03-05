package com.gestion.services;

import com.gestion.entities.EventSponsor;
import com.gestion.entities.EventStats;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventSponsorDAO {

    public EventSponsorDAO() {
        // Self-healing: ensure 'id' is PRIMARY KEY and AUTO_INCREMENT
        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement()) {

            // Try to make it a primary key first (if not already)
            try {
                stmt.execute("ALTER TABLE EventSponsor ADD PRIMARY KEY (id);");
            } catch (SQLException ignored) {
            }

            // Then ensure AUTO_INCREMENT
            stmt.execute("ALTER TABLE EventSponsor MODIFY id INT AUTO_INCREMENT;");
            System.out.println("EventSponsor schema verified (id is AUTO_INCREMENT)");
        } catch (SQLException e) {
            System.err.println("Migration warning while verifying EventSponsor schema: " + e.getMessage());
        }
    }

    // Ajouter une association
    public void ajouter(EventSponsor eventSponsor) throws SQLException {
        String query = "INSERT INTO EventSponsor (event_id, sponsor_id, niveau, montant) VALUES (?, ?, ?, ?)";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, eventSponsor.getEventId());
            pstmt.setInt(2, eventSponsor.getSponsorId());
            pstmt.setString(3, eventSponsor.getNiveau());
            pstmt.setDouble(4, eventSponsor.getMontant());

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                eventSponsor.setId(rs.getInt(1));
            }
        }
    }

    // Modifier une association
    public void modifier(EventSponsor eventSponsor) throws SQLException {
        String query = "UPDATE EventSponsor SET niveau = ?, montant = ? WHERE id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, eventSponsor.getNiveau());
            pstmt.setDouble(2, eventSponsor.getMontant());
            pstmt.setInt(3, eventSponsor.getId());

            pstmt.executeUpdate();
        }
    }

    // Supprimer une association
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM EventSponsor WHERE id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Supprimer toutes les associations d'un événement
    public void supprimerParEvenement(int eventId) throws SQLException {
        String query = "DELETE FROM EventSponsor WHERE event_id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, eventId);
            pstmt.executeUpdate();
        }
    }

    // Supprimer toutes les associations d'un sponsor
    public void supprimerParSponsor(int sponsorId) throws SQLException {
        String query = "DELETE FROM EventSponsor WHERE sponsor_id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, sponsorId);
            pstmt.executeUpdate();
        }
    }

    // Récupérer toutes les associations avec les noms
    public List<EventSponsor> listerTous() throws SQLException {
        List<EventSponsor> associations = new ArrayList<>();
        String query = "SELECT es.*, e.titre as event_nom, s.nom as sponsor_nom " +
                "FROM EventSponsor es " +
                "JOIN Evenement e ON es.event_id = e.id_event " +
                "JOIN Sponsor s ON es.sponsor_id = s.id " +
                "ORDER BY e.date_debut DESC, es.niveau";

        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                associations.add(new EventSponsor(
                        rs.getInt("id"),
                        rs.getString("event_nom"),
                        rs.getString("sponsor_nom"),
                        rs.getString("niveau"),
                        rs.getDouble("montant")));
            }
        }
        return associations;
    }

    // Récupérer les associations par événement
    public List<EventSponsor> getByEvenement(int eventId) throws SQLException {
        List<EventSponsor> associations = new ArrayList<>();
        String query = "SELECT es.*, e.titre as event_nom, s.nom as sponsor_nom " +
                "FROM EventSponsor es " +
                "JOIN Evenement e ON es.event_id = e.id_event " +
                "JOIN Sponsor s ON es.sponsor_id = s.id " +
                "WHERE es.event_id = ? " +
                "ORDER BY es.niveau";

        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                associations.add(new EventSponsor(
                        rs.getInt("id"),
                        rs.getString("event_nom"),
                        rs.getString("sponsor_nom"),
                        rs.getString("niveau"),
                        rs.getDouble("montant")));
            }
        }
        return associations;
    }

    // Récupérer les associations par sponsor
    public List<EventSponsor> getBySponsor(int sponsorId) throws SQLException {
        List<EventSponsor> associations = new ArrayList<>();
        String query = "SELECT es.*, e.titre as event_nom, s.nom as sponsor_nom " +
                "FROM EventSponsor es " +
                "JOIN Evenement e ON es.event_id = e.id_event " +
                "JOIN Sponsor s ON es.sponsor_id = s.id " +
                "WHERE es.sponsor_id = ? " +
                "ORDER BY e.date_debut DESC";

        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, sponsorId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                associations.add(new EventSponsor(
                        rs.getInt("id"),
                        rs.getString("event_nom"),
                        rs.getString("sponsor_nom"),
                        rs.getString("niveau"),
                        rs.getDouble("montant")));
            }
        }
        return associations;
    }

    // Récupérer une association par ID
    public EventSponsor getById(int id) throws SQLException {
        String query = "SELECT es.*, e.titre as event_nom, s.nom as sponsor_nom " +
                "FROM EventSponsor es " +
                "JOIN Evenement e ON es.event_id = e.id_event " +
                "JOIN Sponsor s ON es.sponsor_id = s.id " +
                "WHERE es.id = ?";

        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new EventSponsor(
                        rs.getInt("id"),
                        rs.getString("event_nom"),
                        rs.getString("sponsor_nom"),
                        rs.getString("niveau"),
                        rs.getDouble("montant"));
            }
        }
        return null;
    }

    // Vérifier si une association existe déjà
    public boolean existe(int eventId, int sponsorId) throws SQLException {
        String query = "SELECT COUNT(*) FROM EventSponsor WHERE event_id = ? AND sponsor_id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, eventId);
            pstmt.setInt(2, sponsorId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // Statistiques : totaux par événement
    public List<EventStats> getTotalsParEvenement() throws SQLException {
        List<EventStats> stats = new ArrayList<>();
        String query = "SELECT e.titre AS event_nom, SUM(es.montant) AS total_montant, COUNT(*) AS nb_assoc " +
                "FROM EventSponsor es " +
                "JOIN Evenement e ON es.event_id = e.id_event " +
                "GROUP BY es.event_id, e.titre " +
                "ORDER BY total_montant DESC";

        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                stats.add(new EventStats(
                        rs.getString("event_nom"),
                        rs.getDouble("total_montant"),
                        rs.getInt("nb_assoc")));
            }
        }
        return stats;
    }

    // Calculer le montant total des contributions par événement
    public double getTotalContributionsParEvenement(int eventId) throws SQLException {
        String query = "SELECT SUM(montant) FROM EventSponsor WHERE event_id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0;
    }

    // Compter le nombre d'associations
    public int compter() throws SQLException {
        String query = "SELECT COUNT(*) FROM EventSponsor";
        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // Calculer le montant total des contributions pour un sponsor donné
    public double getTotalContributionsParSponsor(int sponsorId) throws SQLException {
        String query = "SELECT SUM(montant) FROM EventSponsor WHERE sponsor_id = ?";
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, sponsorId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0;
    }
}