package com.ando.financiando.service;

import com.ando.financiando.model.Category;

import java.math.BigDecimal;

public record ParsedExpense(
        BigDecimal amount,
        Category category,
        String description,
        boolean successful,
        boolean hasSuggestion,
        String suggestedCategoryName,
        String suggestedCategoryEmoji
) {

    public static ParsedExpense success(BigDecimal amount, Category category, String description) {
        return new ParsedExpense(amount, category, description, true, false, null, null);
    }

    public static ParsedExpense suggestion(BigDecimal amount, String description,
                                           String suggestedName, String suggestedEmoji) {
        return new ParsedExpense(amount, null, description, false, true, suggestedName, suggestedEmoji);
    }

    public static ParsedExpense failure() {
        return new ParsedExpense(null, null, null, false, false, null, null);
    }
}