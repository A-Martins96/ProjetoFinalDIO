package com.dio.orcamentoai.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.dio.orcamentoai.domain.dto.AudioResponse;
import com.dio.orcamentoai.domain.dto.TransactionResponse;
import com.dio.orcamentoai.exception.BadRequestException;

@Service
public class AiFinanceService {

    private static final Logger logger = LoggerFactory.getLogger(AiFinanceService.class);

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private static final String SYSTEM_PROMPT = """
            Você é um assistente financeiro simples. Responda em português, em uma frase curta.
            Se o usuário pedir para registrar uma receita ou despesa, chame a ferramenta criarTransacao.
            Não invente valores: use exatamente o valor que o usuário falou.
            Categorias válidas: ALIMENTACAO, SAUDE, MORADIA, TRANSPORTE, EDUCACAO, LAZER, SALARIO, INVESTIMENTO, OUTROS.
            Tipos válidos: RECEITA ou DESPESA.
            Se não for um pedido financeiro, diga que você só registra receitas e despesas.
            """;

    // Valor: "1.500,75", "1.500", "50,5", "50.5" ou "50".
    private static final Pattern COMANDO = Pattern.compile(
            "(registre|adicione)\\s+(?:uma\\s+)?(receita|despesa)\\s+de\\s+(?:R\\$\\s*)?"
                    + "(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)"
                    + "\\s*(?:reais|real)?\\s*(?:(?:com|referente\\s+ao|referente\\s+à|em|no|na)\\b)?\\s*(.*)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    // Frases naturais: "gastei 35 reais no almoço", "paguei R$ 120,50 de luz", "recebi 2.000 de salário".
    private static final Pattern FRASE_NATURAL = Pattern.compile(
            "(gastei|paguei|recebi|ganhei)\\s+(?:R\\$\\s*)?"
                    + "(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)"
                    + "\\s*(?:reais|real)?\\s*(?:(?:com|de|em|no|na|do|da)\\b)?\\s*(.*)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private final ChatClient chatClient;
    private final TransactionService transactionService;

    public AiFinanceService(ObjectProvider<ChatModel> chatModelProvider, TransactionService transactionService) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        this.chatClient = chatModel != null ? ChatClient.builder(chatModel).build() : null;
        this.transactionService = transactionService;
    }

    public String processTextCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new BadRequestException("Comando inválido.");
        }

        String normalized = command.trim();
        if (chatClient == null || !OpenAiKey.isConfigured()) {
            return processFallbackCommand(normalized);
        }

        try {
            // A ferramenta criarTransacao é enviada ao modelo; quem valida e grava é o TransactionService.
            String resposta = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(normalized)
                    .tools(this)
                    .call()
                    .content();
            if (resposta == null || resposta.isBlank()) {
                return processFallbackCommand(normalized);
            }
            return resposta;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("IA indisponível, usando o interpretador local para processar o comando.", ex);
            return processFallbackCommand(normalized);
        }
    }

    @Tool(description = "Registra uma receita ou despesa do usuário no banco de dados")
    public TransactionResponse criarTransacao(
            @ToolParam(description = "Descrição curta da transação, por exemplo 'almoço' ou 'aluguel'") String descricao,
            @ToolParam(description = "Valor em reais exatamente como o usuário falou, por exemplo '50', '35,90' ou '1.500'") String valor,
            @ToolParam(description = "Categoria: ALIMENTACAO, SAUDE, MORADIA, TRANSPORTE, EDUCACAO, LAZER, SALARIO, INVESTIMENTO ou OUTROS") String categoria,
            @ToolParam(description = "Tipo: RECEITA ou DESPESA") String tipo) {
        if (descricao == null || descricao.isBlank()) {
            throw new BadRequestException("Descrição obrigatória para a transação.");
        }
        BigDecimal valorConvertido = ValorParser.parse(valor);
        return transactionService.createFromCommand(descricao, valorConvertido, categoria, tipo);
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

        Matcher comando = COMANDO.matcher(command);
        if (comando.find()) {
            String tipo = comando.group(2).trim().toUpperCase(Locale.ROOT);
            return registrar(tipo, comando.group(3), comando.group(4));
        }

        Matcher frase = FRASE_NATURAL.matcher(command);
        if (frase.find()) {
            String verbo = frase.group(1).toLowerCase(Locale.ROOT);
            String tipo = verbo.equals("recebi") || verbo.equals("ganhei") ? "RECEITA" : "DESPESA";
            return registrar(tipo, frase.group(2), frase.group(3));
        }

        return "Comando reconhecido, mas não foi possível identificar a ação financeira desejada.";
    }

    private String registrar(String tipo, String valor, String descricaoBruta) {
        String descricao = descricaoBruta.trim()
                .replaceAll("\\s+hoje$", "")
                .replaceAll("[.!]+$", "")
                .replaceAll("\\s+hoje$", "")
                .trim();
        if (descricao.isBlank()) {
            descricao = "Transação registrada";
        }
        String categoria = inferCategoryFromDescription(descricao);

        TransactionResponse saved = criarTransacao(descricao, valor.trim(), categoria, tipo);
        return String.format("%s de %s registrada com sucesso.",
                "DESPESA".equals(tipo) ? "Despesa" : "Receita",
                NumberFormat.getCurrencyInstance(PT_BR).format(saved.getValue()));
    }

    private String inferCategoryFromDescription(String descricao) {
        String lower = descricao.toLowerCase(Locale.ROOT);
        if (lower.contains("supermercado") || lower.contains("alimentação") || lower.contains("alimentacao") || lower.contains("almoço") || lower.contains("comida")) {
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
        if (lower.contains("salario") || lower.contains("salário") || lower.contains("pagamento") || lower.contains("remuneracao")) {
            return "SALARIO";
        }
        return "OUTROS";
    }
}
