package com.ando.financiando.repository;

import com.ando.financiando.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("SELECT b FROM Budget b JOIN FETCH b.category WHERE b.yearMonth = :yearMonth")
    List<Budget> findByYearMonthWithCategory(@Param("yearMonth") String yearMonth);

    Optional<Budget> findByCategoryIdAndYearMonth(Long categoryId, String yearMonth);
}