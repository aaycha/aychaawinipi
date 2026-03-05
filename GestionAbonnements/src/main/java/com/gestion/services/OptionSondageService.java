package com.gestion.services;

import com.gestion.entities.OptionSondage;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OptionSondageService {
    private final Connection cnx;

    public OptionSondageService() {
        cnx = MyConnection.getInstance().getConnection();
        creerTableSiNexistePas();
    }

    private void creerTableSiNexistePas() {
        String sql = "CREATE TABLE IF NOT EXISTS sondage_options (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "message_id INT NOT NULL, " +
                "option_text VARCHAR(255) NOT NULL, " +
                "votes INT DEFAULT 0" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
            // table votes pour enregistrer qui a voté
            String sqlVotes = "CREATE TABLE IF NOT EXISTS sondage_votes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "option_id INT NOT NULL, " +
                    "user_id INT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            try {
                st.execute(sqlVotes);
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            System.err.println("Erreur création table sondage_options: " + e.getMessage());
        }
    }

    public void ajouterList(int messageId, List<String> options) {
        String sql = "INSERT INTO sondage_options (message_id, option_text, votes) VALUES (?, ?, 0)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (String opt : options) {
                ps.setInt(1, messageId);
                ps.setString(2, opt);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<OptionSondage> getOptionsForMessage(int messageId) {
        List<OptionSondage> list = new ArrayList<>();
        String sql = "SELECT id, message_id, option_text, votes FROM sondage_options WHERE message_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new OptionSondage(
                        rs.getInt("id"),
                        rs.getInt("message_id"),
                        rs.getString("option_text"),
                        rs.getInt("votes")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void voter(int optionId) {
        String sql = "UPDATE sondage_options SET votes = votes + 1 WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, optionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Nouveau : vote en enregistrant l'utilisateur pour éviter double vote
    public void voter(int optionId, int userId) {
        // Récupérer le message_id pour cette option
        Integer messageId = null;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT message_id FROM sondage_options WHERE id = ?")) {
            ps.setInt(1, optionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                messageId = rs.getInt("message_id");
        } catch (SQLException ignored) {
        }

        if (messageId == null)
            return;

        // Si l'utilisateur a déjà voté pour ce message, on ne fait rien
        if (hasUserVoted(messageId, userId))
            return;

        // Incrémenter le compteur et ajouter l'enregistrement dans sondage_votes
        String sql = "UPDATE sondage_options SET votes = votes + 1 WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, optionId);
            ps.executeUpdate();
            try (PreparedStatement ins = cnx
                    .prepareStatement("INSERT INTO sondage_votes (option_id, user_id) VALUES (?, ?)")) {
                ins.setInt(1, optionId);
                ins.setInt(2, userId);
                ins.executeUpdate();
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasUserVoted(int messageId, int userId) {
        String sql = "SELECT v.id FROM sondage_votes v JOIN sondage_options o ON v.option_id = o.id WHERE o.message_id = ? AND v.user_id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    // Retourne l'option_id que l'utilisateur a choisie pour ce message, ou -1 si
    // aucun
    public int getUserVotedOptionId(int messageId, int userId) {
        String sql = "SELECT v.option_id FROM sondage_votes v JOIN sondage_options o ON v.option_id = o.id WHERE o.message_id = ? AND v.user_id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("option_id");
        } catch (SQLException e) {
            // ignore
        }
        return -1;
    }
}
