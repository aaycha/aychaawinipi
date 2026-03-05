package com.gestion.services;

import com.gestion.entities.Evenement;
import com.gestion.interfaces.IEvenementService;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements IEvenementService {

    public EvenementService() {
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS evenement (" +
                "id_event INT AUTO_INCREMENT PRIMARY KEY," +
                "titre VARCHAR(255) NOT NULL," +
                "type VARCHAR(60) NOT NULL," +
                "description TEXT," +
                "date_debut TIMESTAMP NULL," +
                "date_fin TIMESTAMP NULL," +
                "lieu VARCHAR(255)," +
                "image VARCHAR(400)," +
                "spotify_url VARCHAR(500)" +
                ")";

        try (Connection cnx = MyConnection.getConnectionStatic();
                Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);

            // Migration logic for existing tables
            try {
                st.executeUpdate("ALTER TABLE evenement ADD COLUMN image VARCHAR(400)");
                System.out.println("✅ EvenementService: Added missing column 'image'");
            } catch (SQLException ignored) {
            }

            try {
                st.executeUpdate("ALTER TABLE evenement ADD COLUMN spotify_url VARCHAR(500)");
                System.out.println("✅ EvenementService: Added missing column 'spotify_url'");
            } catch (SQLException ignored) {
            }

        } catch (SQLException ex) {
            System.err.println("❌ EvenementService.ensureTableExists: " + ex.getMessage());
        }
    }

    // ----------------- CRUD (English Names) -----------------

    @Override
    public void add(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenement (titre, type, description, date_debut, date_fin, lieu, image, spotify_url) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection cnx = MyConnection.getConnectionStatic();
                PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, safe(e.getTitre()));
            ps.setString(2, safe(e.getType()));
            ps.setString(3, e.getDescription());

            if (e.getDateDebut() != null)
                ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            else
                ps.setNull(4, Types.TIMESTAMP);

            if (e.getDateFin() != null)
                ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            else
                ps.setNull(5, Types.TIMESTAMP);

            ps.setString(6, e.getLieu());
            ps.setString(7, e.getImage());

            String spotify = safe(e.getSpotifyUrl());
            if (spotify.isEmpty())
                ps.setNull(8, Types.VARCHAR);
            else
                ps.setString(8, spotify);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    e.setIdEvent(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Evenement e) throws SQLException {
        String sql = "UPDATE evenement SET titre=?, type=?, description=?, date_debut=?, date_fin=?, lieu=?, image=?, spotify_url=? WHERE id_event=?";

        try (Connection cnx = MyConnection.getConnectionStatic();
                PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, safe(e.getTitre()));
            ps.setString(2, safe(e.getType()));
            ps.setString(3, e.getDescription());

            if (e.getDateDebut() != null)
                ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            else
                ps.setNull(4, Types.TIMESTAMP);

            if (e.getDateFin() != null)
                ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            else
                ps.setNull(5, Types.TIMESTAMP);

            ps.setString(6, e.getLieu());
            ps.setString(7, e.getImage());

            String spotify = safe(e.getSpotifyUrl());
            if (spotify.isEmpty())
                ps.setNull(8, Types.VARCHAR);
            else
                ps.setString(8, spotify);

            ps.setInt(9, e.getIdEvent());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int idEvent) throws SQLException {
        String sql = "DELETE FROM evenement WHERE id_event=?";
        try (Connection cnx = MyConnection.getConnectionStatic();
                PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEvent);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Evenement> getAll() throws SQLException {
        List<Evenement> list = new ArrayList<>();
        String sql = "SELECT * FROM evenement ORDER BY id_event DESC";

        try (Connection cnx = MyConnection.getConnectionStatic();
                Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    // ----------------- CRUD (French Names - IService Aliases) -----------------

    @Override
    public void ajouter(Evenement evenement) throws SQLException {
        add(evenement);
    }

    @Override
    public void modifier(Evenement evenement) throws SQLException {
        update(evenement);
    }

    @Override
    public void supprimer(int id) throws SQLException {
        delete(id);
    }

    @Override
    public List<Evenement> recuperer() throws SQLException {
        return getAll();
    }

    @Override
    public Evenement getOneById(int id) throws SQLException {
        return getById(id);
    }

    @Override
    public Evenement getById(int id) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE id_event=?";
        try (Connection cnx = MyConnection.getConnectionStatic();
                PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return map(rs);
            }
        }
        return null;
    }

    // ----------------- Interface Streams -----------------

    @Override
    public List<Evenement> rechercher(List<Evenement> events, String keyword) {
        if (keyword == null || keyword.isEmpty())
            return events;
        String k = keyword.toLowerCase();
        return events.stream()
                .filter(e -> safe(e.getTitre()).toLowerCase().contains(k) ||
                        safe(e.getLieu()).toLowerCase().contains(k))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Evenement> filtrerParType(List<Evenement> events, String type) {
        if (type == null || type.isEmpty())
            return events;
        return events.stream()
                .filter(e -> safe(e.getType()).equalsIgnoreCase(type))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Evenement> filtrerParLieu(List<Evenement> events, String keyword) {
        if (keyword == null || keyword.isEmpty())
            return events;
        String k = keyword.toLowerCase();
        return events.stream()
                .filter(e -> safe(e.getLieu()).toLowerCase().contains(k))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Evenement> trierParDateAsc(List<Evenement> events) {
        return events.stream()
                .sorted((e1, e2) -> {
                    if (e1.getDateDebut() == null)
                        return 1;
                    if (e2.getDateDebut() == null)
                        return -1;
                    return e1.getDateDebut().compareTo(e2.getDateDebut());
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Evenement> trierParDateDesc(List<Evenement> events) {
        return events.stream()
                .sorted((e1, e2) -> {
                    if (e1.getDateDebut() == null)
                        return 1;
                    if (e2.getDateDebut() == null)
                        return -1;
                    return e2.getDateDebut().compareTo(e1.getDateDebut());
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // ----------------- Helpers -----------------

    private Evenement map(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setIdEvent(rs.getInt("id_event"));
        e.setTitre(rs.getString("titre"));
        e.setType(rs.getString("type"));
        e.setDescription(rs.getString("description"));
        Timestamp td = rs.getTimestamp("date_debut");
        Timestamp tf = rs.getTimestamp("date_fin");
        e.setDateDebut(td != null ? td.toLocalDateTime() : null);
        e.setDateFin(tf != null ? tf.toLocalDateTime() : null);
        e.setLieu(rs.getString("lieu"));
        e.setImage(rs.getString("image"));
        e.setSpotifyUrl(rs.getString("spotify_url"));
        return e;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}