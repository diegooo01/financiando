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
}