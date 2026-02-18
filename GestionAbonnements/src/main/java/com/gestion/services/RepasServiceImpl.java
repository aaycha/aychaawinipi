package com.gestion.services;

import com.gestion.entities.Repas;
import com.gestion.entities.ValidationResult;
import com.gestion.interfaces.RepasService;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC/MySQL du service Repas (plats).
 * Persistance dans la base lammap (tables dédiées admin).
 */
public class RepasServiceImpl implements RepasService {

    private static final String TABLE = "repas_admin";
    private final MyConnection db = MyConnection.getInstance();

    public RepasServiceImpl() {
        ensureTable();
    }

    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS %s (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  restaurant_id BIGINT NOT NULL,
                  restaurant_nom VARCHAR(150) NOT NULL,
                  menu_id BIGINT NOT NULL,
                  menu_nom VARCHAR(150) NOT NULL,
                  nom VARCHAR(120) NOT NULL,
                  description TEXT NOT NULL,
                  prix DECIMAL(10,2) NOT NULL,
                  categorie VARCHAR(40) NOT NULL,
                  type_plat VARCHAR(40) NOT NULL,
                  temps_preparation INT NOT NULL,
                  image_url VARCHAR(255) NOT NULL,
                  disponible BOOLEAN NOT NULL DEFAULT TRUE,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(TABLE);
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de créer la table " + TABLE + " : " + e.getMessage(), e);
        }
    }

    @Override
    public Repas create(Repas repas) {
        if (repas == null) throw new IllegalArgumentException("Le repas ne peut pas être null");
        ValidationResult validation = repas.validate();
        if (validation.hasErrors()) throw new IllegalArgumentException(validation.getAllErrorsAsString());

        String sql = "INSERT INTO " + TABLE + " (restaurant_id, restaurant_nom, menu_id, menu_nom, nom, description, prix, categorie, type_plat, temps_preparation, image_url, disponible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, repas.getRestaurantId());
            ps.setString(2, repas.getRestaurantNom());
            ps.setLong(3, repas.getMenuId());
            ps.setString(4, repas.getMenuNom());
            ps.setString(5, repas.getNom());
            ps.setString(6, repas.getDescription());
            ps.setBigDecimal(7, repas.getPrix());
            ps.setString(8, repas.getCategorie() != null ? repas.getCategorie().name() : null);
            ps.setString(9, repas.getTypePlat() != null ? repas.getTypePlat().name() : null);
            ps.setInt(10, repas.getTempsPreparation());
            ps.setString(11, repas.getImageUrl());
            ps.setBoolean(12, repas.isDisponible());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) repas.setId(keys.getLong(1));
            }
            return repas;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur création repas : " + e.getMessage(), e);
        }
    }

    @Override
    public Repas update(Repas repas) {
        if (repas == null || repas.getId() == null) {
            throw new IllegalArgumentException("Le repas et son ID sont obligatoires");
        }
        ValidationResult validation = repas.validate();
        if (validation.hasErrors()) throw new IllegalArgumentException(validation.getAllErrorsAsString());

        String sql = "UPDATE " + TABLE + " SET restaurant_id=?, restaurant_nom=?, menu_id=?, menu_nom=?, nom=?, description=?, prix=?, categorie=?, type_plat=?, temps_preparation=?, image_url=?, disponible=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, repas.getRestaurantId());
            ps.setString(2, repas.getRestaurantNom());
            ps.setLong(3, repas.getMenuId());
            ps.setString(4, repas.getMenuNom());
            ps.setString(5, repas.getNom());
            ps.setString(6, repas.getDescription());
            ps.setBigDecimal(7, repas.getPrix());
            ps.setString(8, repas.getCategorie() != null ? repas.getCategorie().name() : null);
            ps.setString(9, repas.getTypePlat() != null ? repas.getTypePlat().name() : null);
            ps.setInt(10, repas.getTempsPreparation());
            ps.setString(11, repas.getImageUrl());
            ps.setBoolean(12, repas.isDisponible());
            ps.setLong(13, repas.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Repas non trouvé avec l'ID: " + repas.getId());
            return repas;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur mise à jour repas : " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) return false;
        String sql = "DELETE FROM " + TABLE + " WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression repas : " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Repas> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM " + TABLE + " WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById repas : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repas> findAll() {
        String sql = "SELECT * FROM " + TABLE + " ORDER BY id DESC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Repas> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll repas : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repas> findDisponibles() {
        String sql = "SELECT * FROM " + TABLE + " WHERE disponible=TRUE ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Repas> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findDisponibles repas : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repas> findByRestaurantId(Long restaurantId) {
        if (restaurantId == null) return new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE restaurant_id=? ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Repas> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByRestaurantId repas : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repas> findDisponiblesByRestaurantId(Long restaurantId) {
        if (restaurantId == null) return new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE restaurant_id=? AND disponible=TRUE ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Repas> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findDisponiblesByRestaurantId repas : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repas> findByMenuId(Long menuId) {
        if (menuId == null) return new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE menu_id=? ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, menuId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Repas> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByMenuId repas : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repas> findByCategorie(Repas.Categorie categorie) {
        if (categorie == null) return new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE categorie=? ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, categorie.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<Repas> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByCategorie repas : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repas> findByTypePlat(Repas.TypePlat typePlat) {
        if (typePlat == null) return new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE type_plat=? ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, typePlat.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<Repas> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByTypePlat repas : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repas> searchByNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) return findAll();
        String sql = "SELECT * FROM " + TABLE + " WHERE LOWER(nom) LIKE ? ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + nom.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Repas> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur searchByNom repas : " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) return false;
        String sql = "SELECT 1 FROM " + TABLE + " WHERE id=? LIMIT 1";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur existsById repas : " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur count repas : " + e.getMessage(), e);
        }
    }

    @Override
    public long countByRestaurantId(Long restaurantId) {
        if (restaurantId == null) return 0;
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE restaurant_id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur countByRestaurantId repas : " + e.getMessage(), e);
        }
    }

    private Repas map(ResultSet rs) throws SQLException {
        Repas r = new Repas();
        r.setId(rs.getLong("id"));
        r.setRestaurantId(rs.getLong("restaurant_id"));
        r.setRestaurantNom(rs.getString("restaurant_nom"));
        r.setMenuId(rs.getLong("menu_id"));
        r.setMenuNom(rs.getString("menu_nom"));
        r.setNom(rs.getString("nom"));
        r.setDescription(rs.getString("description"));
        r.setPrix(rs.getBigDecimal("prix"));
        String cat = rs.getString("categorie");
        String type = rs.getString("type_plat");
        r.setCategorie(cat != null ? Repas.Categorie.valueOf(cat) : null);
        r.setTypePlat(type != null ? Repas.TypePlat.valueOf(type) : null);
        r.setTempsPreparation(rs.getInt("temps_preparation"));
        r.setImageUrl(rs.getString("image_url"));
        r.setDisponible(rs.getBoolean("disponible"));
        return r;
    }
}
