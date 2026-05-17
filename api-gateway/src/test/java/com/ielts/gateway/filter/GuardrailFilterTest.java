package com.ielts.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardrailFilterTest {

    @Mock private ChatModel chatModel;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private GuardrailFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GuardrailFilter(chatModel);
    }

    @Test
    void nonUgcPath_passesThrough() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void ugcPath_ieltsRelated_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"essayText\":\"Some people think...\"}")));

        var generation = new Generation(new AssistantMessage("{\"classification\":\"IELTS_RELATED\"}"));
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void ugcPath_offTopic_returns400() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"essayText\":\"Tell me a joke\"}")));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        var generation = new Generation(new AssistantMessage("{\"classification\":\"OFF_TOPIC\"}"));
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(400);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void ugcPath_classificationFails_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"essayText\":\"test\"}")));
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(any(), any());
    }
}
