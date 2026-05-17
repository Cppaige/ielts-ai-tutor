package com.ielts.writing.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ielts.writing.dto.MasterResult;
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
class MasterAgentTest {

    @Mock
    private ChatModel chatModel;

    private MasterAgent agent;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        agent = new MasterAgent(chatModel, objectMapper);
    }

    @Test
    void summarize_validResponse_parsesCorrectly() {
        String jsonResponse = """
                {"overallBand":7.0,"trScore":7.0,"ccScore":7.0,"lrScore":7.0,"graScore":6.5,"overallFeedback":"Well-structured essay with good vocabulary.","polishedEssay":"The polished version..."}
                """;
        var generation = new Generation(new AssistantMessage(jsonResponse));
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        MasterResult result = agent.summarize("essay", "{lrGra json}", "{trCc json}");
        assertEquals(7.0, result.overallBand().doubleValue());
        assertEquals(6.5, result.graScore().doubleValue());
        assertNotNull(result.polishedEssay());
    }

    @Test
    void summarize_invalidJson_retriesAndThrows() {
        var generation = new Generation(new AssistantMessage("invalid"));
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(RuntimeException.class, () -> agent.summarize("essay", "{}", "{}"));
    }
}
