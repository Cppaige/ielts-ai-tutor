package com.ielts.writing.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ielts.writing.dto.LrGraResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LrGraAgentTest {

    @Mock
    private ChatModel chatModel;

    private LrGraAgent agent;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        agent = new LrGraAgent(chatModel, objectMapper);
    }

    @Test
    void analyze_validResponse_parsesCorrectly() {
        String jsonResponse = """
                {"lrScore":7.0,"graScore":6.5,"grammarErrors":[{"original":"he go","correction":"he goes","explanation":"subject-verb agreement"}],"vocabularyHighlights":["sophisticated","paramount"],"summary":"Good vocabulary range with minor grammar issues."}
                """;
        var generation = new Generation(new AssistantMessage(jsonResponse));
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        LrGraResult result = agent.analyze("Some essay text here");
        assertEquals(7.0, result.lrScore().doubleValue());
        assertEquals(6.5, result.graScore().doubleValue());
        assertEquals(1, result.grammarErrors().size());
    }

    @Test
    void analyze_invalidJson_retriesAndThrows() {
        var generation = new Generation(new AssistantMessage("not valid json at all"));
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(RuntimeException.class, () -> agent.analyze("Some essay"));
    }
}
