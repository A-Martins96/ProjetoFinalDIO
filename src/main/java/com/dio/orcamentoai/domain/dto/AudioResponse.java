package com.dio.orcamentoai.domain.dto;

public class AudioResponse {
    private String message;

    public AudioResponse() {
    }

    public AudioResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
