package com.gestion.interfaces;

import com.gestion.entities.Ingredient;
import java.util.List;
import java.util.Optional;

public interface IngredientService {
    Ingredient create(Ingredient ingredient);

    Optional<Ingredient> findById(Long id);

    List<Ingredient> findAll();

    List<Ingredient> findByCategorie(Ingredient.Categorie categorie);

    Ingredient update(Ingredient ingredient);

    boolean delete(Long id);
}
