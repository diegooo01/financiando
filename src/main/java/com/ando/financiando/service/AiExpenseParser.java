package com.ando.financiando.service;

import com.ando.financiando.model.Category;
import com.ando.financiando.repository.CategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiExpenseParser {

    private static final Logger log = LoggerFactory.getLogger(AiExpenseParser.class);

    private final AiClient aiClient;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public AiExpenseParser(AiClient aiClient,
                           CategoryRepository categoryRepository,
                           ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    public ParsedExpense parse(String message) {
        List<Category> categories = categoryRepository.findAll();
        String categoryNames = categories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        String systemPrompt = """
                Eres un asistente que extrae gastos de mensajes en español peruano.
                Devuelve SOLO un JSON válido, sin texto adicional, con esta forma exacta:
                {"amount": number, "category": "string", "description": "string"}

                Reglas:
                - "amount" es el monto numérico del gasto (sin símbolo de moneda).
                - "category" DEBE ser exactamente una de estas: %s
                - Si no encaja en ninguna, usa "Otros".
                - "description" es un resumen corto del gasto.
                - Si el mensaje no contiene ningún gasto con monto, devuelve {"amount": 0, "category": "Otros", "description": ""}.
                """.formatted(categoryNames);

        try {
            String rawResponse = aiClient.chat(systemPrompt, message);
            return parseAiResponse(rawResponse, categories);
        } catch (Exception e) {
            log.warn("Falló el parseo con IA para el mensaje: '{}'. Causa: {}", message, e.getMessage());
            return ParsedExpense.failure();
        }
    }

    private ParsedExpense parseAiResponse(String rawResponse, List<Category> categories) throws Exception {
        String cleaned = rawResponse
                .replaceAll("(?s)```json", "")
                .replaceAll("(?s)```", "")
                .trim();

        JsonNode json = objectMapper.readTree(cleaned);

        BigDecimal amount = json.get("amount").decimalValue();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ParsedExpense.failure();
        }

        String categoryName = json.get("category").asText();
        String description = json.get("description").asText();

        Category category = categories.stream()
                .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElseGet(() -> categories.stream()
                        .filter(c -> c.getName().equals("Otros"))
                        .findFirst()
                        .orElse(categories.get(0)));

        return ParsedExpense.success(amount, category, description);
    }
}