package com.gestion.interfaces;

import com.gestion.entities.CompositionMenu;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service pour gestion des compositions de menus
 */
public interface CompositionMenuService {
    
    CompositionMenu create(CompositionMenu composition);
    Optional<CompositionMenu> findById(Long id);
    List<CompositionMenu> findAll();
    CompositionMenu update(CompositionMenu composition);
    boolean delete(Long id);
    
    // Recherches
    List<CompositionMenu> findByMenuId(Long menuId);
    List<CompositionMenu> findByRepasId(Long repasId);
    List<CompositionMenu> findByParticipantId(Long participantId);
    List<CompositionMenu> findByEvenementId(Long evenementId);
    List<CompositionMenu> findByDate(LocalDate date);
    
    // Opérations sur compositions
    boolean deleteByMenuId(Long menuId);
    boolean deleteByRepasId(Long repasId);
    List<CompositionMenu> reorderCompositions(Long menuId, List<Long> repasIds);
    
    // Génération automatique
    List<CompositionMenu> generateMenuFromRestrictions(Long participantId, Long evenementId, LocalDate date);
}
