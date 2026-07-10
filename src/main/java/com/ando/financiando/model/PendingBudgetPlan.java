package com.ando.financiando.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "pending_budget_plans")
public class PendingBudgetPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userPhone;

    @Column(nullable = false, columnDefinition = "text")
    private String planJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected PendingBudgetPlan() {
    }

    public PendingBudgetPlan(String userPhone, String planJson) {
        this.userPhone = userPhone;
        this.planJson = planJson;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public String getPlanJson() {
        return planJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}