package com.ando.financiando.dto;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        String emoji,
        List<String> keywords
) {
}