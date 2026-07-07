package com.ando.financiando.service;

import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class BalanceService {

    private final TransactionRepository transactionRepository;

    public BalanceService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Balance calculate(LocalDate start, LocalDate end) {
        BigDecimal income = transactionRepository.sumByTypeBetween(
                TransactionType.INCOME, start, end);
        BigDecimal expenses = transactionRepository.sumByTypeBetween(
                TransactionType.EXPENSE, start, end);
        BigDecimal net = income.subtract(expenses);

        return new Balance(income, expenses, net);
    }

    public record Balance(BigDecimal income, BigDecimal expenses, BigDecimal net) {
    }
}