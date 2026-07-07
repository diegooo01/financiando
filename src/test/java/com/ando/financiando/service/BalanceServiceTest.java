package com.ando.financiando.service;

import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BalanceServiceTest {

    private BalanceService balanceService;
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        balanceService = new BalanceService(transactionRepository);
    }

    @Test
    void calculaBalancePositivo() {
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);

        when(transactionRepository.sumByTypeBetween(eq(TransactionType.INCOME), any(), any()))
                .thenReturn(new BigDecimal("2000.00"));
        when(transactionRepository.sumByTypeBetween(eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("750.00"));

        BalanceService.Balance balance = balanceService.calculate(start, end);

        assertThat(balance.income()).isEqualByComparingTo("2000.00");
        assertThat(balance.expenses()).isEqualByComparingTo("750.00");
        assertThat(balance.net()).isEqualByComparingTo("1250.00");
    }

    @Test
    void calculaBalanceNegativo() {
        when(transactionRepository.sumByTypeBetween(eq(TransactionType.INCOME), any(), any()))
                .thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.sumByTypeBetween(eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("800.00"));

        BalanceService.Balance balance = balanceService.calculate(
                LocalDate.now(), LocalDate.now());

        assertThat(balance.net()).isEqualByComparingTo("-300.00");
    }

    @Test
    void calculaBalanceEnCero() {
        when(transactionRepository.sumByTypeBetween(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        BalanceService.Balance balance = balanceService.calculate(
                LocalDate.now(), LocalDate.now());

        assertThat(balance.net()).isEqualByComparingTo("0");
    }
}