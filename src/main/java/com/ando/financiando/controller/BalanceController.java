package com.ando.financiando.controller;

import com.ando.financiando.dto.BalanceResponse;
import com.ando.financiando.service.BalanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/balance")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping
    public BalanceResponse getBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDate today = LocalDate.now();
        LocalDate effectiveStart = (start != null) ? start : today.withDayOfMonth(1);
        LocalDate effectiveEnd = (end != null) ? end : today;

        BalanceService.Balance balance = balanceService.calculate(effectiveStart, effectiveEnd);

        return new BalanceResponse(
                effectiveStart,
                effectiveEnd,
                balance.income(),
                balance.expenses(),
                balance.net()
        );
    }
}