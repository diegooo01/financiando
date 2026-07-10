package com.ando.financiando.dto;

import java.math.BigDecimal;

public record CategorySpendingResponse(
        String categoryName,
        String emoji,
        BigDecimal total
) {
}