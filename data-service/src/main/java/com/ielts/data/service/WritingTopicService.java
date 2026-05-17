package com.ielts.data.service;

import com.ielts.common.exception.BusinessException;
import com.ielts.data.entity.WritingTopic;
import com.ielts.data.repository.WritingTopicRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class WritingTopicService {

    private static final String CACHE_PREFIX = "data:topic:writing:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final WritingTopicRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    public WritingTopicService(WritingTopicRepository repository, RedisTemplate<String, Object> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    public WritingTopic getById(Long id) {
        String key = CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return (WritingTopic) cached;
        }
        WritingTopic topic = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Writing topic not found"));
        redisTemplate.opsForValue().set(key, topic, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return topic;
    }

    public List<WritingTopic> listByTaskType(Integer taskType) {
        return repository.findByTaskType(taskType);
    }

    public List<WritingTopic> listAll() {
        return repository.findAll();
    }
}
