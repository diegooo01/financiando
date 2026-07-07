package com.ando.financiando.dto;

import com.ando.financiando.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCategoryRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre es demasiado largo")
        String name,

        @Size(max = 8, message = "Emoji inválido")
        String emoji,

        TransactionType type,

        List<String> keywords
) {
}