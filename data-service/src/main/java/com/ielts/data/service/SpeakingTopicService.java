package com.ielts.data.service;

import com.ielts.common.exception.BusinessException;
import com.ielts.data.entity.SpeakingTopic;
import com.ielts.data.repository.SpeakingTopicRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SpeakingTopicService {

    private static final String CACHE_PREFIX = "data:topic:speaking:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final SpeakingTopicRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    public SpeakingTopicService(SpeakingTopicRepository repository, RedisTemplate<String, Object> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    public SpeakingTopic getById(Long id) {
        String key = CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return (SpeakingTopic) cached;
        }
        SpeakingTopic topic = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Speaking topic not found"));
        redisTemplate.opsForValue().set(key, topic, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return topic;
    }

    public List<SpeakingTopic> listByPart(Integer part) {
        return repository.findByPart(part);
    }

    public List<SpeakingTopic> listByPartAndCategory(Integer part, String category) {
        return repository.findByPartAndCategory(part, category);
    }
}
