package com.ando.financiando.service;

import com.ando.financiando.model.Transaction;
import com.ando.financiando.model.TransactionSource;
import com.ando.financiando.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class WhatsappService {

    private final ExpenseParser expenseParser;
    private final TransactionRepository transactionRepository;

    public WhatsappService(ExpenseParser expenseParser,
                           TransactionRepository transactionRepository) {
        this.expenseParser = expenseParser;
        this.transactionRepository = transactionRepository;
    }

    public String buildReply(String incomingMessage, String from) {
        ParsedExpense parsed = expenseParser.parse(incomingMessage);

        if (!parsed.successful()) {
            return "No entendí tu gasto 🤔. Escríbelo así: *almuerzo 20* o *taxi 15.50*";
        }

        Transaction transaction = new Transaction(
                parsed.amount(),
                parsed.description(),
                parsed.category(),
                LocalDate.now(),
                TransactionSource.WHATSAPP
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
}