package com.dio.orcamentoai.service;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.dio.orcamentoai.exception.BadRequestException;

/**
 * Transforma um arquivo de áudio em texto usando o modelo de transcrição da OpenAI (Whisper).
 */
@Service
public class TranscriptionService {

    private final ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider;

    public TranscriptionService(ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider) {
        this.transcriptionModelProvider = transcriptionModelProvider;
    }

    public String transcribe(byte[] audio, String filename) {
        if (!OpenAiKey.isConfigured()) {
            throw new BadRequestException(
                    "A transcrição de áudio precisa da variável OPENAI_API_KEY. Para testar sem chave, envie um arquivo .txt com a frase.");
        }

        OpenAiAudioTranscriptionModel model = transcriptionModelProvider.getIfAvailable();
        if (model == null) {
            throw new BadRequestException("Modelo de transcrição não configurado.");
        }

        // A OpenAI identifica o formato (mp3, m4a, wav, webm...) pela extensão do arquivo.
        String nome = (filename == null || filename.isBlank()) ? "audio.mp3" : filename;
        Resource recurso = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return nome;
            }
        };

        AudioTranscriptionResponse response = model.call(new AudioTranscriptionPrompt(recurso));
        String texto = response != null && response.getResult() != null ? response.getResult().getOutput() : null;
        if (texto == null || texto.isBlank()) {
            throw new BadRequestException("Não foi possível entender o áudio.");
        }
        return texto.trim();
    }
}
