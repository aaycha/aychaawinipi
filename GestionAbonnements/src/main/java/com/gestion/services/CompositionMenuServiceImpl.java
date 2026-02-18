package com.gestion.services;

import com.gestion.entities.CompositionMenu;
import com.gestion.interfaces.CompositionMenuService;
import com.gestion.tools.MyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CompositionMenuServiceImpl implements CompositionMenuService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompositionMenuServiceImpl.class);
    private final MyConnection dbConnection = MyConnection.getInstance();
    
    @Override
    public CompositionMenu create(CompositionMenu composition) {
        String sql = """
            INSERT INTO composition_menu 
            (menu_id, repas_id, ordre, type_repas, date, participant_id, evenement_id, actif, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setLong(1, composition.getMenuId());
            ps.setLong(2, composition.getRepasId());
            ps.setInt(3, composition.getOrdre());
            ps.setString(4, composition.getTypeRepas());
            ps.setObject(5, composition.getDate() != null ? Date.valueOf(composition.getDate()) : null);
            ps.setObject(6, composition.getParticipantId());
            ps.setObject(7, composition.getEvenementId());
            ps.setBoolean(8, composition.isActif());
            ps.setString(9, composition.getNotes());
            
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) composition.setId(keys.getLong(1));
            }
            logger.info("Composition menu créée: ID {}", composition.getId());
        } catch (SQLException e) {
            logger.error("Erreur create composition menu", e);
        }
        return composition;
    }
    
    @Override
    public Optional<CompositionMenu> findById(Long id) {
        String sql = "SELECT * FROM composition_menu WHERE id = ?";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapComposition(rs));
            }
        } catch (SQLException e) {
            logger.error("Erreur findById", e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<CompositionMenu> findAll() {
        return executeQuery("SELECT * FROM composition_menu WHERE actif = 1 ORDER BY menu_id, ordre", null);
    }
    
    @Override
    public CompositionMenu update(CompositionMenu composition) {
        String sql = """
            UPDATE composition_menu SET 
            menu_id=?, repas_id=?, ordre=?, type_repas=?, date=?, participant_id=?, evenement_id=?, actif=?, notes=?
            WHERE id=?
            """;
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, composition.getMenuId());
            ps.setLong(2, composition.getRepasId());
            ps.setInt(3, composition.getOrdre());
            ps.setString(4, composition.getTypeRepas());
            ps.setObject(5, composition.getDate() != null ? Date.valueOf(composition.getDate()) : null);
            ps.setObject(6, composition.getParticipantId());
            ps.setObject(7, composition.getEvenementId());
            ps.setBoolean(8, composition.isActif());
            ps.setString(9, composition.getNotes());
            ps.setLong(10, composition.getId());
            
            ps.executeUpdate();
            logger.info("Composition menu mise à jour: ID {}", composition.getId());
        } catch (SQLException e) {
            logger.error("Erreur update composition menu", e);
        }
        return composition;
    }
    
    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM composition_menu WHERE id = ?";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            int affected = ps.executeUpdate();
            logger.info("Composition menu supprimée: ID {}", id);
            return affected > 0;
        } catch (SQLException e) {
            logger.error("Erreur delete composition menu", e);
        }
        return false;
    }
    
    @Override
    public List<CompositionMenu> findByMenuId(Long menuId) {
        return executeQuery("SELECT * FROM composition_menu WHERE menu_id = ? AND actif = 1 ORDER BY ordre", 
            ps -> ps.setLong(1, menuId));
    }
    
    @Override
    public List<CompositionMenu> findByRepasId(Long repasId) {
        return executeQuery("SELECT * FROM composition_menu WHERE repas_id = ? AND actif = 1 ORDER BY ordre", 
            ps -> ps.setLong(1, repasId));
    }
    
    @Override
    public List<CompositionMenu> findByParticipantId(Long participantId) {
        return executeQuery("SELECT * FROM composition_menu WHERE participant_id = ? AND actif = 1 ORDER BY date DESC", 
            ps -> ps.setLong(1, participantId));
    }
    
    @Override
    public List<CompositionMenu> findByEvenementId(Long evenementId) {
        return executeQuery("SELECT * FROM composition_menu WHERE evenement_id = ? AND actif = 1 ORDER BY date DESC", 
            ps -> ps.setLong(1, evenementId));
    }
    
    @Override
    public List<CompositionMenu> findByDate(LocalDate date) {
        return executeQuery("SELECT * FROM composition_menu WHERE date = ? AND actif = 1 ORDER BY ordre", 
            ps -> ps.setDate(1, Date.valueOf(date)));
    }
    
    @Override
    public boolean deleteByMenuId(Long menuId) {
        String sql = "DELETE FROM composition_menu WHERE menu_id = ?";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, menuId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Erreur deleteByMenuId", e);
        }
        return false;
    }
    
    @Override
    public boolean deleteByRepasId(Long repasId) {
        String sql = "DELETE FROM composition_menu WHERE repas_id = ?";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, repasId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Erreur deleteByRepasId", e);
        }
        return false;
    }
    
    @Override
    public List<CompositionMenu> reorderCompositions(Long menuId, List<Long> repasIds) {
        List<CompositionMenu> updated = new ArrayList<>();
        for (int i = 0; i < repasIds.size(); i++) {
            Long repasId = repasIds.get(i);
            List<CompositionMenu> existing = findByMenuId(menuId).stream()
                .filter(c -> c.getRepasId().equals(repasId))
                .collect(Collectors.toList());
            if (!existing.isEmpty()) {
                CompositionMenu comp = existing.get(0);
                comp.setOrdre(i + 1);
                update(comp);
                updated.add(comp);
            }
        }
        return updated;
    }
    
    @Override
    public List<CompositionMenu> generateMenuFromRestrictions(Long participantId, Long evenementId, LocalDate date) {
        // Logique simplifiée: récupérer les repas compatibles avec les restrictions du participant
        // Cette méthode devrait utiliser ParticipantRestauration pour obtenir les restrictions
        // Pour l'instant, retourne une liste vide - à implémenter selon les besoins métier
        logger.info("Génération menu depuis restrictions pour participant {} événement {} date {}", 
            participantId, evenementId, date);
        return new ArrayList<>();
    }
    
    // Helpers
    private List<CompositionMenu> executeQuery(String sql, SQLConsumer<PreparedStatement> setter) {
        List<CompositionMenu> list = new ArrayList<>();
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (setter != null) setter.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapComposition(rs));
            }
        } catch (Exception e) {
            logger.error("SQL Error", e);
        }
        return list;
    }
    
    private CompositionMenu mapComposition(ResultSet rs) throws SQLException {
        CompositionMenu c = new CompositionMenu();
        c.setId(rs.getLong("id"));
        c.setMenuId(rs.getLong("menu_id"));
        c.setRepasId(rs.getLong("repas_id"));
        c.setOrdre(rs.getInt("ordre"));
        c.setTypeRepas(rs.getString("type_repas"));
        Date d = rs.getDate("date");
        if (d != null) c.setDate(d.toLocalDate());
        c.setParticipantId(rs.getObject("participant_id") != null ? rs.getLong("participant_id") : null);
        c.setEvenementId(rs.getObject("evenement_id") != null ? rs.getLong("evenement_id") : null);
        c.setActif(rs.getBoolean("actif"));
        c.setNotes(rs.getString("notes"));
        return c;
    }
    
    @FunctionalInterface
    interface SQLConsumer<T> { void accept(T t) throws Exception; }
}
