package com.ando.financiando.service;

import com.ando.financiando.model.Category;
import com.ando.financiando.model.PendingSuggestion;
import com.ando.financiando.model.Transaction;
import com.ando.financiando.model.TransactionSource;
import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.CategoryRepository;
import com.ando.financiando.repository.PendingSuggestionRepository;
import com.ando.financiando.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class WhatsappService {

    private static final List<String> YES = List.of("si", "sí", "yes", "ok", "dale", "ya", "claro");
    private static final List<String> NO = List.of("no", "nop", "nel", "nada");

    private final ExpenseParser expenseParser;
    private final TransactionRepository transactionRepository;
    private final BalanceService balanceService;
    private final CommandDetector commandDetector;
    private final CategoryRepository categoryRepository;
    private final PendingSuggestionRepository pendingSuggestionRepository;

    public WhatsappService(ExpenseParser expenseParser,
                           TransactionRepository transactionRepository,
                           BalanceService balanceService,
                           CommandDetector commandDetector,
                           CategoryRepository categoryRepository,
                           PendingSuggestionRepository pendingSuggestionRepository) {
        this.expenseParser = expenseParser;
        this.transactionRepository = transactionRepository;
        this.balanceService = balanceService;
        this.commandDetector = commandDetector;
        this.categoryRepository = categoryRepository;
        this.pendingSuggestionRepository = pendingSuggestionRepository;
    }

    @Transactional
    public String buildReply(String incomingMessage, String from) {
        // PASO 1: ¿Hay una sugerencia pendiente para este usuario?
        Optional<PendingSuggestion> pending = pendingSuggestionRepository.findByUserPhone(from);
        if (pending.isPresent()) {
            String answer = normalize(incomingMessage);
            if (YES.contains(answer)) {
                return confirmSuggestion(pending.get());
            }
            if (NO.contains(answer)) {
                return rejectSuggestion(pending.get());
            }
            // Respondió otra cosa: cancelamos la pendiente y seguimos con el mensaje normal
            pendingSuggestionRepository.deleteByUserPhone(from);
        }

        // PASO 2: ¿Es una consulta de balance?
        if (commandDetector.isBalanceQuery(incomingMessage)) {
            return buildBalanceReply();
        }

        // PASO 3: Parsear como gasto
        ParsedExpense parsed = expenseParser.parse(incomingMessage);

        if (parsed.hasSuggestion()) {
            return createPendingSuggestion(from, parsed);
        }

        if (!parsed.successful()) {
            return "No entendí tu gasto 🤔. Escríbelo así: *almuerzo 20* o *taxi 15.50*";
        }

        return registerExpense(parsed.amount(), parsed.description(),
                parsed.category(), incomingMessage);
    }

    private String createPendingSuggestion(String from, ParsedExpense parsed) {
        pendingSuggestionRepository.deleteByUserPhone(from);

        PendingSuggestion suggestion = new PendingSuggestion(
                from,
                parsed.suggestedCategoryName(),
                parsed.suggestedCategoryEmoji(),
                parsed.amount(),
                parsed.description());
        pendingSuggestionRepository.save(suggestion);

        return String.format(
                "🤔 Esto parece de una categoría nueva: %s *%s*.%n" +
                        "¿La creo y registro ahí? Responde *SÍ* o *NO*",
                parsed.suggestedCategoryEmoji(),
                parsed.suggestedCategoryName());
    }

    private String confirmSuggestion(PendingSuggestion suggestion) {
        Category category = new Category(
                suggestion.getSuggestedCategoryName(),
                suggestion.getSuggestedCategoryEmoji(),
                TransactionType.EXPENSE,
                List.of());
        Category saved = categoryRepository.save(category);

        String reply = registerExpense(
                suggestion.getPendingAmount(),
                suggestion.getPendingDescription(),
                saved,
                suggestion.getPendingDescription());

        pendingSuggestionRepository.deleteByUserPhone(suggestion.getUserPhone());

        return "✨ Creé la categoría " + saved.getEmoji() + " *" + saved.getName() + "*.%n"
                .formatted() + reply;
    }

    private String rejectSuggestion(PendingSuggestion suggestion) {
        Category otros = categoryRepository.findByName("Otros")
                .orElseGet(() -> categoryRepository.findAll().get(0));

        String reply = registerExpense(
                suggestion.getPendingAmount(),
                suggestion.getPendingDescription(),
                otros,
                suggestion.getPendingDescription());

        pendingSuggestionRepository.deleteByUserPhone(suggestion.getUserPhone());

        return reply;
    }

    private String registerExpense(java.math.BigDecimal amount, String description,
                                   Category category, String rawMessage) {
        Transaction transaction = new Transaction(
                amount, description, category,
                LocalDate.now(), TransactionSource.WHATSAPP, TransactionType.EXPENSE);
        transaction.setRawMessage(rawMessage);
        transactionRepository.save(transaction);

        return String.format(
                "✅ Registrado: %s S/%.2f en %s (%s)",
                category.getEmoji(), amount, category.getName(), description);
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
                balance.income(), balance.expenses(),
                balance.net().signum() >= 0 ? "✅" : "⚠️", balance.net());
    }

    private String normalize(String message) {
        return message == null ? "" : message.trim().toLowerCase();
    }
}