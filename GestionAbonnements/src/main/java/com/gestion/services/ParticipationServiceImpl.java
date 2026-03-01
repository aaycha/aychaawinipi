package com.gestion.services;

import com.gestion.criteria.ParticipationCriteria;
import com.gestion.entities.Abonnement;
import com.gestion.entities.Participation;
import com.gestion.interfaces.AbonnementService;
import com.gestion.interfaces.ParticipationService;
import com.gestion.tools.MyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des participations - Version Nettoyée et Synchronisée
 */
public class ParticipationServiceImpl implements ParticipationService {

    private static final Logger logger = LoggerFactory.getLogger(ParticipationServiceImpl.class);
    private final MyConnection dbConnection;
    private final AbonnementService abonnementService;
    private final NotificationService notificationService = new NotificationService();
    private final UserService userService = new UserService();

    public ParticipationServiceImpl() {
        this.dbConnection = MyConnection.getInstance();
        this.abonnementService = new com.gestion.services.AbonnementServiceImpl();
        ensureTableMigration();
    }

    private void ensureTableMigration() {
        try (Connection conn = dbConnection.getConnection();
                Statement st = conn.createStatement()) {
            // Check if restaurant_id exists
            try {
                st.execute("ALTER TABLE participations ADD COLUMN restaurant_id BIGINT");
                logger.info("✅ Added column restaurant_id to participations table");
            } catch (SQLException e) {
                // Ignore if already exists (MySQL error 1060 or SQLState 42S21)
                if (!"42S21".equals(e.getSQLState()) && e.getErrorCode() != 1060) {
                    logger.warn("Migration warning (restaurant_id): " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.error("Critical Migration Error: " + e.getMessage());
        }
    }

    private static class ValidationResult {
        final boolean valid;
        final List<String> errors = new ArrayList<>();

        ValidationResult(boolean valid) {
            this.valid = valid;
        }

        String getMessage() {
            return valid ? "Validation réussie" : "Erreurs :\n• " + String.join("\n• ", errors);
        }

        static ValidationResult valid() {
            return new ValidationResult(true);
        }

        static ValidationResult invalid(String... msgs) {
            ValidationResult vr = new ValidationResult(false);
            vr.errors.addAll(Arrays.asList(msgs));
            return vr;
        }
    }

    private ValidationResult validate(Participation p, boolean isUpdate) {
        List<String> errors = new ArrayList<>();

        if (p == null) {
            return ValidationResult.invalid("Participation ne peut pas être null");
        }

        if (p.getUserId() == null || p.getUserId() <= 0)
            errors.add("L'identifiant de l'utilisateur est manquant.");
        if (p.getEvenementId() == null || p.getEvenementId() <= 0)
            errors.add("Aucun événement n'a été sélectionné.");
        if (p.getType() == null)
            errors.add("Le type de participation (Simple, Hébergement, etc.) est requis.");
        if (p.getContexteSocial() == null)
            errors.add("Le contexte social (Solo, Famille, etc.) est requis.");

        if (!isUpdate) {
            if (isAlreadyParticipating(p.getUserId(), p.getEvenementId())) {
                errors.add("Vous êtes déjà inscrit à cet événement.");
            }

            // Règle métier : l'utilisateur doit acheter un abonnement/pass avant la
            // participation
            if (!hasValidAccess(p.getUserId(), p.getEvenementId())) {
                errors.add(
                        "Accès refusé : Vous devez posséder un abonnement global actif ou un 'Pass Expedition' pour cet événement spécifique.");
            }
        }

        // Validation de l'hébergement
        if (p.getHebergementNuits() < 0) {
            errors.add("Le nombre de nuits ne peut pas être négatif.");
        }
        // Si type Hébergement ou contexte spécifique ? (Dépend des règles métier)
        // Mais si p.getHebergementNuits() > 0, on considère que c'est ok.
        // Si le contrôleur a une règle plus stricte, le service l'appuiera ici s'il y a
        // un souci.

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors.toArray(new String[0]));
    }

    private boolean hasValidAccess(Long userId, Long eventId) {
        try {
            List<Abonnement> abs = abonnementService.findByUserId(userId);
            return abs.stream().anyMatch(a -> {
                if (!a.estActif())
                    return false;

                // Si c'est un abonnement global (MENSUEL, ANNUEL, PREMIUM), il donne accès à
                // tout
                if (a.getType() != Abonnement.TypeAbonnement.EVENEMENT_PASS) {
                    return true;
                }

                // Si c'est un pass, il doit correspondre à l'événement spécifique
                return a.getEvenementId() != null && a.getEvenementId().equals(eventId);
            });
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification de l'accès", e);
            return false;
        }
    }

    private void enrichirTarification(Participation p) {
        if (p == null)
            return;
        if (p.getNbAdultes() <= 0)
            p.setNbAdultes(1);
        p.setTotalParticipants(p.getNbAdultes() + p.getNbEnfants());

        boolean estAdherent = false;
        Long activeAbonnementId = null;
        try {
            List<Abonnement> abs = abonnementService.findByUserId(p.getUserId());
            Optional<Abonnement> activeAbs = abs.stream().filter(Abonnement::estActif).findFirst();
            if (activeAbs.isPresent()) {
                estAdherent = true;
                activeAbonnementId = activeAbs.get().getId();
            }
        } catch (Exception e) {
            logger.warn("Erreur check abonnement", e);
        }

        BigDecimal montant = new BigDecimal("25.00").multiply(BigDecimal.valueOf(p.getNbAdultes()))
                .add(new BigDecimal("15.00").multiply(BigDecimal.valueOf(p.getNbEnfants())));

        if (estAdherent)
            montant = montant.multiply(new BigDecimal("0.70"));

        p.setMontantCalcule(montant.setScale(2, RoundingMode.HALF_UP));
        p.setTypeAbonnementChoisi(estAdherent ? "ADHERENT" : "STANDARD");
        p.setAbonnementId(activeAbonnementId);
        if (p.getDevise() == null)
            p.setDevise("TND");
        if (p.getDateInscription() == null)
            p.setDateInscription(LocalDateTime.now());
        if (p.getStatut() == null)
            p.setStatut(Participation.StatutParticipation.EN_ATTENTE);
    }

    @Override
    public Participation create(Participation p) {
        enrichirTarification(p);
        ValidationResult vr = validate(p, false);
        if (!vr.valid)
            throw new IllegalArgumentException(vr.getMessage());

        String sqlWithAb = "INSERT INTO participations (user_id, evenement_id, date_inscription, type, statut, hebergement_nuits, contexte_social, badge_associe, nb_adultes, nb_enfants, nb_chiens, total_participants, type_abonnement, montant_calcule, devise, commentaire, besoins_speciaux, abonnement_id, restaurant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlWithoutAb = "INSERT INTO participations (user_id, evenement_id, date_inscription, type, statut, hebergement_nuits, contexte_social, badge_associe, nb_adultes, nb_enfants, nb_chiens, total_participants, type_abonnement, montant_calcule, devise, commentaire, besoins_speciaux, restaurant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                logger.error("Connexion à la base de données indisponible pour create");
                throw new RuntimeException("Connexion à la base de données indisponible.");
            }

            // Tentative avec abonnement_id
            try (PreparedStatement ps = conn.prepareStatement(sqlWithAb, Statement.RETURN_GENERATED_KEYS)) {
                fillPreparedStatement(ps, p, true);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next())
                        p.setId(keys.getLong(1));
                }
                return p;
            } catch (SQLException e) {
                if (e.getMessage().contains("abonnement_id")) {
                    logger.warn("Colonne 'abonnement_id' absente, repli sur la requête simplifiée.");
                    try (PreparedStatement ps = conn.prepareStatement(sqlWithoutAb, Statement.RETURN_GENERATED_KEYS)) {
                        fillPreparedStatement(ps, p, false);
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next())
                                p.setId(keys.getLong(1));
                        }
                        return p;
                    }
                }
                throw e;
            }
        } catch (SQLException e) {
            handleSqlError(e, "create");
            return null;
        }
    }

    private void fillPreparedStatement(PreparedStatement ps, Participation p, boolean withAbonnement)
            throws SQLException {
        ps.setLong(1, p.getUserId());
        ps.setLong(2, p.getEvenementId());
        ps.setTimestamp(3, Timestamp.valueOf(p.getDateInscription()));
        ps.setString(4, p.getType().name());
        ps.setString(5, p.getStatut().name());
        ps.setInt(6, p.getHebergementNuits());
        ps.setString(7, p.getContexteSocial().name());
        ps.setString(8, p.getBadgeAssocie());
        ps.setInt(9, p.getNbAdultes());
        ps.setInt(10, p.getNbEnfants());
        ps.setInt(11, p.getNbChiens());
        ps.setInt(12, p.getTotalParticipants());
        ps.setString(13, p.getTypeAbonnementChoisi());
        ps.setBigDecimal(14, p.getMontantCalcule());
        ps.setString(15, p.getDevise());
        ps.setString(16, p.getCommentaire());
        ps.setString(17, p.getBesoinsSpeciaux());
        if (withAbonnement) {
            if (p.getAbonnementId() != null) {
                ps.setLong(18, p.getAbonnementId());
            } else {
                ps.setNull(18, Types.BIGINT);
            }
            if (p.getRestaurantId() != null) {
                ps.setLong(19, p.getRestaurantId());
            } else {
                ps.setNull(19, Types.BIGINT);
            }
        } else {
            if (p.getRestaurantId() != null) {
                ps.setLong(18, p.getRestaurantId());
            } else {
                ps.setNull(18, Types.BIGINT);
            }
        }
    }

    private void handleSqlError(SQLException e, String method) {
        String msg = e.getMessage();
        logger.error("Erreur critique lors de {}: {}", method, msg);
        if (msg.contains("Unknown column")) {
            throw new RuntimeException(
                    "Erreur de base de données : certaines colonnes manquent dans la table 'participations'. Veuillez vérifier le schéma SQL.",
                    e);
        }
        throw new RuntimeException("Impossible d'effectuer l'opération " + method + " : " + msg, e);
    }

    private Participation map(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setId(rs.getLong("id"));
        p.setUserId(rs.getLong("user_id"));
        p.setEvenementId(rs.getLong("evenement_id"));
        p.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
        String typeStr = rs.getString("type");
        if (typeStr != null)
            p.setType(Participation.TypeParticipation.valueOf(typeStr));

        String statutStr = rs.getString("statut");
        if (statutStr != null)
            p.setStatut(Participation.StatutParticipation.valueOf(statutStr));

        p.setHebergementNuits(rs.getInt("hebergement_nuits"));

        String contexteStr = rs.getString("contexte_social");
        if (contexteStr != null)
            p.setContexteSocial(Participation.ContexteSocial.valueOf(contexteStr));
        p.setBadgeAssocie(rs.getString("badge_associe"));
        p.setNbAdultes(rs.getInt("nb_adultes"));
        p.setNbEnfants(rs.getInt("nb_enfants"));
        p.setNbChiens(rs.getInt("nb_chiens"));
        p.setTotalParticipants(rs.getInt("total_participants"));
        p.setTypeAbonnementChoisi(rs.getString("type_abonnement"));
        p.setMontantCalcule(rs.getBigDecimal("montant_calcule"));
        p.setDevise(rs.getString("devise"));
        p.setCommentaire(rs.getString("commentaire"));
        p.setBesoinsSpeciaux(rs.getString("besoins_speciaux"));

        // Gestion sécurisée de la colonne abonnement_id (évite le crash si la migration
        // n'est pas faite)
        try {
            long abId = rs.getLong("abonnement_id");
            if (!rs.wasNull())
                p.setAbonnementId(abId);
        } catch (SQLException e) {
        }

        try {
            long restId = rs.getLong("restaurant_id");
            if (!rs.wasNull())
                p.setRestaurantId(restId);
        } catch (SQLException e) {
        }

        return p;
    }

    @Override
    public Optional<Participation> findById(Long id) {
        String sql = "SELECT * FROM participations WHERE id = ?";
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                logger.error("Connexion à la base de données indisponible pour findById");
                return Optional.empty();
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error findById", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Participation> findAll() {
        List<Participation> list = new ArrayList<>();
        String sql = "SELECT * FROM participations ORDER BY date_inscription DESC";
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                logger.error("Connexion à la base de données indisponible pour findAll");
                return list;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(map(rs));
            }
        } catch (SQLException e) {
            logger.error("Error findAll", e);
        }
        return list;
    }

    @Override
    public List<Participation> findByUserId(Long userId) {
        return findAll().stream().filter(p -> p.getUserId().equals(userId)).collect(Collectors.toList());
    }

    @Override
    public List<Participation> findByEvenementId(Long evenementId) {
        return findAll().stream().filter(p -> p.getEvenementId().equals(evenementId)).collect(Collectors.toList());
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM participations WHERE id = ?";
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                logger.error("Connexion à la base de données indisponible pour delete");
                return false;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.error("Error delete", e);
            return false;
        }
    }

    @Override
    public Participation update(Participation p) {
        if (p.getId() == null)
            throw new IllegalArgumentException("ID manquant");

        String sqlWithAb = "UPDATE participations SET statut=?, hebergement_nuits=?, contexte_social=?, badge_associe=?, nb_adultes=?, nb_enfants=?, nb_chiens=?, total_participants=?, type_abonnement=?, montant_calcule=?, devise=?, commentaire=?, besoins_speciaux=?, abonnement_id=?, restaurant_id=? WHERE id=?";
        String sqlWithoutAb = "UPDATE participations SET statut=?, hebergement_nuits=?, contexte_social=?, badge_associe=?, nb_adultes=?, nb_enfants=?, nb_chiens=?, total_participants=?, type_abonnement=?, montant_calcule=?, devise=?, commentaire=?, besoins_speciaux=?, restaurant_id=? WHERE id=?";

        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                logger.error("Connexion à la base de données indisponible pour update");
                throw new RuntimeException("Connexion à la base de données indisponible.");
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlWithAb)) {
                fillUpdatePreparedStatement(ps, p, true);
                ps.executeUpdate();
                return p;
            } catch (SQLException e) {
                if (e.getMessage().contains("abonnement_id")) {
                    logger.warn("Colonne 'abonnement_id' absente lors de l'update, repli sur la requête simplifiée.");
                    try (PreparedStatement ps = conn.prepareStatement(sqlWithoutAb)) {
                        fillUpdatePreparedStatement(ps, p, false);
                        ps.executeUpdate();
                        return p;
                    }
                }
                throw e;
            }
        } catch (SQLException e) {
            handleSqlError(e, "update");
            return null;
        }
    }

    private void fillUpdatePreparedStatement(PreparedStatement ps, Participation p, boolean withAbonnement)
            throws SQLException {
        ps.setString(1, p.getStatut().name());
        ps.setInt(2, p.getHebergementNuits());
        ps.setString(3, p.getContexteSocial().name());
        ps.setString(4, p.getBadgeAssocie());
        ps.setInt(5, p.getNbAdultes());
        ps.setInt(6, p.getNbEnfants());
        ps.setInt(7, p.getNbChiens());
        ps.setInt(8, p.getTotalParticipants());
        ps.setString(9, p.getTypeAbonnementChoisi());
        ps.setBigDecimal(10, p.getMontantCalcule());
        ps.setString(11, p.getDevise());
        ps.setString(12, p.getCommentaire());
        ps.setString(13, p.getBesoinsSpeciaux());
        if (withAbonnement) {
            if (p.getAbonnementId() != null) {
                ps.setLong(14, p.getAbonnementId());
            } else {
                ps.setNull(14, Types.BIGINT);
            }
            if (p.getRestaurantId() != null) {
                ps.setLong(15, p.getRestaurantId());
            } else {
                ps.setNull(15, Types.BIGINT);
            }
            ps.setLong(16, p.getId());
        } else {
            if (p.getRestaurantId() != null) {
                ps.setLong(14, p.getRestaurantId());
            } else {
                ps.setNull(14, Types.BIGINT);
            }
            ps.setLong(15, p.getId());
        }
    }

    @Override
    public boolean isAlreadyParticipating(Long userId, Long evenementId) {
        String sql = "SELECT 1 FROM participations WHERE user_id = ? AND evenement_id = ? LIMIT 1";
        try (Connection c = dbConnection.getConnection()) {
            if (c == null) {
                logger.error("Connexion à la base de données indisponible pour isAlreadyParticipating");
                return false;
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setLong(2, evenementId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            return false;
        }
    }

    // Autres méthodes de l'interface non détaillées ici par souci de brièveté mais
    // nécessaires
    @Override
    public List<Participation> findAll(String sortBy, String sortOrder) {
        return findAll();
    }

    @Override
    public Optional<Participation> findOneById(Long id) {
        return findById(id);
    }

    @Override
    public List<Participation> search(ParticipationCriteria criteria) {
        return findAll().stream()
                .filter(p -> (criteria.getUserId() == null || p.getUserId().equals(criteria.getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Participation> findByStatut(Participation.StatutParticipation statut) {
        return findAll().stream().filter(p -> p.getStatut() == statut).collect(Collectors.toList());
    }

    @Override
    public List<Participation> findByType(Participation.TypeParticipation type) {
        return findAll().stream().filter(p -> p.getType() == type).collect(Collectors.toList());
    }

    @Override
    public List<Participation> findByContexteSocial(Participation.ContexteSocial contexte) {
        return findAll().stream().filter(p -> p.getContexteSocial() == contexte).collect(Collectors.toList());
    }

    @Override
    public List<Participation> findByDateInscriptionBetween(LocalDateTime debut, LocalDateTime fin) {
        return findAll().stream()
                .filter(p -> !p.getDateInscription().isBefore(debut) && !p.getDateInscription().isAfter(fin))
                .collect(Collectors.toList());
    }

    @Override
    public List<Participation> findByHebergementNuitsMinimum(int nuitsMin) {
        return findAll().stream().filter(p -> p.getHebergementNuits() >= nuitsMin).collect(Collectors.toList());
    }

    @Override
    public List<Participation> findParticipationsConfirmees() {
        return findByStatut(Participation.StatutParticipation.CONFIRME);
    }

    @Override
    public List<Participation> findParticipationsEnAttente() {
        return findByStatut(Participation.StatutParticipation.EN_ATTENTE);
    }

    @Override
    public List<Participation> findListeAttente(Long evenementId) {
        return findByEvenementId(evenementId).stream()
                .filter(p -> p.getStatut() == Participation.StatutParticipation.EN_LISTE_ATTENTE)
                .collect(Collectors.toList());
    }

    @Override
    public Participation confirmerParticipation(Long id) {
        return findById(id).map(p -> {
            p.setStatut(Participation.StatutParticipation.CONFIRME);
            Participation updated = update(p);
            if (updated != null) {
                try {
                    // Send Notification
                    notificationService.create(new com.gestion.entities.Notification(
                            p.getUserId(),
                            "Participation Confirmée",
                            "Votre participation à l'événement a été approuvée par l'administrateur.",
                            com.gestion.entities.Notification.NotificationType.SUCCESS));

                    // Award Loyalty Points
                    com.gestion.entities.User user = userService.getUserById(p.getUserId().intValue());
                    if (user != null) {
                        user.setLoyaltyPoints(user.getLoyaltyPoints() + p.getPointsEarned());
                        userService.modifier(user);
                    }
                } catch (Exception e) {
                    logger.error("Erreur lors de la notification/fidélité: " + e.getMessage());
                }
            }
            return updated;
        }).orElse(null);
    }

    @Override
    public Participation annulerParticipation(Long id, String raison) {
        return findById(id).map(p -> {
            p.setStatut(Participation.StatutParticipation.ANNULE);
            p.setCommentaire(p.getCommentaire() + " [Annulé: " + raison + "]");
            Participation updated = update(p);
            if (updated != null) {
                try {
                    notificationService.create(new com.gestion.entities.Notification(
                            p.getUserId(),
                            "Participation Refusée/Annulée",
                            "Votre participation a été annulée. Raison: " + raison,
                            com.gestion.entities.Notification.NotificationType.WARNING));
                } catch (Exception e) {
                    logger.error("Erreur notification annulation: " + e.getMessage());
                }
            }
            return updated;
        }).orElse(null);
    }

    @Override
    public Participation ajouterListeAttente(Long id) {
        return findById(id).map(p -> {
            p.setStatut(Participation.StatutParticipation.EN_LISTE_ATTENTE);
            return update(p);
        }).orElse(null);
    }

    @Override
    public Participation promouvoirListeAttente(Long id) {
        return confirmerParticipation(id);
    }

    @Override
    public boolean verifierDisponibiliteEvenement(Long evenementId) {
        return getPlacesDisponibles(evenementId) > 0;
    }

    @Override
    public int getPlacesDisponibles(Long evenementId) {
        return 100 - getNombreParticipantsConfirmes(evenementId);
    }

    @Override
    public int getNombreParticipantsConfirmes(Long evenementId) {
        return (int) findByEvenementId(evenementId).stream()
                .filter(p -> p.getStatut() == Participation.StatutParticipation.CONFIRME).count();
    }

    @Override
    public List<Participation> findAvecHebergement() {
        return findAll().stream().filter(p -> p.getHebergementNuits() > 0).collect(Collectors.toList());
    }

    @Override
    public Participation modifierHebergement(Long id, int nouvellesNuits) {
        return findById(id).map(p -> {
            p.setHebergementNuits(nouvellesNuits);
            return update(p);
        }).orElse(null);
    }

    @Override
    public boolean validerHebergement(Participation participation) {
        return participation.getHebergementNuits() >= 0;
    }

    @Override
    public void attribuerBadge(Long id) {
        findById(id).ifPresent(p -> {
            p.setBadgeAssocie("BADGE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            update(p);
        });
    }

    @Override
    public List<Participation> findByBadge(String badge) {
        return findAll().stream().filter(p -> badge.equals(p.getBadgeAssocie())).collect(Collectors.toList());
    }

    @Override
    public List<Participation> findParticipationsAvecBadge() {
        return findAll().stream().filter(p -> p.getBadgeAssocie() != null && !p.getBadgeAssocie().isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public int calculerPointsParticipation(Long userId) {
        return (int) findByUserId(userId).stream()
                .filter(p -> p.getStatut() == Participation.StatutParticipation.CONFIRME).count() * 10;
    }

    @Override
    public List<Participation> suggestionsMatchingGroupe(Long participationId) {
        return new ArrayList<>();
    }

    @Override
    public List<Participation> findParticipationsSimilaires(Long userId, Participation.ContexteSocial contexte) {
        return findAll().stream().filter(p -> !p.getUserId().equals(userId) && p.getContexteSocial() == contexte)
                .collect(Collectors.toList());
    }

    @Override
    public boolean creerMatchingGroupe(List<Long> participationIds) {
        return true;
    }

    @Override
    public long countByStatut(Participation.StatutParticipation statut) {
        return findByStatut(statut).size();
    }

    @Override
    public long countByType(Participation.TypeParticipation type) {
        return findByType(type).size();
    }

    @Override
    public long countByContexteSocial(Participation.ContexteSocial contexte) {
        return findByContexteSocial(contexte).size();
    }

    @Override
    public List<Participation> findParticipationsPeriod(LocalDateTime debut, LocalDateTime fin) {
        return findByDateInscriptionBetween(debut, fin);
    }

    @Override
    public double calculerTauxConfirmation(Long evenementId) {
        List<Participation> all = findByEvenementId(evenementId);
        if (all.isEmpty())
            return 0.0;
        return (double) all.stream().filter(p -> p.getStatut() == Participation.StatutParticipation.CONFIRME).count()
                / all.size();
    }

    @Override
    public double calculerTauxAnnulation(Long evenementId) {
        List<Participation> all = findByEvenementId(evenementId);
        if (all.isEmpty())
            return 0.0;
        return (double) all.stream().filter(p -> p.getStatut() == Participation.StatutParticipation.ANNULE).count()
                / all.size();
    }

    @Override
    public boolean validerParticipation(Participation participation) {
        return validate(participation, participation.getId() != null).valid;
    }

    @Override
    public boolean verifierConflitDates(Long userId, Long evenementId) {
        return false;
    }

    @Override
    public boolean peutEtreSupprimee(Long id) {
        return true;
    }

    @Override
    public List<Participation> findParticipationsAvecRecommandations() {
        return new ArrayList<>();
    }

    @Override
    public boolean synchroniserAvecTransport(Long id) {
        return true;
    }

    @Override
    public boolean synchroniserAvecPaiement(Long id) {
        return true;
    }

    @Override
    public List<Participation> findParticipationsAbonnementPremium() {
        return findAll().stream().filter(p -> "PREMIUM".equals(p.getTypeAbonnementChoisi()))
                .collect(Collectors.toList());
    }

    @Override
    public String exporterCalendrier(Long userId) {
        return "BEGIN:VCALENDAR\nEND:VCALENDAR";
    }

    @Override
    public List<Participation> importerDonneesExterne(String source) {
        return new ArrayList<>();
    }

    @Override
    public boolean integrerCalendrierExterne(Long userId, String icalData) {
        return true;
    }

    @Override
    public void envoyerConfirmationInscription(Long id) {
        logger.info("Notification confirmation inscription pour ID {}", id);
    }

    @Override
    public void envoyerNotificationAnnulation(Long id) {
        logger.info("Notification annulation pour ID {}", id);
    }

    @Override
    public void envoyerNotificationConfirmation(Long id) {
        logger.info("Notification confirmation pour ID {}", id);
    }

    @Override
    public void notifierListeAttente(Long evenementId) {
        logger.info("Notification liste d'attente pour Event {}", evenementId);
    }

    @Override
    public void envoyerRappelEvenement(Long id) {
        logger.info("Rappel événement pour ID {}", id);
    }
}