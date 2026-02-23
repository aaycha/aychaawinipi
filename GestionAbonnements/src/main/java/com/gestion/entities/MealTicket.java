package com.gestion.entities;

import java.time.LocalDateTime;

/**
 * Entity representing a Meal Ticket with QR Code.
 */
public class MealTicket {
    private Long id;
    private Long participationId;
    private Long userId;
    private String qrCode;
    private LocalDateTime timeSlot; // Date and time for the meal
    private boolean used;
    private LocalDateTime usedAt;

    public MealTicket() {
    }

    public MealTicket(Long participationId, Long userId, String qrCode, LocalDateTime timeSlot) {
        this.participationId = participationId;
        this.userId = userId;
        this.qrCode = qrCode;
        this.timeSlot = timeSlot;
        this.used = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParticipationId() {
        return participationId;
    }

    public void setParticipationId(Long participationId) {
        this.participationId = participationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public LocalDateTime getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(LocalDateTime timeSlot) {
        this.timeSlot = timeSlot;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}
