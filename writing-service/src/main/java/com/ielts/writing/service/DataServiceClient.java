package com.ielts.writing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class DataServiceClient {

    private final WebClient webClient;

    public DataServiceClient(@Value("${data-service.base-url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public List<Float> getEmbedding(String text) {
        // TODO: Call Aliyun embedding API
        // For MVP, return empty list which will skip RAG
        return List.of();
    }
}
