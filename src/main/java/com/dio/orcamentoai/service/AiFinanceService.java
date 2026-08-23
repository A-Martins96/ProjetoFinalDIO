package com.dio.orcamentoai.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.dio.orcamentoai.domain.dto.AudioResponse;
import com.dio.orcamentoai.domain.dto.TransactionResponse;
import com.dio.orcamentoai.exception.BadRequestException;

@Service
public class AiFinanceService {

    private static final Logger logger = LoggerFactory.getLogger(AiFinanceService.class);

    private final ChatModel chatModel;
    private final TransactionService transactionService;

    public AiFinanceService(ChatModel chatModel, TransactionService transactionService) {
        this.chatModel = chatModel;
        this.transactionService = transactionService;
    }

    public String processTextCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new BadRequestException("Comando inválido.");
        }

        String normalized = command.trim();
        if (chatModel == null || System.getenv("OPENAI_API_KEY") == null || System.getenv("OPENAI_API_KEY").isBlank() || "demo-key".equalsIgnoreCase(System.getenv("OPENAI_API_KEY"))) {
            return processFallbackCommand(normalized);
        }

        String systemPrompt = "Você é um assistente financeiro simples. Responda em português. " +
                "Se o usuário pedir para registrar uma transação, chame a função criarTransacao. " +
                "Se ele pedir para listar ou consultar transações, responda com o resumo das transações. " +
                "Se ele perguntar quanto gastou, calcule o total das despesas. " +
                "Use apenas as categorias válidas: ALIMENTACAO, SAUDE, MORADIA, TRANSPORTE, EDUCACAO, LAZER, SALARIO, INVESTIMENTO, OUTROS.";

        try {
            ChatResponse response = chatModel.call(new Prompt(systemPrompt + "\nComando do usuário: " + normalized));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return processFallbackCommand(normalized);
            }
            return response.getResult().getOutput().getText();
        } catch (Exception ex) {
            logger.warn("IA indisponível, usando fallback local para processar o comando.", ex);
            return processFallbackCommand(normalized);
        }
    }

    @Tool(description = "Cria uma transação financeira a partir de um texto do usuário")
    public TransactionResponse criarTransacao(String descricao, String valor, String categoria, String tipo) {
        if (descricao == null || descricao.isBlank()) {
            throw new BadRequestException("Descrição obrigatória para a transação.");
        }
        if (valor == null || valor.isBlank()) {
            throw new BadRequestException("Valor obrigatório para a transação.");
        }
        BigDecimal valorBigDecimal = new BigDecimal(valor.replace(".", "").replace(",", "."));
        return transactionService.createFromCommand(descricao, valorBigDecimal, categoria, tipo);
    }

    public AudioResponse processAudioText(String transcriptText) {
        String response = processTextCommand(transcriptText);
        return new AudioResponse(response);
    }

    private String processFallbackCommand(String command) {
        String lower = command.toLowerCase(Locale.ROOT);

        if (lower.contains("quais são minhas transações") || lower.contains("listar transacoes") || lower.contains("minhas transações") || lower.contains("transações")) {
            return "As transações disponíveis podem ser consultadas através do endpoint de listagem.";
        }

        if (lower.contains("quanto eu gastei") || lower.contains("gasto total") || lower.contains("total das despesas")) {
            return "O total de despesas pode ser calculado a partir das transações cadastradas.";
        }

        Pattern pattern = Pattern.compile("(registre|adicione|registre uma|adicione uma)\\s+(?:uma\\s+)?(receita|despesa)\\s+de\\s+R?\\$?\\s*(\\d+(?:[.,]\\d{1,2})?)\\s+(?:reais|real)?\\s*(?:com|referente\\s+ao|referente\\s+à|em|no|na)?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
            String tipo = matcher.group(2).trim().toUpperCase(Locale.ROOT);
            String valor = matcher.group(3).trim();
            String descricao = matcher.group(4).trim();
            if (descricao.isBlank()) {
                descricao = "Transação registrada";
            }
            String categoria = inferCategoryFromDescription(descricao);

            TransactionResponse saved = criarTransacao(descricao, valor, categoria, tipo);
            return String.format(Locale.forLanguageTag("pt-BR"), "%s de R$ %s registrada com sucesso.",
                    tipo.equals("DESPESA") ? "Despesa" : "Receita",
                    valor.replace('.', ',')
            );
        }

        return "Comando reconhecido, mas não foi possível identificar a ação financeira desejada.";
    }

    private String inferCategoryFromDescription(String descricao) {
        String lower = descricao.toLowerCase(Locale.ROOT);
        if (lower.contains("supermercado") || lower.contains("alimentacao") || lower.contains("almoço") || lower.contains("comida")) {
            return "ALIMENTACAO";
        }
        if (lower.contains("saúde") || lower.contains("medicamento") || lower.contains("hospital")) {
            return "SAUDE";
        }
        if (lower.contains("aluguel") || lower.contains("casa") || lower.contains("morar") || lower.contains("moradia")) {
            return "MORADIA";
        }
        if (lower.contains("transporte") || lower.contains("uber") || lower.contains("ônibus") || lower.contains("combustivel")) {
            return "TRANSPORTE";
        }
        if (lower.contains("salario") || lower.contains("pagamento") || lower.contains("remuneracao")) {
            return "SALARIO";
        }
        return "OUTROS";
    }
}
