package com.ielts.speaking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

@Component
public class DataServiceClient {

    private final WebClient webClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public DataServiceClient(@Value("${data-service.base-url}") String baseUrl,
                             RedisTemplate<String, Object> redisTemplate) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.redisTemplate = redisTemplate;
    }

    public String getSpeakingTopicQuestions(Long topicId) {
        String cacheKey = "speaking:topic:" + topicId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (String) cached;
        }

        String result = webClient.get()
                .uri("/data/speaking-topics/{id}", topicId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (result != null) {
            redisTemplate.opsForValue().set(cacheKey, result, 30, TimeUnit.MINUTES);
        }
        return result;
    }
}
