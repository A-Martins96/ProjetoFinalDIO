package com.dio.orcamentoai.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dio.orcamentoai.domain.enums.TransactionCategory;
import com.dio.orcamentoai.domain.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "valor")
    private BigDecimal value;

    @Column(name = "descricao")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria")
    private TransactionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TransactionType type;

    @Column(name = "data_transacao")
    private LocalDate date;

    public Transaction() {
    }

    public Transaction(BigDecimal value, String description, TransactionCategory category, TransactionType type, LocalDate date) {
        this.value = value;
        this.description = description;
        this.category = category;
        this.type = type;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
