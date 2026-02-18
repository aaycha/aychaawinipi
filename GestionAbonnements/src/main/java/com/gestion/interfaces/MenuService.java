package com.gestion.interfaces;

import com.gestion.entities.Menu;
import java.util.List;
import java.util.Optional;

/**
 * Interface de service pour la gestion des menus
 */
public interface MenuService {

    Menu create(Menu menu);

    Menu update(Menu menu);

    boolean delete(Long id);

    Optional<Menu> findById(Long id);

    List<Menu> findAll();

    List<Menu> findActifs();

    List<Menu> findByRestaurantId(Long restaurantId);

    List<Menu> findActifsByRestaurantId(Long restaurantId);

    List<Menu> searchByNom(String nom);

    boolean existsById(Long id);

    long count();

    long countByRestaurantId(Long restaurantId);
}
