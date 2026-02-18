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

/**
 * Service de gestion des participations - Version Nettoyée et Synchronisée
 */
public class ParticipationServiceImpl implements ParticipationService {

    private static final Logger logger = LoggerFactory.getLogger(ParticipationServiceImpl.class);
    private final MyConnection dbConnection;
    private final AbonnementService abonnementService;

    public ParticipationServiceImpl() {
        this.dbConnection = MyConnection.getInstance();
        this.abonnementService = new com.gestion.services.AbonnementServiceImpl();
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
            errors.add("User ID obligatoire");
        if (p.getEvenementId() == null || p.getEvenementId() <= 0)
            errors.add("Événement ID obligatoire");
        if (p.getType() == null)
            errors.add("Type obligatoire");
        if (p.getContexteSocial() == null)
            errors.add("Contexte social obligatoire");

        if (!isUpdate && isAlreadyParticipating(p.getUserId(), p.getEvenementId())) {
            errors.add("L'utilisateur est déjà inscrit à cet événement.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors.toArray(new String[0]));
    }

    private void enrichirTarification(Participation p) {
        if (p == null)
            return;
        if (p.getNbAdultes() <= 0)
            p.setNbAdultes(1);
        p.setTotalParticipants(p.getNbAdultes() + p.getNbEnfants());

        boolean estAdherent = false;
        try {
            List<Abonnement> abs = abonnementService.findByUserId(p.getUserId());
            estAdherent = abs.stream().anyMatch(Abonnement::estActif);
        } catch (Exception e) {
            logger.warn("Erreur check abonnement", e);
        }

        BigDecimal montant = new BigDecimal("25.00").multiply(BigDecimal.valueOf(p.getNbAdultes()))
                .add(new BigDecimal("15.00").multiply(BigDecimal.valueOf(p.getNbEnfants())));

        if (estAdherent)
            montant = montant.multiply(new BigDecimal("0.70"));

        p.setMontantCalcule(montant.setScale(2, RoundingMode.HALF_UP));
        p.setTypeAbonnementChoisi(estAdherent ? "ADHERENT" : "STANDARD");
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

        String sql = "INSERT INTO participations (user_id, evenement_id, date_inscription, type, statut, hebergement_nuits, contexte_social, badge_associe, nb_adultes, nb_enfants, nb_chiens, total_participants, type_abonnement, montant_calcule, devise, commentaire, besoins_speciaux) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    p.setId(keys.getLong(1));
            }
            return p;
        } catch (SQLException e) {
            logger.error("Erreur create participation", e);
            throw new RuntimeException(e);
        }
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
        return p;
    }

    @Override
    public Optional<Participation> findById(Long id) {
        String sql = "SELECT * FROM participations WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(map(rs));
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
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(map(rs));
        } catch (SQLException e) {
            logger.error("Error findAll", e);
        }
        return list;
    }

    @Override
    public List<Participation> findByUserId(Long userId) {
        return findAll().stream().filter(p -> p.getUserId().equals(userId)).toList();
    }

    @Override
    public List<Participation> findByEvenementId(Long evenementId) {
        return findAll().stream().filter(p -> p.getEvenementId().equals(evenementId)).toList();
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM participations WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error delete", e);
            return false;
        }
    }

    @Override
    public Participation update(Participation p) {
        if (p.getId() == null)
            throw new IllegalArgumentException("ID manquant");
        String sql = "UPDATE participations SET statut=?, hebergement_nuits=?, contexte_social=?, badge_associe=?, nb_adultes=?, nb_enfants=?, nb_chiens=?, total_participants=?, type_abonnement=?, montant_calcule=?, devise=?, commentaire=?, besoins_speciaux=? WHERE id=?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
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
            ps.setLong(14, p.getId());
            ps.executeUpdate();
            return p;
        } catch (SQLException e) {
            logger.error("Error update", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isAlreadyParticipating(Long userId, Long evenementId) {
        String sql = "SELECT 1 FROM participations WHERE user_id = ? AND evenement_id = ? LIMIT 1";
        try (Connection c = dbConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
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
                .filter(p -> (criteria.getUserId() == null || p.getUserId().equals(criteria.getUserId()))).toList();
    }

    @Override
    public List<Participation> findByStatut(Participation.StatutParticipation statut) {
        return findAll().stream().filter(p -> p.getStatut() == statut).toList();
    }

    @Override
    public List<Participation> findByType(Participation.TypeParticipation type) {
        return findAll().stream().filter(p -> p.getType() == type).toList();
    }

    @Override
    public List<Participation> findByContexteSocial(Participation.ContexteSocial contexte) {
        return findAll().stream().filter(p -> p.getContexteSocial() == contexte).toList();
    }

    @Override
    public List<Participation> findByDateInscriptionBetween(LocalDateTime debut, LocalDateTime fin) {
        return findAll().stream()
                .filter(p -> !p.getDateInscription().isBefore(debut) && !p.getDateInscription().isAfter(fin)).toList();
    }

    @Override
    public List<Participation> findByHebergementNuitsMinimum(int nuitsMin) {
        return findAll().stream().filter(p -> p.getHebergementNuits() >= nuitsMin).toList();
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
                .filter(p -> p.getStatut() == Participation.StatutParticipation.EN_LISTE_ATTENTE).toList();
    }

    @Override
    public Participation confirmerParticipation(Long id) {
        return findById(id).map(p -> {
            p.setStatut(Participation.StatutParticipation.CONFIRME);
            return update(p);
        }).orElse(null);
    }

    @Override
    public Participation annulerParticipation(Long id, String raison) {
        return findById(id).map(p -> {
            p.setStatut(Participation.StatutParticipation.ANNULE);
            p.setCommentaire(p.getCommentaire() + " [Annulé: " + raison + "]");
            return update(p);
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
        return findAll().stream().filter(p -> p.getHebergementNuits() > 0).toList();
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
        return findAll().stream().filter(p -> badge.equals(p.getBadgeAssocie())).toList();
    }

    @Override
    public List<Participation> findParticipationsAvecBadge() {
        return findAll().stream().filter(p -> p.getBadgeAssocie() != null && !p.getBadgeAssocie().isEmpty()).toList();
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
                .toList();
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
        return findAll().stream().filter(p -> "PREMIUM".equals(p.getTypeAbonnementChoisi())).toList();
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