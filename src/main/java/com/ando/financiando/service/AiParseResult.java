package com.ando.financiando.service;

import java.math.BigDecimal;

public record AiParseResult(
        BigDecimal amount,
        String description,
        String existingCategoryName,
        String suggestedNewCategoryName,
        String suggestedNewCategoryEmoji,
        boolean successful
) {

    public boolean suggestsNewCategory() {
        return suggestedNewCategoryName != null && !suggestedNewCategoryName.isBlank();
    }

    public static AiParseResult failure() {
        return new AiParseResult(null, null, null, null, null, false);
    }
}