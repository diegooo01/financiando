package com.ando.financiando.dto;

import com.ando.financiando.model.TransactionSource;

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
        Instant createdAt
) {
}