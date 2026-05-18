package com.ielts.data.service;

import com.ielts.common.exception.BusinessException;
import com.ielts.data.entity.SpeakingTopic;
import com.ielts.data.repository.SpeakingTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SpeakingTopicService {

    private static final Logger log = LoggerFactory.getLogger(SpeakingTopicService.class); // 👈 1. 创建 log 对象
    private static final String CACHE_PREFIX = "data:topic:speaking:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final SpeakingTopicRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    public SpeakingTopicService(SpeakingTopicRepository repository, RedisTemplate<String, Object> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    public SpeakingTopic getById(Long id) {
        log.info("准备获取 SpeakingTopic，ID: {}", id); // 👈 2. 直接使用 log 对象

        String key = CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            log.debug("从 Redis 缓存中命中数据，Key: {}", key); // 👈 记录缓存命中
            return (SpeakingTopic) cached;
        }

        log.warn("Redis 缓存未命中，准备查询数据库，ID: {}", id); // 👈 记录查库动作
        SpeakingTopic topic = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("数据库中未找到对应的 SpeakingTopic，ID: {}", id); // 👈 记录错误
                    return new BusinessException(404, "Speaking topic not found");
                });
        log.info("成功获取 SpeakingTopic，ID: {}", id); // 👈 记录查库成功
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
