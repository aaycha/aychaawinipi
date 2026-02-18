package com.gestion.interfaces;

import com.gestion.entities.Repas;
import java.util.List;
import java.util.Optional;

/**
 * Interface de service pour la gestion des repas (plats)
 */
public interface RepasService {

    Repas create(Repas repas);

    Repas update(Repas repas);

    boolean delete(Long id);

    Optional<Repas> findById(Long id);

    List<Repas> findAll();

    List<Repas> findDisponibles();

    List<Repas> findByRestaurantId(Long restaurantId);

    List<Repas> findDisponiblesByRestaurantId(Long restaurantId);

    List<Repas> findByMenuId(Long menuId);

    List<Repas> findByCategorie(Repas.Categorie categorie);

    List<Repas> findByTypePlat(Repas.TypePlat typePlat);

    List<Repas> searchByNom(String nom);

    boolean existsById(Long id);

    long count();

    long countByRestaurantId(Long restaurantId);
}
