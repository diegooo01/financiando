package com.ando.financiando.service;

import com.ando.financiando.model.Category;
import com.ando.financiando.model.PendingBudgetPlan;
import com.ando.financiando.model.PendingSuggestion;
import com.ando.financiando.model.Transaction;
import com.ando.financiando.model.TransactionSource;
import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.CategoryRepository;
import com.ando.financiando.repository.PendingBudgetPlanRepository;
import com.ando.financiando.repository.PendingSuggestionRepository;
import com.ando.financiando.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final BudgetService budgetService;
    private final AiBudgetParser aiBudgetParser;
    private final AiOnboardingParser aiOnboardingParser;
    private final AiBudgetPlanner aiBudgetPlanner;
    private final PendingBudgetPlanRepository pendingBudgetPlanRepository;
    private final ObjectMapper objectMapper;
    private final AiInsightsService aiInsightsService;

    public WhatsappService(ExpenseParser expenseParser,
                           TransactionRepository transactionRepository,
                           BalanceService balanceService,
                           CommandDetector commandDetector,
                           CategoryRepository categoryRepository,
                           PendingSuggestionRepository pendingSuggestionRepository,
                           BudgetService budgetService,
                           AiBudgetParser aiBudgetParser,
                           AiOnboardingParser aiOnboardingParser,
                           AiBudgetPlanner aiBudgetPlanner,
                           PendingBudgetPlanRepository pendingBudgetPlanRepository,
                           ObjectMapper objectMapper,
                           AiInsightsService aiInsightsService) {
        this.expenseParser = expenseParser;
        this.transactionRepository = transactionRepository;
        this.balanceService = balanceService;
        this.commandDetector = commandDetector;
        this.categoryRepository = categoryRepository;
        this.pendingSuggestionRepository = pendingSuggestionRepository;
        this.budgetService = budgetService;
        this.aiBudgetParser = aiBudgetParser;
        this.aiOnboardingParser = aiOnboardingParser;
        this.aiBudgetPlanner = aiBudgetPlanner;
        this.pendingBudgetPlanRepository = pendingBudgetPlanRepository;
        this.objectMapper = objectMapper;
        this.aiInsightsService = aiInsightsService;
    }

    @Transactional
    public String buildReply(String incomingMessage, String from) {
        // PASO 1: ¿Hay una sugerencia de categoría pendiente?
        Optional<PendingSuggestion> pending = pendingSuggestionRepository.findByUserPhone(from);
        if (pending.isPresent()) {
            String answer = normalize(incomingMessage);
            if (YES.contains(answer)) {
                return confirmSuggestion(pending.get());
            }
            if (NO.contains(answer)) {
                return rejectSuggestion(pending.get());
            }
            pendingSuggestionRepository.deleteByUserPhone(from);
        }

        // PASO 1.5: ¿Hay un plan de presupuestos pendiente?
        Optional<PendingBudgetPlan> pendingPlan = pendingBudgetPlanRepository.findByUserPhone(from);
        if (pendingPlan.isPresent()) {
            String answer = normalize(incomingMessage);
            if (YES.contains(answer)) {
                return confirmBudgetPlan(pendingPlan.get());
            }
            if (NO.contains(answer)) {
                pendingBudgetPlanRepository.deleteByUserPhone(from);
                return "❌ Descarté la propuesta. Puedes definir presupuestos manualmente: *presupuesto Comida 500*";
            }
            pendingBudgetPlanRepository.deleteByUserPhone(from);
        }

        // PASO 2: ¿Es una consulta de balance?
        if (commandDetector.isBalanceQuery(incomingMessage)) {
            return buildBalanceReply();
        }

        // PASO 2.2: petición de insights/análisis
        if (commandDetector.isInsightsQuery(incomingMessage)) {
            return aiInsightsService.generateMonthlyInsights();
        }

        // PASO 2.3: petición de configurar presupuestos con IA
        if (commandDetector.isOnboardingRequest(incomingMessage)) {
            return handleOnboarding(incomingMessage, from);
        }

        // PASO 2.5: comandos de presupuesto
        Optional<CommandDetector.BudgetCommand> budgetSet = commandDetector.parseBudgetSet(incomingMessage);
        if (budgetSet.isPresent()) {
            return handleSetBudget(budgetSet.get());
        }
        if (commandDetector.isBudgetQuery(incomingMessage)) {
            return buildBudgetReply();
        }
        // Respaldo con IA: si menciona "presupuesto" pero el regex no lo entendió
        if (mentionsBudget(incomingMessage)) {
            Optional<AiBudgetParser.BudgetIntent> aiBudget = aiBudgetParser.parse(incomingMessage);
            if (aiBudget.isPresent()) {
                return handleSetBudgetFromAi(aiBudget.get());
            }
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

        return "✨ Creé la categoría " + saved.getEmoji() + " *" + saved.getName() + "*.\n" + reply;
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

    private String registerExpense(BigDecimal amount, String description,
                                   Category category, String rawMessage) {
        TransactionType type = category.getType();

        Transaction transaction = new Transaction(
                amount, description, category,
                LocalDate.now(), TransactionSource.WHATSAPP, type);
        transaction.setRawMessage(rawMessage);
        transactionRepository.save(transaction);

        String verb = (type == TransactionType.INCOME) ? "Ingreso" : "Registrado";
        String confirmation = String.format(
                "✅ %s: %s S/%.2f en %s (%s)",
                verb, category.getEmoji(), amount, category.getName(), description);

        // Las alertas de presupuesto solo aplican a gastos
        if (type == TransactionType.EXPENSE) {
            String alert = buildAlertIfNeeded(category);
            if (!alert.isEmpty()) {
                return confirmation + "\n\n" + alert;
            }
        }

        return confirmation;
    }

    private String buildAlertIfNeeded(Category category) {
        String currentMonth = java.time.YearMonth.now().toString();
        BudgetService.BudgetAlert alert = budgetService.checkAlert(category.getId(), currentMonth);

        if (alert.level() == BudgetService.AlertLevel.NONE || alert.status() == null) {
            return "";
        }

        BudgetService.BudgetStatus s = alert.status();

        if (alert.level() == BudgetService.AlertLevel.EXCEEDED) {
            return String.format(
                    "🔴 ¡Te pasaste! Llevas %d%% de tu presupuesto de %s (S/%.2f de S/%.2f).",
                    s.percentUsed(), s.categoryName(), s.spent(), s.limit());
        }

        // WARNING
        return String.format(
                "🟡 Cuidado: llevas %d%% de tu presupuesto de %s (S/%.2f de S/%.2f).",
                s.percentUsed(), s.categoryName(), s.spent(), s.limit());
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

    private String handleOnboarding(String message, String from) {
        Optional<AiOnboardingParser.OnboardingData> data = aiOnboardingParser.parse(message);
        if (data.isEmpty()) {
            return "🤔 Para configurar tus presupuestos dime cuánto ganas al mes y cuánto quieres ahorrar.\n" +
                    "Ej: *configura mis presupuestos, gano 2000 y quiero ahorrar 400*";
        }

        BigDecimal income = data.get().income();
        BigDecimal savings = data.get().savingsGoal();

        Optional<List<AiBudgetPlanner.ProposedBudget>> proposal =
                aiBudgetPlanner.propose(income, savings);

        if (proposal.isEmpty()) {
            return "😕 No pude generar una propuesta. Verifica que tu meta de ahorro sea menor a tu ingreso e intenta de nuevo.";
        }

        List<AiBudgetPlanner.ProposedBudget> budgets = proposal.get();

        try {
            String planJson = objectMapper.writeValueAsString(budgets);
            pendingBudgetPlanRepository.deleteByUserPhone(from);
            pendingBudgetPlanRepository.save(new PendingBudgetPlan(from, planJson));
        } catch (Exception e) {
            return "😕 Ocurrió un error al preparar la propuesta. Intenta de nuevo.";
        }

        return buildProposalMessage(income, savings, budgets);
    }

    private String buildProposalMessage(BigDecimal income, BigDecimal savings,
                                        List<AiBudgetPlanner.ProposedBudget> budgets) {
        BigDecimal total = budgets.stream()
                .map(AiBudgetPlanner.ProposedBudget::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 Con ingreso S/%.2f y meta de ahorro S/%.2f, te propongo:%n%n",
                income, savings));
        for (AiBudgetPlanner.ProposedBudget b : budgets) {
            sb.append(String.format("%s %s: S/%.2f%n", b.emoji(), b.categoryName(), b.amount()));
        }
        sb.append(String.format("%nTotal gastos: S/%.2f | Ahorro: S/%.2f%n%n", total, savings));
        sb.append("¿Los guardo? Responde *SÍ* o *NO*\n");
        sb.append("💡 Es una sugerencia orientativa, ajústala a tu realidad.");
        return sb.toString();
    }

    private String confirmBudgetPlan(PendingBudgetPlan plan) {
        try {
            List<AiBudgetPlanner.ProposedBudget> budgets = objectMapper.readValue(
                    plan.getPlanJson(),
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, AiBudgetPlanner.ProposedBudget.class));

            String currentMonth = java.time.YearMonth.now().toString();
            for (AiBudgetPlanner.ProposedBudget b : budgets) {
                budgetService.setBudget(b.categoryId(), b.amount(), currentMonth);
            }

            pendingBudgetPlanRepository.deleteByUserPhone(plan.getUserPhone());

            return "✅ ¡Listo! Guardé tus presupuestos del mes. Escribe *mis presupuestos* para verlos cuando quieras.";
        } catch (Exception e) {
            pendingBudgetPlanRepository.deleteByUserPhone(plan.getUserPhone());
            return "😕 Ocurrió un error al guardar. Intenta configurar de nuevo.";
        }
    }

    private String normalize(String message) {
        return message == null ? "" : message.trim().toLowerCase();
    }

    private String handleSetBudget(CommandDetector.BudgetCommand command) {
        Optional<Category> category =
                categoryRepository.findByName(capitalize(command.categoryName()));

        if (category.isEmpty()) {
            return "🤔 No encontré la categoría *" + command.categoryName() +
                    "*. Revisa el nombre o créala primero.";
        }

        String currentMonth = java.time.YearMonth.now().toString();
        budgetService.setBudget(category.get().getId(), command.amount(), currentMonth);

        return String.format("✅ Presupuesto de %s *%s* fijado en S/%.2f para este mes.",
                category.get().getEmoji(), category.get().getName(), command.amount());
    }

    private String buildBudgetReply() {
        String currentMonth = java.time.YearMonth.now().toString();
        List<BudgetService.BudgetStatus> statuses = budgetService.getStatusForMonth(currentMonth);

        if (statuses.isEmpty()) {
            return "📋 No tienes presupuestos este mes. Define uno así: *presupuesto Comida 500*";
        }

        StringBuilder sb = new StringBuilder("📋 Tus presupuestos de este mes:%n%n".formatted());
        for (BudgetService.BudgetStatus s : statuses) {
            String icon = s.percentUsed() >= 100 ? "🔴" : s.percentUsed() >= 80 ? "🟡" : "🟢";
            sb.append(String.format("%s %s %s: S/%.2f / S/%.2f (%d%%)%n",
                    icon, s.emoji(), s.categoryName(),
                    s.spent(), s.limit(), s.percentUsed()));
        }
        return sb.toString().trim();
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String t = text.trim();
        return t.substring(0, 1).toUpperCase() + t.substring(1).toLowerCase();
    }

    private boolean mentionsBudget(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.trim().toLowerCase();
        return normalized.contains("presupuesto");
    }

    private String handleSetBudgetFromAi(AiBudgetParser.BudgetIntent intent) {
        String currentMonth = java.time.YearMonth.now().toString();
        budgetService.setBudget(intent.categoryId(), intent.amount(), currentMonth);

        return String.format("✅ Presupuesto de %s *%s* fijado en S/%.2f para este mes.",
                intent.emoji(), intent.categoryName(), intent.amount());
    }
}