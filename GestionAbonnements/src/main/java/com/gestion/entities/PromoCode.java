package com.gestion.entities;

import java.time.LocalDate;

/**
 * Entity representing a Promotional Code.
 */
public class PromoCode {
    private Long id;
    private String code;
    private int discountPercentage;
    private LocalDate expirationDate;
    private boolean active;
    private int usageLimit;
    private int currentUsage;

    public PromoCode() {
    }

    public PromoCode(String code, int discountPercentage, LocalDate expirationDate, int usageLimit) {
        this.code = code;
        this.discountPercentage = discountPercentage;
        this.expirationDate = expirationDate;
        this.usageLimit = usageLimit;
        this.active = true;
        this.currentUsage = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(int discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getCurrentUsage() {
        return currentUsage;
    }

    public void setCurrentUsage(int currentUsage) {
        this.currentUsage = currentUsage;
    }

    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    public boolean canBeUsed() {
        return active && !isExpired() && (usageLimit <= 0 || currentUsage < usageLimit);
    }
}
