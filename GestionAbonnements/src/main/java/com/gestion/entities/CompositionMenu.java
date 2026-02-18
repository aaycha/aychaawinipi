package com.gestion.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité pour composition de menu (lien entre menu et repas)
 */
public class CompositionMenu {
    
    private Long id;
    private Long menuId;
    private Long repasId;
    private Integer ordre; // Ordre d'affichage dans le menu
    private String typeRepas; // PETIT_DEJEUNER, DEJEUNER, DINER
    private LocalDate date;
    private Long participantId;
    private Long evenementId;
    
    // Métadonnées
    private boolean actif = true;
    private String notes;
    
    // Relations (chargées depuis DB)
    private Restauration menu;
    private RepasDetaille repas;
    
    public CompositionMenu() {}
    
    public CompositionMenu(Long menuId, Long repasId, Integer ordre) {
        this.menuId = menuId;
        this.repasId = repasId;
        this.ordre = ordre;
    }
    
    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }
    
    public Long getRepasId() { return repasId; }
    public void setRepasId(Long repasId) { this.repasId = repasId; }
    
    public Integer getOrdre() { return ordre; }
    public void setOrdre(Integer ordre) { this.ordre = ordre; }
    
    public String getTypeRepas() { return typeRepas; }
    public void setTypeRepas(String typeRepas) { this.typeRepas = typeRepas; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }
    
    public Long getEvenementId() { return evenementId; }
    public void setEvenementId(Long evenementId) { this.evenementId = evenementId; }
    
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Restauration getMenu() { return menu; }
    public void setMenu(Restauration menu) { this.menu = menu; }
    
    public RepasDetaille getRepas() { return repas; }
    public void setRepas(RepasDetaille repas) { this.repas = repas; }
}
