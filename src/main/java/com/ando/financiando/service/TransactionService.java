package com.ando.financiando.service;

import com.ando.financiando.dto.CreateTransactionRequest;
import com.ando.financiando.dto.TransactionResponse;
import com.ando.financiando.exception.NotFoundException;
import com.ando.financiando.model.Category;
import com.ando.financiando.model.Transaction;
import com.ando.financiando.model.TransactionSource;
import com.ando.financiando.model.TransactionType;
import com.ando.financiando.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    public TransactionResponse create(CreateTransactionRequest request) {
        Category category = categoryService.getEntityById(request.categoryId());

        Transaction transaction = new Transaction(
                request.amount(),
                request.description(),
                category,
                request.occurredAt(),
                TransactionSource.DASHBOARD,
                request.type() != null ? request.type() : TransactionType.EXPENSE
        );

        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    public List<TransactionResponse> findAll() {
        return transactionRepository.findAllWithCategory()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TransactionResponse findById(Long id) {
        Transaction transaction = getEntityById(id);
        return toResponse(transaction);
    }

    public TransactionResponse update(Long id, CreateTransactionRequest request) {
        Transaction transaction = getEntityById(id);
        Category category = categoryService.getEntityById(request.categoryId());

        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setCategory(category);
        transaction.setOccurredAt(request.occurredAt());

        transactionRepository.save(transaction);

        Transaction reloaded = getEntityById(id);
        return toResponse(reloaded);
    }

    public void delete(Long id) {
        Transaction transaction = getEntityById(id);
        transactionRepository.delete(transaction);
    }

    private Transaction getEntityById(Long id) {
        return transactionRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new NotFoundException("Transacción no encontrada: " + id));
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getDescription(),
                t.getCategory().getId(),
                t.getCategory().getName(),
                t.getOccurredAt(),
                t.getSource(),
                t.getType(),
                t.getCreatedAt()
        );
    }
}