package com.dio.orcamentoai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.dio.orcamentoai.exception.BadRequestException;
import com.dio.orcamentoai.service.ValorParser;

class ValorParserTest {

    @ParameterizedTest(name = "\"{0}\" vira {1}")
    @CsvSource(delimiter = '|', value = {
            "50|50",
            "50,5|50.5",
            "50.5|50.5",
            "50.00|50.00",
            "35,90|35.90",
            "1.500|1500",
            "1.500,75|1500.75",
            "1,500.75|1500.75",
            "1.000.000|1000000",
            "R$ 35,90|35.90",
            "r$12|12"
    })
    void converteFormatosComuns(String entrada, String esperado) {
        assertThat(ValorParser.parse(entrada)).isEqualByComparingTo(new BigDecimal(esperado));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "0", "-10", "0,00"})
    void recusaValoresInvalidos(String entrada) {
        assertThatThrownBy(() -> ValorParser.parse(entrada)).isInstanceOf(BadRequestException.class);
    }
}
