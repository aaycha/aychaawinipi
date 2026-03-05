package com.gestion.services;

import com.gestion.entities.MessageChat;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageChatService {

    private final Connection cnx;

    public MessageChatService() {
        cnx = MyConnection.getInstance().getConnection();
        creerColonneUserSiNexistePas();
        // S'assurer que les colonnes nécessaires existent (type_message, fichier_path,
        // latitude, longitude)
        ensureColumnsExist();
    }

    private void creerColonneUserSiNexistePas() {
        try (Statement st = cnx.createStatement()) {
            st.execute("ALTER TABLE message_chat ADD COLUMN IF NOT EXISTS id_user INT DEFAULT 1");
        } catch (SQLException e) {
            System.out.println("Colonne id_user existe déjà ou erreur : " + e.getMessage());
        }
    }

    // CREATE
    public int ajouter(MessageChat m) {
        String sql = "INSERT INTO message_chat (contenu, id_groupe, id_user, type_message, fichier_path, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getContenu() != null ? m.getContenu() : "");
            ps.setInt(2, m.getIdGroupe());
            ps.setInt(3, m.getIdUser());
            ps.setString(4, m.getTypeMessage() != null ? m.getTypeMessage() : "TEXT");
            ps.setString(5, m.getFichierPath());
            ps.setObject(6, m.getLatitude());
            ps.setObject(7, m.getLongitude());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int newId = rs.getInt(1);
                m.setId(newId);
                System.out.println("✅ Message ajouté ! ID=" + newId);
                return newId;
            }
            return -1;
        } catch (SQLException e) {
            System.out.println("Ajout message échoué: " + e.getMessage());
            // Si erreur liée au schéma (colonnes manquantes), tenter de créer les colonnes
            // puis retenter
            if (e.getMessage() != null && (e.getMessage().contains("type_message")
                    || e.getMessage().contains("fichier_path") || e.getMessage().contains("latitude")
                    || e.getMessage().contains("longitude") || e.getMessage().contains("Unknown column"))) {
                try {
                    ensureColumnsExist();
                    // Retenter l'insertion une fois
                    try (PreparedStatement ps2 = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        ps2.setString(1, m.getContenu() != null ? m.getContenu() : "");
                        ps2.setInt(2, m.getIdGroupe());
                        ps2.setInt(3, m.getIdUser());
                        ps2.setString(4, m.getTypeMessage() != null ? m.getTypeMessage() : "TEXT");
                        ps2.setString(5, m.getFichierPath());
                        ps2.setObject(6, m.getLatitude());
                        ps2.setObject(7, m.getLongitude());
                        ps2.executeUpdate();
                        ResultSet rs2 = ps2.getGeneratedKeys();
                        if (rs2.next()) {
                            int newId = rs2.getInt(1);
                            m.setId(newId);
                            System.out.println("✅ Message ajouté après migration des colonnes ! ID=" + newId);
                            return newId;
                        }
                    }
                } catch (Exception ex2) {
                    System.out.println("Échec tentative de création des colonnes: " + ex2.getMessage());
                }

                // Si tout échoue, retomber à l'ancien fallback (insert minimal)
                try (PreparedStatement ps = cnx.prepareStatement(
                        "INSERT INTO message_chat (contenu, id_groupe, id_user) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, m.getContenu() != null ? m.getContenu() : "");
                    ps.setInt(2, m.getIdGroupe());
                    ps.setInt(3, m.getIdUser());
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        m.setId(newId);
                        System.out.println("✅ Message ajouté (fallback) ! ID=" + newId);
                        return newId;
                    }
                    return -1;
                } catch (SQLException ex3) {
                    System.out.println("❌ Erreur ajout message fallback");
                    ex3.printStackTrace();
                    return -1;
                }
            } else {
                System.out.println("❌ Erreur ajout message");
                e.printStackTrace();
                return -1;
            }
        }
    }

    /**
     * Tente d'ajouter les colonnes utilisées par la messagerie si elles n'existent
     * pas.
     * Méthode idempotente (utilise IF NOT EXISTS quand disponible) pour rendre le
     * code résilient
     * vis-à-vis d'anciens schémas SQLite/MySQL qui pourraient être différents.
     */
    private void ensureColumnsExist() {
        try (Statement st = cnx.createStatement()) {
            // Les syntaxes ALTER TABLE ADD COLUMN IF NOT EXISTS existent sur certaines
            // bases (Postgres, MySQL récent).
            // On essaye plusieurs variantes en silence pour maximiser la compatibilité.
            try {
                st.execute("ALTER TABLE message_chat ADD COLUMN IF NOT EXISTS type_message VARCHAR(50) DEFAULT 'TEXT'");
            } catch (SQLException ignored) {
                try {
                    st.execute("ALTER TABLE message_chat ADD COLUMN type_message VARCHAR(50)");
                } catch (SQLException ignored2) {
                }
            }
            try {
                st.execute("ALTER TABLE message_chat ADD COLUMN IF NOT EXISTS fichier_path TEXT");
            } catch (SQLException ignored) {
                try {
                    st.execute("ALTER TABLE message_chat ADD COLUMN fichier_path TEXT");
                } catch (SQLException ignored2) {
                }
            }
            try {
                st.execute("ALTER TABLE message_chat ADD COLUMN IF NOT EXISTS latitude DOUBLE");
            } catch (SQLException ignored) {
                try {
                    st.execute("ALTER TABLE message_chat ADD COLUMN latitude DOUBLE");
                } catch (SQLException ignored2) {
                }
            }
            try {
                st.execute("ALTER TABLE message_chat ADD COLUMN IF NOT EXISTS longitude DOUBLE");
            } catch (SQLException ignored) {
                try {
                    st.execute("ALTER TABLE message_chat ADD COLUMN longitude DOUBLE");
                } catch (SQLException ignored2) {
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la tentative d'ajout des colonnes message_chat: " + e.getMessage());
        }
    }

    // READ ALL (DB -> List -> Stream)
    public List<MessageChat> afficher() {
        List<MessageChat> list = new ArrayList<>();
        String sql = "SELECT id, contenu, date_envoi, id_groupe, id_user, type_message, fichier_path, latitude, longitude FROM message_chat";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int idUser = 1; // Default
                try {
                    idUser = rs.getInt("id_user");
                } catch (Exception ignored) {
                }
                MessageChat msg = new MessageChat(rs.getInt("id"), rs.getString("contenu"),
                        rs.getTimestamp("date_envoi"), rs.getInt("id_groupe"), idUser);
                try {
                    msg.setTypeMessage(rs.getString("type_message"));
                } catch (Exception ignored) {
                }
                try {
                    msg.setFichierPath(rs.getString("fichier_path"));
                } catch (Exception ignored) {
                }
                try {
                    msg.setLatitude(rs.getDouble("latitude"));
                    if (rs.wasNull())
                        msg.setLatitude(null);
                } catch (Exception ignored) {
                }
                try {
                    msg.setLongitude(rs.getDouble("longitude"));
                    if (rs.wasNull())
                        msg.setLongitude(null);
                } catch (Exception ignored) {
                }
                list.add(msg);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null
                    && (e.getMessage().contains("type_message") || e.getMessage().contains("Unknown column"))) {
                try (Statement st = cnx.createStatement();
                        ResultSet rs = st
                                .executeQuery("SELECT id, contenu, date_envoi, id_groupe, id_user FROM message_chat")) {
                    while (rs.next()) {
                        int idUser = 1; // Default
                        try {
                            idUser = rs.getInt("id_user");
                        } catch (Exception ignored) {
                        }
                        list.add(new MessageChat(rs.getInt("id"), rs.getString("contenu"),
                                rs.getTimestamp("date_envoi"), rs.getInt("id_groupe"), idUser));
                    }
                } catch (SQLException ex2) {
                    System.out.println("❌ Erreur affichage messages");
                    ex2.printStackTrace();
                }
            } else {
                System.out.println("❌ Erreur affichage messages");
                e.printStackTrace();
            }
        }

        // ✅ Stream: tri DESC par date_envoi
        return list.stream()
                .sorted((a, b) -> {
                    Timestamp da = a.getDateEnvoi();
                    Timestamp db = b.getDateEnvoi();
                    if (da == null && db == null)
                        return 0;
                    if (da == null)
                        return 1;
                    if (db == null)
                        return -1;
                    return db.compareTo(da);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // READ BY ID (Stream)
    public MessageChat getById(int id) {
        return afficher().stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // READ messages d’un groupe (Stream)
    public List<MessageChat> afficherParGroupe(int idGroupe) {
        // ✅ Stream: filter + tri ASC (style chat)
        return afficher().stream()
                .filter(m -> m.getIdGroupe() == idGroupe)
                .sorted((a, b) -> {
                    Timestamp da = a.getDateEnvoi();
                    Timestamp db = b.getDateEnvoi();
                    if (da == null && db == null)
                        return 0;
                    if (da == null)
                        return -1;
                    if (db == null)
                        return 1;
                    return da.compareTo(db);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // UPDATE
    public void modifier(MessageChat m) {
        String sql = "UPDATE message_chat SET contenu = ?, type_message = ?, fichier_path = ?, latitude = ?, longitude = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, m.getContenu());
            ps.setString(2, m.getTypeMessage() != null ? m.getTypeMessage() : "TEXT");
            ps.setString(3, m.getFichierPath());
            ps.setObject(4, m.getLatitude());
            ps.setObject(5, m.getLongitude());
            ps.setInt(6, m.getId());
            int updated = ps.executeUpdate();
            System.out.println(updated > 0 ? "✅ Message modifié !" : "⚠️ Aucun message trouvé");
        } catch (SQLException e) {
            try (PreparedStatement ps = cnx.prepareStatement("UPDATE message_chat SET contenu = ? WHERE id = ?")) {
                ps.setString(1, m.getContenu());
                ps.setInt(2, m.getId());
                ps.executeUpdate();
            } catch (SQLException ex2) {
                System.out.println("❌ Erreur modification message");
                ex2.printStackTrace();
            }
        }
    }

    // DELETE
    public void supprimer(int id) {
        String sql = "DELETE FROM message_chat WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            System.out.println(deleted > 0 ? "✅ Message supprimé !" : "⚠️ Aucun message trouvé");
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression message");
            e.printStackTrace();
        }
    }

    // BONUS Stream: rechercher un mot dans les messages d’un groupe
    public List<MessageChat> rechercherDansGroupe(int idGroupe, String mot) {
        String m = (mot == null) ? "" : mot.toLowerCase();
        return afficherParGroupe(idGroupe).stream()
                .filter(msg -> msg.getContenu() != null && msg.getContenu().toLowerCase().contains(m))
                .collect(java.util.stream.Collectors.toList());
    }
}