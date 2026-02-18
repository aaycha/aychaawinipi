package com.gestion.services;

import com.gestion.entities.Ticket;
import com.gestion.interfaces.TicketService;
import com.gestion.tools.MyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implémentation du service Ticket - Version Nettoyée
 */
public class TicketServiceImpl implements TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketServiceImpl.class);
    private final MyConnection dbConnection;

    public TicketServiceImpl() {
        this.dbConnection = MyConnection.getInstance();
    }

    @Override
    public Ticket create(Ticket t) {
        if (t.getCodeUnique() == null)
            t.setCodeUnique(UUID.randomUUID().toString().substring(0, 13).toUpperCase());
        if (t.getDateCreation() == null)
            t.setDateCreation(LocalDateTime.now());
        if (t.getDateExpiration() == null)
            t.setDateExpiration(t.getDateCreation().plusDays(7));

        String sql = "INSERT INTO tickets (user_id, participation_id, type, code_unique, latitude, longitude, lieu, statut, format, date_creation, date_expiration, qr_code, informations_supplementaires) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, t.getUserId());
            ps.setLong(2, t.getParticipationId());
            ps.setString(3, t.getType().name());
            ps.setString(4, t.getCodeUnique());
            ps.setObject(5, t.getLatitude(), Types.DOUBLE);
            ps.setObject(6, t.getLongitude(), Types.DOUBLE);
            ps.setString(7, t.getLieu());
            ps.setString(8, t.getStatut().name());
            ps.setString(9, t.getFormat().name());
            ps.setTimestamp(10, Timestamp.valueOf(t.getDateCreation()));
            ps.setTimestamp(11, Timestamp.valueOf(t.getDateExpiration()));
            ps.setString(12, t.getQrCode());
            ps.setString(13, t.getInformationsSupplementaires());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    t.setId(keys.getLong(1));
            }
            return t;
        } catch (SQLException e) {
            logger.error("Error create ticket", e);
            throw new RuntimeException(e);
        }
    }

    private Ticket map(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setId(rs.getLong("id"));
        t.setUserId(rs.getLong("user_id"));
        t.setParticipationId(rs.getLong("participation_id"));
        String typeStr = rs.getString("type");
        if (typeStr != null)
            t.setType(Ticket.TypeTicket.valueOf(typeStr));

        t.setCodeUnique(rs.getString("code_unique"));
        t.setLatitude(rs.getDouble("latitude"));
        t.setLongitude(rs.getDouble("longitude"));
        t.setLieu(rs.getString("lieu"));

        String statutStr = rs.getString("statut");
        if (statutStr != null)
            t.setStatut(Ticket.StatutTicket.valueOf(statutStr));

        String formatStr = rs.getString("format");
        if (formatStr != null)
            t.setFormat(Ticket.FormatTicket.valueOf(formatStr));
        t.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
        Timestamp exp = rs.getTimestamp("date_expiration");
        if (exp != null)
            t.setDateExpiration(exp.toLocalDateTime());
        t.setQrCode(rs.getString("qr_code"));
        t.setInformationsSupplementaires(rs.getString("informations_supplementaires"));
        return t;
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.error("Error findById ticket", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Ticket> findAll() {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets ORDER BY date_creation DESC";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(map(rs));
        } catch (SQLException e) {
            logger.error("Error findAll tickets", e);
        }
        return list;
    }

    @Override
    public List<Ticket> findByParticipationId(Long participationId) {
        return findAll().stream().filter(t -> t.getParticipationId().equals(participationId)).toList();
    }

    @Override
    public List<Ticket> findByUserId(Long userId) {
        return findAll().stream().filter(t -> t.getUserId().equals(userId)).toList();
    }

    @Override
    public List<Ticket> findByType(Ticket.TypeTicket type) {
        return findAll().stream().filter(t -> t.getType() == type).toList();
    }

    @Override
    public List<Ticket> findByStatut(Ticket.StatutTicket statut) {
        return findAll().stream().filter(t -> t.getStatut() == statut).toList();
    }

    @Override
    public List<Ticket> findByFormat(Ticket.FormatTicket format) {
        return findAll().stream().filter(t -> t.getFormat() == format).toList();
    }

    @Override
    public Ticket update(Ticket t) {
        String sql = "UPDATE tickets SET statut=?, format=?, informations_supplementaires=? WHERE id=?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getStatut().name());
            ps.setString(2, t.getFormat().name());
            ps.setString(3, t.getInformationsSupplementaires());
            ps.setLong(4, t.getId());
            ps.executeUpdate();
            return t;
        } catch (SQLException e) {
            logger.error("Error update ticket", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM tickets WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error delete ticket", e);
            return false;
        }
    }

    @Override
    public List<Ticket> findAll(String sortBy, String sortOrder) {
        return findAll();
    }

    @Override
    public List<Ticket> findByCoordonnees(Double latitude, Double longitude, Double rayonKm) {
        return findAll();
    }

    @Override
    public List<Ticket> findByLieu(String lieu) {
        return findAll().stream().filter(t -> t.getLieu() != null && t.getLieu().contains(lieu)).toList();
    }

    @Override
    public Ticket creerTicketSelonChoix(Long participationId, Long userId, Ticket.TypeTicket type, Double latitude,
            Double longitude, String lieu, Ticket.FormatTicket format) {
        Ticket t = new Ticket();
        t.setParticipationId(participationId);
        t.setUserId(userId);
        t.setType(type);
        t.setLatitude(latitude);
        t.setLongitude(longitude);
        t.setLieu(lieu);
        t.setFormat(format);
        t.setStatut(Ticket.StatutTicket.VALIDE);
        return create(t);
    }

    @Override
    public Ticket marquerCommeUtilise(Long id) {
        return findById(id).map(t -> {
            t.setStatut(Ticket.StatutTicket.UTILISE);
            return update(t);
        }).orElse(null);
    }

    @Override
    public Ticket annulerTicket(Long id) {
        return findById(id).map(t -> {
            t.setStatut(Ticket.StatutTicket.ANNULE);
            return update(t);
        }).orElse(null);
    }

    @Override
    public List<Ticket> findTicketsValides() {
        return findByStatut(Ticket.StatutTicket.VALIDE);
    }

    @Override
    public List<Ticket> findTicketsExpires() {
        return findByStatut(Ticket.StatutTicket.EXPIRE);
    }

    @Override
    public boolean validerTicket(String codeUnique) {
        return findAll().stream()
                .anyMatch(t -> t.getCodeUnique().equals(codeUnique) && t.getStatut() == Ticket.StatutTicket.VALIDE);
    }

    @Override
    public List<Ticket> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin) {
        return findAll().stream().filter(t -> !t.getDateCreation().isBefore(debut) && !t.getDateCreation().isAfter(fin))
                .toList();
    }

    @Override
    public List<Ticket> findByDateExpirationBefore(LocalDateTime date) {
        return findAll().stream().filter(t -> t.getDateExpiration() != null && t.getDateExpiration().isBefore(date))
                .toList();
    }

    @Override
    public boolean validerTicket(Ticket ticket) {
        return ticket != null && ticket.getUserId() != null;
    }

    @Override
    public boolean peutEtreSupprime(Long id) {
        return true;
    }
}