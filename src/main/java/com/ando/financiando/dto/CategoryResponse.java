package com.ando.financiando.dto;

import com.ando.financiando.model.TransactionType;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        String emoji,
        TransactionType type,
        List<String> keywords
) {
}