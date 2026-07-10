package com.ando.financiando.service;

import com.ando.financiando.model.Budget;
import com.ando.financiando.model.Category;
import com.ando.financiando.repository.BudgetRepository;
import com.ando.financiando.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository,
                         CategoryService categoryService) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    public Budget setBudget(Long categoryId, BigDecimal limit, String yearMonth) {
        Category category = categoryService.getEntityById(categoryId);

        Budget budget = budgetRepository
                .findByCategoryIdAndYearMonth(categoryId, yearMonth)
                .orElse(new Budget(category, limit, yearMonth));

        budget.setMonthlyLimit(limit);

        return budgetRepository.save(budget);
    }

    public List<BudgetStatus> getStatusForMonth(String yearMonth) {
        List<Budget> budgets = budgetRepository.findByYearMonthWithCategory(yearMonth);

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return budgets.stream()
                .map(budget -> {
                    BigDecimal spent = transactionRepository.sumExpensesByCategoryBetween(
                            budget.getCategory().getId(), start, end);
                    return buildStatus(budget, spent);
                })
                .toList();
    }

    private BudgetStatus buildStatus(Budget budget, BigDecimal spent) {
        BigDecimal limit = budget.getMonthlyLimit();
        BigDecimal remaining = limit.subtract(spent);

        int percentUsed = 0;
        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            percentUsed = spent.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        return new BudgetStatus(
                budget.getCategory().getName(),
                budget.getCategory().getEmoji(),
                limit,
                spent,
                remaining,
                percentUsed);
    }

    public record BudgetStatus(
            String categoryName,
            String emoji,
            BigDecimal limit,
            BigDecimal spent,
            BigDecimal remaining,
            int percentUsed) {
    }
}