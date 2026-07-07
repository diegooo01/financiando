package com.ando.financiando.service;

import com.ando.financiando.model.Category;
import com.ando.financiando.model.PendingSuggestion;
import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.CategoryRepository;
import com.ando.financiando.repository.PendingSuggestionRepository;
import com.ando.financiando.repository.TransactionRepository;
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

        service = new WhatsappService(expenseParser, transactionRepository,
                balanceService, commandDetector, categoryRepository,
                pendingSuggestionRepository);

        // Por defecto: sin pendiente, no es balance
        when(pendingSuggestionRepository.findByUserPhone(anyString()))
                .thenReturn(Optional.empty());
        when(commandDetector.isBalanceQuery(anyString())).thenReturn(false);
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
        // Guardó el estado pendiente y NO registró el gasto todavía
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
        // NO creó categoría nueva, pero sí registró el gasto
        verify(categoryRepository, never()).save(any());
        verify(transactionRepository).save(any());
        verify(pendingSuggestionRepository).deleteByUserPhone(PHONE);
    }
}