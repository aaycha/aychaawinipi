package com.gestion.services;

import com.gestion.entities.Evenement;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {

    public List<Evenement> recommendFromFilter(FilterCriteria c,
                                               List<Evenement> allEvents,
                                               List<Evenement> filteredEvents,
                                               int limit) {

        if (allEvents == null) return Collections.emptyList();
        if (filteredEvents == null) filteredEvents = Collections.emptyList();
        if (limit <= 0) return Collections.emptyList();

        Set<Integer> filteredIds = filteredEvents.stream()
                .filter(Objects::nonNull)
                .map(Evenement::getIdEvent)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();

        return allEvents.stream()
                .filter(Objects::nonNull)
                .filter(e -> !filteredIds.contains(e.getIdEvent())) // pas dupliquer
                .map(e -> new Scored(e, score(e, c)))
                .filter(s -> s.score > 0)
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.score, a.score);
                    if (cmp != 0) return cmp;

                    // Même score : tri par proximité à maintenant (plus proche en premier)
                    LocalDateTime da = a.event.getDateDebut();
                    LocalDateTime db = b.event.getDateDebut();

                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;

                    long aDist = Math.abs(ChronoUnit.MINUTES.between(da, now));
                    long bDist = Math.abs(ChronoUnit.MINUTES.between(db, now));
                    return Long.compare(aDist, bDist);
                })
                .limit(limit)
                .map(s -> s.event)
                .collect(Collectors.toList()); // ✅ Java 11 (pas .toList())
    }

    private int score(Evenement e, FilterCriteria c) {
        if (e == null || c == null) return 0;

        int s = 0;

        // 1) TYPE (gros poids)
        if (notBlank(c.getType()) && !"TOUS".equalsIgnoreCase(c.getType()) && notBlank(e.getType())) {
            if (c.getType().trim().equalsIgnoreCase(e.getType().trim())) s += 60;
        }

        // 2) DATE (proximité)
        if (c.getDateFrom() != null && e.getDateDebut() != null) {
            long diff = Math.abs(ChronoUnit.DAYS.between(e.getDateDebut(), c.getDateFrom()));
            if (diff <= 7) s += 25;
            else if (diff <= 30) s += 10;
        }

        if (c.getDateTo() != null && e.getDateDebut() != null) {
            if (!e.getDateDebut().isAfter(c.getDateTo())) s += 10;
        }

        // 3) KEYWORD (titre/description/lieu)
        if (notBlank(c.getKeyword())) {
            String kw = c.getKeyword().trim().toLowerCase(Locale.ROOT);
            String hay = (safe(e.getTitre()) + " " + safe(e.getDescription()) + " " + safe(e.getLieu()))
                    .toLowerCase(Locale.ROOT);

            if (hay.contains(kw)) s += 30;
        }

        // Si l'utilisateur n'a rien filtré, on propose quand même
        boolean noFilter =
                (!notBlank(c.getType()) || "TOUS".equalsIgnoreCase(c.getType())) &&
                        c.getDateFrom() == null &&
                        c.getDateTo() == null &&
                        !notBlank(c.getKeyword());

        if (noFilter) {
            LocalDateTime now = LocalDateTime.now();
            if (e.getDateDebut() == null) return 5;
            if (e.getDateDebut().isAfter(now)) return 10;
            return 5;
        }

        return s;
    }

    private static class Scored {
        Evenement event;
        int score;

        Scored(Evenement e, int s) {
            this.event = e;
            this.score = s;
        }
    }

    // ✅ version safe (évite isBlank si ton projet est mal configuré)
    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}