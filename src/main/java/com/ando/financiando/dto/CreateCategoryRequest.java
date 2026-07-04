package com.ando.financiando.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre es demasiado largo")
        String name,

        @Size(max = 8, message = "Emoji inválido")
        String emoji
) {
}