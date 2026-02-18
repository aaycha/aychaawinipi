package com.gestion.services;

import com.gestion.entities.ParticipantRestauration;
import com.gestion.entities.Restauration;
import com.gestion.interfaces.RestaurationService;
import com.gestion.tools.MyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RestaurationServiceImpl implements RestaurationService {

    private static final Logger logger = LoggerFactory.getLogger(RestaurationServiceImpl.class);
    private final MyConnection dbConnection = MyConnection.getInstance();

    private <T> List<T> executeQuery(String sql,
                                     SQLConsumer<PreparedStatement> setter,
                                     SQLFunction<ResultSet, T> mapper) {
        List<T> list = new java.util.ArrayList<>();
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
                            T item = mapper.apply(rs);
                            if (item != null) list.add(item);
                        } catch (Exception e) {
                            logger.error("Mapping error in executeQuery for SQL: {}", sql, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("SQL Execution Error for query: {}", sql, e);
        }
        return list;
    }

    // ================= RESTAURATION UNIFIÉE - STREAM =================

    @Override
    public Restauration create(Restauration r) {
        if (r == null || r.getType() == null) return null;
        switch (r.getType()) {
            case MENU -> insertMenu(r);
            case OPTION -> insertOption(r);
            case REPAS -> insertRepas(r);
            case RESTRICTION -> insertRestriction(r);
            case PRESENCE -> insertPresence(r);
        }
        return r;
    }

    @Override
    public Optional<Restauration> findById(Long id, Restauration.TypeRestauration type) {
        return findAll(type).stream().filter(x -> id.equals(x.getId())).findFirst();
    }

    @Override
    public List<Restauration> findAll(Restauration.TypeRestauration type) {
        return switch (type) {
            case MENU -> executeQuery("SELECT * FROM menu_proposition", null, this::mapMenu).stream()
                    .peek(m -> m.setType(Restauration.TypeRestauration.MENU))
                    .collect(Collectors.toList());
            case OPTION -> executeQuery("SELECT * FROM option_restauration", null, this::mapOption).stream()
                    .peek(o -> o.setType(Restauration.TypeRestauration.OPTION))
                    .collect(Collectors.toList());
            case REPAS -> executeQuery("SELECT * FROM repas", null, this::mapRepas).stream()
                    .peek(x -> x.setType(Restauration.TypeRestauration.REPAS))
                    .collect(Collectors.toList());
            case RESTRICTION -> executeQuery("SELECT * FROM restriction_alimentaire", null, this::mapRestriction).stream()
                    .peek(x -> x.setType(Restauration.TypeRestauration.RESTRICTION))
                    .collect(Collectors.toList());
            case PRESENCE -> executeQuery("SELECT * FROM presence", null, this::mapPresence).stream()
                    .peek(x -> x.setType(Restauration.TypeRestauration.PRESENCE))
                    .collect(Collectors.toList());
        };
    }

    @Override
    public Restauration update(Restauration r) {
        if (r == null || r.getType() == null || r.getId() == null) {
            return r;
        }
        try {
            switch (r.getType()) {
                case MENU -> updateMenu(r);
                case REPAS -> updateRepas(r);
                default -> throw new UnsupportedOperationException("Update non supporté pour le type " + r.getType());
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour de la restauration", e);
        }
        return r;
    }

    @Override
    public boolean delete(Long id, Restauration.TypeRestauration type) {
        if (id == null || type == null) {
            return false;
        }
        return switch (type) {
            case MENU -> deleteById("DELETE FROM menu_proposition WHERE id=?", id);
            case REPAS -> deleteById("DELETE FROM repas WHERE id=?", id);
            default -> throw new UnsupportedOperationException("Suppression non supportée pour le type " + type);
        };
    }

    @Override
    public List<Restauration> findMenusActifs() {
        return findAll(Restauration.TypeRestauration.MENU).stream()
                .filter(Restauration::isActif)
                .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getNom(), b.getNom()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Restauration> findOptionsByTypeEvenement(String typeEvenement) {
        // Récupérer toutes les options (la colonne type_evenement peut ne pas exister dans certaines bases)
        List<Restauration> allOptions = findAll(Restauration.TypeRestauration.OPTION);
        
        // Filtrer par type_evenement si spécifié
        if (typeEvenement != null && !typeEvenement.isBlank()) {
            return allOptions.stream()
                    .peek(o -> o.setType(Restauration.TypeRestauration.OPTION))
                    .filter(Restauration::isActif)
                    .filter(o -> typeEvenement.equalsIgnoreCase(o.getTypeEvenement()))
                    .collect(Collectors.toList());
        }
        
        // Sinon retourner toutes les options actives
        return allOptions.stream()
                .peek(o -> o.setType(Restauration.TypeRestauration.OPTION))
                .filter(Restauration::isActif)
                .collect(Collectors.toList());
    }

    @Override
    public List<Restauration> findRepasByParticipantId(Long participantId) {
        String sql = "SELECT * FROM repas WHERE participant_id=?";
        return executeQuery(sql, ps -> ps.setLong(1, participantId), this::mapRepas).stream()
                .peek(r -> r.setType(Restauration.TypeRestauration.REPAS))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Restauration> findRepasByDate(LocalDate date) {
        String sql = "SELECT * FROM repas WHERE date=?";
        return executeQuery(sql, ps -> ps.setDate(1, Date.valueOf(date)), this::mapRepas).stream()
                .peek(r -> r.setType(Restauration.TypeRestauration.REPAS))
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasRepasForParticipantAndDate(Long participantId, LocalDate date) {
        return findRepasByParticipantId(participantId).stream()
                .anyMatch(r -> date.equals(r.getDate()));
    }

    @Override
    public List<Restauration> findRestrictionsActives() {
        return findAll(Restauration.TypeRestauration.RESTRICTION).stream()
                .filter(Restauration::isActif)
                .collect(Collectors.toList());
    }

    @Override
    public List<Restauration> findAllPresences() {
        return executeQuery("SELECT * FROM presence", null, this::mapPresence).stream()
                .peek(p -> p.setType(Restauration.TypeRestauration.PRESENCE))
                .collect(Collectors.toList());
    }

    @Override
    public List<Restauration> findPresencesByParticipantId(Long participantId) {
        return findAllPresences().stream()
                .filter(p -> participantId.equals(p.getParticipantId()))
                .collect(Collectors.toList());
    }

    // ================= INSERTS =================

    private void insertMenu(Restauration r) {
        String sql = "INSERT INTO menu_proposition (nom, option_restauration_id, actif) VALUES (?,?,?)";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getNom());
            ps.setObject(2, r.getOptionRestaurationId());
            ps.setBoolean(3, r.isActif());
            ps.executeUpdate();
            setGeneratedId(ps, r);
        } catch (SQLException e) { logger.error("insertMenu", e); }
    }

    // ================= UPDATES / DELETES =================

    private void updateMenu(Restauration r) throws SQLException {
        String sql = "UPDATE menu_proposition SET nom=?, option_restauration_id=?, actif=? WHERE id=?";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getNom());
            ps.setObject(2, r.getOptionRestaurationId());
            ps.setBoolean(3, r.isActif());
            ps.setLong(4, r.getId());
            ps.executeUpdate();
        }
    }

    private void updateRepas(Restauration r) throws SQLException {
        if (r.getPrix() != null && r.getPrix().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Prix invalide");
        }
        String sql = "UPDATE repas SET nom_repas=?, prix=?, date=?, participant_id=? WHERE id=?";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getNomRepas());
            ps.setBigDecimal(2, r.getPrix());
            ps.setDate(3, Date.valueOf(r.getDate()));
            ps.setLong(4, r.getParticipantId());
            ps.setLong(5, r.getId());
            ps.executeUpdate();
        }
    }

    private boolean deleteById(String sql, Long id) {
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression", e);
            return false;
        }
    }

    private void insertOption(Restauration r) {
        String sql = "INSERT INTO option_restauration (libelle, type_evenement, actif) VALUES (?,?,?)";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getLibelle());
            ps.setString(2, r.getTypeEvenement());
            ps.setBoolean(3, r.isActif());
            ps.executeUpdate();
            setGeneratedId(ps, r);
        } catch (SQLException e) { logger.error("insertOption", e); }
    }

    private void insertRepas(Restauration r) {
        if (r.getPrix() != null && r.getPrix().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Prix invalide");
        if (hasRepasForParticipantAndDate(r.getParticipantId(), r.getDate()))
            throw new IllegalStateException("Doublon interdit");
        String sql = "INSERT INTO repas (nom_repas, prix, date, participant_id) VALUES (?,?,?,?)";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getNomRepas());
            ps.setBigDecimal(2, r.getPrix());
            ps.setDate(3, Date.valueOf(r.getDate()));
            ps.setLong(4, r.getParticipantId());
            ps.executeUpdate();
            setGeneratedId(ps, r);
        } catch (SQLException e) { logger.error("insertRepas", e); }
    }

    private void insertRestriction(Restauration r) {
        String sql = "INSERT INTO restriction_alimentaire (libelle, description, actif) VALUES (?,?,?)";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getRestrictionLibelle());
            ps.setString(2, r.getRestrictionDescription());
            ps.setBoolean(3, r.isActif());
            ps.executeUpdate();
            setGeneratedId(ps, r);
        } catch (SQLException e) { logger.error("insertRestriction", e); }
    }

    private void insertPresence(Restauration r) {
        String sql = "INSERT INTO presence (participant_id, date, abonnement_actif) VALUES (?,?,?)";
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, r.getParticipantId());
            ps.setDate(2, Date.valueOf(r.getDatePresence()));
            ps.setBoolean(3, r.isAbonnementActif());
            ps.executeUpdate();
            setGeneratedId(ps, r);
        } catch (SQLException e) { logger.error("insertPresence", e); }
    }

    private void setGeneratedId(PreparedStatement ps, Restauration r) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) r.setId(keys.getLong(1));
        }
    }

    // ================= MAPPERS RESTAURATION =================

    private Restauration mapMenu(ResultSet rs) throws SQLException {
        Restauration r = new Restauration(Restauration.TypeRestauration.MENU);
        r.setId(rs.getLong("id"));
        r.setNom(rs.getString("nom"));
        r.setOptionRestaurationId(rs.getLong("option_restauration_id"));
        r.setActif(rs.getBoolean("actif"));
        return r;
    }

    private Restauration mapOption(ResultSet rs) throws SQLException {
        Restauration r = new Restauration(Restauration.TypeRestauration.OPTION);
        r.setId(rs.getLong("id"));
        r.setLibelle(rs.getString("libelle"));
        // Gérer le cas où la colonne type_evenement n'existe pas
        try {
            r.setTypeEvenement(rs.getString("type_evenement"));
        } catch (SQLException e) {
            // Colonne n'existe pas, laisser null
            r.setTypeEvenement(null);
        }
        r.setActif(rs.getBoolean("actif"));
        return r;
    }

    private Restauration mapRepas(ResultSet rs) throws SQLException {
        Restauration r = new Restauration(Restauration.TypeRestauration.REPAS);
        r.setId(rs.getLong("id"));
        r.setNomRepas(rs.getString("nom_repas"));
        r.setPrix(rs.getBigDecimal("prix"));
        Date d = rs.getDate("date");
        if (d != null) r.setDate(d.toLocalDate());
        r.setParticipantId(rs.getLong("participant_id"));
        return r;
    }

    private Restauration mapRestriction(ResultSet rs) throws SQLException {
        Restauration r = new Restauration(Restauration.TypeRestauration.RESTRICTION);
        r.setId(rs.getLong("id"));
        r.setRestrictionLibelle(rs.getString("libelle"));
        r.setRestrictionDescription(rs.getString("description"));
        r.setActif(rs.getBoolean("actif"));
        return r;
    }

    private Restauration mapPresence(ResultSet rs) throws SQLException {
        Restauration r = new Restauration(Restauration.TypeRestauration.PRESENCE);
        r.setId(rs.getLong("id"));
        r.setParticipantId(rs.getLong("participant_id"));
        Date d = rs.getDate("date");
        if (d != null) r.setDatePresence(d.toLocalDate());
        r.setAbonnementActif(rs.getBoolean("abonnement_actif"));
        return r;
    }

    // ================= PARTICIPANT RESTAURATION =================

    @Override
    public ParticipantRestauration createBesoin(ParticipantRestauration besoin) {
        String sql = """
                INSERT INTO participant_restauration
                (participant_id, evenement_id, besoin_libelle, restriction_libelle, niveau_gravite,
                 menu_proposition_id, date_limite_modification, annule)
                VALUES (?,?,?,?,?,?,?,?)
                """;
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, besoin.getParticipantId());
            ps.setLong(2, besoin.getEvenementId());
            ps.setString(3, besoin.getBesoinLibelle());
            ps.setString(4, besoin.getRestrictionLibelle());
            ps.setString(5, besoin.getNiveauGravite());
            ps.setObject(6, besoin.getMenuPropositionId());
            ps.setObject(7, besoin.getDateLimiteModification());
            ps.setBoolean(8, besoin.isAnnule());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) besoin.setId(keys.getLong(1));
            }
        } catch (SQLException e) { logger.error("createBesoin", e); }
        return besoin;
    }

    @Override
    public Optional<ParticipantRestauration> findBesoinById(Long id) {
        return executeQuery("SELECT * FROM participant_restauration WHERE id=?",
                        ps -> ps.setLong(1, id), this::mapParticipantRestauration)
                .stream().findFirst();
    }

    @Override
    public List<ParticipantRestauration> findBesoinsByParticipantId(Long participantId) {
        return executeQuery("SELECT * FROM participant_restauration WHERE participant_id=?",
                ps -> ps.setLong(1, participantId), this::mapParticipantRestauration);
    }

    @Override
    public ParticipantRestauration updateBesoin(ParticipantRestauration besoin) {
        String sql = "UPDATE participant_restauration SET besoin_libelle=?, restriction_libelle=?, niveau_gravite=?, annule=? WHERE id=?";
        try (Connection c = dbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, besoin.getBesoinLibelle());
            ps.setString(2, besoin.getRestrictionLibelle());
            ps.setString(3, besoin.getNiveauGravite());
            ps.setBoolean(4, besoin.isAnnule());
            ps.setLong(5, besoin.getId());
            ps.executeUpdate();
        } catch (SQLException e) { logger.error("updateBesoin", e); }
        return besoin;
    }

    @Override
    public boolean deleteBesoin(Long id) {
        try (Connection c = dbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM participant_restauration WHERE id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { logger.error("deleteBesoin", e); }
        return false;
    }

    @Override
    public boolean peutModifierChoixRepas(Long id) {
        return findBesoinById(id)
                .map(c -> c.getDateLimiteModification() == null || !LocalDate.now().isAfter(c.getDateLimiteModification()))
                .orElse(false);
    }

    private ParticipantRestauration mapParticipantRestauration(ResultSet rs) throws SQLException {
        ParticipantRestauration p = new ParticipantRestauration();
        p.setId(rs.getLong("id"));
        p.setParticipantId(rs.getLong("participant_id"));
        p.setEvenementId(rs.getLong("evenement_id"));
        p.setBesoinLibelle(rs.getString("besoin_libelle"));
        p.setRestrictionLibelle(rs.getString("restriction_libelle"));
        p.setNiveauGravite(rs.getString("niveau_gravite"));
        p.setMenuPropositionId(rs.getLong("menu_proposition_id"));
        Date d = rs.getDate("date_limite_modification");
        if (d != null) p.setDateLimiteModification(d.toLocalDate());
        p.setAnnule(rs.getBoolean("annule"));
        return p;
    }

    @FunctionalInterface
    interface SQLConsumer<T> { void accept(T t) throws Exception; }

    @FunctionalInterface
    interface SQLFunction<T, R> { R apply(T t) throws Exception; }
}
