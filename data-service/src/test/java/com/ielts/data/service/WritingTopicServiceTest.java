package com.ielts.data.service;

import com.ielts.data.entity.WritingTopic;
import com.ielts.data.repository.WritingTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WritingTopicServiceTest {

    @Mock
    private WritingTopicRepository writingTopicRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private WritingTopicService service;

    @BeforeEach
    void setUp() {
        service = new WritingTopicService(writingTopicRepository, redisTemplate);
    }

    @Test
    void getById_cacheHit_returnsFromCache() {
        WritingTopic cached = new WritingTopic();
        cached.setId(1L);
        cached.setTitle("Describe a chart");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("data:topic:writing:1")).thenReturn(cached);

        WritingTopic result = service.getById(1L);
        assertEquals("Describe a chart", result.getTitle());
        verify(writingTopicRepository, never()).findById(any());
    }

    @Test
    void getById_cacheMiss_fetchesFromDbAndCaches() {
        WritingTopic topic = new WritingTopic();
        topic.setId(1L);
        topic.setTitle("Describe a chart");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("data:topic:writing:1")).thenReturn(null);
        when(writingTopicRepository.findById(1L)).thenReturn(Optional.of(topic));

        WritingTopic result = service.getById(1L);
        assertEquals("Describe a chart", result.getTitle());
        verify(valueOperations).set("data:topic:writing:1", topic, 30, TimeUnit.MINUTES);
    }

    @Test
    void getById_notFound_throws() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("data:topic:writing:99")).thenReturn(null);
        when(writingTopicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.ielts.common.exception.BusinessException.class, () -> service.getById(99L));
    }
}
