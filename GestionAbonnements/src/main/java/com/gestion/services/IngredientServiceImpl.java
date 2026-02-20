package com.gestion.services;

import com.gestion.entities.Ingredient;
import com.gestion.interfaces.IngredientService;
import com.gestion.tools.MyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngredientServiceImpl implements IngredientService {

    private static final Logger logger = LoggerFactory.getLogger(IngredientServiceImpl.class);
    private final MyConnection dbConnection = MyConnection.getInstance();

    public IngredientServiceImpl() {
        initTable();
    }

    private void initTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS ingredients (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    nom VARCHAR(100) NOT NULL,
                    categorie VARCHAR(50) NOT NULL,
                    prix_supplement DECIMAL(10,2) DEFAULT 0.00,
                    calories INT DEFAULT 0,
                    icon_url VARCHAR(255),
                    actif BOOLEAN DEFAULT TRUE
                )
                """;
        try (Connection c = dbConnection.getConnection();
                Statement st = c.createStatement()) {
            st.execute(sql);

            // Ajouter quelques ingrédients par défaut si la table est vide
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ingredients")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertDefaultIngredients();
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur initTable ingredients", e);
        }
    }

    private void insertDefaultIngredients() {
        String sql = "INSERT INTO ingredients (nom, categorie, prix_supplement, calories) VALUES (?, ?, ?, ?)";
        try (Connection c = dbConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            Object[][] defaults = {
                    { "Œufs pochés", "BASE", 0.0, 140 },
                    { "Riz complet", "BASE", 0.0, 210 },
                    { "Quinoa", "BASE", 1.5, 220 },
                    { "Poulet grillé", "PROTEINE", 3.0, 165 },
                    { "Tofu bio", "PROTEINE", 2.5, 80 },
                    { "Crevettes", "PROTEINE", 4.0, 120 },
                    { "Avocat", "LEGUME", 2.0, 160 },
                    { "Tomates cerises", "LEGUME", 0.0, 20 },
                    { "Épinards frais", "LEGUME", 0.0, 15 },
                    { "Sauce Curry", "SAUCE", 0.5, 60 },
                    { "Huile d'olive & Citron", "SAUCE", 0.0, 45 },
                    { "Graines de sésame", "TOPPING", 0.5, 30 },
                    { "Noix de cajou", "TOPPING", 1.0, 150 }
            };

            for (Object[] d : defaults) {
                ps.setString(1, (String) d[0]);
                ps.setString(2, (String) d[1]);
                ps.setBigDecimal(3, BigDecimal.valueOf((Double) d[2]));
                ps.setInt(4, (Integer) d[3]);
                ps.addBatch();
            }
            ps.executeBatch();
            logger.info("Ingrédients par défaut insérés");
        } catch (SQLException e) {
            logger.error("Erreur insertDefaultIngredients", e);
        }
    }

    @Override
    public Ingredient create(Ingredient ingredient) {
        String sql = "INSERT INTO ingredients (nom, categorie, prix_supplement, calories, icon_url, actif) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = dbConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ingredient.getNom());
            ps.setString(2, ingredient.getCategorie().name());
            ps.setBigDecimal(3, ingredient.getPrixSupplement());
            ps.setInt(4, ingredient.getCalories());
            ps.setString(5, ingredient.getIconUrl());
            ps.setBoolean(6, ingredient.isActif());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    ingredient.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            logger.error("Error create ingredient", e);
        }
        return ingredient;
    }

    @Override
    public Optional<Ingredient> findById(Long id) {
        String sql = "SELECT * FROM ingredients WHERE id = ?";
        try (Connection c = dbConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.error("Error findById ingredient", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Ingredient> findAll() {
        List<Ingredient> list = new ArrayList<>();
        try (Connection c = dbConnection.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM ingredients WHERE actif = 1")) {
            while (rs.next())
                list.add(map(rs));
        } catch (SQLException e) {
            logger.error("Error findAll ingredients", e);
        }
        return list;
    }

    @Override
    public List<Ingredient> findByCategorie(Ingredient.Categorie categorie) {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT * FROM ingredients WHERE categorie = ? AND actif = 1";
        try (Connection c = dbConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, categorie.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(map(rs));
            }
        } catch (SQLException e) {
            logger.error("Error findByCategorie ingredients", e);
        }
        return list;
    }

    @Override
    public Ingredient update(Ingredient ingredient) {
        String sql = "UPDATE ingredients SET nom=?, categorie=?, prix_supplement=?, calories=?, icon_url=?, actif=? WHERE id=?";
        try (Connection c = dbConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ingredient.getNom());
            ps.setString(2, ingredient.getCategorie().name());
            ps.setBigDecimal(3, ingredient.getPrixSupplement());
            ps.setInt(4, ingredient.getCalories());
            ps.setString(5, ingredient.getIconUrl());
            ps.setBoolean(6, ingredient.isActif());
            ps.setLong(7, ingredient.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error update ingredient", e);
        }
        return ingredient;
    }

    @Override
    public boolean delete(Long id) {
        try (Connection c = dbConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM ingredients WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error delete ingredient", e);
        }
        return false;
    }

    private Ingredient map(ResultSet rs) throws SQLException {
        Ingredient i = new Ingredient();
        i.setId(rs.getLong("id"));
        i.setNom(rs.getString("nom"));
        i.setCategorie(Ingredient.Categorie.valueOf(rs.getString("categorie")));
        i.setPrixSupplement(rs.getBigDecimal("prix_supplement"));
        i.setCalories(rs.getInt("calories"));
        i.setIconUrl(rs.getString("icon_url"));
        i.setActif(rs.getBoolean("actif"));
        return i;
    }
}
