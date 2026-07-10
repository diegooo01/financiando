package com.ando.financiando.dto;

import java.math.BigDecimal;

public record BudgetStatusResponse(
        String categoryName,
        String emoji,
        BigDecimal limit,
        BigDecimal spent,
        BigDecimal remaining,
        int percentUsed
) {
}