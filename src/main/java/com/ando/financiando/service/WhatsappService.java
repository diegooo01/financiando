package com.ando.financiando.service;

import com.ando.financiando.model.Transaction;
import com.ando.financiando.model.TransactionSource;
import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class WhatsappService {

    private final ExpenseParser expenseParser;
    private final TransactionRepository transactionRepository;
    private final BalanceService balanceService;
    private final CommandDetector commandDetector;

    public WhatsappService(ExpenseParser expenseParser,
                           TransactionRepository transactionRepository,
                           BalanceService balanceService,
                           CommandDetector commandDetector) {
        this.expenseParser = expenseParser;
        this.transactionRepository = transactionRepository;
        this.balanceService = balanceService;
        this.commandDetector = commandDetector;
    }

    public String buildReply(String incomingMessage, String from) {
        if (commandDetector.isBalanceQuery(incomingMessage)) {
            return buildBalanceReply();
        }

        ParsedExpense parsed = expenseParser.parse(incomingMessage);

        if (!parsed.successful()) {
            return "No entendí tu gasto 🤔. Escríbelo así: *almuerzo 20* o *taxi 15.50*";
        }

        Transaction transaction = new Transaction(
                parsed.amount(),
                parsed.description(),
                parsed.category(),
                LocalDate.now(),
                TransactionSource.WHATSAPP,
                TransactionType.EXPENSE
        );
        transaction.setRawMessage(incomingMessage);

        transactionRepository.save(transaction);

        return String.format(
                "✅ Registrado: %s S/%.2f en %s %s",
                parsed.category().getEmoji(),
                parsed.amount(),
                parsed.category().getName(),
                "(" + parsed.description() + ")"
        );
    }

    private String buildBalanceReply() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        BalanceService.Balance balance = balanceService.calculate(monthStart, today);

        return String.format(
                "📊 Tu balance de este mes:%n%n" +
                        "💰 Ingresos: S/%.2f%n" +
                        "💸 Gastos: S/%.2f%n" +
                        "%s Balance: S/%.2f",
                balance.income(),
                balance.expenses(),
                balance.net().signum() >= 0 ? "✅" : "⚠️",
                balance.net()
        );
    }
}