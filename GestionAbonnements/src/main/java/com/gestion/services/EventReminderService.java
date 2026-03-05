package com.gestion.services;

import com.gestion.entities.Evenement;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventReminderService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Planifie un rappel "before" (ex: Duration.ofMinutes(30)) avant la dateDebut de l'événement.
     */
    public void scheduleReminder(Evenement ev, Duration before) {
        if (ev == null || ev.getDateDebut() == null || before == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime trigger = ev.getDateDebut().minus(before);

        long delayMs = Duration.between(now, trigger).toMillis();
        if (delayMs <= 0) return; // trop tard ou événement déjà passé

        scheduler.schedule(() -> Platform.runLater(() -> showInfo(
                "Rappel événement",
                safe(ev.getTitre()) + " commence bientôt (" + before.toMinutes() + " min)"
        )), delayMs, TimeUnit.MILLISECONDS);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}