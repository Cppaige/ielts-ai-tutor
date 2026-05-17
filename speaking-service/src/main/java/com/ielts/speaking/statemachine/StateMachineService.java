package com.ielts.speaking.statemachine;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StateMachineService {

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final long SESSION_TTL_SECONDS = 3600;

    private static final String LUA_TRANSITION = """
            local current = redis.call('HGET', KEYS[1], 'state')
            if current ~= ARGV[1] then
              return {0, current}
            end
            redis.call('HSET', KEYS[1], 'state', ARGV[2])
            for i = 3, #ARGV, 2 do
              redis.call('HSET', KEYS[1], ARGV[i], ARGV[i+1])
            end
            redis.call('EXPIRE', KEYS[1], %d)
            return {1, ARGV[2]}
            """.formatted(SESSION_TTL_SECONDS);

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> transitionScript;

    public StateMachineService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.transitionScript = new DefaultRedisScript<>(LUA_TRANSITION, List.class);
    }

    public void createSession(Long sessionId, Long userId, Long topicId, String persona, String part1Questions) {
        String key = SESSION_KEY_PREFIX + sessionId;
        Map<String, String> fields = new HashMap<>();
        fields.put("state", SessionState.PART1_QA.name());
        fields.put("userId", String.valueOf(userId));
        fields.put("topicId", String.valueOf(topicId));
        fields.put("persona", persona);
        fields.put("part1Index", "0");
        fields.put("part1Questions", part1Questions);
        fields.put("turnCount", "0");
        fields.put("createdAt", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, java.time.Duration.ofSeconds(SESSION_TTL_SECONDS));
    }

    public TransitionResult transition(Long sessionId, SessionState expectedState, SessionState newState, Map<String, String> additionalFields) {
        String key = SESSION_KEY_PREFIX + sessionId;
        List<String> args = new ArrayList<>();
        args.add(expectedState.name());
        args.add(newState.name());
        for (var entry : additionalFields.entrySet()) {
            args.add(entry.getKey());
            args.add(entry.getValue());
        }

        @SuppressWarnings("unchecked")
        List<Object> result = redisTemplate.execute(transitionScript, List.of(key), args.toArray());

        if (result == null || result.isEmpty()) {
            return new TransitionResult(false, expectedState);
        }

        long success = ((Number) result.get(0)).longValue();
        SessionState currentState = SessionState.valueOf((String) result.get(1));
        return new TransitionResult(success == 1, currentState);
    }

    public Map<Object, Object> getSession(Long sessionId) {
        return redisTemplate.opsForHash().entries(SESSION_KEY_PREFIX + sessionId);
    }

    public void incrementField(Long sessionId, String field) {
        redisTemplate.opsForHash().increment(SESSION_KEY_PREFIX + sessionId, field, 1);
    }

    public record TransitionResult(boolean success, SessionState currentState) {}
}
