package com.ando.financiando.service;

import com.ando.financiando.model.Category;
import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.CategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AiBudgetParser {

    private static final Logger log = LoggerFactory.getLogger(AiBudgetParser.class);

    private final AiClient aiClient;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public AiBudgetParser(AiClient aiClient,
                          CategoryRepository categoryRepository,
                          ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<BudgetIntent> parse(String message) {
        List<Category> expenseCategories = categoryRepository.findAll().stream()
                .filter(c -> c.getType() == TransactionType.EXPENSE)
                .toList();

        String categoryNames = expenseCategories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        String systemPrompt = """
                El usuario quiere definir un presupuesto mensual para una categoría de gasto.
                Extrae la categoría y el monto del mensaje, sin importar el orden en que aparezcan.
                Devuelve SOLO un JSON válido, sin texto adicional:
                {"category": "string", "amount": number}

                Reglas:
                - "category" DEBE ser exactamente una de estas: %s
                - Si el usuario menciona una categoría parecida, elige la más cercana de la lista.
                - "amount" es el monto numérico (sin símbolo de moneda).
                - Si no puedes identificar categoría o monto, devuelve {"category": null, "amount": 0}.
                """.formatted(categoryNames);

        try {
            String raw = aiClient.chat(systemPrompt, message);
            return parseResponse(raw, expenseCategories);
        } catch (Exception e) {
            log.warn("Falló el parseo de presupuesto con IA: '{}'. Causa: {}", message, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BudgetIntent> parseResponse(String raw, List<Category> categories) throws Exception {
        String cleaned = raw
                .replaceAll("(?s)```json", "")
                .replaceAll("(?s)```", "")
                .trim();

        JsonNode json = objectMapper.readTree(cleaned);

        JsonNode categoryNode = json.get("category");
        JsonNode amountNode = json.get("amount");

        if (categoryNode == null || categoryNode.isNull() || amountNode == null) {
            return Optional.empty();
        }

        String categoryName = categoryNode.asText();
        BigDecimal amount = amountNode.decimalValue();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        Category matched = categories.stream()
                .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            return Optional.empty();
        }

        return Optional.of(new BudgetIntent(matched.getId(), matched.getName(),
                matched.getEmoji(), amount));
    }

    public record BudgetIntent(Long categoryId, String categoryName,
                               String emoji, BigDecimal amount) {
    }
}