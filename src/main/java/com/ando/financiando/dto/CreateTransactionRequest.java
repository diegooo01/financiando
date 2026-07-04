package com.ando.financiando.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(

        @NotNull(message = "El monto es obligatorio")
        @PositiveOrZero(message = "El monto no puede ser negativo")
        BigDecimal amount,

        @Size(max = 255, message = "La descripción es demasiado larga")
        String description,

        @NotNull(message = "La categoría es obligatoria")
        Long categoryId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate occurredAt
) {
}