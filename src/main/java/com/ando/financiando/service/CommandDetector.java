package com.ando.financiando.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CommandDetector {

    private static final List<String> BALANCE_PHRASES = List.of(
            "balance", "como voy", "como voy este mes", "cuanto voy",
            "cuanto gaste", "cuanto he gastado", "cuanto llevo", "cuanto gastado",
            "mis finanzas", "mis gastos", "resumen", "mi resumen", "estado",
            "como van mis cuentas", "mis cuentas"
    );

    private static final List<String> BUDGET_QUERY_PHRASES = List.of(
            "mis presupuestos", "presupuestos", "mi presupuesto",
            "como van mis presupuestos", "ver presupuestos"
    );

    // Detecta "presupuesto <categoria> <monto>", ej: "presupuesto Comida 500"
    private static final Pattern BUDGET_SET_PATTERN = Pattern.compile(
            "presupuesto\\s+(.+?)\\s+(\\d+(?:[.,]\\d{1,2})?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> ONBOARDING_VERBS = List.of(
            "configura", "configurar", "arma", "armar", "crea", "crear",
            "planifica", "planificar", "distribuye", "distribuir", "reparte", "repartir"
    );

    private static final List<String> INCOME_SIGNALS = List.of(
            "gano", "sueldo", "ingreso", "salario", "recibo", "cobro", "gana"
    );

    public boolean isBalanceQuery(String message) {
        String normalized = normalize(message);
        return !normalized.isBlank() && BALANCE_PHRASES.stream().anyMatch(normalized::contains);
    }

    public boolean isBudgetQuery(String message) {
        String normalized = normalize(message);
        return !normalized.isBlank() && BUDGET_QUERY_PHRASES.stream().anyMatch(normalized::contains);
    }

    public boolean isOnboardingRequest(String message) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return false;
        }

        boolean mentionsBudget = normalized.contains("presupuesto");
        boolean hasOnboardingVerb = ONBOARDING_VERBS.stream().anyMatch(normalized::contains);
        boolean mentionsIncome = INCOME_SIGNALS.stream().anyMatch(normalized::contains);

        // Es onboarding si menciona presupuesto Y (un verbo de configurar O habla de su ingreso)
        return mentionsBudget && (hasOnboardingVerb || mentionsIncome);
    }

    public Optional<BudgetCommand> parseBudgetSet(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = BUDGET_SET_PATTERN.matcher(message.trim());
        if (matcher.matches()) {
            String categoryName = matcher.group(1).trim();
            BigDecimal amount = new BigDecimal(matcher.group(2).replace(",", "."));
            return Optional.of(new BudgetCommand(categoryName, amount));
        }
        return Optional.empty();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.trim().toLowerCase();
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    public record BudgetCommand(String categoryName, BigDecimal amount) {
    }

    private static final List<String> INSIGHTS_PHRASES = List.of(
            "analiza mis gastos", "analiza mis finanzas", "insights",
            "dame insights", "analizame", "como estan mis finanzas",
            "recomendaciones", "consejos", "que opinas de mis gastos",
            "analisis", "revisa mis gastos"
    );

    public boolean isInsightsQuery(String message) {
        String normalized = normalize(message);
        return !normalized.isBlank() && INSIGHTS_PHRASES.stream().anyMatch(normalized::contains);
    }
}