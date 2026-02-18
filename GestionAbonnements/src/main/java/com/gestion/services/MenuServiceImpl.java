package com.gestion.services;

import com.gestion.entities.Menu;
import com.gestion.entities.ValidationResult;
import com.gestion.interfaces.MenuService;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC/MySQL du service Menu.
 * Persistance dans la base lammap (tables dédiées admin).
 */
public class MenuServiceImpl implements MenuService {

    private static final String TABLE = "menus_admin";
    private final MyConnection db = MyConnection.getInstance();

    public MenuServiceImpl() {
        ensureTable();
    }

    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS %s (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  restaurant_id BIGINT NOT NULL,
                  restaurant_nom VARCHAR(150) NOT NULL,
                  nom VARCHAR(100) NOT NULL,
                  description VARCHAR(500) NOT NULL,
                  prix DECIMAL(10,2) NOT NULL,
                  date_debut DATE NOT NULL,
                  date_fin DATE NOT NULL,
                  actif BOOLEAN NOT NULL DEFAULT TRUE,
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
    public Menu create(Menu menu) {
        if (menu == null) throw new IllegalArgumentException("Le menu ne peut pas être null");
        ValidationResult validation = menu.validate();
        if (validation.hasErrors()) throw new IllegalArgumentException(validation.getAllErrorsAsString());

        String sql = "INSERT INTO " + TABLE + " (restaurant_id, restaurant_nom, nom, description, prix, date_debut, date_fin, actif) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, menu.getRestaurantId());
            ps.setString(2, menu.getRestaurantNom());
            ps.setString(3, menu.getNom());
            ps.setString(4, menu.getDescription());
            ps.setBigDecimal(5, menu.getPrix());
            ps.setDate(6, Date.valueOf(menu.getDateDebut()));
            ps.setDate(7, Date.valueOf(menu.getDateFin()));
            ps.setBoolean(8, menu.isActif());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) menu.setId(keys.getLong(1));
            }
            return menu;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur création menu : " + e.getMessage(), e);
        }
    }

    @Override
    public Menu update(Menu menu) {
        if (menu == null || menu.getId() == null) {
            throw new IllegalArgumentException("Le menu et son ID sont obligatoires");
        }
        ValidationResult validation = menu.validate();
        if (validation.hasErrors()) throw new IllegalArgumentException(validation.getAllErrorsAsString());

        String sql = "UPDATE " + TABLE + " SET restaurant_id=?, restaurant_nom=?, nom=?, description=?, prix=?, date_debut=?, date_fin=?, actif=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, menu.getRestaurantId());
            ps.setString(2, menu.getRestaurantNom());
            ps.setString(3, menu.getNom());
            ps.setString(4, menu.getDescription());
            ps.setBigDecimal(5, menu.getPrix());
            ps.setDate(6, Date.valueOf(menu.getDateDebut()));
            ps.setDate(7, Date.valueOf(menu.getDateFin()));
            ps.setBoolean(8, menu.isActif());
            ps.setLong(9, menu.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Menu non trouvé avec l'ID: " + menu.getId());
            return menu;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur mise à jour menu : " + e.getMessage(), e);
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
            throw new RuntimeException("Erreur suppression menu : " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Menu> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM " + TABLE + " WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById menu : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Menu> findAll() {
        String sql = "SELECT * FROM " + TABLE + " ORDER BY id DESC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Menu> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll menus : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Menu> findActifs() {
        String sql = "SELECT * FROM " + TABLE + " WHERE actif=TRUE ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Menu> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findActifs menus : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Menu> findByRestaurantId(Long restaurantId) {
        if (restaurantId == null) return new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE restaurant_id=? ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Menu> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByRestaurantId menus : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Menu> findActifsByRestaurantId(Long restaurantId) {
        if (restaurantId == null) return new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE restaurant_id=? AND actif=TRUE ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Menu> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findActifsByRestaurantId menus : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Menu> searchByNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) return findAll();
        String sql = "SELECT * FROM " + TABLE + " WHERE LOWER(nom) LIKE ? ORDER BY nom ASC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + nom.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Menu> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur searchByNom menus : " + e.getMessage(), e);
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
            throw new RuntimeException("Erreur existsById menu : " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur count menus : " + e.getMessage(), e);
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
            throw new RuntimeException("Erreur countByRestaurantId menus : " + e.getMessage(), e);
        }
    }

    private Menu map(ResultSet rs) throws SQLException {
        Menu m = new Menu();
        m.setId(rs.getLong("id"));
        m.setRestaurantId(rs.getLong("restaurant_id"));
        m.setRestaurantNom(rs.getString("restaurant_nom"));
        m.setNom(rs.getString("nom"));
        m.setDescription(rs.getString("description"));
        m.setPrix(rs.getBigDecimal("prix"));
        Date d1 = rs.getDate("date_debut");
        Date d2 = rs.getDate("date_fin");
        m.setDateDebut(d1 != null ? d1.toLocalDate() : null);
        m.setDateFin(d2 != null ? d2.toLocalDate() : null);
        m.setActif(rs.getBoolean("actif"));
        return m;
    }
}
