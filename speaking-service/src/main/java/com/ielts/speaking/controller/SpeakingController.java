package com.ielts.speaking.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.speaking.dto.StartSessionRequest;
import com.ielts.speaking.dto.TurnRequest;
import com.ielts.speaking.dto.TurnResponse;
import com.ielts.speaking.service.SessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/speaking")
public class SpeakingController {

    private final SessionService sessionService;

    public SpeakingController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/sessions")
    public ApiResponse<Long> startSession(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody StartSessionRequest request) {
        // MVP: hardcoded questions, will be fetched from data-service in full implementation
        List<String> questions = List.of(
                "Where are you from?",
                "Do you work or study?",
                "What do you like about your hometown?",
                "How do you usually spend your weekends?"
        );
        Long sessionId = sessionService.startSession(userId, request, questions);
        return ApiResponse.success(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/turns")
    public ApiResponse<TurnResponse> processTurn(@PathVariable Long sessionId,
                                                  @RequestBody TurnRequest request) {
        TurnResponse response = sessionService.processTurn(sessionId, request.audioData(), request.textInput());
        return ApiResponse.success(response);
    }
}
