package com.ando.financiando.service;

import com.ando.financiando.model.Category;
import com.ando.financiando.repository.CategoryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExpenseParser {

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("\\d+(?:[.,]\\d{1,2})?");

    private static final String DEFAULT_CATEGORY_NAME = "Otros";

    private final CategoryRepository categoryRepository;
    private final AiExpenseParser aiExpenseParser;

    public ExpenseParser(CategoryRepository categoryRepository,
                         AiExpenseParser aiExpenseParser) {
        this.categoryRepository = categoryRepository;
        this.aiExpenseParser = aiExpenseParser;
    }

    public ParsedExpense parse(String message) {
        if (message == null || message.isBlank()) {
            return ParsedExpense.failure();
        }

        String normalized = message.trim().toLowerCase();

        BigDecimal amount = extractAmount(normalized);
        if (amount == null) {
            return ParsedExpense.failure();
        }

        List<Category> categories = categoryRepository.findAll();
        Category matched = matchCategoryByKeyword(normalized, categories);

        if (matched != null) {
            return ParsedExpense.success(amount, matched, message.trim());
        }

        AiParseResult ai = aiExpenseParser.parse(message);

        if (ai.successful()) {
            if (ai.suggestsNewCategory()) {
                return ParsedExpense.suggestion(
                        ai.amount(),
                        ai.description(),
                        ai.suggestedNewCategoryName(),
                        ai.suggestedNewCategoryEmoji());
            }

            Category aiCategory = findByName(ai.existingCategoryName(), categories);
            if (aiCategory != null) {
                return ParsedExpense.success(ai.amount(), aiCategory, ai.description());
            }
        }

        Category fallback = findDefaultCategory(categories);
        return ParsedExpense.success(amount, fallback, message.trim());
    }

    private BigDecimal extractAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            String number = matcher.group().replace(",", ".");
            return new BigDecimal(number);
        }
        return null;
    }

    private Category matchCategoryByKeyword(String text, List<Category> categories) {
        for (Category category : categories) {
            for (String keyword : category.getKeywords()) {
                if (text.contains(keyword.toLowerCase())) {
                    return category;
                }
            }
        }
        return null;
    }

    private Category findByName(String name, List<Category> categories) {
        if (name == null) {
            return null;
        }
        return categories.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private Category findDefaultCategory(List<Category> categories) {
        return categories.stream()
                .filter(c -> c.getName().equals(DEFAULT_CATEGORY_NAME))
                .findFirst()
                .orElse(categories.isEmpty() ? null : categories.get(0));
    }
}