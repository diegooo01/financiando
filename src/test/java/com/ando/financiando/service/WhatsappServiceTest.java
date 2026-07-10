package com.ando.financiando.service;

import com.ando.financiando.model.Category;
import com.ando.financiando.model.PendingSuggestion;
import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.CategoryRepository;
import com.ando.financiando.repository.PendingBudgetPlanRepository;
import com.ando.financiando.repository.PendingSuggestionRepository;
import com.ando.financiando.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsappServiceTest {

    private ExpenseParser expenseParser;
    private TransactionRepository transactionRepository;
    private BalanceService balanceService;
    private CommandDetector commandDetector;
    private CategoryRepository categoryRepository;
    private PendingSuggestionRepository pendingSuggestionRepository;
    private BudgetService budgetService;
    private AiBudgetParser aiBudgetParser;
    private AiOnboardingParser aiOnboardingParser;
    private AiBudgetPlanner aiBudgetPlanner;
    private PendingBudgetPlanRepository pendingBudgetPlanRepository;
    private AiInsightsService aiInsightsService;

    private WhatsappService service;

    private static final String PHONE = "whatsapp:+51999999999";

    @BeforeEach
    void setUp() {
        expenseParser = mock(ExpenseParser.class);
        transactionRepository = mock(TransactionRepository.class);
        balanceService = mock(BalanceService.class);
        commandDetector = mock(CommandDetector.class);
        categoryRepository = mock(CategoryRepository.class);
        pendingSuggestionRepository = mock(PendingSuggestionRepository.class);
        budgetService = mock(BudgetService.class);
        aiBudgetParser = mock(AiBudgetParser.class);
        aiOnboardingParser = mock(AiOnboardingParser.class);
        aiBudgetPlanner = mock(AiBudgetPlanner.class);
        pendingBudgetPlanRepository = mock(PendingBudgetPlanRepository.class);
        aiInsightsService = mock(AiInsightsService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        service = new WhatsappService(expenseParser, transactionRepository,
                balanceService, commandDetector, categoryRepository,
                pendingSuggestionRepository, budgetService, aiBudgetParser,
                aiOnboardingParser, aiBudgetPlanner, pendingBudgetPlanRepository,
                objectMapper, aiInsightsService);

        // Neutralizamos todas las ramas nuevas por defecto
        when(pendingSuggestionRepository.findByUserPhone(anyString()))
                .thenReturn(Optional.empty());
        when(pendingBudgetPlanRepository.findByUserPhone(anyString()))
                .thenReturn(Optional.empty());
        when(commandDetector.isBalanceQuery(anyString())).thenReturn(false);
        when(commandDetector.isInsightsQuery(anyString())).thenReturn(false);
        when(commandDetector.isOnboardingRequest(anyString())).thenReturn(false);
        when(commandDetector.parseBudgetSet(anyString())).thenReturn(Optional.empty());
        when(commandDetector.isBudgetQuery(anyString())).thenReturn(false);
        when(aiBudgetParser.parse(anyString())).thenReturn(Optional.empty());
        when(budgetService.checkAlert(any(), anyString()))
                .thenReturn(new BudgetService.BudgetAlert(BudgetService.AlertLevel.NONE, null));
    }

    @Test
    void registraGastoNormal() {
        Category comida = new Category("Comida", "🍽️", TransactionType.EXPENSE, List.of());
        when(expenseParser.parse("almuerzo 20"))
                .thenReturn(ParsedExpense.success(new BigDecimal("20"), comida, "almuerzo 20"));

        String reply = service.buildReply("almuerzo 20", PHONE);

        assertThat(reply).contains("Registrado");
        assertThat(reply).contains("Comida");
        verify(transactionRepository).save(any());
    }

    @Test
    void cuandoHaySugerenciaGuardaEstadoPendiente() {
        when(expenseParser.parse(anyString()))
                .thenReturn(ParsedExpense.suggestion(
                        new BigDecimal("30"), "comida perro", "Mascotas", "🐶"));

        String reply = service.buildReply("comida para el perro 30", PHONE);

        assertThat(reply).contains("categoría nueva");
        assertThat(reply).contains("Mascotas");
        verify(pendingSuggestionRepository).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void confirmarSugerenciaCreaCategoriaYRegistra() {
        PendingSuggestion pending = new PendingSuggestion(
                PHONE, "Mascotas", "🐶", new BigDecimal("30"), "comida perro");
        when(pendingSuggestionRepository.findByUserPhone(PHONE))
                .thenReturn(Optional.of(pending));

        Category saved = new Category("Mascotas", "🐶", TransactionType.EXPENSE, List.of());
        when(categoryRepository.save(any())).thenReturn(saved);

        String reply = service.buildReply("si", PHONE);

        assertThat(reply).contains("Creé la categoría");
        assertThat(reply).contains("Mascotas");
        verify(categoryRepository).save(any());
        verify(transactionRepository).save(any());
        verify(pendingSuggestionRepository).deleteByUserPhone(PHONE);
    }

    @Test
    void rechazarSugerenciaRegistraEnOtros() {
        PendingSuggestion pending = new PendingSuggestion(
                PHONE, "Mascotas", "🐶", new BigDecimal("30"), "comida perro");
        when(pendingSuggestionRepository.findByUserPhone(PHONE))
                .thenReturn(Optional.of(pending));

        Category otros = new Category("Otros", "📦", TransactionType.EXPENSE, List.of());
        when(categoryRepository.findByName("Otros")).thenReturn(Optional.of(otros));

        String reply = service.buildReply("no", PHONE);

        assertThat(reply).contains("Registrado");
        verify(categoryRepository, never()).save(any());
        verify(transactionRepository).save(any());
        verify(pendingSuggestionRepository).deleteByUserPhone(PHONE);
    }

    @Test
    void onboardingProponePlanYGuardaPendiente() {
        when(commandDetector.isOnboardingRequest(anyString())).thenReturn(true);
        when(aiOnboardingParser.parse(anyString()))
                .thenReturn(Optional.of(new AiOnboardingParser.OnboardingData(
                        new BigDecimal("2000"), new BigDecimal("400"))));
        when(aiBudgetPlanner.propose(any(), any()))
                .thenReturn(Optional.of(List.of(
                        new AiBudgetPlanner.ProposedBudget(1L, "Comida", "🍽️", new BigDecimal("800")),
                        new AiBudgetPlanner.ProposedBudget(2L, "Transporte", "🚌", new BigDecimal("800")))));

        String reply = service.buildReply("configura mis presupuestos, gano 2000 ahorro 400", PHONE);

        assertThat(reply).contains("te propongo");
        assertThat(reply).contains("Comida");
        verify(pendingBudgetPlanRepository).save(any());
        verify(budgetService, never()).setBudget(any(), any(), anyString());
    }

    @Test
    void confirmarPlanGuardaLosPresupuestos() throws Exception {
        String planJson = new ObjectMapper().writeValueAsString(List.of(
                new AiBudgetPlanner.ProposedBudget(1L, "Comida", "🍽️", new BigDecimal("800")),
                new AiBudgetPlanner.ProposedBudget(2L, "Transporte", "🚌", new BigDecimal("800"))));

        com.ando.financiando.model.PendingBudgetPlan plan =
                new com.ando.financiando.model.PendingBudgetPlan(PHONE, planJson);
        when(pendingBudgetPlanRepository.findByUserPhone(PHONE))
                .thenReturn(Optional.of(plan));

        String reply = service.buildReply("si", PHONE);

        assertThat(reply).contains("Guardé tus presupuestos");
        verify(budgetService, org.mockito.Mockito.times(2)).setBudget(any(), any(), anyString());
        verify(pendingBudgetPlanRepository).deleteByUserPhone(PHONE);
    }

    @Test
    void insightsDevuelveAnalisis() {
        when(commandDetector.isInsightsQuery(anyString())).thenReturn(true);
        when(aiInsightsService.generateMonthlyInsights())
                .thenReturn("📊 Observaciones de tu mes:\n\n🔝 Tu mayor gasto es Comida");

        String reply = service.buildReply("analiza mis gastos", PHONE);

        assertThat(reply).contains("Observaciones");
        verify(aiInsightsService).generateMonthlyInsights();
        verify(transactionRepository, never()).save(any());
    }
}