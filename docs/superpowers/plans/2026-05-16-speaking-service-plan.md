# Speaking Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现口语对练系统 MVP（Part 1）：状态机管理、ASR/TTS 编排、LLM 考官对话、会话结束后 Kafka 入队生成报告。

**Architecture:** Redis Hash 存储会话状态，Lua 脚本原子推进。每轮对话：前端上传音频 → 阿里云 ASR → LLM 生成考官回复 → 阿里云 TTS → 返回音频 URL。会话结束后发 Kafka 生成报告，报告完成后发摘要到 data-service。

**Tech Stack:** Java 21, Spring Boot 3.x, Spring AI (DeepSeek), Spring Data Redis, Spring Kafka, 阿里云 NLS SDK, MySQL

**Depends on:** Plan 1 (Docker Compose), Plan 2 (Data Service) 完成

---

## File Structure

```
speaking-service/src/main/java/com/ielts/speaking/
├── SpeakingServiceApplication.java          (已存在)
├── config/
│   ├── KafkaTopicConfig.java                (已存在)
│   ├── RedisConfig.java
│   └── NlsClientConfig.java
├── entity/
│   ├── SpeakingSession.java
│   ├── SessionTurn.java
│   └── SpeakingReport.java
├── repository/
│   ├── SpeakingSessionRepository.java
│   ├── SessionTurnRepository.java
│   └── SpeakingReportRepository.java
├── dto/
│   ├── StartSessionRequest.java
│   ├── TurnRequest.java
│   ├── TurnResponse.java
│   ├── ReportRequestMessage.java
│   └── SessionResultMessage.java
├── statemachine/
│   ├── SessionState.java
│   └── StateMachineService.java
├── speech/
│   ├── AsrService.java
│   └── TtsService.java
├── service/
│   ├── SessionService.java
│   ├── ExaminerService.java
│   ├── ReportGenerator.java
│   └── DataServiceClient.java
├── kafka/
│   ├── ReportRequestConsumer.java
│   └── SessionResultProducer.java
└── controller/
    └── SpeakingController.java

speaking-service/src/test/java/com/ielts/speaking/
├── statemachine/
│   └── StateMachineServiceTest.java
├── service/
│   ├── SessionServiceTest.java
│   └── ExaminerServiceTest.java
└── kafka/
    └── ReportRequestConsumerTest.java
```

---

## Task 1: Entity + Repository 层

**Files:**
- Create: `speaking-service/src/main/java/com/ielts/speaking/entity/SpeakingSession.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/entity/SessionTurn.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/entity/SpeakingReport.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/repository/SpeakingSessionRepository.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/repository/SessionTurnRepository.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/repository/SpeakingReportRepository.java`

- [ ] **Step 1: Create SpeakingSession.java**

```java
package com.ielts.speaking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_sessions")
public class SpeakingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "examiner_persona")
    private ExaminerPersona examinerPersona = ExaminerPersona.ENCOURAGING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public enum ExaminerPersona { ENCOURAGING, STRICT }
    public enum SessionStatus { IN_PROGRESS, COMPLETED, ABANDONED }

    @PrePersist
    protected void onCreate() { startedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public ExaminerPersona getExaminerPersona() { return examinerPersona; }
    public void setExaminerPersona(ExaminerPersona examinerPersona) { this.examinerPersona = examinerPersona; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
```

- [ ] **Step 2: Create SessionTurn.java**

```java
package com.ielts.speaking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_turns")
public class SessionTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Integer part;

    @Column(name = "turn_order", nullable = false)
    private Integer turnOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurnRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum TurnRole { EXAMINER, CANDIDATE }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Integer getPart() { return part; }
    public void setPart(Integer part) { this.part = part; }
    public Integer getTurnOrder() { return turnOrder; }
    public void setTurnOrder(Integer turnOrder) { this.turnOrder = turnOrder; }
    public TurnRole getRole() { return role; }
    public void setRole(TurnRole role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 3: Create SpeakingReport.java**

```java
package com.ielts.speaking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_reports")
public class SpeakingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false)
    private Long sessionId;

    @Column(name = "fluency_score")
    private BigDecimal fluencyScore;

    @Column(name = "lexical_score")
    private BigDecimal lexicalScore;

    @Column(name = "grammar_score")
    private BigDecimal grammarScore;

    @Column(name = "pronunciation_score")
    private BigDecimal pronunciationScore;

    @Column(name = "overall_band")
    private BigDecimal overallBand;

    @Column(columnDefinition = "JSON")
    private String detail;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public BigDecimal getFluencyScore() { return fluencyScore; }
    public void setFluencyScore(BigDecimal fluencyScore) { this.fluencyScore = fluencyScore; }
    public BigDecimal getLexicalScore() { return lexicalScore; }
    public void setLexicalScore(BigDecimal lexicalScore) { this.lexicalScore = lexicalScore; }
    public BigDecimal getGrammarScore() { return grammarScore; }
    public void setGrammarScore(BigDecimal grammarScore) { this.grammarScore = grammarScore; }
    public BigDecimal getPronunciationScore() { return pronunciationScore; }
    public void setPronunciationScore(BigDecimal pronunciationScore) { this.pronunciationScore = pronunciationScore; }
    public BigDecimal getOverallBand() { return overallBand; }
    public void setOverallBand(BigDecimal overallBand) { this.overallBand = overallBand; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 4: Create repositories**

```java
// SpeakingSessionRepository.java
package com.ielts.speaking.repository;

import com.ielts.speaking.entity.SpeakingSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {}
```

```java
// SessionTurnRepository.java
package com.ielts.speaking.repository;

import com.ielts.speaking.entity.SessionTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionTurnRepository extends JpaRepository<SessionTurn, Long> {
    List<SessionTurn> findBySessionIdOrderByTurnOrder(Long sessionId);
}
```

```java
// SpeakingReportRepository.java
package com.ielts.speaking.repository;

import com.ielts.speaking.entity.SpeakingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpeakingReportRepository extends JpaRepository<SpeakingReport, Long> {
    Optional<SpeakingReport> findBySessionId(Long sessionId);
}
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl speaking-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add speaking-service/src/main/java/com/ielts/speaking/entity/ speaking-service/src/main/java/com/ielts/speaking/repository/
git commit -m "feat(speaking-service): add JPA entities and repositories"
```

---

## Task 2: 状态机 + Lua 脚本

**Files:**
- Create: `speaking-service/src/main/java/com/ielts/speaking/statemachine/SessionState.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/statemachine/StateMachineService.java`
- Test: `speaking-service/src/test/java/com/ielts/speaking/statemachine/StateMachineServiceTest.java`

- [ ] **Step 1: Create SessionState.java**

```java
package com.ielts.speaking.statemachine;

public enum SessionState {
    PART1_QA,
    PART2_INTRO,
    PART2_CANDIDATE_SPEAKING,
    PART3_DISCUSSION,
    SESSION_ENDED;

    public SessionState next() {
        return switch (this) {
            case PART1_QA -> PART2_INTRO;
            case PART2_INTRO -> PART2_CANDIDATE_SPEAKING;
            case PART2_CANDIDATE_SPEAKING -> PART3_DISCUSSION;
            case PART3_DISCUSSION -> SESSION_ENDED;
            case SESSION_ENDED -> SESSION_ENDED;
        };
    }
}
```

- [ ] **Step 2: Write failing test for StateMachineService**

```java
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
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(List.of(1L, "PART2_INTRO"));

        var result = service.transition(123L, SessionState.PART1_QA, SessionState.PART2_INTRO, Map.of());
        assertTrue(result.success());
        assertEquals(SessionState.PART2_INTRO, result.currentState());
    }

    @Test
    void transition_stateMismatch_fails() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(List.of(0L, "PART1_QA"));

        var result = service.transition(123L, SessionState.PART2_INTRO, SessionState.PART3_DISCUSSION, Map.of());
        assertFalse(result.success());
        assertEquals(SessionState.PART1_QA, result.currentState());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -pl speaking-service -Dtest=StateMachineServiceTest -am`
Expected: FAIL — StateMachineService not found

- [ ] **Step 4: Implement StateMachineService.java**

```java
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
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -pl speaking-service -Dtest=StateMachineServiceTest -am`
Expected: 2 tests PASS

- [ ] **Step 6: Commit**

```bash
git add speaking-service/src/main/java/com/ielts/speaking/statemachine/ speaking-service/src/test/java/com/ielts/speaking/statemachine/
git commit -m "feat(speaking-service): implement state machine with Redis Lua atomic transitions"
```

---

## Task 3: ASR/TTS Service + NlsClient Bean

**Files:**
- Create: `speaking-service/src/main/java/com/ielts/speaking/config/NlsClientConfig.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/speech/AsrService.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/speech/TtsService.java`

- [ ] **Step 1: Create NlsClientConfig.java**

```java
package com.ielts.speaking.config;

import com.alibaba.nls.client.NlsClient;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NlsClientConfig {

    private NlsClient nlsClient;

    @Bean
    public NlsClient nlsClient(@Value("${aliyun.nls.access-key-id}") String accessKeyId,
                                @Value("${aliyun.nls.access-key-secret}") String accessKeySecret) {
        nlsClient = new NlsClient(accessKeyId, accessKeySecret);
        return nlsClient;
    }

    @PreDestroy
    public void destroy() {
        if (nlsClient != null) {
            nlsClient.shutdown();
        }
    }
}
```

- [ ] **Step 2: Create AsrService.java**

```java
package com.ielts.speaking.speech;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AsrService {

    private final String appKey;

    public AsrService(@Value("${aliyun.nls.app-key}") String appKey) {
        this.appKey = appKey;
    }

    public String transcribe(byte[] audioData) {
        // MVP placeholder: integrate with Aliyun NLS SDK
        // Real implementation will use SpeechRecognizer from nls-sdk-tts
        // For now, return placeholder to allow end-to-end testing
        throw new UnsupportedOperationException("ASR integration pending - use mock in tests");
    }
}
```

- [ ] **Step 3: Create TtsService.java**

```java
package com.ielts.speaking.speech;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TtsService {

    private final String appKey;

    public TtsService(@Value("${aliyun.nls.app-key}") String appKey) {
        this.appKey = appKey;
    }

    public String synthesize(String text) {
        // MVP placeholder: integrate with Aliyun NLS SDK
        // Real implementation will use SpeechSynthesizer, save to file, return URL
        // For now, return placeholder URL
        throw new UnsupportedOperationException("TTS integration pending - use mock in tests");
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -pl speaking-service -am`
Expected: BUILD SUCCESS (NLS SDK dependency may need adding to pom.xml — if compilation fails, add placeholder interface)

- [ ] **Step 5: Commit**

```bash
git add speaking-service/src/main/java/com/ielts/speaking/config/NlsClientConfig.java speaking-service/src/main/java/com/ielts/speaking/speech/
git commit -m "feat(speaking-service): add NlsClient bean config and ASR/TTS service stubs"
```

---

## Task 4: Examiner Service（LLM 考官对话）

**Files:**
- Create: `speaking-service/src/main/java/com/ielts/speaking/service/ExaminerService.java`
- Test: `speaking-service/src/test/java/com/ielts/speaking/service/ExaminerServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.ielts.speaking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExaminerServiceTest {

    @Mock
    private ChatModel chatModel;

    private ExaminerService examinerService;

    @BeforeEach
    void setUp() {
        examinerService = new ExaminerService(chatModel);
    }

    @Test
    void generateResponse_part1_returnsExaminerReply() {
        var generation = new Generation("That's interesting. Can you tell me more about your hometown?");
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        List<ExaminerService.DialogTurn> history = List.of(
                new ExaminerService.DialogTurn("examiner", "Where are you from?"),
                new ExaminerService.DialogTurn("candidate", "I'm from Beijing.")
        );

        String response = examinerService.generateResponse("ENCOURAGING", 1, "Where are you from?", history);
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl speaking-service -Dtest=ExaminerServiceTest -am`
Expected: FAIL — ExaminerService not found

- [ ] **Step 3: Implement ExaminerService.java**

```java
package com.ielts.speaking.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExaminerService {

    private static final String ENCOURAGING_PERSONA = """
            You are a friendly and encouraging IELTS speaking examiner. You make candidates feel comfortable,
            give positive acknowledgments, and ask follow-up questions naturally. Speak in a warm, conversational tone.
            """;

    private static final String STRICT_PERSONA = """
            You are a professional and formal IELTS speaking examiner. You maintain a neutral tone,
            ask questions directly, and move through the test efficiently without excessive encouragement.
            """;

    private static final String EXAMINER_INSTRUCTIONS = """
            You are conducting an IELTS speaking test Part %d.
            Current question: %s
            
            Based on the candidate's response, provide a brief natural acknowledgment and then ask the next question.
            If this is the last question in the set, just provide a brief transition statement.
            
            Keep your response concise (1-3 sentences). Speak naturally as an examiner would.
            Only output your spoken response, no labels or formatting.
            """;

    private final ChatModel chatModel;

    public ExaminerService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generateResponse(String persona, int part, String currentQuestion, List<DialogTurn> history) {
        String personaPrompt = "ENCOURAGING".equals(persona) ? ENCOURAGING_PERSONA : STRICT_PERSONA;
        String instructions = String.format(EXAMINER_INSTRUCTIONS, part, currentQuestion);

        String historyText = history.stream()
                .map(turn -> turn.role() + ": " + turn.content())
                .collect(Collectors.joining("\n"));

        String fullPrompt = personaPrompt + "\n" + instructions + "\n\nConversation so far:\n" + historyText;

        var response = chatModel.call(new Prompt(fullPrompt));
        return response.getResult().getOutput().getText();
    }

    public record DialogTurn(String role, String content) {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl speaking-service -Dtest=ExaminerServiceTest -am`
Expected: 1 test PASS

- [ ] **Step 5: Commit**

```bash
git add speaking-service/src/main/java/com/ielts/speaking/service/ExaminerService.java speaking-service/src/test/java/com/ielts/speaking/service/
git commit -m "feat(speaking-service): implement examiner service with persona-based LLM dialogue"
```

---

## Task 5: Session Service + Controller

**Files:**
- Create: `speaking-service/src/main/java/com/ielts/speaking/dto/StartSessionRequest.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/dto/TurnRequest.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/dto/TurnResponse.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/service/SessionService.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/service/DataServiceClient.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/controller/SpeakingController.java`

- [ ] **Step 1: Create DTOs**

```java
// StartSessionRequest.java
package com.ielts.speaking.dto;

public record StartSessionRequest(Long topicId, String persona) {}
```

```java
// TurnRequest.java
package com.ielts.speaking.dto;

public record TurnRequest(byte[] audioData, String transcriptOverride) {}
```

```java
// TurnResponse.java
package com.ielts.speaking.dto;

public record TurnResponse(
    String candidateTranscript,
    String examinerResponse,
    String examinerAudioUrl,
    String currentState,
    boolean sessionEnded
) {}
```

- [ ] **Step 2: Create DataServiceClient.java**

```java
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
```

- [ ] **Step 3: Create SessionService.java**

```java
package com.ielts.speaking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ielts.speaking.dto.StartSessionRequest;
import com.ielts.speaking.dto.TurnResponse;
import com.ielts.speaking.entity.SessionTurn;
import com.ielts.speaking.entity.SpeakingSession;
import com.ielts.speaking.repository.SessionTurnRepository;
import com.ielts.speaking.repository.SpeakingSessionRepository;
import com.ielts.speaking.speech.AsrService;
import com.ielts.speaking.speech.TtsService;
import com.ielts.speaking.statemachine.SessionState;
import com.ielts.speaking.statemachine.StateMachineService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private final SpeakingSessionRepository sessionRepository;
    private final SessionTurnRepository turnRepository;
    private final StateMachineService stateMachine;
    private final ExaminerService examinerService;
    private final AsrService asrService;
    private final TtsService ttsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SessionService(SpeakingSessionRepository sessionRepository,
                          SessionTurnRepository turnRepository,
                          StateMachineService stateMachine,
                          ExaminerService examinerService,
                          AsrService asrService,
                          TtsService ttsService,
                          KafkaTemplate<String, Object> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
        this.stateMachine = stateMachine;
        this.examinerService = examinerService;
        this.asrService = asrService;
        this.ttsService = ttsService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Long startSession(Long userId, StartSessionRequest request, List<String> questions) {
        SpeakingSession session = new SpeakingSession();
        session.setUserId(userId);
        session.setTopicId(request.topicId());
        session.setExaminerPersona(
                SpeakingSession.ExaminerPersona.valueOf(request.persona() != null ? request.persona() : "ENCOURAGING"));
        session = sessionRepository.save(session);

        try {
            String questionsJson = objectMapper.writeValueAsString(questions);
            stateMachine.createSession(session.getId(), userId, request.topicId(),
                    session.getExaminerPersona().name(), questionsJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return session.getId();
    }

    public TurnResponse processTurn(Long sessionId, byte[] audioData, String transcriptOverride) {
        Map<Object, Object> sessionData = stateMachine.getSession(sessionId);
        if (sessionData.isEmpty()) {
            throw new RuntimeException("Session not found or expired");
        }

        String state = (String) sessionData.get("state");
        String persona = (String) sessionData.get("persona");
        int part1Index = Integer.parseInt((String) sessionData.get("part1Index"));
        String part1QuestionsJson = (String) sessionData.get("part1Questions");

        String candidateText = transcriptOverride != null ? transcriptOverride : asrService.transcribe(audioData);

        List<SessionTurn> existingTurns = turnRepository.findBySessionIdOrderByTurnOrder(sessionId);
        int nextOrder = existingTurns.size() + 1;

        SessionTurn candidateTurn = new SessionTurn();
        candidateTurn.setSessionId(sessionId);
        candidateTurn.setPart(1);
        candidateTurn.setTurnOrder(nextOrder);
        candidateTurn.setRole(SessionTurn.TurnRole.CANDIDATE);
        candidateTurn.setContent(candidateText);
        turnRepository.save(candidateTurn);

        List<String> questions;
        try {
            questions = objectMapper.readValue(part1QuestionsJson, List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<ExaminerService.DialogTurn> history = existingTurns.stream()
                .map(t -> new ExaminerService.DialogTurn(t.getRole().name().toLowerCase(), t.getContent()))
                .collect(Collectors.toList());
        history.add(new ExaminerService.DialogTurn("candidate", candidateText));

        String currentQuestion = part1Index < questions.size() ? questions.get(part1Index) : "";
        String examinerResponse = examinerService.generateResponse(persona, 1, currentQuestion, history);
        String audioUrl = ttsService.synthesize(examinerResponse);

        SessionTurn examinerTurn = new SessionTurn();
        examinerTurn.setSessionId(sessionId);
        examinerTurn.setPart(1);
        examinerTurn.setTurnOrder(nextOrder + 1);
        examinerTurn.setRole(SessionTurn.TurnRole.EXAMINER);
        examinerTurn.setContent(examinerResponse);
        examinerTurn.setAudioUrl(audioUrl);
        turnRepository.save(examinerTurn);

        int newIndex = part1Index + 1;
        stateMachine.incrementField(sessionId, "part1Index");
        stateMachine.incrementField(sessionId, "turnCount");

        boolean sessionEnded = false;
        if (newIndex >= questions.size()) {
            stateMachine.transition(sessionId, SessionState.PART1_QA, SessionState.SESSION_ENDED, Map.of());
            endSession(sessionId);
            sessionEnded = true;
        }

        return new TurnResponse(candidateText, examinerResponse, audioUrl, state, sessionEnded);
    }

    private void endSession(Long sessionId) {
        SpeakingSession session = sessionRepository.findById(sessionId).orElseThrow();
        session.setStatus(SpeakingSession.SessionStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);

        Map<String, Object> message = Map.of(
                "version", 1,
                "sessionId", sessionId,
                "userId", session.getUserId(),
                "topicId", session.getTopicId(),
                "requestedAt", java.time.Instant.now().toString()
        );
        kafkaTemplate.send("speaking.report.request", String.valueOf(sessionId), message);
    }
}
```

- [ ] **Step 4: Create SpeakingController.java**

```java
package com.ielts.speaking.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.speaking.dto.StartSessionRequest;
import com.ielts.speaking.dto.TurnResponse;
import com.ielts.speaking.service.SessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/speaking")
public class SpeakingController {

    private final SessionService sessionService;

    public SpeakingController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/sessions")
    public ApiResponse<Long> startSession(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody StartSessionRequest request) {
        // MVP: hardcoded questions, will be fetched from data-service in full implementation
        List<String> questions = List.of(
                "Where are you from?",
                "Do you work or study?",
                "What do you like about your hometown?",
                "How do you usually spend your weekends?"
        );
        Long sessionId = sessionService.startSession(userId, request, questions);
        return ApiResponse.success(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/turns")
    public ApiResponse<TurnResponse> processTurn(@PathVariable Long sessionId,
                                                  @RequestBody TurnRequest request) {
        TurnResponse response = sessionService.processTurn(sessionId, request.audioData(), request.transcriptOverride());
        return ApiResponse.success(response);
    }

    record TurnRequest(byte[] audioData, String transcriptOverride) {}
}
```

- [ ] **Step 5: Create RedisConfig.java**

```java
package com.ielts.speaking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 6: Verify compilation**

Run: `mvn compile -pl speaking-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add speaking-service/src/main/java/com/ielts/speaking/dto/ speaking-service/src/main/java/com/ielts/speaking/service/ speaking-service/src/main/java/com/ielts/speaking/controller/ speaking-service/src/main/java/com/ielts/speaking/config/RedisConfig.java
git commit -m "feat(speaking-service): add session service, controller, and data service client"
```

---

## Task 6: Report Generator + Kafka Consumer/Producer

**Files:**
- Create: `speaking-service/src/main/java/com/ielts/speaking/service/ReportGenerator.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/kafka/ReportRequestConsumer.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/kafka/SessionResultProducer.java`

- [ ] **Step 1: Create ReportGenerator.java**

```java
package com.ielts.speaking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ielts.speaking.entity.SessionTurn;
import com.ielts.speaking.entity.SpeakingReport;
import com.ielts.speaking.repository.SessionTurnRepository;
import com.ielts.speaking.repository.SpeakingReportRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportGenerator {

    private static final String REPORT_PROMPT = """
            You are an IELTS speaking examiner. Analyze the following speaking test transcript and provide scores.
            
            Return a JSON object with this exact structure:
            {
              "fluencyScore": <number 0-9, step 0.5>,
              "lexicalScore": <number 0-9, step 0.5>,
              "grammarScore": <number 0-9, step 0.5>,
              "pronunciationScore": <number 0-9, step 0.5>,
              "overallBand": <number, average rounded to nearest 0.5>,
              "feedback": "<detailed feedback>"
            }
            
            Transcript:
            %s
            
            Return ONLY the JSON object.
            """;

    private final ChatModel chatModel;
    private final SessionTurnRepository turnRepository;
    private final SpeakingReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    public ReportGenerator(ChatModel chatModel, SessionTurnRepository turnRepository,
                           SpeakingReportRepository reportRepository, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.turnRepository = turnRepository;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    public SpeakingReport generate(Long sessionId) {
        List<SessionTurn> turns = turnRepository.findBySessionIdOrderByTurnOrder(sessionId);
        String transcript = turns.stream()
                .map(t -> t.getRole().name() + ": " + t.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = String.format(REPORT_PROMPT, transcript);

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                var response = chatModel.call(new Prompt(prompt));
                String content = response.getResult().getOutput().getText().trim();
                if (content.startsWith("```")) {
                    content = content.replaceAll("```json?\\n?", "").replaceAll("```$", "").trim();
                }

                var node = objectMapper.readTree(content);

                SpeakingReport report = new SpeakingReport();
                report.setSessionId(sessionId);
                report.setFluencyScore(new BigDecimal(node.get("fluencyScore").asText()));
                report.setLexicalScore(new BigDecimal(node.get("lexicalScore").asText()));
                report.setGrammarScore(new BigDecimal(node.get("grammarScore").asText()));
                report.setPronunciationScore(new BigDecimal(node.get("pronunciationScore").asText()));
                report.setOverallBand(new BigDecimal(node.get("overallBand").asText()));
                report.setDetail(content);

                return reportRepository.save(report);
            } catch (Exception e) {
                if (attempt == 1) {
                    throw new RuntimeException("Report generation failed after retry", e);
                }
            }
        }
        throw new RuntimeException("Report generation failed");
    }
}
```

- [ ] **Step 2: Create SessionResultProducer.java**

```java
package com.ielts.speaking.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Component
public class SessionResultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SessionResultProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(Long sessionId, Long userId, Long topicId, Long reportId,
                     BigDecimal overallBand, BigDecimal fluencyScore, BigDecimal lexicalScore,
                     BigDecimal grammarScore, BigDecimal pronunciationScore) {
        Map<String, Object> message = Map.of(
                "version", 1,
                "sessionId", sessionId,
                "userId", userId,
                "topicId", topicId,
                "serviceRecordId", reportId,
                "type", "SPEAKING",
                "overallBand", overallBand,
                "fluencyScore", fluencyScore,
                "lexicalScore", lexicalScore,
                "grammarScore", grammarScore,
                "pronunciationScore", pronunciationScore,
                "completedAt", Instant.now().toString()
        );
        kafkaTemplate.send("speaking.session.result", String.valueOf(userId), message);
    }
}
```

- [ ] **Step 3: Create ReportRequestConsumer.java**

```java
package com.ielts.speaking.kafka;

import com.ielts.speaking.entity.SpeakingReport;
import com.ielts.speaking.entity.SpeakingSession;
import com.ielts.speaking.repository.SpeakingSessionRepository;
import com.ielts.speaking.service.ReportGenerator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReportRequestConsumer {

    private final ReportGenerator reportGenerator;
    private final SessionResultProducer resultProducer;
    private final SpeakingSessionRepository sessionRepository;

    public ReportRequestConsumer(ReportGenerator reportGenerator,
                                 SessionResultProducer resultProducer,
                                 SpeakingSessionRepository sessionRepository) {
        this.reportGenerator = reportGenerator;
        this.resultProducer = resultProducer;
        this.sessionRepository = sessionRepository;
    }

    @KafkaListener(topics = "speaking.report.request", groupId = "speaking-service")
    public void consume(Map<String, Object> message) {
        Long sessionId = ((Number) message.get("sessionId")).longValue();

        SpeakingReport report = reportGenerator.generate(sessionId);

        SpeakingSession session = sessionRepository.findById(sessionId).orElseThrow();
        resultProducer.send(sessionId, session.getUserId(), session.getTopicId(),
                report.getId(), report.getOverallBand(), report.getFluencyScore(),
                report.getLexicalScore(), report.getGrammarScore(), report.getPronunciationScore());
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -pl speaking-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add speaking-service/src/main/java/com/ielts/speaking/service/ReportGenerator.java speaking-service/src/main/java/com/ielts/speaking/kafka/
git commit -m "feat(speaking-service): add report generator and Kafka consumer/producer"
```

---

## Summary

完成后 speaking-service 提供：
- POST /speaking/sessions — 创建口语会话，初始化 Redis 状态机
- POST /speaking/sessions/{id}/turns — 处理一轮对话（ASR → LLM → TTS → 返回音频 URL）
- Redis Lua 脚本原子状态推进，防止并发错乱
- 会话结束后 Kafka 入队生成评分报告
- 报告完成后 Kafka 发布摘要到 data-service
- NlsClient 单例 Bean + @PreDestroy 优雅关闭
- MVP 范围：Part 1 完整流程，Part 2/3 留 V1
