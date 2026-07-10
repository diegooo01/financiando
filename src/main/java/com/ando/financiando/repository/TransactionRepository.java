package com.ando.financiando.repository;

import com.ando.financiando.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t JOIN FETCH t.category")
    List<Transaction> findAllWithCategory();

    @Query("SELECT t FROM Transaction t JOIN FETCH t.category WHERE t.id = :id")
    Optional<Transaction> findByIdWithCategory(@Param("id") Long id);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.category WHERE t.occurredAt BETWEEN :start AND :end")
    List<Transaction> findByOccurredAtBetween(LocalDate start, LocalDate end);

    List<Transaction> findByCategoryId(Long categoryId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.type = :type AND t.occurredAt BETWEEN :start AND :end
            """)
    java.math.BigDecimal sumByTypeBetween(
            @org.springframework.data.repository.query.Param("type") com.ando.financiando.model.TransactionType type,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.category.id = :categoryId
              AND t.type = com.ando.financiando.model.TransactionType.EXPENSE
              AND t.occurredAt BETWEEN :start AND :end
            """)
    java.math.BigDecimal sumExpensesByCategoryBetween(
            @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

    @Query("""
            SELECT c.name AS categoryName, c.emoji AS emoji, SUM(t.amount) AS total
            FROM Transaction t
            JOIN t.category c
            WHERE t.type = com.ando.financiando.model.TransactionType.EXPENSE
              AND t.occurredAt BETWEEN :start AND :end
            GROUP BY c.name, c.emoji
            ORDER BY SUM(t.amount) DESC
            """)
    List<com.ando.financiando.repository.projection.CategoryTotal> sumByCategoryBetween(
            @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);
}