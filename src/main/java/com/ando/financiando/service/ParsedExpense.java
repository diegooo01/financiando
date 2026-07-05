package com.ando.financiando.service;

import com.ando.financiando.model.Category;

import java.math.BigDecimal;

public record ParsedExpense(
        BigDecimal amount,
        Category category,
        String description,
        boolean successful
) {

    public static ParsedExpense success(BigDecimal amount, Category category, String description) {
        return new ParsedExpense(amount, category, description, true);
    }

    public static ParsedExpense failure() {
        return new ParsedExpense(null, null, null, false);
    }
}