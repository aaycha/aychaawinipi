package com.gestion.services;

import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageReactionService {
    private final Connection cnx;

    public MessageReactionService() {
        cnx = MyConnection.getInstance().getConnection();
        creerTableSiNexistePas();
    }

    private void creerTableSiNexistePas() {
        String sql = "CREATE TABLE IF NOT EXISTS message_reactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "id_message INT NOT NULL, " +
                "id_user INT NOT NULL, " +
                "emoji VARCHAR(10) NOT NULL, " +
                "date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_reaction (id_message, id_user, emoji)" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
            System.out.println("✅ Table message_reactions vérifiée");
        } catch (SQLException e) {
            System.out.println("Erreur création table message_reactions: " + e.getMessage());
        }
    }

    /**
     * Ajoute une réaction à un message (or toggle si existe déjà)
     */
    public void addReaction(int messageId, int userId, String emoji) {
        // Vérifier si la réaction existe
        String checkSql = "SELECT id FROM message_reactions WHERE id_message = ? AND id_user = ? AND emoji = ?";
        try (PreparedStatement check = cnx.prepareStatement(checkSql)) {
            check.setInt(1, messageId);
            check.setInt(2, userId);
            check.setString(3, emoji);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                // Réaction existe, supprimer (toggle)
                removeReaction(messageId, userId, emoji);
            } else {
                // Ajouter la réaction
                String insertSql = "INSERT INTO message_reactions (id_message, id_user, emoji) VALUES (?, ?, ?)";
                try (PreparedStatement ps = cnx.prepareStatement(insertSql)) {
                    ps.setInt(1, messageId);
                    ps.setInt(2, userId);
                    ps.setString(3, emoji);
                    ps.executeUpdate();
                    System.out.println("✅ Réaction ajoutée: emoji=" + emoji + " msg=" + messageId);
                } catch (SQLException ex) {
                    System.out.println("❌ Erreur ajout réaction: " + ex.getMessage());
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur vérification réaction: " + e.getMessage());
        }
    }

    /**
     * Supprime une réaction
     */
    public void removeReaction(int messageId, int userId, String emoji) {
        String sql = "DELETE FROM message_reactions WHERE id_message = ? AND id_user = ? AND emoji = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            ps.setString(3, emoji);
            ps.executeUpdate();
            System.out.println("✅ Réaction supprimée: emoji=" + emoji);
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression réaction: " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les réactions pour un message
     * Retourne une Map<emoji, count>
     */
    public Map<String, Integer> getReactionsForMessage(int messageId) {
        Map<String, Integer> reactions = new HashMap<>();
        String sql = "SELECT emoji, COUNT(*) as count FROM message_reactions WHERE id_message = ? GROUP BY emoji";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String emoji = rs.getString("emoji");
                int count = rs.getInt("count");
                reactions.put(emoji, count);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lecture réactions: " + e.getMessage());
        }
        return reactions;
    }

    /**
     * Vérifie si un utilisateur a déjà réagi avec cet emoji
     */
    public boolean hasUserReacted(int messageId, int userId, String emoji) {
        String sql = "SELECT id FROM message_reactions WHERE id_message = ? AND id_user = ? AND emoji = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            ps.setString(3, emoji);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Récupère les utilisateurs qui ont réagi avec un emoji donné
     */
    public List<Integer> getUsersWhoReacted(int messageId, String emoji) {
        List<Integer> userIds = new ArrayList<>();
        String sql = "SELECT id_user FROM message_reactions WHERE id_message = ? AND emoji = ? ORDER BY date_ajout";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setString(2, emoji);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                userIds.add(rs.getInt("id_user"));
            }
        } catch (SQLException e) {
            System.out.println("Erreur lecture utilisateurs réactions: " + e.getMessage());
        }
        return userIds;
    }
}
