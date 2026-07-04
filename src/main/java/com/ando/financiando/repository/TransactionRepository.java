package com.ando.financiando.repository;

import com.ando.financiando.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByOccurredAtBetween(LocalDate start, LocalDate end);

    List<Transaction> findByCategoryId(Long categoryId);
}