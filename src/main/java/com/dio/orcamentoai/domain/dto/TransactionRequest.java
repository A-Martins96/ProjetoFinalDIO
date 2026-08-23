package com.dio.orcamentoai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {

    @NotNull(message = "valor é obrigatório")
    @Positive(message = "valor deve ser maior que zero")
    private BigDecimal value;

    @NotBlank(message = "descrição é obrigatória")
    private String description;

    @NotBlank(message = "categoria é obrigatória")
    private String category;

    @NotBlank(message = "tipo é obrigatório")
    private String type;

    private LocalDate date;

    public TransactionRequest() {
    }

    public TransactionRequest(BigDecimal value, String description, String category, String type, LocalDate date) {
        this.value = value;
        this.description = description;
        this.category = category;
        this.type = type;
        this.date = date;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
