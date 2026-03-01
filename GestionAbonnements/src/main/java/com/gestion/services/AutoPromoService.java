package com.gestion.services;

import com.gestion.entities.PromoCode;
import com.gestion.entities.RepasDetaille;
import com.gestion.interfaces.RepasDetailleService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service de génération automatique de codes promo pour les plats les moins
 * vendus.
 * Identifie les 3 plats avec le moins de ventes et leur attribue une réduction.
 */
public class AutoPromoService {

    private static final Logger LOGGER = Logger.getLogger(AutoPromoService.class.getName());
    private static final int DEFAULT_DISCOUNT = 15; // 15%
    private static final int TOP_N_LEAST_SOLD = 3;

    private final RepasDetailleService repasService;
    private final PromoCodeService promoCodeService;

    // Cache des IDs de plats avec promo auto (rechargé à chaque appel)
    private final Map<Long, PromoCode> autoPromoDishes = new HashMap<>();

    public AutoPromoService() {
        this.repasService = new RepasDetailleServiceImpl();
        this.promoCodeService = new PromoCodeService();
    }

    /**
     * Analyse les ventes et crée/met à jour les promos automatiques
     * pour les plats les moins populaires.
     */
    public void generateAutoPromos() {
        try {
            List<RepasDetaille> allDishes = repasService.findAll();
            if (allDishes.isEmpty())
                return;

            // Trier par nombre de choix (ascendant)
            List<RepasDetaille> leastSold = allDishes.stream()
                    .sorted(Comparator.comparingInt(RepasDetaille::getChoixCount))
                    .limit(TOP_N_LEAST_SOLD)
                    .collect(Collectors.toList());

            autoPromoDishes.clear();

            for (RepasDetaille dish : leastSold) {
                String code = "AUTO-" + dish.getNom()
                        .toUpperCase()
                        .replaceAll("[^A-Z0-9]", "")
                        .substring(0, Math.min(dish.getNom().replaceAll("[^A-Za-z0-9]", "").length(), 8));

                try {
                    PromoCode existing = promoCodeService.getByCode(code);
                    if (existing != null) {
                        autoPromoDishes.put(dish.getId(), existing);
                    } else {
                        PromoCode promo = new PromoCode();
                        promo.setCode(code);
                        promo.setDiscountPercentage(DEFAULT_DISCOUNT);
                        promo.setExpirationDate(LocalDate.now().plusDays(30));
                        promo.setActive(true);
                        promo.setUsageLimit(50);
                        promo.setCurrentUsage(0);
                        promoCodeService.create(promo);

                        PromoCode created = promoCodeService.getByCode(code);
                        if (created != null) {
                            autoPromoDishes.put(dish.getId(), created);
                        }
                    }
                    LOGGER.info("Auto-promo " + code + " (-" + DEFAULT_DISCOUNT + "%) pour: " + dish.getNom());
                } catch (Exception e) {
                    LOGGER.warning("Impossible de créer auto-promo pour " + dish.getNom() + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Erreur génération auto-promos: " + e.getMessage());
        }
    }

    /**
     * Vérifie si un plat a une promo automatique active.
     */
    public boolean hasAutoPromo(Long dishId) {
        return autoPromoDishes.containsKey(dishId);
    }

    /**
     * Retourne le pourcentage de réduction pour un plat, ou 0 si aucun.
     */
    public int getDiscountPercent(Long dishId) {
        PromoCode promo = autoPromoDishes.get(dishId);
        return promo != null ? promo.getDiscountPercentage() : 0;
    }

    /**
     * Retourne le code promo pour un plat, ou null si aucun.
     */
    public String getPromoCode(Long dishId) {
        PromoCode promo = autoPromoDishes.get(dishId);
        return promo != null ? promo.getCode() : null;
    }

    /**
     * Retourne la map complète des promos auto actives.
     */
    public Map<Long, PromoCode> getAutoPromoDishes() {
        return autoPromoDishes;
    }
}
