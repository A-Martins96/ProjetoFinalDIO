package com.dio.orcamentoai;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.dio.orcamentoai.service.AiFinanceService;

@SpringBootTest
class AiFinanceIntegrationTest {

    @Autowired
    private AiFinanceService aiFinanceService;

    @Test
    void shouldHandleVoiceLikeCommandWithoutExternalApi() {
        String response = aiFinanceService.processTextCommand("Registre uma despesa de 50 reais com alimentação.");
        assertThat(response).contains("Despesa").contains("50");
    }
}
