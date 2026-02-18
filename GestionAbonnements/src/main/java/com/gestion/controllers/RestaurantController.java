package com.gestion.controllers;

import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import com.gestion.interfaces.RestaurantService;
import com.gestion.services.RestaurantServiceImpl;

import java.util.List;
import java.util.Optional;

/**
 * Contrôleur Restaurant - Gère les opérations CRUD pour les restaurants
 */
public class RestaurantController {

    private final RestaurantService service = new RestaurantServiceImpl();

    // ================= CREATE =================

    public Restaurant createRestaurant(String nom, String adresse, String telephone, 
                                       String email, String description, String imageUrl) {
        Restaurant restaurant = new Restaurant();
        restaurant.setNom(nom);
        restaurant.setAdresse(adresse);
        restaurant.setTelephone(telephone);
        restaurant.setEmail(email);
        restaurant.setDescription(description);
        restaurant.setImageUrl(imageUrl);
        restaurant.setActif(true);

        ValidationResult validation = restaurant.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getAllErrorsAsString());
        }

        return service.create(restaurant);
    }

    public Restaurant createRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Le restaurant ne peut pas être null");
        }
        ValidationResult validation = restaurant.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getAllErrorsAsString());
        }
        return service.create(restaurant);
    }

    // ================= READ =================

    public List<Restaurant> getAllRestaurants() {
        return service.findAll();
    }

    public List<Restaurant> getActiveRestaurants() {
        return service.findActifs();
    }

    public Optional<Restaurant> getRestaurantById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return service.findById(id);
    }

    public List<Restaurant> searchRestaurants(String nom) {
        return service.searchByNom(nom);
    }

    // ================= UPDATE =================

    public Restaurant updateRestaurant(Restaurant restaurant) {
        if (restaurant == null || restaurant.getId() == null) {
            throw new IllegalArgumentException("Le restaurant et son ID sont obligatoires pour la mise à jour");
        }

        ValidationResult validation = restaurant.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getAllErrorsAsString());
        }

        return service.update(restaurant);
    }

    public Restaurant updateRestaurant(Long id, String nom, String adresse, String telephone,
                                       String email, String description, String imageUrl, Boolean actif) {
        Optional<Restaurant> existing = service.findById(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Restaurant non trouvé avec l'ID: " + id);
        }

        Restaurant restaurant = existing.get();
        restaurant.setNom(nom);
        restaurant.setAdresse(adresse);
        restaurant.setTelephone(telephone);
        restaurant.setEmail(email);
        restaurant.setDescription(description);
        restaurant.setImageUrl(imageUrl);
        if (actif != null) {
            restaurant.setActif(actif);
        }

        return service.update(restaurant);
    }

    // ================= DELETE =================

    public boolean deleteRestaurant(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du restaurant est obligatoire");
        }
        return service.delete(id);
    }

    // ================= VALIDATION =================

    public ValidationResult validateRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            ValidationResult result = new ValidationResult();
            result.addGlobalError("Le restaurant ne peut pas être null");
            return result;
        }
        return restaurant.validate();
    }

    public boolean isValid(Restaurant restaurant) {
        return !validateRestaurant(restaurant).hasErrors();
    }

    // ================= UTILITIES =================

    public boolean restaurantExists(Long id) {
        return service.existsById(id);
    }

    public long countRestaurants() {
        return service.count();
    }

    public RestaurantService getService() {
        return service;
    }
}
