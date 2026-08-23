package com.dio.orcamentoai.service;

import com.dio.orcamentoai.domain.dto.TransactionRequest;
import com.dio.orcamentoai.domain.dto.TransactionResponse;
import com.dio.orcamentoai.domain.entity.Transaction;
import com.dio.orcamentoai.domain.enums.TransactionCategory;
import com.dio.orcamentoai.domain.enums.TransactionType;
import com.dio.orcamentoai.exception.BadRequestException;
import com.dio.orcamentoai.exception.ResourceNotFoundException;
import com.dio.orcamentoai.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse create(TransactionRequest request) {
        validateRequest(request);

        Transaction transaction = new Transaction();
        transaction.setValue(request.getValue());
        transaction.setDescription(request.getDescription().trim());
        transaction.setCategory(parseCategory(request.getCategory()));
        transaction.setType(parseType(request.getType()));
        transaction.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());

        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    public List<TransactionResponse> list() {
        return transactionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada."));
        return toResponse(transaction);
    }

    public TransactionResponse createFromCommand(String description, BigDecimal value, String category, String type) {
        TransactionRequest request = new TransactionRequest();
        request.setDescription(description);
        request.setValue(value);
        request.setCategory(category);
        request.setType(type);
        request.setDate(LocalDate.now());
        return create(request);
    }

    private void validateRequest(TransactionRequest request) {
        if (request == null) {
            throw new BadRequestException("Requisição inválida.");
        }
        if (request.getValue() == null) {
            throw new BadRequestException("valor é obrigatório");
        }
        if (request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("valor deve ser maior que zero");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new BadRequestException("descrição é obrigatória");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new BadRequestException("categoria é obrigatória");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new BadRequestException("tipo é obrigatório");
        }
        parseCategory(request.getCategory());
        parseType(request.getType());
    }

    private TransactionCategory parseCategory(String value) {
        try {
            return TransactionCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("categoria inválida. Use uma das opções: ALIMENTACAO, SAUDE, MORADIA, TRANSPORTE, EDUCACAO, LAZER, SALARIO, INVESTIMENTO, OUTROS");
        }
    }

    private TransactionType parseType(String value) {
        try {
            return TransactionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("tipo inválido. Use RECEITA ou DESPESA");
        }
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getValue(),
                transaction.getDescription(),
                transaction.getCategory() != null ? transaction.getCategory().name() : null,
                transaction.getType() != null ? transaction.getType().name() : null,
                transaction.getDate()
        );
    }
}
