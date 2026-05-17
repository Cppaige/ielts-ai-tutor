package com.ielts.speaking.service;

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
class ExaminerServiceTest {

    @Mock
    private ChatModel chatModel;

    private ExaminerService examinerService;

    @BeforeEach
    void setUp() {
        examinerService = new ExaminerService(chatModel);
    }

    @Test
    void generateResponse_part1_returnsExaminerReply() {
        var generation = new Generation(new AssistantMessage("That's interesting. Can you tell me more about your hometown?"));
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        List<ExaminerService.DialogTurn> history = List.of(
                new ExaminerService.DialogTurn("examiner", "Where are you from?"),
                new ExaminerService.DialogTurn("candidate", "I'm from Beijing.")
        );

        String response = examinerService.generateResponse("ENCOURAGING", 1, "Where are you from?", history);
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }
}
