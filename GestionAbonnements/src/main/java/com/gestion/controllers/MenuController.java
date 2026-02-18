package com.gestion.controllers;

import com.gestion.entities.Menu;
import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import com.gestion.interfaces.MenuService;
import com.gestion.interfaces.RestaurantService;
import com.gestion.services.MenuServiceImpl;
import com.gestion.services.RestaurantServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur Menu - Gère les opérations CRUD pour les menus
 */
public class MenuController {

    private final MenuService menuService = new MenuServiceImpl();
    private final RestaurantService restaurantService = new RestaurantServiceImpl();

    // ================= CREATE =================

    public Menu createMenu(Long restaurantId, String nom, String description, 
                          BigDecimal prix, LocalDate dateDebut, LocalDate dateFin) {
        Menu menu = new Menu();
        menu.setRestaurantId(restaurantId);
        menu.setNom(nom);
        menu.setDescription(description);
        menu.setPrix(prix);
        menu.setDateDebut(dateDebut);
        menu.setDateFin(dateFin);
        menu.setActif(true);

        // Enrichir avec le nom du restaurant si disponible
        if (restaurantId != null) {
            restaurantService.findById(restaurantId).ifPresent(r -> 
                menu.setRestaurantNom(r.getNom())
            );
        }

        ValidationResult validation = menu.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getAllErrorsAsString());
        }

        return menuService.create(menu);
    }

    public Menu createMenu(Menu menu) {
        if (menu == null) {
            throw new IllegalArgumentException("Le menu ne peut pas être null");
        }

        // Enrichir avec le nom du restaurant si disponible
        if (menu.getRestaurantId() != null && menu.getRestaurantNom() == null) {
            restaurantService.findById(menu.getRestaurantId()).ifPresent(r -> 
                menu.setRestaurantNom(r.getNom())
            );
        }

        ValidationResult validation = menu.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getAllErrorsAsString());
        }

        return menuService.create(menu);
    }

    // ================= READ =================

    public List<Menu> getAllMenus() {
        // Enrichir avec les noms de restaurants
        List<Menu> menus = menuService.findAll();
        menus.forEach(this::enrichMenuWithRestaurantName);
        return menus;
    }

    public List<Menu> getActiveMenus() {
        List<Menu> menus = menuService.findActifs();
        menus.forEach(this::enrichMenuWithRestaurantName);
        return menus;
    }

    public List<Menu> getMenusByRestaurant(Long restaurantId) {
        List<Menu> menus = menuService.findByRestaurantId(restaurantId);
        menus.forEach(this::enrichMenuWithRestaurantName);
        return menus;
    }

    public List<Menu> getActiveMenusByRestaurant(Long restaurantId) {
        List<Menu> menus = menuService.findActifsByRestaurantId(restaurantId);
        menus.forEach(this::enrichMenuWithRestaurantName);
        return menus;
    }

    public Optional<Menu> getMenuById(Long id) {
        Optional<Menu> menu = menuService.findById(id);
        menu.ifPresent(this::enrichMenuWithRestaurantName);
        return menu;
    }

    public List<Menu> searchMenus(String nom) {
        List<Menu> menus = menuService.searchByNom(nom);
        menus.forEach(this::enrichMenuWithRestaurantName);
        return menus;
    }

    // ================= UPDATE =================

    public Menu updateMenu(Menu menu) {
        if (menu == null || menu.getId() == null) {
            throw new IllegalArgumentException("Le menu et son ID sont obligatoires pour la mise à jour");
        }

        ValidationResult validation = menu.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getAllErrorsAsString());
        }

        return menuService.update(menu);
    }

    public Menu updateMenu(Long id, Long restaurantId, String nom, String description,
                          BigDecimal prix, LocalDate dateDebut, LocalDate dateFin, Boolean actif) {
        Optional<Menu> existing = menuService.findById(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Menu non trouvé avec l'ID: " + id);
        }

        Menu menu = existing.get();
        menu.setRestaurantId(restaurantId);
        menu.setNom(nom);
        menu.setDescription(description);
        menu.setPrix(prix);
        menu.setDateDebut(dateDebut);
        menu.setDateFin(dateFin);
        if (actif != null) {
            menu.setActif(actif);
        }

        // Enrichir avec le nom du restaurant
        if (restaurantId != null) {
            restaurantService.findById(restaurantId).ifPresent(r -> 
                menu.setRestaurantNom(r.getNom())
            );
        }

        return menuService.update(menu);
    }

    // ================= DELETE =================

    public boolean deleteMenu(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du menu est obligatoire");
        }
        return menuService.delete(id);
    }

    // ================= VALIDATION =================

    public ValidationResult validateMenu(Menu menu) {
        if (menu == null) {
            ValidationResult result = new ValidationResult();
            result.addGlobalError("Le menu ne peut pas être null");
            return result;
        }
        return menu.validate();
    }

    public boolean isValid(Menu menu) {
        return !validateMenu(menu).hasErrors();
    }

    // ================= UTILITIES =================

    private void enrichMenuWithRestaurantName(Menu menu) {
        if (menu.getRestaurantId() != null && menu.getRestaurantNom() == null) {
            restaurantService.findById(menu.getRestaurantId()).ifPresent(r -> 
                menu.setRestaurantNom(r.getNom())
            );
        }
    }

    public List<Restaurant> getAvailableRestaurants() {
        return restaurantService.findActifs();
    }

    public boolean menuExists(Long id) {
        return menuService.existsById(id);
    }

    public long countMenus() {
        return menuService.count();
    }

    public long countMenusByRestaurant(Long restaurantId) {
        return menuService.countByRestaurantId(restaurantId);
    }

    public MenuService getMenuService() {
        return menuService;
    }

    public RestaurantService getRestaurantService() {
        return restaurantService;
    }
}
