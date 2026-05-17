package com.ielts.writing.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class QdrantClient {

    private final WebClient webClient;
    private final String collection;
    private final ObjectMapper objectMapper;

    public QdrantClient(@Value("${qdrant.host}") String host,
                        @Value("${qdrant.port}") int port,
                        @Value("${qdrant.collection}") String collection,
                        ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl("http://" + host + ":" + port)
                .build();
        this.collection = collection;
        this.objectMapper = objectMapper;
    }

    public List<SearchResult> search(List<Float> queryVector, int taskType, String category, int topK) {
        Map<String, Object> filter = Map.of(
                "must", List.of(
                        Map.of("key", "task_type", "match", Map.of("value", taskType)),
                        Map.of("key", "category", "match", Map.of("value", category))
                )
        );

        Map<String, Object> body = Map.of(
                "vector", queryVector,
                "filter", filter,
                "limit", topK,
                "score_threshold", 0.7,
                "with_payload", true
        );

        try {
            String responseBody = webClient.post()
                    .uri("/collections/{collection}/points/search", collection)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResults(responseBody);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SearchResult> parseResults(String responseBody) {
        List<SearchResult> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resultArray = root.get("result");
            if (resultArray != null && resultArray.isArray()) {
                for (JsonNode node : resultArray) {
                    JsonNode payload = node.get("payload");
                    results.add(new SearchResult(
                            payload.get("exemplar_id").asLong(),
                            payload.get("excerpt").asText(),
                            node.get("score").floatValue()
                    ));
                }
            }
        } catch (Exception e) {
            // Return empty on parse failure
        }
        return results;
    }

    public record SearchResult(Long exemplarId, String excerpt, float score) {}
}
