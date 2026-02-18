package com.gestion.controllers;

import com.gestion.entities.Menu;
import com.gestion.entities.Repas;
import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import com.gestion.interfaces.MenuService;
import com.gestion.interfaces.RepasService;
import com.gestion.interfaces.RestaurantService;
import com.gestion.services.MenuServiceImpl;
import com.gestion.services.RepasServiceImpl;
import com.gestion.services.RestaurantServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur Repas - Gère les opérations CRUD pour les repas (plats)
 */
public class RepasController {

    private final RepasService repasService = new RepasServiceImpl();
    private final RestaurantService restaurantService = new RestaurantServiceImpl();
    private final MenuService menuService = new MenuServiceImpl();

    // ================= CREATE =================

    public Repas createRepas(Long restaurantId, Long menuId, String nom, String description,
                             BigDecimal prix, Repas.Categorie categorie, Repas.TypePlat typePlat,
                             Integer tempsPreparation, String imageUrl) {
        Repas repas = new Repas();
        repas.setRestaurantId(restaurantId);
        repas.setMenuId(menuId);
        repas.setNom(nom);
        repas.setDescription(description);
        repas.setPrix(prix);
        repas.setCategorie(categorie);
        repas.setTypePlat(typePlat);
        repas.setTempsPreparation(tempsPreparation);
        repas.setImageUrl(imageUrl);
        repas.setDisponible(true);

        enrichRepasWithNames(repas);

        ValidationResult validation = repas.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getAllErrorsAsString());
        }

        return repasService.create(repas);
    }

    public Repas createRepas(Repas repas) {
        if (repas == null) {
            throw new IllegalArgumentException("Le repas ne peut pas être null");
        }

        enrichRepasWithNames(repas);

        ValidationResult validation = repas.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getAllErrorsAsString());
        }

        return repasService.create(repas);
    }

    // ================= READ =================

    public List<Repas> getAllRepas() {
        List<Repas> repas = repasService.findAll();
        repas.forEach(this::enrichRepasWithNames);
        return repas;
    }

    public List<Repas> getDisponibleRepas() {
        List<Repas> repas = repasService.findDisponibles();
        repas.forEach(this::enrichRepasWithNames);
        return repas;
    }

    public List<Repas> getRepasByRestaurant(Long restaurantId) {
        List<Repas> repas = repasService.findByRestaurantId(restaurantId);
        repas.forEach(this::enrichRepasWithNames);
        return repas;
    }

    public List<Repas> getDisponibleRepasByRestaurant(Long restaurantId) {
        List<Repas> repas = repasService.findDisponiblesByRestaurantId(restaurantId);
        repas.forEach(this::enrichRepasWithNames);
        return repas;
    }

    public List<Repas> getRepasByMenu(Long menuId) {
        List<Repas> repas = repasService.findByMenuId(menuId);
        repas.forEach(this::enrichRepasWithNames);
        return repas;
    }

    public List<Repas> getRepasByCategorie(Repas.Categorie categorie) {
        List<Repas> repas = repasService.findByCategorie(categorie);
        repas.forEach(this::enrichRepasWithNames);
        return repas;
    }

    public List<Repas> getRepasByTypePlat(Repas.TypePlat typePlat) {
        List<Repas> repas = repasService.findByTypePlat(typePlat);
        repas.forEach(this::enrichRepasWithNames);
        return repas;
    }

    public Optional<Repas> getRepasById(Long id) {
        Optional<Repas> repas = repasService.findById(id);
        repas.ifPresent(this::enrichRepasWithNames);
        return repas;
    }

    public List<Repas> searchRepas(String nom) {
        List<Repas> repas = repasService.searchByNom(nom);
        repas.forEach(this::enrichRepasWithNames);
        return repas;
    }

    // ================= UPDATE =================

    public Repas updateRepas(Repas repas) {
        if (repas == null || repas.getId() == null) {
            throw new IllegalArgumentException("Le repas et son ID sont obligatoires pour la mise à jour");
        }

        ValidationResult validation = repas.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getAllErrorsAsString());
        }

        return repasService.update(repas);
    }

    public Repas updateRepas(Long id, Long restaurantId, Long menuId, String nom, String description,
                             BigDecimal prix, Repas.Categorie categorie, Repas.TypePlat typePlat,
                             Integer tempsPreparation, String imageUrl, Boolean disponible) {
        Optional<Repas> existing = repasService.findById(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Repas non trouvé avec l'ID: " + id);
        }

        Repas repas = existing.get();
        repas.setRestaurantId(restaurantId);
        repas.setMenuId(menuId);
        repas.setNom(nom);
        repas.setDescription(description);
        repas.setPrix(prix);
        repas.setCategorie(categorie);
        repas.setTypePlat(typePlat);
        repas.setTempsPreparation(tempsPreparation);
        repas.setImageUrl(imageUrl);
        if (disponible != null) {
            repas.setDisponible(disponible);
        }

        enrichRepasWithNames(repas);

        return repasService.update(repas);
    }

    // ================= DELETE =================

    public boolean deleteRepas(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du repas est obligatoire");
        }
        return repasService.delete(id);
    }

    // ================= VALIDATION =================

    public ValidationResult validateRepas(Repas repas) {
        if (repas == null) {
            ValidationResult result = new ValidationResult();
            result.addGlobalError("Le repas ne peut pas être null");
            return result;
        }
        return repas.validate();
    }

    public boolean isValid(Repas repas) {
        return !validateRepas(repas).hasErrors();
    }

    // ================= UTILITIES =================

    private void enrichRepasWithNames(Repas repas) {
        if (repas.getRestaurantId() != null && repas.getRestaurantNom() == null) {
            restaurantService.findById(repas.getRestaurantId())
                .ifPresent(r -> repas.setRestaurantNom(r.getNom()));
        }
        if (repas.getMenuId() != null && repas.getMenuNom() == null) {
            menuService.findById(repas.getMenuId())
                .ifPresent(m -> repas.setMenuNom(m.getNom()));
        }
    }

    public List<Restaurant> getAvailableRestaurants() {
        return restaurantService.findActifs();
    }

    public List<Menu> getAvailableMenus() {
        return menuService.findActifs();
    }

    public List<Menu> getMenusByRestaurant(Long restaurantId) {
        return menuService.findActifsByRestaurantId(restaurantId);
    }

    public boolean repasExists(Long id) {
        return repasService.existsById(id);
    }

    public long countRepas() {
        return repasService.count();
    }

    public long countRepasByRestaurant(Long restaurantId) {
        return repasService.countByRestaurantId(restaurantId);
    }

    public RepasService getRepasService() {
        return repasService;
    }

    public RestaurantService getRestaurantService() {
        return restaurantService;
    }

    public MenuService getMenuService() {
        return menuService;
    }
}
