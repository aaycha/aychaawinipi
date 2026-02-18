package com.gestion.services;

import com.gestion.criteria.AbonnementCriteria;
import com.gestion.entities.Abonnement;
import com.gestion.interfaces.AbonnementService;
import com.gestion.tools.MyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/**
 * Implémentation du service Abonnement - Version Nettoyée
 */
public class AbonnementServiceImpl implements AbonnementService {

    private static final Logger logger = LoggerFactory.getLogger(AbonnementServiceImpl.class);
    private final MyConnection dbConnection;

    public AbonnementServiceImpl() {
        this.dbConnection = MyConnection.getInstance();
    }

    @Override
    public Abonnement create(Abonnement a) {
        String sql = "INSERT INTO abonnements (user_id, type, date_debut, date_fin, prix, statut, avantages, auto_renew, points_accumules, churn_score) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.getUserId());
            ps.setString(2, a.getType().name());
            ps.setDate(3, Date.valueOf(a.getDateDebut()));
            ps.setDate(4, Date.valueOf(a.getDateFin()));
            ps.setBigDecimal(5, a.getPrix());
            ps.setString(6, a.getStatut().name());
            ps.setString(7, "{}");
            ps.setBoolean(8, a.isAutoRenew());
            ps.setInt(9, a.getPointsAccumules());
            ps.setDouble(10, a.getChurnScore());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    a.setId(keys.getLong(1));
            }
            return a;
        } catch (SQLException e) {
            logger.error("Error create abonnement", e);
            throw new RuntimeException(e);
        }
    }

    private Abonnement map(ResultSet rs) throws SQLException {
        Abonnement a = new Abonnement();
        a.setId(rs.getLong("id"));
        a.setUserId(rs.getLong("user_id"));
        String typeStr = rs.getString("type");
        if (typeStr != null)
            a.setType(Abonnement.TypeAbonnement.valueOf(typeStr));

        a.setDateDebut(rs.getDate("date_debut").toLocalDate());
        a.setDateFin(rs.getDate("date_fin").toLocalDate());
        a.setPrix(rs.getBigDecimal("prix"));

        String statutStr = rs.getString("statut");
        if (statutStr != null)
            a.setStatut(Abonnement.StatutAbonnement.valueOf(statutStr));
        a.setAutoRenew(rs.getBoolean("auto_renew"));
        a.setPointsAccumules(rs.getInt("points_accumules"));
        a.setChurnScore(rs.getDouble("churn_score"));
        return a;
    }

    @Override
    public Optional<Abonnement> findById(Long id) {
        String sql = "SELECT * FROM abonnements WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.error("Error findById abonnement", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Abonnement> findAll() {
        List<Abonnement> list = new ArrayList<>();
        String sql = "SELECT * FROM abonnements ORDER BY id DESC";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(map(rs));
        } catch (SQLException e) {
            logger.error("Error findAll abonnements", e);
        }
        return list;
    }

    @Override
    public List<Abonnement> findByUserId(Long userId) {
        return findAll().stream().filter(a -> a.getUserId().equals(userId)).toList();
    }

    @Override
    public Abonnement update(Abonnement a) {
        String sql = "UPDATE abonnements SET type=?, date_debut=?, date_fin=?, prix=?, statut=?, auto_renew=?, points_accumules=?, churn_score=? WHERE id=?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getType().name());
            ps.setDate(2, Date.valueOf(a.getDateDebut()));
            ps.setDate(3, Date.valueOf(a.getDateFin()));
            ps.setBigDecimal(4, a.getPrix());
            ps.setString(5, a.getStatut().name());
            ps.setBoolean(6, a.isAutoRenew());
            ps.setInt(7, a.getPointsAccumules());
            ps.setDouble(8, a.getChurnScore());
            ps.setLong(9, a.getId());
            ps.executeUpdate();
            return a;
        } catch (SQLException e) {
            logger.error("Error update abonnement", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM abonnements WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error delete abonnement", e);
            return false;
        }
    }

    @Override
    public List<Abonnement> findAll(String sortBy, String sortOrder) {
        return findAll();
    }

    @Override
    public List<Abonnement> search(AbonnementCriteria criteria) {
        return findAll();
    }

    @Override
    public BigDecimal getTotalPointsAccumules() {
        return BigDecimal.valueOf(findAll().stream().mapToInt(Abonnement::getPointsAccumules).sum());
    }

    @Override
    public BigDecimal getTotalPointsAccumulesByUserId(Long userId) {
        return BigDecimal.valueOf(findByUserId(userId).stream().mapToInt(Abonnement::getPointsAccumules).sum());
    }

    @Override
    public List<Abonnement> findByStatut(Abonnement.StatutAbonnement statut) {
        return findAll().stream().filter(a -> a.getStatut() == statut).toList();
    }

    @Override
    public List<Abonnement> findByType(Abonnement.TypeAbonnement type) {
        return findAll().stream().filter(a -> a.getType() == type).toList();
    }

    @Override
    public List<Abonnement> findByDateFinBefore(LocalDate date) {
        return findAll().stream().filter(a -> a.getDateFin().isBefore(date)).toList();
    }

    @Override
    public List<Abonnement> findByDateFinBetween(LocalDate debut, LocalDate fin) {
        return findAll().stream().filter(a -> !a.getDateFin().isBefore(debut) && !a.getDateFin().isAfter(fin)).toList();
    }

    @Override
    public List<Abonnement> findAbonnementsProchesExpiration(int jours) {
        return findByDateFinBefore(LocalDate.now().plusDays(jours));
    }

    @Override
    public List<Abonnement> findByAutoRenew(boolean autoRenew) {
        return findAll().stream().filter(a -> a.isAutoRenew() == autoRenew).toList();
    }

    @Override
    public List<Abonnement> findByPointsMinimum(int pointsMin) {
        return findAll().stream().filter(a -> a.getPointsAccumules() >= pointsMin).toList();
    }

    @Override
    public Abonnement upgradeAbonnement(Long id, Abonnement.TypeAbonnement nouveauType) {
        return findById(id).map(a -> {
            a.setType(nouveauType);
            return update(a);
        }).orElse(null);
    }

    @Override
    public Abonnement downgradeAbonnement(Long id, Abonnement.TypeAbonnement nouveauType) {
        return findById(id).map(a -> {
            a.setType(nouveauType);
            return update(a);
        }).orElse(null);
    }

    @Override
    public boolean toggleAutoRenew(Long id, boolean autoRenew) {
        return findById(id).map(a -> {
            a.setAutoRenew(autoRenew);
            update(a);
            return true;
        }).orElse(false);
    }

    @Override
    public Abonnement renouvelerAbonnement(Long id) {
        return findById(id).map(a -> {
            a.setDateFin(a.getDateFin().plusYears(1));
            return update(a);
        }).orElse(null);
    }

    @Override
    public boolean suspendreAbonnement(Long id, String raison) {
        return findById(id).map(a -> {
            a.setStatut(Abonnement.StatutAbonnement.SUSPENDU);
            update(a);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean reactiverAbonnement(Long id) {
        return findById(id).map(a -> {
            a.setStatut(Abonnement.StatutAbonnement.ACTIF);
            update(a);
            return true;
        }).orElse(false);
    }

    @Override
    public void ajouterPoints(Long id, int points) {
        findById(id).ifPresent(a -> {
            a.setPointsAccumules(a.getPointsAccumules() + points);
            update(a);
        });
    }

    @Override
    public boolean utiliserPoints(Long id, int points) {
        return findById(id).map(a -> {
            if (a.getPointsAccumules() >= points) {
                a.setPointsAccumules(a.getPointsAccumules() - points);
                update(a);
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Override
    public List<Abonnement> findTopUtilisateursParPoints(int limite) {
        return findAll().stream().sorted(Comparator.comparingInt(Abonnement::getPointsAccumules).reversed())
                .limit(limite).toList();
    }

    @Override
    public long countByStatut(Abonnement.StatutAbonnement statut) {
        return findByStatut(statut).size();
    }

    @Override
    public long countByType(Abonnement.TypeAbonnement type) {
        return findByType(type).size();
    }

    @Override
    public BigDecimal calculerRevenuTotal() {
        return findAll().stream().map(Abonnement::getPrix).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculerRevenuParMois(int mois, int annee) {
        return BigDecimal.ZERO;
    }

    @Override
    public List<Abonnement> findAbonnementsRisqueChurn(double seuil) {
        return findAll().stream().filter(a -> a.getChurnScore() >= seuil).toList();
    }

    @Override
    public double calculerTauxRetention(int mois) {
        return 100.0;
    }

    @Override
    public boolean validerAbonnement(Abonnement abonnement) {
        return true;
    }

    @Override
    public boolean verifierDisponiteUpgrade(Long id, Abonnement.TypeAbonnement nouveauType) {
        return true;
    }

    @Override
    public boolean peutEtreSupprime(Long id) {
        return true;
    }

    @Override
    public void envoyerRappelExpiration(Long id) {
        logger.info("Rappel expiration pour ID {}", id);
    }

    @Override
    public void envoyerConfirmationRenouvellement(Long id) {
        logger.info("Confirmation renouvellement pour ID {}", id);
    }

    @Override
    public void envoyerNotificationChangementStatut(Long id, Abonnement.StatutAbonnement ancienStatut) {
        logger.info("Changement statut pour ID {}", id);
    }

    @Override
    public List<Abonnement> findAbonnementsAvecParticipationsActives() {
        return new ArrayList<>();
    }

    @Override
    public List<Abonnement> findAbonnementsSansParticipation(int derniersMois) {
        return new ArrayList<>();
    }

    @Override
    public boolean synchroniserAvecPaiement(Long id) {
        return true;
    }
}
