package com.gestion.entities;

import java.sql.Timestamp;

public class EquipementVue {
    private Long id;
    private Long equipementId;
    private String userId;
    private Timestamp lastViewed;

    public EquipementVue() {
    }

    public EquipementVue(Long equipementId, String userId) {
        this.equipementId = equipementId;
        this.userId = userId;
    }

    public EquipementVue(Long id, Long equipementId, String userId, Timestamp lastViewed) {
        this.id = id;
        this.equipementId = equipementId;
        this.userId = userId;
        this.lastViewed = lastViewed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEquipementId() {
        return equipementId;
    }

    public void setEquipementId(Long equipementId) {
        this.equipementId = equipementId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getLastViewed() {
        return lastViewed;
    }

    public void setLastViewed(Timestamp lastViewed) {
        this.lastViewed = lastViewed;
    }
}
