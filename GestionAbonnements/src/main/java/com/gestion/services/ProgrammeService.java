package com.gestion.services;

import com.gestion.entities.Programme;
import com.gestion.interfaces.IProgrammeService;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProgrammeService implements IProgrammeService {

    private final Connection cnx;

    public ProgrammeService() {
        cnx = MyConnection.getInstance().getConnection();
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql =
                "CREATE TABLE IF NOT EXISTS programme (" +
                        "id_prog INT AUTO_INCREMENT PRIMARY KEY," +
                        "event_id INT NOT NULL," +
                        "titre VARCHAR(255) NOT NULL," +
                        "date_debut TIMESTAMP NOT NULL," +
                        "date_fin TIMESTAMP NULL" +
                        ")";
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException ex) {
            System.err.println("❌ ProgrammeService.ensureTableExists: " + ex.getMessage());
        }
    }


    public void add(Programme p) throws SQLException {
        String sql = "INSERT INTO programme (event_id, titre, date_debut, date_fin) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, p.getEventId());
            ps.setString(2, p.getTitre());

            if (p.getDebut() != null) ps.setTimestamp(3, Timestamp.valueOf(p.getDebut()));
            else ps.setNull(3, Types.TIMESTAMP);

            if (p.getFin() != null) ps.setTimestamp(4, Timestamp.valueOf(p.getFin()));
            else ps.setNull(4, Types.TIMESTAMP);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setIdProg(rs.getInt(1));
                }
            }
        }
    }


    public void update(Programme p) throws SQLException {
        String sql = "UPDATE programme SET event_id=?, titre=?, date_debut=?, date_fin=? WHERE id_prog=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, p.getEventId());
            ps.setString(2, p.getTitre());

            if (p.getDebut() != null) ps.setTimestamp(3, Timestamp.valueOf(p.getDebut()));
            else ps.setNull(3, Types.TIMESTAMP);

            if (p.getFin() != null) ps.setTimestamp(4, Timestamp.valueOf(p.getFin()));
            else ps.setNull(4, Types.TIMESTAMP);

            ps.setInt(5, p.getIdProg());
            ps.executeUpdate();
        }
    }


    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM programme WHERE id_prog=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }


    public List<Programme> getAll() throws SQLException {
        String sql = "SELECT * FROM programme ORDER BY id_prog DESC";
        List<Programme> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        }

        return list;
    }

    @Override
    public void ajouter(Programme programme) throws SQLException {

    }

    @Override
    public void modifier(Programme programme) throws SQLException {

    }

    @Override
    public void supprimer(int id) throws SQLException {

    }

    @Override
    public List<Programme> recuperer() throws SQLException {
        return List.of();
    }

    @Override
    public Programme getOneById(int id) throws SQLException {
        String sql = "SELECT * FROM programme WHERE id_prog=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    @Override
    public List<Programme> getByEventId(int eventId) throws SQLException {
        String sql = "SELECT * FROM programme WHERE event_id = ? ORDER BY date_debut ASC";
        List<Programme> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }

        return list;
    }

    // =====================================================
    // ✅ CONTRÔLE CHEVAUCHEMENT (ANTI 2 PROG AU MÊME TEMPS)
    // =====================================================

    /** vrai si un programme du même event chevauche [start, end[ */
    public boolean existsOverlap(int eventId, LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql =
                "SELECT COUNT(*) " +
                        "FROM programme " +
                        "WHERE event_id = ? " +
                        "AND NOT (date_fin <= ? OR date_debut >= ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** récupère le premier programme qui chevauche pour afficher un message clair */
    public Programme getFirstOverlap(int eventId, LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql =
                "SELECT * " +
                        "FROM programme " +
                        "WHERE event_id = ? " +
                        "AND NOT (date_fin <= ? OR date_debut >= ?) " +
                        "ORDER BY date_debut ASC " +
                        "LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    // =====================================================

    private Programme map(ResultSet rs) throws SQLException {
        Programme p = new Programme();

        p.setIdProg(rs.getInt("id_prog"));
        p.setEventId(rs.getInt("event_id"));
        p.setTitre(rs.getString("titre"));

        Timestamp td = rs.getTimestamp("date_debut");
        Timestamp tf = rs.getTimestamp("date_fin");

        p.setDebut(td != null ? td.toLocalDateTime() : null);
        p.setFin(tf != null ? tf.toLocalDateTime() : null);

        return p;
    }
}