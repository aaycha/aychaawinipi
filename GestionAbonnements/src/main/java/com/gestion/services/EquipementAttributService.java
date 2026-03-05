package com.gestion.services;

import com.gestion.entities.EquipementAttribut;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipementAttributService {

    private final Connection cnx;

    public EquipementAttributService() {
        cnx = MyConnection.getInstance().getConnection();
    }

    public void ajouter(EquipementAttribut a) {
        String sql = "INSERT INTO equipement_attributs (equipement_id, nom_attribut, valeur, description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, a.getEquipementId());
            ps.setString(2, a.getNomAttribut());
            ps.setString(3, a.getValeur());
            ps.setString(4, a.getDescription());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout attribut: " + e.getMessage());
        }
    }

    public void supprimerParEquipement(long equipementId) {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM equipement_attributs WHERE equipement_id = ?")) {
            ps.setLong(1, equipementId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression attributs: " + e.getMessage());
        }
    }

    public List<EquipementAttribut> getByEquipementId(long equipementId) {
        List<EquipementAttribut> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT id, equipement_id, nom_attribut, valeur, description FROM equipement_attributs WHERE equipement_id = ?")) {
            ps.setLong(1, equipementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new EquipementAttribut(
                        rs.getInt("id"),
                        rs.getLong("equipement_id"),
                        rs.getString("nom_attribut"),
                        rs.getString("valeur"),
                        rs.getString("description")));
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur lecture attributs: " + e.getMessage());
        }
        return list;
    }
}
