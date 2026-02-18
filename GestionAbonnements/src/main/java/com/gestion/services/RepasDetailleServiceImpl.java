package com.gestion.services;

import com.gestion.entities.RepasDetaille;
import com.gestion.interfaces.RepasDetailleService;
import com.gestion.tools.MyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RepasDetailleServiceImpl implements RepasDetailleService {
    
    private static final Logger logger = LoggerFactory.getLogger(RepasDetailleServiceImpl.class);
    private final MyConnection dbConnection = MyConnection.getInstance();
    
    @Override
    public RepasDetaille create(RepasDetaille repas) {
        String sql = """
            INSERT INTO repas_detaille 
            (nom, description, prix, calories, type_repas, date, participant_id, evenement_id,
             ingredients, allergenes, vegetarien, vegan, sans_gluten, halal, actif, image_url, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, repas.getNom());
            ps.setString(2, repas.getDescription());
            ps.setBigDecimal(3, repas.getPrix());
            ps.setObject(4, repas.getCalories());
            ps.setString(5, repas.getTypeRepas());
            ps.setObject(6, repas.getDate() != null ? Date.valueOf(repas.getDate()) : null);
            ps.setObject(7, repas.getParticipantId());
            ps.setObject(8, repas.getEvenementId());
            ps.setString(9, String.join(",", repas.getIngredients()));
            ps.setString(10, String.join(",", repas.getAllergenes()));
            ps.setBoolean(11, repas.isVegetarien());
            ps.setBoolean(12, repas.isVegan());
            ps.setBoolean(13, repas.isSansGluten());
            ps.setBoolean(14, repas.isHalal());
            ps.setBoolean(15, repas.isActif());
            ps.setString(16, repas.getImageUrl());
            ps.setString(17, repas.getNotes());
            
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) repas.setId(keys.getLong(1));
            }
            logger.info("Repas détaillé créé: ID {}", repas.getId());
        } catch (SQLException e) {
            logger.error("Erreur create repas détaillé", e);
        }
        return repas;
    }
    
    @Override
    public Optional<RepasDetaille> findById(Long id) {
        String sql = "SELECT * FROM repas_detaille WHERE id = ?";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRepas(rs));
            }
        } catch (SQLException e) {
            logger.error("Erreur findById", e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<RepasDetaille> findAll() {
        return executeQuery("SELECT * FROM repas_detaille WHERE actif = 1 ORDER BY nom", null);
    }
    
    @Override
    public RepasDetaille update(RepasDetaille repas) {
        String sql = """
            UPDATE repas_detaille SET 
            nom=?, description=?, prix=?, calories=?, type_repas=?, date=?, participant_id=?, evenement_id=?,
            ingredients=?, allergenes=?, vegetarien=?, vegan=?, sans_gluten=?, halal=?, actif=?, image_url=?, notes=?
            WHERE id=?
            """;
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, repas.getNom());
            ps.setString(2, repas.getDescription());
            ps.setBigDecimal(3, repas.getPrix());
            ps.setObject(4, repas.getCalories());
            ps.setString(5, repas.getTypeRepas());
            ps.setObject(6, repas.getDate() != null ? Date.valueOf(repas.getDate()) : null);
            ps.setObject(7, repas.getParticipantId());
            ps.setObject(8, repas.getEvenementId());
            ps.setString(9, String.join(",", repas.getIngredients()));
            ps.setString(10, String.join(",", repas.getAllergenes()));
            ps.setBoolean(11, repas.isVegetarien());
            ps.setBoolean(12, repas.isVegan());
            ps.setBoolean(13, repas.isSansGluten());
            ps.setBoolean(14, repas.isHalal());
            ps.setBoolean(15, repas.isActif());
            ps.setString(16, repas.getImageUrl());
            ps.setString(17, repas.getNotes());
            ps.setLong(18, repas.getId());
            
            ps.executeUpdate();
            logger.info("Repas détaillé mis à jour: ID {}", repas.getId());
        } catch (SQLException e) {
            logger.error("Erreur update repas détaillé", e);
        }
        return repas;
    }
    
    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM repas_detaille WHERE id = ?";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            int affected = ps.executeUpdate();
            logger.info("Repas détaillé supprimé: ID {}", id);
            return affected > 0;
        } catch (SQLException e) {
            logger.error("Erreur delete repas détaillé", e);
        }
        return false;
    }
    
    @Override
    public List<RepasDetaille> findByParticipantId(Long participantId) {
        return executeQuery("SELECT * FROM repas_detaille WHERE participant_id = ? AND actif = 1 ORDER BY date DESC", 
            ps -> ps.setLong(1, participantId));
    }
    
    @Override
    public List<RepasDetaille> findByEvenementId(Long evenementId) {
        return executeQuery("SELECT * FROM repas_detaille WHERE evenement_id = ? AND actif = 1 ORDER BY date DESC", 
            ps -> ps.setLong(1, evenementId));
    }
    
    @Override
    public List<RepasDetaille> findByDate(LocalDate date) {
        return executeQuery("SELECT * FROM repas_detaille WHERE date = ? AND actif = 1 ORDER BY nom", 
            ps -> ps.setDate(1, Date.valueOf(date)));
    }
    
    @Override
    public List<RepasDetaille> findByTypeRepas(String typeRepas) {
        return executeQuery("SELECT * FROM repas_detaille WHERE type_repas = ? AND actif = 1 ORDER BY nom", 
            ps -> ps.setString(1, typeRepas));
    }
    
    @Override
    public List<RepasDetaille> findByPrixRange(BigDecimal min, BigDecimal max) {
        return executeQuery("SELECT * FROM repas_detaille WHERE prix BETWEEN ? AND ? AND actif = 1 ORDER BY prix", 
            ps -> { ps.setBigDecimal(1, min); ps.setBigDecimal(2, max); });
    }
    
    @Override
    public List<RepasDetaille> findByCaloriesRange(Integer min, Integer max) {
        return executeQuery("SELECT * FROM repas_detaille WHERE calories BETWEEN ? AND ? AND actif = 1 ORDER BY calories", 
            ps -> { ps.setInt(1, min); ps.setInt(2, max); });
    }
    
    @Override
    public List<RepasDetaille> findByRestrictions(boolean vegetarien, boolean vegan, boolean sansGluten, boolean halal) {
        StringBuilder sql = new StringBuilder("SELECT * FROM repas_detaille WHERE actif = 1");
        List<Object> params = new ArrayList<>();
        if (vegetarien) { sql.append(" AND vegetarien = 1"); }
        if (vegan) { sql.append(" AND vegan = 1"); }
        if (sansGluten) { sql.append(" AND sans_gluten = 1"); }
        if (halal) { sql.append(" AND halal = 1"); }
        sql.append(" ORDER BY nom");
        return executeQuery(sql.toString(), null);
    }
    
    @Override
    public List<RepasDetaille> findByAllergene(String allergene) {
        return findAll().stream()
            .filter(r -> r.getAllergenes().contains(allergene))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RepasDetaille> searchByName(String nom) {
        return executeQuery("SELECT * FROM repas_detaille WHERE nom LIKE ? AND actif = 1 ORDER BY nom", 
            ps -> ps.setString(1, "%" + nom + "%"));
    }
    
    @Override
    public BigDecimal getTotalPrixByParticipant(Long participantId) {
        return findByParticipantId(participantId).stream()
            .map(RepasDetaille::getPrix)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Override
    public Integer getTotalCaloriesByParticipant(Long participantId) {
        return findByParticipantId(participantId).stream()
            .mapToInt(r -> r.getCalories() != null ? r.getCalories() : 0)
            .sum();
    }
    
    @Override
    public BigDecimal getMoyennePrix() {
        List<RepasDetaille> all = findAll();
        if (all.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = all.stream().map(RepasDetaille::getPrix).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(all.size()), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    @Override
    public Integer getMoyenneCalories() {
        List<RepasDetaille> all = findAll();
        if (all.isEmpty()) return 0;
        int total = all.stream().mapToInt(r -> r.getCalories() != null ? r.getCalories() : 0).sum();
        return total / all.size();
    }
    
    // Helpers
    private List<RepasDetaille> executeQuery(String sql, SQLConsumer<PreparedStatement> setter) {
        List<RepasDetaille> list = new ArrayList<>();
        Connection c = null;
        try {
            c = dbConnection.getConnection();
            if (c == null) {
                logger.error("Connection value is null for query: {}", sql);
                return list;
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (setter != null) setter.accept(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            list.add(mapRepas(rs));
                        } catch (Exception e) {
                            logger.error("Mapping error in RepasDetaille executeQuery: {}", sql, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("SQL Execution Error in RepasDetaille for query: {}", sql, e);
        }
        return list;
    }
    
    private RepasDetaille mapRepas(ResultSet rs) throws SQLException {
        RepasDetaille r = new RepasDetaille();
        r.setId(rs.getLong("id"));
        r.setNom(rs.getString("nom"));
        r.setDescription(rs.getString("description"));
        r.setPrix(rs.getBigDecimal("prix"));
        r.setCalories(rs.getObject("calories") != null ? rs.getInt("calories") : null);
        r.setTypeRepas(rs.getString("type_repas"));
        Date d = rs.getDate("date");
        if (d != null) r.setDate(d.toLocalDate());
        r.setParticipantId(rs.getObject("participant_id") != null ? rs.getLong("participant_id") : null);
        r.setEvenementId(rs.getObject("evenement_id") != null ? rs.getLong("evenement_id") : null);
        
        String ing = rs.getString("ingredients");
        if (ing != null && !ing.isEmpty()) {
            r.getIngredients().addAll(List.of(ing.split(",")));
        }
        String all = rs.getString("allergenes");
        if (all != null && !all.isEmpty()) {
            r.getAllergenes().addAll(List.of(all.split(",")));
        }
        
        r.setVegetarien(rs.getBoolean("vegetarien"));
        r.setVegan(rs.getBoolean("vegan"));
        r.setSansGluten(rs.getBoolean("sans_gluten"));
        r.setHalal(rs.getBoolean("halal"));
        r.setActif(rs.getBoolean("actif"));
        r.setImageUrl(rs.getString("image_url"));
        r.setNotes(rs.getString("notes"));
        return r;
    }
    
    @FunctionalInterface
    interface SQLConsumer<T> { void accept(T t) throws Exception; }
}
