package com.ando.financiando.service;

import com.ando.financiando.model.Budget;
import com.ando.financiando.model.Category;
import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.BudgetRepository;
import com.ando.financiando.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BudgetServiceTest {

    private BudgetRepository budgetRepository;
    private TransactionRepository transactionRepository;
    private CategoryService categoryService;
    private BudgetService budgetService;

    private Category comida;

    @BeforeEach
    void setUp() {
        budgetRepository = mock(BudgetRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        categoryService = mock(CategoryService.class);
        budgetService = new BudgetService(budgetRepository, transactionRepository, categoryService);

        comida = new Category("Comida", "🍽️", TransactionType.EXPENSE, java.util.List.of());
    }

    private void givenBudgetOf(String limit) {
        Budget budget = new Budget(comida, new BigDecimal(limit), "2026-07");
        when(budgetRepository.findByCategoryIdAndYearMonth(eq(1L), eq("2026-07")))
                .thenReturn(Optional.of(budget));
    }

    private void givenSpent(String spent) {
        when(transactionRepository.sumExpensesByCategoryBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal(spent));
    }

    @Test
    void sinPresupuestoNoHayAlerta() {
        when(budgetRepository.findByCategoryIdAndYearMonth(any(), any()))
                .thenReturn(Optional.empty());

        BudgetService.BudgetAlert alert = budgetService.checkAlert(1L, "2026-07");

        assertThat(alert.level()).isEqualTo(BudgetService.AlertLevel.NONE);
    }

    @Test
    void gastoBajoNoActivaAlerta() {
        givenBudgetOf("100");
        givenSpent("50"); // 50%

        BudgetService.BudgetAlert alert = budgetService.checkAlert(1L, "2026-07");

        assertThat(alert.level()).isEqualTo(BudgetService.AlertLevel.NONE);
    }

    @Test
    void alOchentaPorcientoActivaWarning() {
        givenBudgetOf("100");
        givenSpent("85"); // 85%

        BudgetService.BudgetAlert alert = budgetService.checkAlert(1L, "2026-07");

        assertThat(alert.level()).isEqualTo(BudgetService.AlertLevel.WARNING);
        assertThat(alert.status().percentUsed()).isEqualTo(85);
    }

    @Test
    void alCienPorcientoActivaExceeded() {
        givenBudgetOf("100");
        givenSpent("100"); // 100%

        BudgetService.BudgetAlert alert = budgetService.checkAlert(1L, "2026-07");

        assertThat(alert.level()).isEqualTo(BudgetService.AlertLevel.EXCEEDED);
    }

    @Test
    void alPasarseActivaExceeded() {
        givenBudgetOf("100");
        givenSpent("130"); // 130%

        BudgetService.BudgetAlert alert = budgetService.checkAlert(1L, "2026-07");

        assertThat(alert.level()).isEqualTo(BudgetService.AlertLevel.EXCEEDED);
        assertThat(alert.status().percentUsed()).isEqualTo(130);
    }

    @Test
    void justoDebajoDelOchentaNoActivaWarning() {
        givenBudgetOf("100");
        givenSpent("79"); // 79%

        BudgetService.BudgetAlert alert = budgetService.checkAlert(1L, "2026-07");

        assertThat(alert.level()).isEqualTo(BudgetService.AlertLevel.NONE);
    }
}