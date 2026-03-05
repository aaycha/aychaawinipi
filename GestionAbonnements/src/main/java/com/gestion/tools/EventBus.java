package com.gestion.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Petit bus d'événements in-process.
 * Usage : EventBus.on("view", payload -> { ... }); EventBus.emit("view",
 * equipementId);
 */
public class EventBus {
    private static final Map<String, List<Consumer<Object>>> listeners = new HashMap<>();

    public static synchronized void on(String event, Consumer<Object> listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    public static synchronized void off(String event, Consumer<Object> listener) {
        List<Consumer<Object>> l = listeners.get(event);
        if (l != null)
            l.remove(listener);
    }

    public static synchronized void emit(String event, Object payload) {
        List<Consumer<Object>> l = listeners.get(event);
        if (l == null)
            return;
        // Make a copy to avoid concurrent modification
        List<Consumer<Object>> copy = new ArrayList<>(l);
        for (Consumer<Object> c : copy) {
            try {
                c.accept(payload);
            } catch (Exception e) {
                System.out.println("❌ EventBus listener error: " + e.getMessage());
            }
        }
    }
}
