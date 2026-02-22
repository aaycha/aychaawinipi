package com.gestion.services;

import com.gestion.entities.Evenement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EvenementService {

    // ====== FILTRES ======

    // Filtrer: événements futurs
    public List<Evenement> futurs(List<Evenement> events) {
        LocalDateTime now = LocalDateTime.now();
        return events.stream()
                .filter(e -> e.getDateDebut() != null && e.getDateDebut().isAfter(now))
                .collect(Collectors.toList());
    }

    // Filtrer: par type (SOIREE/RANDONNEE/CAMPING/SEJOUR)
    public List<Evenement> parType(List<Evenement> events, String type) {
        String t = safeUpper(type);
        return events.stream()
                .filter(e -> safeUpper(e.getType()).equals(t))
                .collect(Collectors.toList());
    }

    // Filtrer: par lieu (contient)
    public List<Evenement> parLieuContient(List<Evenement> events, String keyword) {
        String k = safeLower(keyword);
        return events.stream()
                .filter(e -> safeLower(e.getLieu()).contains(k))
                .collect(Collectors.toList());
    }

    // Filtrer: entre deux dates (sur dateDebut)
    public List<Evenement> entreDates(List<Evenement> events, LocalDateTime from, LocalDateTime to) {
        return events.stream()
                .filter(e -> e.getDateDebut() != null)
                .filter(e -> !e.getDateDebut().isBefore(from) && !e.getDateDebut().isAfter(to))
                .collect(Collectors.toList());
    }

    // ====== TRI ======

    // Trier par dateDebut asc
    public List<Evenement> trierParDateAsc(List<Evenement> events) {
        return events.stream()
                .sorted(Comparator.comparing(Evenement::getDateDebut,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // Trier par dateDebut desc
    public List<Evenement> trierParDateDesc(List<Evenement> events) {
        return events.stream()
                .sorted(Comparator.comparing(Evenement::getDateDebut,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    // Trier par titre A-Z
    public List<Evenement> trierParTitre(List<Evenement> events) {
        return events.stream()
                .sorted(Comparator.comparing(e -> safeLower(e.getTitre())))
                .collect(Collectors.toList());
    }

    // ====== RECHERCHE ======

    // Recherche globale (titre OU description OU lieu) contient keyword
    public List<Evenement> rechercher(List<Evenement> events, String keyword) {
        String k = safeLower(keyword);
        return events.stream()
                .filter(e -> safeLower(e.getTitre()).contains(k) ||
                        safeLower(e.getDescription()).contains(k) ||
                        safeLower(e.getLieu()).contains(k))
                .collect(Collectors.toList());
    }

    // ====== DATABASE ======
    public List<com.gestion.entities.Evenement> findAll() {
        List<com.gestion.entities.Evenement> list = new ArrayList<>();
        com.gestion.tools.MyConnection db = com.gestion.tools.MyConnection.getInstance();
        String sql = "SELECT * FROM evenement ORDER BY date_debut DESC";
        try {
            java.sql.Connection conn = db.getConnection();
            if (conn == null)
                return list;
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
                    java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.gestion.entities.Evenement e = new com.gestion.entities.Evenement();
                    e.setIdEvent(rs.getInt("id_event"));
                    e.setTitre(rs.getString("titre"));
                    e.setLieu(rs.getString("lieu"));
                    java.sql.Timestamp d = rs.getTimestamp("date_debut");
                    if (d != null)
                        e.setDateDebut(d.toLocalDateTime());
                    list.add(e);
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Erreur EvenementService.findAll: " + e.getMessage());
        }
        return list;
    }

    // ====== Helpers ======
    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    private String safeUpper(String s) {
        return s == null ? "" : s.toUpperCase(Locale.ROOT).trim();
    }
}
