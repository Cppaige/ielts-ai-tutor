package com.ielts.writing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DataServiceClient {

    private static final String DASHSCOPE_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final String EMBEDDING_MODEL = "text-embedding-v2";

    private final WebClient webClient;
    private final WebClient embeddingClient;
    private final String dashscopeApiKey;
    private final ObjectMapper objectMapper;

    public DataServiceClient(@Value("${data-service.base-url}") String baseUrl,
                             @Value("${dashscope.api-key:}") String dashscopeApiKey,
                             ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.embeddingClient = WebClient.builder().baseUrl(DASHSCOPE_URL).build();
        this.dashscopeApiKey = dashscopeApiKey;
        this.objectMapper = objectMapper;
    }

    public List<Float> getEmbedding(String text) {
        if (text == null || text.isBlank() || dashscopeApiKey == null || dashscopeApiKey.isBlank()) {
            return List.of();
        }

        Map<String, Object> body = Map.of(
                "model", EMBEDDING_MODEL,
                "input", Map.of("texts", List.of(text))
        );

        try {
            String responseBody = embeddingClient.post()
                    .header("Authorization", "Bearer " + dashscopeApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseEmbedding(responseBody);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Float> parseEmbedding(String responseBody) {
        List<Float> embedding = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode embeddings = root.path("output").path("embeddings");
            if (embeddings.isArray() && !embeddings.isEmpty()) {
                JsonNode vector = embeddings.get(0).path("embedding");
                if (vector.isArray()) {
                    for (JsonNode v : vector) {
                        embedding.add(v.floatValue());
                    }
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return embedding;
    }
}
