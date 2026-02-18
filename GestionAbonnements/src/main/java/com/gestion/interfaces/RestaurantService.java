package com.gestion.interfaces;

import com.gestion.entities.Restaurant;
import java.util.List;
import java.util.Optional;

/**
 * Interface de service pour la gestion des restaurants
 */
public interface RestaurantService {

    Restaurant create(Restaurant restaurant);

    Restaurant update(Restaurant restaurant);

    boolean delete(Long id);

    Optional<Restaurant> findById(Long id);

    List<Restaurant> findAll();

    List<Restaurant> findActifs();

    List<Restaurant> searchByNom(String nom);

    boolean existsById(Long id);

    long count();
}
