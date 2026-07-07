package com.ando.financiando.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pending_suggestions")
public class PendingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userPhone;

    @Column(nullable = false)
    private String suggestedCategoryName;

    @Column(length = 8)
    private String suggestedCategoryEmoji;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pendingAmount;

    @Column(length = 255)
    private String pendingDescription;

    @Column(nullable = false)
    private Instant createdAt;

    protected PendingSuggestion() {
    }

    public PendingSuggestion(String userPhone, String suggestedCategoryName,
                             String suggestedCategoryEmoji, BigDecimal pendingAmount,
                             String pendingDescription) {
        this.userPhone = userPhone;
        this.suggestedCategoryName = suggestedCategoryName;
        this.suggestedCategoryEmoji = suggestedCategoryEmoji;
        this.pendingAmount = pendingAmount;
        this.pendingDescription = pendingDescription;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public String getSuggestedCategoryName() {
        return suggestedCategoryName;
    }

    public String getSuggestedCategoryEmoji() {
        return suggestedCategoryEmoji;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public String getPendingDescription() {
        return pendingDescription;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}