package com.gestion.interfaces;

import com.gestion.entities.RepasDetaille;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service pour gestion des repas détaillés
 */
public interface RepasDetailleService {
    
    RepasDetaille create(RepasDetaille repas);
    Optional<RepasDetaille> findById(Long id);
    List<RepasDetaille> findAll();
    RepasDetaille update(RepasDetaille repas);
    boolean delete(Long id);
    
    // Recherches
    List<RepasDetaille> findByParticipantId(Long participantId);
    List<RepasDetaille> findByEvenementId(Long evenementId);
    List<RepasDetaille> findByDate(LocalDate date);
    List<RepasDetaille> findByTypeRepas(String typeRepas);
    
    // Filtres avancés
    List<RepasDetaille> findByPrixRange(BigDecimal min, BigDecimal max);
    List<RepasDetaille> findByCaloriesRange(Integer min, Integer max);
    List<RepasDetaille> findByRestrictions(boolean vegetarien, boolean vegan, boolean sansGluten, boolean halal);
    List<RepasDetaille> findByAllergene(String allergene);
    List<RepasDetaille> searchByName(String nom);
    
    // Analytics
    BigDecimal getTotalPrixByParticipant(Long participantId);
    Integer getTotalCaloriesByParticipant(Long participantId);
    BigDecimal getMoyennePrix();
    Integer getMoyenneCalories();
}
