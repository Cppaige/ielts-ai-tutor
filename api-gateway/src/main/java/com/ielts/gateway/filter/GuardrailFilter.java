package com.ielts.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(2)
public class GuardrailFilter extends OncePerRequestFilter {

    private static final Set<String> UGC_PATHS = Set.of("/writing/submit", "/api/writing/submit");
    private static final String CLASSIFICATION_PROMPT = """
            你是一个意图分类器。判断以下用户输入是否与雅思考试相关。
            只输出 JSON: {"classification": "IELTS_RELATED"} 或 {"classification": "OFF_TOPIC"}

            用户输入:
            %s""";

    private final ChatModel chatModel;

    public GuardrailFilter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!isUgcRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String body = request.getReader().lines().collect(Collectors.joining("\n"));

        try {
            String prompt = String.format(CLASSIFICATION_PROMPT, body);
            var chatResponse = chatModel.call(new Prompt(prompt));
            String result = chatResponse.getResult().getOutput().getContent();

            if (result != null && result.contains("OFF_TOPIC")) {
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":400,\"message\":\"Content not related to IELTS\"}");
                return;
            }
        } catch (Exception e) {
            // Classification failure: fail open
        }

        HttpServletRequest wrappedRequest = new CachedBodyRequestWrapper(request, body);
        chain.doFilter(wrappedRequest, response);
    }

    private boolean isUgcRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return false;
        String uri = request.getRequestURI();
        return UGC_PATHS.stream().anyMatch(uri::startsWith);
    }

    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final String body;

        CachedBodyRequestWrapper(HttpServletRequest request, String body) {
            super(request);
            this.body = body;
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new StringReader(body));
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            byte[] bytes = body.getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            return new jakarta.servlet.ServletInputStream() {
                @Override public int read() { return bais.read(); }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(jakarta.servlet.ReadListener listener) {}
            };
        }
    }
}
