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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AiBudgetPlanner {

    private static final Logger log = LoggerFactory.getLogger(AiBudgetPlanner.class);

    private final AiClient aiClient;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public AiBudgetPlanner(AiClient aiClient,
                           CategoryRepository categoryRepository,
                           ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<List<ProposedBudget>> propose(BigDecimal income, BigDecimal savingsGoal) {
        List<Category> expenseCategories = categoryRepository.findAll().stream()
                .filter(c -> c.getType() == TransactionType.EXPENSE)
                .toList();

        if (expenseCategories.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal toDistribute = income.subtract(savingsGoal);
        if (toDistribute.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        String categoryList = expenseCategories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        String systemPrompt = """
                Eres un asistente de finanzas personales. Propón un reparto mensual de
                presupuesto por categoría de gasto, orientándote en principios generales
                como la regla 50/30/20 (necesidades, deseos, ahorro), adaptado a Perú.

                Devuelve SOLO un JSON válido, sin texto adicional, con esta forma:
                {"budgets": [{"category": "string", "amount": number}, ...]}

                Datos:
                - Ingreso mensual: %s
                - Meta de ahorro: %s
                - Total a repartir entre gastos: %s
                - Categorías de gasto disponibles: %s

                Reglas:
                - Usa SOLO las categorías de la lista.
                - La suma de todos los "amount" debe ser igual al total a repartir (%s).
                - Prioriza necesidades (Comida, Servicios, Transporte, Salud) sobre deseos.
                - Montos redondeados a números enteros.
                """.formatted(income, savingsGoal, toDistribute, categoryList, toDistribute);

        try {
            String raw = aiClient.chat(systemPrompt, "Genera el reparto de presupuestos.");
            return parseResponse(raw, expenseCategories, toDistribute);
        } catch (Exception e) {
            log.warn("Falló la generación de presupuestos con IA. Causa: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<List<ProposedBudget>> parseResponse(String raw, List<Category> categories,
                                                         BigDecimal toDistribute) throws Exception {
        String json = extractJson(raw);
        if (json == null) {
            return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(json);
        JsonNode budgetsNode = root.get("budgets");
        if (budgetsNode == null || !budgetsNode.isArray() || budgetsNode.isEmpty()) {
            return Optional.empty();
        }

        List<ProposedBudget> proposals = new ArrayList<>();
        for (JsonNode item : budgetsNode) {
            String categoryName = item.path("category").asText(null);
            JsonNode amountNode = item.get("amount");
            if (categoryName == null || amountNode == null) {
                continue;
            }

            Category matched = categories.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                    .findFirst()
                    .orElse(null);

            if (matched != null) {
                proposals.add(new ProposedBudget(
                        matched.getId(), matched.getName(),
                        matched.getEmoji(), amountNode.decimalValue()));
            }
        }

        return proposals.isEmpty() ? Optional.empty() : Optional.of(proposals);
    }

    private String extractJson(String raw) {
        if (raw == null) {
            return null;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return null;
    }

    public record ProposedBudget(Long categoryId, String categoryName,
                                 String emoji, BigDecimal amount) {
    }
}