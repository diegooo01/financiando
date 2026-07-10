package com.ando.financiando.service;

import com.ando.financiando.repository.TransactionRepository;
import com.ando.financiando.repository.projection.CategoryTotal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiInsightsService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightsService.class);

    private static final String DISCLAIMER =
            "\n\n💡 Son observaciones sobre tus datos, no asesoría financiera profesional.";

    private final TransactionRepository transactionRepository;
    private final BalanceService balanceService;
    private final AiClient aiClient;

    public AiInsightsService(TransactionRepository transactionRepository,
                             BalanceService balanceService,
                             AiClient aiClient) {
        this.transactionRepository = transactionRepository;
        this.balanceService = balanceService;
        this.aiClient = aiClient;
    }

    public String generateMonthlyInsights() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        List<CategoryTotal> totals = transactionRepository.sumByCategoryBetween(start, end);

        if (totals.isEmpty()) {
            return "📊 Aún no tienes gastos registrados este mes. Registra algunos y te daré observaciones.";
        }

        BalanceService.Balance balance = balanceService.calculate(start, LocalDate.now());

        String data = buildDataSummary(totals, balance);

        String systemPrompt = """
                Eres un asistente de finanzas personales. Analiza los datos de gastos del usuario
                y da 2 a 4 observaciones BREVES y útiles, basadas SOLO en los números presentados.

                Reglas estrictas:
                - Describe patrones que ves en los datos (categoría con más gasto, proporción del ingreso, etc.).
                - NO des consejos de inversión ni recomendaciones financieras prescriptivas.
                - NO te presentes como asesor financiero.
                - Usa un tono cercano y claro, en español peruano.
                - Cada observación en una línea, empezando con un emoji relevante.
                - Sé conciso. Máximo 4 observaciones.
                """;

        try {
            String insights = aiClient.chat(systemPrompt, data);
            return "📊 Observaciones de tu mes:\n\n" + insights.trim() + DISCLAIMER;
        } catch (Exception e) {
            log.warn("Falló la generación de insights. Causa: {}", e.getMessage());
            return buildFallbackInsights(totals, balance);
        }
    }

    private String buildDataSummary(List<CategoryTotal> totals, BalanceService.Balance balance) {
        String categoryLines = totals.stream()
                .map(t -> "- " + t.getCategoryName() + ": S/" + t.getTotal())
                .collect(Collectors.joining("\n"));

        return String.format("""
                Ingresos del mes: S/%s
                Gastos del mes: S/%s
                Balance: S/%s

                Gasto por categoría:
                %s
                """, balance.income(), balance.expenses(), balance.net(), categoryLines);
    }

    private String buildFallbackInsights(List<CategoryTotal> totals, BalanceService.Balance balance) {
        CategoryTotal top = totals.get(0);
        String balanceLine = balance.net().signum() >= 0
                ? "✅ Tu balance es positivo: S/" + balance.net()
                : "⚠️ Tu balance es negativo: S/" + balance.net();

        return "📊 Observaciones de tu mes:\n\n" +
                "🔝 Tu mayor gasto es " + top.getCategoryName() + " con S/" + top.getTotal() + "\n" +
                balanceLine + DISCLAIMER;
    }
}