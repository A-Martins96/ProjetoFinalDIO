package com.dio.orcamentoai.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dio.orcamentoai.domain.dto.AudioResponse;
import com.dio.orcamentoai.exception.BadRequestException;
import com.dio.orcamentoai.service.AiFinanceService;

@RestController
@RequestMapping("")
public class AudioController {

    private final AiFinanceService aiFinanceService;

    public AudioController(AiFinanceService aiFinanceService) {
        this.aiFinanceService = aiFinanceService;
    }

    @PostMapping("/audio")
    public ResponseEntity<AudioResponse> processAudio(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo de áudio vazio.");
        }

        try {
            String transcript = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            if (transcript.isBlank()) {
                throw new BadRequestException("Arquivo de áudio vazio.");
            }
            return ResponseEntity.ok(aiFinanceService.processAudioText(transcript));
        } catch (IOException ex) {
            throw new BadRequestException("Erro na transcrição do áudio.");
        }
    }
}
