package com.ielts.speaking.dto;

public record TurnResponse(
    String candidateTranscript,
    String examinerResponse,
    String examinerAudioUrl,
    String currentState,
    boolean sessionEnded
) {}
