package com.ando.financiando.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;

@Component
public class CommandDetector {

    private static final List<String> BALANCE_PHRASES = List.of(
            "balance",
            "como voy",
            "como voy este mes",
            "cuanto voy",
            "cuanto gaste",
            "cuanto he gastado",
            "cuanto llevo",
            "cuanto gastado",
            "mis finanzas",
            "mis gastos",
            "resumen",
            "mi resumen",
            "estado",
            "como van mis cuentas",
            "mis cuentas"
    );

    public boolean isBalanceQuery(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = normalize(message);

        return BALANCE_PHRASES.stream().anyMatch(normalized::contains);
    }

    private String normalize(String text) {
        String lower = text.trim().toLowerCase();
        String withoutAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents;
    }
}