package com.gestion.services;

import com.gestion.entities.Equipment;
import com.gestion.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EquipmentService {

    private final Connection cnx;

    public EquipmentService() {
        cnx = MyConnection.getInstance().getConnection(); // ✅ ta méthode
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS equipment (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "event_id INT NOT NULL, " +
                "libelle VARCHAR(255) NOT NULL, " +
                "FOREIGN KEY (event_id) REFERENCES evenement(id_event) ON DELETE CASCADE" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);
            // ✅ S'assurer que 'id' est AUTO_INCREMENT (fix pour erreur 'no default value')
            try {
                st.executeUpdate("ALTER TABLE equipment MODIFY id INT AUTO_INCREMENT");
            } catch (SQLException ignored) {
                // Déjà fait ou erreur mineure
            }
        } catch (SQLException ex) {
            System.err.println("❌ EquipmentService.ensureTableExists: " + ex.getMessage());
        }
    }

    public void add(Equipment e) {
        String sql = "INSERT INTO equipment (event_id, libelle) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, e.getEventId());
            ps.setString(2, e.getLibelle());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("❌ EquipmentService.add: " + ex.getMessage());
        }
    }

    public void addAll(int eventId, List<String> libelles) {
        if (libelles == null || libelles.isEmpty())
            return;

        String sql = "INSERT INTO equipment (event_id, libelle) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (String lib : libelles) {
                if (lib == null || lib.trim().isBlank())
                    continue;
                ps.setInt(1, eventId);
                ps.setString(2, lib.trim());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            System.err.println("❌ EquipmentService.addAll: " + ex.getMessage());
        }
    }

    public List<Equipment> getByEventId(int eventId) {
        List<Equipment> list = new ArrayList<>();
        String sql = "SELECT id, event_id, libelle FROM equipment WHERE event_id = ? ORDER BY id";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Equipment e = new Equipment();
                    e.setId(rs.getInt("id"));
                    e.setEventId(rs.getInt("event_id"));
                    e.setLibelle(rs.getString("libelle"));
                    list.add(e);
                }
            }

        } catch (SQLException ex) {
            System.err.println("❌ EquipmentService.getByEventId: " + ex.getMessage());
        }

        return list;
    }

    public void deleteByEventId(int eventId) {
        String sql = "DELETE FROM equipment WHERE event_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("❌ EquipmentService.deleteByEventId: " + ex.getMessage());
        }
    }

    /** Suggestions d'équipements selon le type d'événement (Java 11 compatible) */
    public static List<String> getSuggestionsByType(String type) {
        if (type == null || type.isBlank())
            return List.of();

        String t = type.trim().toUpperCase();
        switch (t) {
            case "CAMPING":
                return List.of(
                        "Tente", "Sac de couchage", "Matelas gonflable", "Lampe de poche",
                        "Réchaud de camping", "Gourde", "Couteau de poche", "Trousse de premiers secours");

            case "RANDONNEE":
                return List.of(
                        "Chaussures de marche", "Sac à dos", "Bâtons de marche", "Gourde",
                        "Lunettes de soleil", "Crème solaire", "Casquette", "Trousse de premiers secours");

            case "SOIREE":
                return List.of(
                        "Dress code respecté", "Déguisement", "Maquillage spécial", "Accessoires",
                        "Chaussures confortables", "Veste légère");

            case "SEJOUR":
                return List.of(
                        "Valise / Sac de voyage", "Trousse de toilette", "Adaptateur", "Documents");

            default:
                return List.of();
        }
    }
}