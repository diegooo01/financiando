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

    public AiParseResult parse(String message) {
        List<Category> categories = categoryRepository.findAll();
        String categoryNames = categories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        String systemPrompt = """
                Eres un asistente que extrae gastos de mensajes en español peruano y los clasifica.
                Devuelve SOLO un JSON válido, sin texto adicional, con esta forma:
                {"amount": number, "description": "string", "existingCategory": "string o null", "newCategory": "string o null", "newCategoryEmoji": "string o null"}

                Categorías existentes: %s

                Cómo clasificar:
                - Analiza el TEMA REAL del gasto, no solo palabras sueltas.
                  Ejemplo: "comida para el perro" NO es Comida (esa es para personas), es tema de MASCOTAS.
                  Ejemplo: "veterinario" NO es Salud (esa es para personas), es tema de MASCOTAS.
                - Si el tema encaja claramente en una categoría existente, pon su nombre en "existingCategory" y "newCategory" en null.
                - Si el tema es claramente distinto a las categorías existentes y es algo que una persona registraría de forma recurrente (ej: Mascotas, Educación, Tecnología, Ropa, Regalos, Viajes, Hogar), SUGIERE una categoría nueva: pon "existingCategory" en null, "newCategory" con un nombre corto y "newCategoryEmoji" con un emoji apropiado.
                - Prefiere sugerir una categoría específica antes que forzar el gasto en una existente que no encaja bien o en "Otros".
                - "amount" es el monto numérico (sin símbolo de moneda).
                - "description" es un resumen corto del gasto.
                - Si el mensaje no contiene ningún gasto con monto, devuelve amount 0.
                """.formatted(categoryNames);

        try {
            String rawResponse = aiClient.chat(systemPrompt, message);
            return parseAiResponse(rawResponse);
        } catch (Exception e) {
            log.warn("Falló el parseo con IA para el mensaje: '{}'. Causa: {}", message, e.getMessage());
            return AiParseResult.failure();
        }
    }

    private AiParseResult parseAiResponse(String rawResponse) throws Exception {
        String cleaned = extractJson(rawResponse);
        if (cleaned == null) {
            log.warn("La IA no devolvió un JSON reconocible: {}", rawResponse);
            return AiParseResult.failure();
        }

        JsonNode json = objectMapper.readTree(cleaned);

        BigDecimal amount = json.get("amount").decimalValue();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return AiParseResult.failure();
        }

        String description = getText(json, "description");
        String existingCategory = getText(json, "existingCategory");
        String newCategory = getText(json, "newCategory");
        String newCategoryEmoji = getText(json, "newCategoryEmoji");

        return new AiParseResult(
                amount, description, existingCategory,
                newCategory, newCategoryEmoji, true);
    }

    private String getText(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() || value.equalsIgnoreCase("null") ? null : value;
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
}