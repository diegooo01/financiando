package com.ando.financiando.dto;

import com.ando.financiando.model.TransactionSource;
import com.ando.financiando.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        String description,
        Long categoryId,
        String categoryName,
        LocalDate occurredAt,
        TransactionSource source,
        TransactionType type,
        Instant createdAt
) {
}