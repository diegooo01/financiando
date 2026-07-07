package com.ando.financiando.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceResponse(
        LocalDate start,
        LocalDate end,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal net
) {
}