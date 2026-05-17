package com.ielts.speaking.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateMachineServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private StateMachineService service;

    @BeforeEach
    void setUp() {
        service = new StateMachineService(redisTemplate);
    }

    @Test
    void transition_validState_succeeds() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(List.of(1L, "PART2_INTRO"));

        var result = service.transition(123L, SessionState.PART1_QA, SessionState.PART2_INTRO, Map.of());
        assertTrue(result.success());
        assertEquals(SessionState.PART2_INTRO, result.currentState());
    }

    @Test
    void transition_stateMismatch_fails() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(List.of(0L, "PART1_QA"));

        var result = service.transition(123L, SessionState.PART2_INTRO, SessionState.PART3_DISCUSSION, Map.of());
        assertFalse(result.success());
        assertEquals(SessionState.PART1_QA, result.currentState());
    }
}
