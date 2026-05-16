# Data Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现用户注册/登录/JWT 签发、题库 CRUD、练习记录索引存储、Redis Cache-Aside 缓存。

**Architecture:** Spring Boot REST 服务，Spring Data JPA 持久化，Spring Security 做密码加密，jjwt 签发 JWT，Spring Data Redis + Lettuce 做 Cache-Aside，Spring Kafka 消费评分/报告摘要写入 practice_records。

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security, jjwt, Spring Data Redis, Spring Kafka, MySQL 8

**Depends on:** Plan 1 (Docker Compose 脚手架) 完成

---

## File Structure

```
data-service/src/main/java/com/ielts/data/
├── DataServiceApplication.java          (已存在)
├── config/
│   ├── SecurityConfig.java              (Spring Security 配置)
│   └── RedisConfig.java                 (Redis 序列化配置)
├── entity/
│   ├── User.java
│   ├── WritingTopic.java
│   ├── SpeakingTopic.java
│   └── PracticeRecord.java
├── repository/
│   ├── UserRepository.java
│   ├── WritingTopicRepository.java
│   ├── SpeakingTopicRepository.java
│   └── PracticeRecordRepository.java
├── service/
│   ├── AuthService.java
│   ├── WritingTopicService.java
│   ├── SpeakingTopicService.java
│   └── PracticeRecordService.java
├── controller/
│   ├── AuthController.java
│   ├── WritingTopicController.java
│   ├── SpeakingTopicController.java
│   └── PracticeRecordController.java
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── PracticeRecordMessage.java
├── kafka/
│   └── PracticeRecordConsumer.java
└── util/
    └── JwtUtil.java

data-service/src/test/java/com/ielts/data/
├── service/
│   ├── AuthServiceTest.java
│   └── WritingTopicServiceTest.java
├── controller/
│   └── AuthControllerTest.java
└── kafka/
    └── PracticeRecordConsumerTest.java
```

---

## Task 1: JPA Entity 层

**Files:**
- Create: `data-service/src/main/java/com/ielts/data/entity/User.java`
- Create: `data-service/src/main/java/com/ielts/data/entity/WritingTopic.java`
- Create: `data-service/src/main/java/com/ielts/data/entity/SpeakingTopic.java`
- Create: `data-service/src/main/java/com/ielts/data/entity/PracticeRecord.java`

- [ ] **Step 1: Create User.java**

```java
package com.ielts.data.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String nickname;

    @Column(name = "target_band")
    private BigDecimal targetBand;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public BigDecimal getTargetBand() { return targetBand; }
    public void setTargetBand(BigDecimal targetBand) { this.targetBand = targetBand; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 2: Create WritingTopic.java**

```java
package com.ielts.data.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "writing_topics")
public class WritingTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type", nullable = false)
    private Integer taskType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "chart_type")
    private String chartType;

    @Column(name = "chart_description", columnDefinition = "TEXT")
    private String chartDescription;

    private String category;

    private Integer difficulty;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTaskType() { return taskType; }
    public void setTaskType(Integer taskType) { this.taskType = taskType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    public String getChartDescription() { return chartDescription; }
    public void setChartDescription(String chartDescription) { this.chartDescription = chartDescription; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 3: Create SpeakingTopic.java**

```java
package com.ielts.data.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_topics")
public class SpeakingTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer part;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "cue_card", columnDefinition = "TEXT")
    private String cueCard;

    @Column(name = "follow_up_questions", columnDefinition = "JSON")
    private String followUpQuestions;

    private String category;

    private String season;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getPart() { return part; }
    public void setPart(Integer part) { this.part = part; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getCueCard() { return cueCard; }
    public void setCueCard(String cueCard) { this.cueCard = cueCard; }
    public String getFollowUpQuestions() { return followUpQuestions; }
    public void setFollowUpQuestions(String followUpQuestions) { this.followUpQuestions = followUpQuestions; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 4: Create PracticeRecord.java**

```java
package com.ielts.data.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_records")
public class PracticeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PracticeType type;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "service_record_id", nullable = false)
    private Long serviceRecordId;

    @Column(name = "overall_band")
    private BigDecimal overallBand;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PracticeType { WRITING, SPEAKING }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public PracticeType getType() { return type; }
    public void setType(PracticeType type) { this.type = type; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Long getServiceRecordId() { return serviceRecordId; }
    public void setServiceRecordId(Long serviceRecordId) { this.serviceRecordId = serviceRecordId; }
    public BigDecimal getOverallBand() { return overallBand; }
    public void setOverallBand(BigDecimal overallBand) { this.overallBand = overallBand; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl data-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add data-service/src/main/java/com/ielts/data/entity/
git commit -m "feat(data-service): add JPA entities for users, topics, and practice records"
```

---

## Task 2: Repository 层

**Files:**
- Create: `data-service/src/main/java/com/ielts/data/repository/UserRepository.java`
- Create: `data-service/src/main/java/com/ielts/data/repository/WritingTopicRepository.java`
- Create: `data-service/src/main/java/com/ielts/data/repository/SpeakingTopicRepository.java`
- Create: `data-service/src/main/java/com/ielts/data/repository/PracticeRecordRepository.java`

- [ ] **Step 1: Create UserRepository.java**

```java
package com.ielts.data.repository;

import com.ielts.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 2: Create WritingTopicRepository.java**

```java
package com.ielts.data.repository;

import com.ielts.data.entity.WritingTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WritingTopicRepository extends JpaRepository<WritingTopic, Long> {
    List<WritingTopic> findByTaskType(Integer taskType);
    List<WritingTopic> findByCategory(String category);
}
```

- [ ] **Step 3: Create SpeakingTopicRepository.java**

```java
package com.ielts.data.repository;

import com.ielts.data.entity.SpeakingTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpeakingTopicRepository extends JpaRepository<SpeakingTopic, Long> {
    List<SpeakingTopic> findByPart(Integer part);
    List<SpeakingTopic> findByCategory(String category);
    List<SpeakingTopic> findByPartAndCategory(Integer part, String category);
}
```

- [ ] **Step 4: Create PracticeRecordRepository.java**

```java
package com.ielts.data.repository;

import com.ielts.data.entity.PracticeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeRecordRepository extends JpaRepository<PracticeRecord, Long> {
    Page<PracticeRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<PracticeRecord> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, PracticeRecord.PracticeType type, Pageable pageable);
}
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl data-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add data-service/src/main/java/com/ielts/data/repository/
git commit -m "feat(data-service): add JPA repositories"
```

---

## Task 3: JWT 工具类 + Security 配置

**Files:**
- Create: `data-service/src/main/java/com/ielts/data/util/JwtUtil.java`
- Create: `data-service/src/main/java/com/ielts/data/config/SecurityConfig.java`
- Create: `data-service/src/main/java/com/ielts/data/config/RedisConfig.java`
- Test: `data-service/src/test/java/com/ielts/data/util/JwtUtilTest.java`

- [ ] **Step 1: Write failing test for JwtUtil**

```java
package com.ielts.data.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-that-is-at-least-32-bytes-long", 7200000L);
    }

    @Test
    void generateToken_containsUserId() {
        String token = jwtUtil.generateToken(1L, "test@example.com");
        assertNotNull(token);
        assertEquals(1L, jwtUtil.extractUserId(token));
    }

    @Test
    void generateToken_containsEmail() {
        String token = jwtUtil.generateToken(1L, "test@example.com");
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(1L, "test@example.com");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl data-service -Dtest=JwtUtilTest -am`
Expected: FAIL — JwtUtil class not found

- [ ] **Step 3: Implement JwtUtil.java**

```java
package com.ielts.data.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(String secret, long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl data-service -Dtest=JwtUtilTest -am`
Expected: 4 tests PASS

- [ ] **Step 5: Create SecurityConfig.java**

```java
package com.ielts.data.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 6: Create RedisConfig.java**

```java
package com.ielts.data.config;

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
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 7: Verify compilation**

Run: `mvn compile -pl data-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add data-service/src/main/java/com/ielts/data/util/ data-service/src/main/java/com/ielts/data/config/ data-service/src/test/java/com/ielts/data/util/
git commit -m "feat(data-service): add JWT util, Security config, Redis config with tests"
```

---

## Task 4: Auth Service（注册 + 登录）

**Files:**
- Create: `data-service/src/main/java/com/ielts/data/dto/RegisterRequest.java`
- Create: `data-service/src/main/java/com/ielts/data/dto/LoginRequest.java`
- Create: `data-service/src/main/java/com/ielts/data/dto/LoginResponse.java`
- Create: `data-service/src/main/java/com/ielts/data/service/AuthService.java`
- Test: `data-service/src/test/java/com/ielts/data/service/AuthServiceTest.java`

- [ ] **Step 1: Create DTO records**

```java
// RegisterRequest.java
package com.ielts.data.dto;

public record RegisterRequest(String email, String password, String nickname) {}
```

```java
// LoginRequest.java
package com.ielts.data.dto;

public record LoginRequest(String email, String password) {}
```

```java
// LoginResponse.java
package com.ielts.data.dto;

public record LoginResponse(String token, Long userId, String email) {}
```

- [ ] **Step 2: Write failing test for AuthService**

```java
package com.ielts.data.service;

import com.ielts.data.dto.LoginRequest;
import com.ielts.data.dto.LoginResponse;
import com.ielts.data.dto.RegisterRequest;
import com.ielts.data.entity.User;
import com.ielts.data.repository.UserRepository;
import com.ielts.data.util.JwtUtil;
import com.ielts.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = new JwtUtil("test-secret-key-that-is-at-least-32-bytes-long", 7200000L);
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void register_newUser_succeeds() {
        var request = new RegisterRequest("test@example.com", "password123", "Test");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        Long userId = authService.register(request);
        assertEquals(1L, userId);
    }

    @Test
    void register_duplicateEmail_throws() {
        var request = new RegisterRequest("test@example.com", "password123", "Test");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));
    }

    @Test
    void login_validCredentials_returnsToken() {
        var request = new LoginRequest("test@example.com", "password123");
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(request);
        assertNotNull(response.token());
        assertEquals(1L, response.userId());
    }

    @Test
    void login_wrongPassword_throws() {
        var request = new LoginRequest("test@example.com", "wrong");
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> authService.login(request));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -pl data-service -Dtest=AuthServiceTest -am`
Expected: FAIL — AuthService class not found

- [ ] **Step 4: Implement AuthService.java**

```java
package com.ielts.data.service;

import com.ielts.common.exception.BusinessException;
import com.ielts.data.dto.LoginRequest;
import com.ielts.data.dto.LoginResponse;
import com.ielts.data.dto.RegisterRequest;
import com.ielts.data.entity.User;
import com.ielts.data.repository.UserRepository;
import com.ielts.data.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Long register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(409, "Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user = userRepository.save(user);
        return user.getId();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(401, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(token, user.getId(), user.getEmail());
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -pl data-service -Dtest=AuthServiceTest -am`
Expected: 4 tests PASS

- [ ] **Step 6: Commit**

```bash
git add data-service/src/main/java/com/ielts/data/dto/ data-service/src/main/java/com/ielts/data/service/AuthService.java data-service/src/test/java/com/ielts/data/service/AuthServiceTest.java
git commit -m "feat(data-service): implement auth service with register and login"
```

---

## Task 5: Auth Controller

**Files:**
- Create: `data-service/src/main/java/com/ielts/data/controller/AuthController.java`
- Test: `data-service/src/test/java/com/ielts/data/controller/AuthControllerTest.java`

- [ ] **Step 1: Write failing test for AuthController**

```java
package com.ielts.data.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ielts.data.dto.LoginRequest;
import com.ielts.data.dto.LoginResponse;
import com.ielts.data.dto.RegisterRequest;
import com.ielts.data.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_validRequest_returns200() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(1L);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("test@example.com", "pass123", "Test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void login_validRequest_returnsToken() throws Exception {
        var response = new LoginResponse("jwt-token", 1L, "test@example.com");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("test@example.com", "pass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.userId").value(1));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl data-service -Dtest=AuthControllerTest -am`
Expected: FAIL — AuthController not found

- [ ] **Step 3: Implement AuthController.java**

```java
package com.ielts.data.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.data.dto.LoginRequest;
import com.ielts.data.dto.LoginResponse;
import com.ielts.data.dto.RegisterRequest;
import com.ielts.data.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl data-service -Dtest=AuthControllerTest -am`
Expected: 2 tests PASS

- [ ] **Step 5: Commit**

```bash
git add data-service/src/main/java/com/ielts/data/controller/AuthController.java data-service/src/test/java/com/ielts/data/controller/AuthControllerTest.java
git commit -m "feat(data-service): add auth controller with register and login endpoints"
```

---

## Task 6: 题库 Service + Cache-Aside

**Files:**
- Create: `data-service/src/main/java/com/ielts/data/service/WritingTopicService.java`
- Create: `data-service/src/main/java/com/ielts/data/service/SpeakingTopicService.java`
- Test: `data-service/src/test/java/com/ielts/data/service/WritingTopicServiceTest.java`

- [ ] **Step 1: Write failing test for WritingTopicService**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl data-service -Dtest=WritingTopicServiceTest -am`
Expected: FAIL — WritingTopicService not found

- [ ] **Step 3: Implement WritingTopicService.java**

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl data-service -Dtest=WritingTopicServiceTest -am`
Expected: 3 tests PASS

- [ ] **Step 5: Implement SpeakingTopicService.java**

```java
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
```

- [ ] **Step 6: Verify compilation**

Run: `mvn compile -pl data-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add data-service/src/main/java/com/ielts/data/service/WritingTopicService.java data-service/src/main/java/com/ielts/data/service/SpeakingTopicService.java data-service/src/test/java/com/ielts/data/service/WritingTopicServiceTest.java
git commit -m "feat(data-service): add topic services with Redis Cache-Aside"
```

---

## Task 7: 题库 Controller

**Files:**
- Create: `data-service/src/main/java/com/ielts/data/controller/WritingTopicController.java`
- Create: `data-service/src/main/java/com/ielts/data/controller/SpeakingTopicController.java`

- [ ] **Step 1: Create WritingTopicController.java**

```java
package com.ielts.data.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.data.entity.WritingTopic;
import com.ielts.data.service.WritingTopicService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data/writing-topics")
public class WritingTopicController {

    private final WritingTopicService service;

    public WritingTopicController(WritingTopicService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<WritingTopic>> list(@RequestParam(required = false) Integer taskType) {
        if (taskType != null) {
            return ApiResponse.success(service.listByTaskType(taskType));
        }
        return ApiResponse.success(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<WritingTopic> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
```

- [ ] **Step 2: Create SpeakingTopicController.java**

```java
package com.ielts.data.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.data.entity.SpeakingTopic;
import com.ielts.data.service.SpeakingTopicService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data/speaking-topics")
public class SpeakingTopicController {

    private final SpeakingTopicService service;

    public SpeakingTopicController(SpeakingTopicService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SpeakingTopic>> listByPart(
            @RequestParam Integer part,
            @RequestParam(required = false) String category) {
        if (category != null) {
            return ApiResponse.success(service.listByPartAndCategory(part, category));
        }
        return ApiResponse.success(service.listByPart(part));
    }

    @GetMapping("/{id}")
    public ApiResponse<SpeakingTopic> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl data-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add data-service/src/main/java/com/ielts/data/controller/WritingTopicController.java data-service/src/main/java/com/ielts/data/controller/SpeakingTopicController.java
git commit -m "feat(data-service): add topic controllers with REST endpoints"
```

---

## Task 8: Kafka Consumer（练习记录摘要）

**Files:**
- Create: `data-service/src/main/java/com/ielts/data/dto/PracticeRecordMessage.java`
- Create: `data-service/src/main/java/com/ielts/data/service/PracticeRecordService.java`
- Create: `data-service/src/main/java/com/ielts/data/kafka/PracticeRecordConsumer.java`
- Create: `data-service/src/main/java/com/ielts/data/controller/PracticeRecordController.java`
- Test: `data-service/src/test/java/com/ielts/data/kafka/PracticeRecordConsumerTest.java`

- [ ] **Step 1: Create PracticeRecordMessage.java**

```java
package com.ielts.data.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PracticeRecordMessage(
    int version,
    Long userId,
    Long topicId,
    Long serviceRecordId,
    String type,
    BigDecimal overallBand,
    Instant completedAt
) {}
```

- [ ] **Step 2: Create PracticeRecordService.java**

```java
package com.ielts.data.service;

import com.ielts.data.dto.PracticeRecordMessage;
import com.ielts.data.entity.PracticeRecord;
import com.ielts.data.repository.PracticeRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PracticeRecordService {

    private final PracticeRecordRepository repository;

    public PracticeRecordService(PracticeRecordRepository repository) {
        this.repository = repository;
    }

    public void saveFromMessage(PracticeRecordMessage message) {
        PracticeRecord record = new PracticeRecord();
        record.setUserId(message.userId());
        record.setTopicId(message.topicId());
        record.setServiceRecordId(message.serviceRecordId());
        record.setType(PracticeRecord.PracticeType.valueOf(message.type()));
        record.setOverallBand(message.overallBand());
        repository.save(record);
    }

    public Page<PracticeRecord> listByUser(Long userId, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public Page<PracticeRecord> listByUserAndType(Long userId, PracticeRecord.PracticeType type, int page, int size) {
        return repository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, PageRequest.of(page, size));
    }
}
```

- [ ] **Step 3: Write failing test for PracticeRecordConsumer**

```java
package com.ielts.data.kafka;

import com.ielts.data.dto.PracticeRecordMessage;
import com.ielts.data.service.PracticeRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PracticeRecordConsumerTest {

    @Mock
    private PracticeRecordService practiceRecordService;

    @InjectMocks
    private PracticeRecordConsumer consumer;

    @Test
    void consumeWritingResult_savesRecord() {
        var message = new PracticeRecordMessage(1, 1001L, 42L, 12345L, "WRITING", new BigDecimal("7.0"), Instant.now());
        consumer.consumeWritingResult(message);
        verify(practiceRecordService).saveFromMessage(message);
    }

    @Test
    void consumeSpeakingResult_savesRecord() {
        var message = new PracticeRecordMessage(1, 1001L, 88L, 5678L, "SPEAKING", new BigDecimal("6.5"), Instant.now());
        consumer.consumeSpeakingResult(message);
        verify(practiceRecordService).saveFromMessage(message);
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `mvn test -pl data-service -Dtest=PracticeRecordConsumerTest -am`
Expected: FAIL — PracticeRecordConsumer not found

- [ ] **Step 5: Implement PracticeRecordConsumer.java**

```java
package com.ielts.data.kafka;

import com.ielts.data.dto.PracticeRecordMessage;
import com.ielts.data.service.PracticeRecordService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PracticeRecordConsumer {

    private final PracticeRecordService practiceRecordService;

    public PracticeRecordConsumer(PracticeRecordService practiceRecordService) {
        this.practiceRecordService = practiceRecordService;
    }

    @KafkaListener(topics = "writing.scoring.result", groupId = "data-service")
    public void consumeWritingResult(PracticeRecordMessage message) {
        practiceRecordService.saveFromMessage(message);
    }

    @KafkaListener(topics = "speaking.session.result", groupId = "data-service")
    public void consumeSpeakingResult(PracticeRecordMessage message) {
        practiceRecordService.saveFromMessage(message);
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn test -pl data-service -Dtest=PracticeRecordConsumerTest -am`
Expected: 2 tests PASS

- [ ] **Step 7: Create PracticeRecordController.java**

```java
package com.ielts.data.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.data.entity.PracticeRecord;
import com.ielts.data.service.PracticeRecordService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/data/practice-records")
public class PracticeRecordController {

    private final PracticeRecordService service;

    public PracticeRecordController(PracticeRecordService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<PracticeRecord>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (type != null) {
            return ApiResponse.success(service.listByUserAndType(userId, PracticeRecord.PracticeType.valueOf(type), page, size));
        }
        return ApiResponse.success(service.listByUser(userId, page, size));
    }
}
```

- [ ] **Step 8: Verify compilation**

Run: `mvn compile -pl data-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add data-service/src/main/java/com/ielts/data/dto/PracticeRecordMessage.java data-service/src/main/java/com/ielts/data/service/PracticeRecordService.java data-service/src/main/java/com/ielts/data/kafka/ data-service/src/main/java/com/ielts/data/controller/PracticeRecordController.java data-service/src/test/java/com/ielts/data/kafka/
git commit -m "feat(data-service): add Kafka consumer for practice records and REST endpoint"
```

---

## Task 9: JwtUtil Bean 注册 + 全局异常处理

**Files:**
- Create: `data-service/src/main/java/com/ielts/data/config/JwtConfig.java`
- Create: `data-service/src/main/java/com/ielts/data/config/GlobalExceptionHandler.java`

- [ ] **Step 1: Create JwtConfig.java**

```java
package com.ielts.data.config;

import com.ielts.data.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.expiration}") long expiration) {
        return new JwtUtil(secret, expiration);
    }
}
```

- [ ] **Step 2: Create GlobalExceptionHandler.java**

```java
package com.ielts.data.config;

import com.ielts.common.dto.ApiResponse;
import com.ielts.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getCode())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "Internal server error"));
    }
}
```

- [ ] **Step 3: Verify full test suite**

Run: `mvn test -pl data-service -am`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add data-service/src/main/java/com/ielts/data/config/JwtConfig.java data-service/src/main/java/com/ielts/data/config/GlobalExceptionHandler.java
git commit -m "feat(data-service): add JWT bean config and global exception handler"
```

---

## Summary

完成后 data-service 提供：
- POST /auth/register — 用户注册
- POST /auth/login — 用户登录，返回 JWT
- GET /data/writing-topics — 写作题库列表（支持 taskType 过滤）
- GET /data/writing-topics/{id} — 单个写作题目（Cache-Aside）
- GET /data/speaking-topics — 口语题库列表（支持 part + category 过滤）
- GET /data/speaking-topics/{id} — 单个口语题目（Cache-Aside）
- GET /data/practice-records — 用户练习记录（分页）
- Kafka consumer 消费 writing.scoring.result 和 speaking.session.result
