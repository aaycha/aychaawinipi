package com.gestion.services;

import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import com.gestion.interfaces.RestaurantService;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC/MySQL du service Restaurant.
 * Persistance dans la base lammap (tables dédiées admin).
 */
public class RestaurantServiceImpl implements RestaurantService {

    private static final String TABLE = "restaurants_admin";
    private final MyConnection db = MyConnection.getInstance();

    public RestaurantServiceImpl() {
        ensureTable();
    }

    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS %s (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  nom VARCHAR(100) NOT NULL,
                  adresse VARCHAR(255) NOT NULL,
                  telephone VARCHAR(20) NOT NULL,
                  email VARCHAR(100) NOT NULL,
                  description TEXT NOT NULL,
                  image_url VARCHAR(255) NOT NULL,
                  actif BOOLEAN NOT NULL DEFAULT TRUE,
                  date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(TABLE);
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de créer la table " + TABLE + " : " + e.getMessage(), e);
        }
    }

    @Override
    public Restaurant create(Restaurant restaurant) {
        if (restaurant == null) throw new IllegalArgumentException("Le restaurant ne peut pas être null");
        ValidationResult validation = restaurant.validate();
        if (validation.hasErrors()) throw new IllegalArgumentException(validation.getAllErrorsAsString());

        String sql = "INSERT INTO " + TABLE + " (nom, adresse, telephone, email, description, image_url, actif) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, restaurant.getNom());
            ps.setString(2, restaurant.getAdresse());
            ps.setString(3, restaurant.getTelephone());
            ps.setString(4, restaurant.getEmail());
            ps.setString(5, restaurant.getDescription());
            ps.setString(6, restaurant.getImageUrl());
            ps.setBoolean(7, restaurant.isActif());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) restaurant.setId(keys.getLong(1));
            }
            return restaurant;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur création restaurant : " + e.getMessage(), e);
        }
    }

    @Override
    public Restaurant update(Restaurant restaurant) {
        if (restaurant == null || restaurant.getId() == null) {
            throw new IllegalArgumentException("Le restaurant et son ID sont obligatoires");
        }
        ValidationResult validation = restaurant.validate();
        if (validation.hasErrors()) throw new IllegalArgumentException(validation.getAllErrorsAsString());

        String sql = "UPDATE " + TABLE + " SET nom=?, adresse=?, telephone=?, email=?, description=?, image_url=?, actif=? WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, restaurant.getNom());
            ps.setString(2, restaurant.getAdresse());
            ps.setString(3, restaurant.getTelephone());
            ps.setString(4, restaurant.getEmail());
            ps.setString(5, restaurant.getDescription());
            ps.setString(6, restaurant.getImageUrl());
            ps.setBoolean(7, restaurant.isActif());
            ps.setLong(8, restaurant.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Restaurant non trouvé avec l'ID: " + restaurant.getId());
            return restaurant;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur mise à jour restaurant : " + e.getMessage(), e);
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
            throw new RuntimeException("Erreur suppression restaurant : " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Restaurant> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM " + TABLE + " WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById restaurant : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Restaurant> findAll() {
        String sql = "SELECT * FROM " + TABLE + " ORDER BY id DESC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Restaurant> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll restaurants : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Restaurant> findActifs() {
        String sql = "SELECT * FROM " + TABLE + " WHERE actif=TRUE ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Restaurant> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findActifs restaurants : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Restaurant> searchByNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) return findAll();
        String sql = "SELECT * FROM " + TABLE + " WHERE LOWER(nom) LIKE ? ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + nom.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Restaurant> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche restaurants : " + e.getMessage(), e);
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
            throw new RuntimeException("Erreur existsById restaurant : " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur count restaurants : " + e.getMessage(), e);
        }
    }

    private Restaurant map(ResultSet rs) throws SQLException {
        Restaurant r = new Restaurant();
        r.setId(rs.getLong("id"));
        r.setNom(rs.getString("nom"));
        r.setAdresse(rs.getString("adresse"));
        r.setTelephone(rs.getString("telephone"));
        r.setEmail(rs.getString("email"));
        r.setDescription(rs.getString("description"));
        r.setImageUrl(rs.getString("image_url"));
        r.setActif(rs.getBoolean("actif"));
        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) r.setDateCreation(ts.toLocalDateTime());
        else r.setDateCreation(LocalDateTime.now());
        return r;
    }
}
