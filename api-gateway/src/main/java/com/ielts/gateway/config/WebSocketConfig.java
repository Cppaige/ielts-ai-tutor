package com.ielts.gateway.config;

import com.ielts.gateway.websocket.ScoringProgressHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ScoringProgressHandler scoringProgressHandler;

    public WebSocketConfig(ScoringProgressHandler scoringProgressHandler) {
        this.scoringProgressHandler = scoringProgressHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(scoringProgressHandler, "/ws/scoring/{submissionId}")
                .setAllowedOrigins("*");
    }
}
