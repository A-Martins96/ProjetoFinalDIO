package com.dio.orcamentoai;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dio.orcamentoai.domain.dto.TransactionRequest;
import com.dio.orcamentoai.domain.dto.TransactionResponse;
import com.dio.orcamentoai.service.TransactionService;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionService transactionService;

    @Test
    void shouldCreateTransaction() {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("50.00"),
                "Supermercado",
                "ALIMENTACAO",
                "DESPESA",
                null
        );

        TransactionResponse response = transactionService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getDescription()).isEqualTo("Supermercado");
    }

    @Test
    void shouldListTransactions() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":50.00,\"description\":\"Mercado\",\"category\":\"ALIMENTACAO\",\"type\":\"DESPESA\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldProcessMainCommand() {
        String response = transactionService.createFromCommand(
                "Supermercado",
                new BigDecimal("50.00"),
                "ALIMENTACAO",
                "DESPESA"
        ).getDescription();

        assertThat(response).isEqualTo("Supermercado");
    }
}
