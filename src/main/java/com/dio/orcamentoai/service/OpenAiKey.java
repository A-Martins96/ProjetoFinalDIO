package com.dio.orcamentoai.service;

/**
 * Diz se há uma chave real da OpenAI no ambiente.
 * Sem ela, a API usa o interpretador local e não chama serviços externos.
 */
final class OpenAiKey {

    private OpenAiKey() {
    }

    static boolean isConfigured() {
        String key = System.getenv("OPENAI_API_KEY");
        return key != null && !key.isBlank() && !"demo-key".equalsIgnoreCase(key.trim());
    }
}
