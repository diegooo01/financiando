package com.ando.financiando.repository;

import com.ando.financiando.model.PendingBudgetPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingBudgetPlanRepository extends JpaRepository<PendingBudgetPlan, Long> {

    Optional<PendingBudgetPlan> findByUserPhone(String userPhone);

    void deleteByUserPhone(String userPhone);
}