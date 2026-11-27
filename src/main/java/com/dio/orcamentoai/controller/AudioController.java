package com.dio.orcamentoai.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dio.orcamentoai.domain.dto.AudioResponse;
import com.dio.orcamentoai.exception.BadRequestException;
import com.dio.orcamentoai.service.AiFinanceService;
import com.dio.orcamentoai.service.TranscriptionService;

@RestController
@RequestMapping("")
public class AudioController {

    private final AiFinanceService aiFinanceService;
    private final TranscriptionService transcriptionService;

    public AudioController(AiFinanceService aiFinanceService, TranscriptionService transcriptionService) {
        this.aiFinanceService = aiFinanceService;
        this.transcriptionService = transcriptionService;
    }

    /**
     * Recebe um áudio (mp3, m4a, wav, webm...) e transcreve antes de processar o comando.
     * Um arquivo .txt com a frase também é aceito, para testar sem chave da OpenAI.
     */
    @PostMapping("/audio")
    public ResponseEntity<AudioResponse> processAudio(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo de áudio vazio.");
        }

        try {
            byte[] conteudo = file.getBytes();
            String texto = isTexto(file)
                    ? new String(conteudo, StandardCharsets.UTF_8).replace("﻿", "").trim()
                    : transcriptionService.transcribe(conteudo, file.getOriginalFilename());

            if (texto.isBlank()) {
                throw new BadRequestException("Arquivo de áudio vazio.");
            }
            return ResponseEntity.ok(aiFinanceService.processAudioText(texto));
        } catch (IOException ex) {
            throw new BadRequestException("Não foi possível ler o arquivo enviado.");
        }
    }

    private boolean isTexto(MultipartFile file) {
        String tipo = file.getContentType();
        String nome = file.getOriginalFilename();
        return (tipo != null && tipo.startsWith("text/"))
                || (nome != null && nome.toLowerCase(Locale.ROOT).endsWith(".txt"));
    }
}
