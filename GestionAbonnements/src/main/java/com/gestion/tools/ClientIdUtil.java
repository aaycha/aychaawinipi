package com.gestion.tools;

import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Utilitaire pour generer et conserver un ID client unique sur cette machine.
 */
public class ClientIdUtil {
    private static final String CLIENT_ID_KEY = "lamma_client_uuid";
    private static final Preferences prefs = Preferences.userNodeForPackage(ClientIdUtil.class);

    public static String getClientId() {
        String id = prefs.get(CLIENT_ID_KEY, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.put(CLIENT_ID_KEY, id);
        }
        return id;
    }
}
