package com.ando.financiando.service;

import com.ando.financiando.model.Category;
import com.ando.financiando.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpenseParserTest {

    private ExpenseParser parser;

    @BeforeEach
    void setUp() {
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        AiExpenseParser aiExpenseParser = mock(AiExpenseParser.class);

        Category comida = new Category("Comida", "🍽️", List.of("almuerzo", "comida", "cena"));
        Category transporte = new Category("Transporte", "🚌", List.of("taxi", "uber", "bus"));
        Category otros = new Category("Otros", "📦", List.of());

        when(categoryRepository.findAll())
                .thenReturn(List.of(comida, transporte, otros));

        when(aiExpenseParser.parse(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(ParsedExpense.failure());

        parser = new ExpenseParser(categoryRepository, aiExpenseParser);
    }

    @Test
    void extraeMontoEnteroYCategoria() {
        ParsedExpense result = parser.parse("almuerzo 20");

        assertThat(result.successful()).isTrue();
        assertThat(result.amount()).isEqualByComparingTo("20");
        assertThat(result.category().getName()).isEqualTo("Comida");
    }

    @Test
    void extraeMontoConDecimales() {
        ParsedExpense result = parser.parse("taxi 15.50");

        assertThat(result.successful()).isTrue();
        assertThat(result.amount()).isEqualByComparingTo("15.50");
        assertThat(result.category().getName()).isEqualTo("Transporte");
    }

    @Test
    void aceptaComaComoDecimal() {
        ParsedExpense result = parser.parse("cena 30,90");

        assertThat(result.amount()).isEqualByComparingTo("30.90");
        assertThat(result.category().getName()).isEqualTo("Comida");
    }

    @Test
    void usaOtrosCuandoNoHayKeywordNiIa() {
        ParsedExpense result = parser.parse("no se que 40");

        assertThat(result.successful()).isTrue();
        assertThat(result.category().getName()).isEqualTo("Otros");
    }

    @Test
    void fallaCuandoNoHayMonto() {
        ParsedExpense result = parser.parse("hola que tal");

        assertThat(result.successful()).isFalse();
    }

    @Test
    void fallaConMensajeVacio() {
        ParsedExpense result = parser.parse("   ");

        assertThat(result.successful()).isFalse();
    }

    @Test
    void ignoraMayusculas() {
        ParsedExpense result = parser.parse("ALMUERZO 25");

        assertThat(result.category().getName()).isEqualTo("Comida");
    }
}