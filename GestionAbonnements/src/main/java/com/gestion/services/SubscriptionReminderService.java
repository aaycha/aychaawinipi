package com.gestion.services;

import com.gestion.entities.Abonnement;
import com.gestion.interfaces.AbonnementService;
import com.gestion.entities.Notification;
import java.util.List;

public class SubscriptionReminderService {
    private final AbonnementService abonnementService = new AbonnementServiceImpl();
    private final NotificationService notificationService = new NotificationService();

    /**
     * Checks all active subscriptions and sends reminders if they expire within the
     * given days.
     */
    public void checkAndSendReminders(int daysThreshold) {
        try {
            List<Abonnement> all = abonnementService.findAll();
            for (Abonnement a : all) {
                if (a.estActif() && a.estProcheExpiration(daysThreshold)) {
                    // Send notification if not already sent (we could track this in DB, but for now
                    // just send)
                    notificationService.create(new Notification(
                            a.getUserId(),
                            "Rappel d'expiration",
                            "Votre abonnement " + a.getType().getLabel() + " expire le " + a.getDateFin()
                                    + ". Pensez à le renouveler !",
                            Notification.NotificationType.WARNING));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
