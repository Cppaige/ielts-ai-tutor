package com.ielts.speaking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ielts.speaking.dto.StartSessionRequest;
import com.ielts.speaking.dto.TurnResponse;
import com.ielts.speaking.entity.SessionTurn;
import com.ielts.speaking.entity.SpeakingSession;
import com.ielts.speaking.repository.SessionTurnRepository;
import com.ielts.speaking.repository.SpeakingSessionRepository;
import com.ielts.speaking.speech.AsrService;
import com.ielts.speaking.speech.TtsService;
import com.ielts.speaking.statemachine.SessionState;
import com.ielts.speaking.statemachine.StateMachineService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private final SpeakingSessionRepository sessionRepository;
    private final SessionTurnRepository turnRepository;
    private final StateMachineService stateMachine;
    private final ExaminerService examinerService;
    private final AsrService asrService;
    private final TtsService ttsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SessionService(SpeakingSessionRepository sessionRepository,
                          SessionTurnRepository turnRepository,
                          StateMachineService stateMachine,
                          ExaminerService examinerService,
                          AsrService asrService,
                          TtsService ttsService,
                          KafkaTemplate<String, Object> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
        this.stateMachine = stateMachine;
        this.examinerService = examinerService;
        this.asrService = asrService;
        this.ttsService = ttsService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Long startSession(Long userId, StartSessionRequest request, List<String> questions) {
        SpeakingSession session = new SpeakingSession();
        session.setUserId(userId);
        session.setTopicId(request.topicId());
        session.setExaminerPersona(
                SpeakingSession.ExaminerPersona.valueOf(request.persona() != null ? request.persona() : "ENCOURAGING"));
        session = sessionRepository.save(session);

        try {
            String questionsJson = objectMapper.writeValueAsString(questions);
            stateMachine.createSession(session.getId(), userId, request.topicId(),
                    session.getExaminerPersona().name(), questionsJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return session.getId();
    }

    public TurnResponse processTurn(Long sessionId, byte[] audioData, String transcriptOverride) {
        Map<Object, Object> sessionData = stateMachine.getSession(sessionId);
        if (sessionData.isEmpty()) {
            throw new RuntimeException("Session not found or expired");
        }

        String state = (String) sessionData.get("state");
        String persona = (String) sessionData.get("persona");
        int part1Index = Integer.parseInt((String) sessionData.get("part1Index"));
        String part1QuestionsJson = (String) sessionData.get("part1Questions");

        String candidateText = transcriptOverride != null ? transcriptOverride : asrService.transcribe(audioData);

        List<SessionTurn> existingTurns = turnRepository.findBySessionIdOrderByTurnOrder(sessionId);
        int nextOrder = existingTurns.size() + 1;

        SessionTurn candidateTurn = new SessionTurn();
        candidateTurn.setSessionId(sessionId);
        candidateTurn.setPart(1);
        candidateTurn.setTurnOrder(nextOrder);
        candidateTurn.setRole(SessionTurn.TurnRole.CANDIDATE);
        candidateTurn.setContent(candidateText);
        turnRepository.save(candidateTurn);

        List<String> questions;
        try {
            questions = objectMapper.readValue(part1QuestionsJson, List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<ExaminerService.DialogTurn> history = existingTurns.stream()
                .map(t -> new ExaminerService.DialogTurn(t.getRole().name().toLowerCase(), t.getContent()))
                .collect(Collectors.toList());
        history.add(new ExaminerService.DialogTurn("candidate", candidateText));

        String currentQuestion = part1Index < questions.size() ? questions.get(part1Index) : "";
        String examinerResponse = examinerService.generateResponse(persona, 1, currentQuestion, history);
        String audioUrl = ttsService.synthesize(examinerResponse);

        SessionTurn examinerTurn = new SessionTurn();
        examinerTurn.setSessionId(sessionId);
        examinerTurn.setPart(1);
        examinerTurn.setTurnOrder(nextOrder + 1);
        examinerTurn.setRole(SessionTurn.TurnRole.EXAMINER);
        examinerTurn.setContent(examinerResponse);
        examinerTurn.setAudioUrl(audioUrl);
        turnRepository.save(examinerTurn);

        int newIndex = part1Index + 1;
        stateMachine.incrementField(sessionId, "part1Index");
        stateMachine.incrementField(sessionId, "turnCount");

        boolean sessionEnded = false;
        if (newIndex >= questions.size()) {
            stateMachine.transition(sessionId, SessionState.PART1_QA, SessionState.SESSION_ENDED, Map.of());
            endSession(sessionId);
            sessionEnded = true;
        }

        return new TurnResponse(candidateText, examinerResponse, audioUrl, state, sessionEnded);
    }

    private void endSession(Long sessionId) {
        SpeakingSession session = sessionRepository.findById(sessionId).orElseThrow();
        session.setStatus(SpeakingSession.SessionStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);

        Map<String, Object> message = Map.of(
                "version", 1,
                "sessionId", sessionId,
                "userId", session.getUserId(),
                "topicId", session.getTopicId(),
                "requestedAt", java.time.Instant.now().toString()
        );
        kafkaTemplate.send("speaking.report.request", String.valueOf(sessionId), message);
    }
}
