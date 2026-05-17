package com.ielts.writing.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProgressNotifier {

    private static final String CHANNEL_PREFIX = "scoring.progress:";

    private final StringRedisTemplate redisTemplate;

    public ProgressNotifier(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void notify(Long submissionId, String status) {
        String channel = CHANNEL_PREFIX + submissionId;
        String message = "{\"submissionId\":" + submissionId + ",\"status\":\"" + status + "\"}";
        redisTemplate.convertAndSend(channel, message);
    }
}
