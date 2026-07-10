package com.ando.financiando.controller;

import com.ando.financiando.dto.BudgetStatusResponse;
import com.ando.financiando.dto.CategorySpendingResponse;
import com.ando.financiando.repository.TransactionRepository;
import com.ando.financiando.repository.projection.CategoryTotal;
import com.ando.financiando.service.BudgetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;

    public ReportController(TransactionRepository transactionRepository,
                            BudgetService budgetService) {
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
    }

    @GetMapping("/spending-by-category")
    public List<CategorySpendingResponse> spendingByCategory() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        List<CategoryTotal> totals = transactionRepository.sumByCategoryBetween(start, end);

        return totals.stream()
                .map(t -> new CategorySpendingResponse(
                        t.getCategoryName(), t.getEmoji(), t.getTotal()))
                .toList();
    }

    @GetMapping("/budget-status")
    public List<BudgetStatusResponse> budgetStatus() {
        String currentMonth = YearMonth.now().toString();
        List<BudgetService.BudgetStatus> statuses = budgetService.getStatusForMonth(currentMonth);

        return statuses.stream()
                .map(s -> new BudgetStatusResponse(
                        s.categoryName(), s.emoji(), s.limit(),
                        s.spent(), s.remaining(), s.percentUsed()))
                .toList();
    }
}