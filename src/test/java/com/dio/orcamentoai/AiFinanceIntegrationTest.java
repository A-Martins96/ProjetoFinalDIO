package com.dio.orcamentoai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.dio.orcamentoai.domain.dto.TransactionResponse;
import com.dio.orcamentoai.repository.TransactionRepository;
import com.dio.orcamentoai.service.AiFinanceService;
import com.dio.orcamentoai.service.TransactionService;

/**
 * Estes testes rodam sem OPENAI_API_KEY: o comando passa pelo interpretador local.
 * Com uma chave real no ambiente eles são pulados, para não chamar a API paga.
 */
@DisabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "(?!demo-key$).+")
@SpringBootTest
@AutoConfigureMockMvc
class AiFinanceIntegrationTest {

    @Autowired
    private AiFinanceService aiFinanceService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldHandleVoiceLikeCommandWithoutExternalApi() {
        String response = aiFinanceService.processTextCommand("Registre uma despesa de 50 reais com alimentação.");
        assertThat(response).contains("Despesa").contains("50");
    }

    @Test
    void shouldKeepDecimalValueWithDot() {
        long antes = transactionRepository.count();

        aiFinanceService.processTextCommand("Registre uma despesa de 50.5 reais com almoço");

        TransactionResponse ultima = ultimaTransacao();
        assertThat(transactionRepository.count()).isEqualTo(antes + 1);
        assertThat(ultima.getValue()).isEqualByComparingTo(new BigDecimal("50.5"));
    }

    @Test
    void shouldUnderstandBrazilianThousandsAndCents() {
        String response = aiFinanceService.processTextCommand("Adicione uma receita de 1.500,75 reais referente ao salário");

        assertThat(response).contains("Receita");
        assertThat(ultimaTransacao().getValue()).isEqualByComparingTo(new BigDecimal("1500.75"));
        assertThat(ultimaTransacao().getCategory()).isEqualTo("SALARIO");
    }

    @Test
    void shouldUnderstandNaturalSentence() {
        String response = aiFinanceService.processTextCommand("Gastei 35 reais no almoço hoje.");

        assertThat(response).startsWith("Despesa");
        TransactionResponse ultima = ultimaTransacao();
        assertThat(ultima.getValue()).isEqualByComparingTo(new BigDecimal("35"));
        assertThat(ultima.getDescription()).isEqualTo("almoço");
        assertThat(ultima.getCategory()).isEqualTo("ALIMENTACAO");
    }

    @Test
    void shouldAcceptTextFileOnAudioEndpoint() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "file", "frase.txt", "text/plain",
                "Registre uma despesa de 35,90 reais com almoço".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/audio").file(arquivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        assertThat(ultimaTransacao().getValue()).isEqualByComparingTo(new BigDecimal("35.90"));
    }

    @Test
    void shouldExplainThatRealAudioNeedsApiKey() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "file", "comando.mp3", "audio/mpeg", new byte[] {1, 2, 3, 4});

        mockMvc.perform(multipart("/audio").file(audio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("OPENAI_API_KEY")));
    }

    private TransactionResponse ultimaTransacao() {
        return transactionService.list().stream()
                .max(java.util.Comparator.comparing(TransactionResponse::getId))
                .orElseThrow();
    }
}
