package com.dio.orcamentoai.service;

import java.math.BigDecimal;

import com.dio.orcamentoai.exception.BadRequestException;

/**
 * Converte valores escritos por pessoas ou pelo modelo de IA em BigDecimal.
 *
 * Aceita o formato brasileiro e o formato com ponto decimal:
 * "50" -> 50, "50,5" -> 50.5, "50.5" -> 50.5, "1.500" -> 1500,
 * "1.500,75" -> 1500.75, "1,500.75" -> 1500.75, "R$ 35,90" -> 35.90.
 */
public final class ValorParser {

    private ValorParser() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Valor obrigatório para a transação.");
        }

        String valor = raw.trim()
                .replaceAll("(?i)r\\$", "")
                .replaceAll("\\s", "");

        String normalizado = normalizar(valor);

        BigDecimal resultado;
        try {
            resultado = new BigDecimal(normalizado);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Valor inválido: " + raw);
        }

        if (resultado.signum() <= 0) {
            throw new BadRequestException("valor deve ser maior que zero");
        }
        return resultado;
    }

    private static String normalizar(String valor) {
        boolean temPonto = valor.indexOf('.') >= 0;
        boolean temVirgula = valor.indexOf(',') >= 0;

        if (temPonto && temVirgula) {
            // O último separador é o decimal; o outro é separador de milhar.
            if (valor.lastIndexOf(',') > valor.lastIndexOf('.')) {
                return valor.replace(".", "").replace(",", ".");
            }
            return valor.replace(",", "");
        }

        if (temVirgula) {
            // Uma vírgula é decimal (padrão brasileiro); várias são milhar.
            return contar(valor, ',') == 1 ? valor.replace(",", ".") : valor.replace(",", "");
        }

        if (temPonto) {
            // "1.500" ou "1.500.000" é milhar no padrão brasileiro; "50.5" e "50.00" são decimais.
            return valor.matches("\\d{1,3}(\\.\\d{3})+") ? valor.replace(".", "") : valor;
        }

        return valor;
    }

    private static int contar(String texto, char alvo) {
        int total = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == alvo) {
                total++;
            }
        }
        return total;
    }
}
