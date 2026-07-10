package com.ando.financiando.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class AiOnboardingParser {

    private static final Logger log = LoggerFactory.getLogger(AiOnboardingParser.class);

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public AiOnboardingParser(AiClient aiClient, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    public Optional<OnboardingData> parse(String message) {
        String systemPrompt = """
                El usuario quiere configurar sus presupuestos mensuales.
                Extrae su ingreso mensual y su meta de ahorro del mensaje.
                Devuelve SOLO un JSON válido, sin texto adicional:
                {"income": number, "savingsGoal": number}

                Reglas:
                - "income" es el ingreso o sueldo mensual (número, sin símbolo).
                - "savingsGoal" es cuánto quiere ahorrar al mes. Si no lo menciona, usa 0.
                - Si no puedes identificar el ingreso, devuelve {"income": 0, "savingsGoal": 0}.
                """;

        try {
            String raw = aiClient.chat(systemPrompt, message);
            return parseResponse(raw);
        } catch (Exception e) {
            log.warn("Falló el parseo de onboarding con IA: '{}'. Causa: {}", message, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<OnboardingData> parseResponse(String raw) throws Exception {
        String json = extractJson(raw);
        if (json == null) {
            return Optional.empty();
        }

        JsonNode node = objectMapper.readTree(json);
        JsonNode incomeNode = node.get("income");
        if (incomeNode == null) {
            return Optional.empty();
        }

        BigDecimal income = incomeNode.decimalValue();
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal savingsGoal = BigDecimal.ZERO;
        JsonNode savingsNode = node.get("savingsGoal");
        if (savingsNode != null && !savingsNode.isNull()) {
            savingsGoal = savingsNode.decimalValue();
            if (savingsGoal.compareTo(BigDecimal.ZERO) < 0) {
                savingsGoal = BigDecimal.ZERO;
            }
        }

        return Optional.of(new OnboardingData(income, savingsGoal));
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

    public record OnboardingData(BigDecimal income, BigDecimal savingsGoal) {
    }
}