package com.gestion.services;

import com.gestion.entities.GroupeChat;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupeChatService {

    private final Connection cnx;

    public GroupeChatService() {
        cnx = MyConnection.getInstance().getConnection();
        creerColonneCreateurSiNexistePas();
    }

    private void creerColonneCreateurSiNexistePas() {
        try (Statement st = cnx.createStatement()) {
            st.execute("ALTER TABLE groupe_chat ADD COLUMN IF NOT EXISTS id_createur INT DEFAULT 1");
        } catch (SQLException e) {
            System.out.println("Colonne id_createur existe déjà ou erreur : " + e.getMessage());
        }
    }

    // CREATE
    public void ajouter(GroupeChat g) {
        String sql = "INSERT INTO groupe_chat (nom, description, type, id_createur) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, g.getNom());
            ps.setString(2, g.getDescription());
            ps.setString(3, g.getType());
            ps.setInt(4, g.getIdCreateur());
            ps.executeUpdate();
            System.out.println("✅ Groupe ajouté !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout groupe");
            e.printStackTrace();
        }
    }

    // READ ALL (DB -> List -> Stream)
    public List<GroupeChat> afficher() {
        List<GroupeChat> list = new ArrayList<>();
        String sql = "SELECT id, nom, description, type, date_creation, id_createur FROM groupe_chat";

        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                int idCreateur = 1;
                try {
                    idCreateur = rs.getInt("id_createur");
                } catch (Exception ignored) {
                }

                list.add(new GroupeChat(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getTimestamp("date_creation"),
                        idCreateur));
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur affichage groupes");
            e.printStackTrace();
        }

        // ✅ Stream: tri par date_creation DESC
        return list.stream()
                .sorted((a, b) -> {
                    Timestamp da = a.getDateCreation();
                    Timestamp db = b.getDateCreation();
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
    public GroupeChat getById(int id) {
        return afficher().stream()
                .filter(g -> g.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // UPDATE
    public void modifier(GroupeChat g) {
        String sql = "UPDATE groupe_chat SET nom = ?, description = ?, type = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, g.getNom());
            ps.setString(2, g.getDescription());
            ps.setString(3, g.getType());
            ps.setInt(4, g.getId());
            int updated = ps.executeUpdate();
            System.out.println(updated > 0 ? "✅ Groupe modifié !" : "⚠️ Aucun groupe trouvé");
        } catch (SQLException e) {
            System.out.println("❌ Erreur modification groupe");
            e.printStackTrace();
        }
    }

    // DELETE
    public void supprimer(int id) {
        String sql = "DELETE FROM groupe_chat WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            System.out.println(deleted > 0 ? "✅ Groupe supprimé !" : "⚠️ Aucun groupe trouvé");
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression groupe");
            e.printStackTrace();
        }
    }

    // BONUS Stream: rechercher par type (PUBLIC/PRIVATE)
    public List<GroupeChat> rechercherParType(String type) {
        String t = (type == null) ? "" : type.trim().toLowerCase();
        return afficher().stream()
                .filter(g -> g.getType() != null && g.getType().trim().toLowerCase().equals(t))
                .collect(java.util.stream.Collectors.toList());
    }

    // BONUS Stream: rechercher par mot dans le nom
    public List<GroupeChat> rechercherParNom(String mot) {
        String m = (mot == null) ? "" : mot.trim().toLowerCase();
        return afficher().stream()
                .filter(g -> g.getNom() != null && g.getNom().toLowerCase().contains(m))
                .collect(java.util.stream.Collectors.toList());
    }
}