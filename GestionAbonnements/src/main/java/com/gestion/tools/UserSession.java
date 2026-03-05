package com.gestion.tools;

import java.util.prefs.Preferences;

/**
 * Gestionnaire de session utilisateur simplifie pour la boutique
 * (EquipementStore).
 * Utilise les Preferences Java pour persister l'identifiant localement.
 */
public class UserSession {
    private static final String USER_ID_KEY = "lamma_user_id";
    private static final Preferences prefs = Preferences.userNodeForPackage(UserSession.class);

    public static String getUserId() {
        return prefs.get(USER_ID_KEY, null);
    }

    public static void saveUserId(String userId) {
        if (userId != null && !userId.isBlank()) {
            prefs.put(USER_ID_KEY, userId.trim());
        }
    }

    public static void clear() {
        prefs.remove(USER_ID_KEY);
    }
}
