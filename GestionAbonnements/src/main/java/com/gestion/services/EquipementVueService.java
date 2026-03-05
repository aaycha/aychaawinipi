package com.gestion.services;

import com.gestion.entities.EquipementVue;
import com.gestion.tools.MyConnection;
import com.gestion.tools.EventBus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipementVueService {

    private final Connection cnx;
    private final equipemementservicewael equipementService;

    public EquipementVueService() {
        cnx = MyConnection.getInstance().getConnection();
        equipementService = new equipemementservicewael();
    }

    /**
     * Enregistre une vue unique par utilisateur (userId) pour un équipement.
     * Si l'utilisateur n'a pas encore vu l'équipement, ajoute une ligne et
     * incrémente le compteur global.
     * Si userId est null ou vide, retourne false et ne compte pas.
     */
    public boolean registerView(Long equipementId, String userId) {
        if (equipementId == null || userId == null || userId.isBlank())
            return false;
        try {
            // Vérifier existence d'une vue pour ce couple (equipement,user)
            String select = "SELECT id FROM equipement_vues WHERE equipement_id = ? AND user_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(select)) {
                ps.setLong(1, equipementId);
                ps.setString(2, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    // mise à jour du timestamp uniquement
                    String upd = "UPDATE equipement_vues SET last_viewed = CURRENT_TIMESTAMP WHERE id = ?";
                    try (PreparedStatement ps2 = cnx.prepareStatement(upd)) {
                        ps2.setLong(1, rs.getLong("id"));
                        ps2.executeUpdate();
                    }
                    return false; // déjà compté auparavant
                }
            }

            // Insérer nouvelle vue unique
            String insert = "INSERT INTO equipement_vues (equipement_id, user_id, last_viewed) VALUES (?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement ps = cnx.prepareStatement(insert)) {
                ps.setLong(1, equipementId);
                ps.setString(2, userId);
                ps.executeUpdate();
            }

            // Incrémenter le compteur global (colonne nombre_vues)
            equipementService.incrementerVues(equipementId);

            // Emit event pour UI: payload = equipementId
            try {
                EventBus.emit("equipement:viewed", equipementId);
            } catch (Exception ignored) {
            }

            return true;
        } catch (SQLException e) {
            System.out.println("❌ Erreur EquipementVueService.registerView: " + e.getMessage());
            return false;
        }
    }

    public int getViewsCount(Long equipementId) {
        if (equipementId == null)
            return 0;
        try (PreparedStatement ps = cnx
                .prepareStatement("SELECT COUNT(*) AS c FROM equipement_vues WHERE equipement_id = ?")) {
            ps.setLong(1, equipementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("c");
        } catch (SQLException e) {
            System.out.println("❌ Erreur getViewsCount: " + e.getMessage());
        }
        return 0;
    }

    public List<EquipementVue> getViewsForEquipement(Long equipementId) {
        List<EquipementVue> list = new ArrayList<>();
        if (equipementId == null)
            return list;
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT id, equipement_id, user_id, last_viewed FROM equipement_vues WHERE equipement_id = ?")) {
            ps.setLong(1, equipementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new EquipementVue(rs.getLong("id"), rs.getLong("equipement_id"), rs.getString("user_id"),
                        rs.getTimestamp("last_viewed")));
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur getViewsForEquipement: " + e.getMessage());
        }
        return list;
    }
}
