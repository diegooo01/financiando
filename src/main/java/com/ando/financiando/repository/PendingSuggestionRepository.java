package com.ando.financiando.repository;

import com.ando.financiando.model.PendingSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingSuggestionRepository extends JpaRepository<PendingSuggestion, Long> {

    Optional<PendingSuggestion> findByUserPhone(String userPhone);

    void deleteByUserPhone(String userPhone);
}