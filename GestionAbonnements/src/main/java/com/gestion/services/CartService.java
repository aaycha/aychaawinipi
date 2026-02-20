package com.gestion.services;

import com.gestion.entities.RepasDetaille;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service simple pour gérer le panier de l'utilisateur.
 */
public class CartService {

    private static CartService instance;
    private final ObservableMap<RepasDetaille, Integer> items = FXCollections.observableHashMap();

    private CartService() {
    }

    public static synchronized CartService getInstance() {
        if (instance == null) {
            instance = new CartService();
        }
        return instance;
    }

    public void addItem(RepasDetaille repas) {
        items.put(repas, items.getOrDefault(repas, 0) + 1);
    }

    public void removeItem(RepasDetaille repas) {
        if (items.containsKey(repas)) {
            int count = items.get(repas);
            if (count > 1) {
                items.put(repas, count - 1);
            } else {
                items.remove(repas);
            }
        }
    }

    public void clear() {
        items.clear();
    }

    public ObservableMap<RepasDetaille, Integer> getItems() {
        return items;
    }

    public BigDecimal getTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<RepasDetaille, Integer> entry : items.entrySet()) {
            total = total.add(entry.getKey().getPrix().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        return total;
    }

    public int getItemCount() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }
}
