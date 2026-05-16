# API Gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 API Gateway：JWT 校验、路由转发、WebSocket 进度推送、Guardrail 意图分类。

**Architecture:** Spring Boot Web 服务，Filter 链做 JWT 校验，WebClient 做路由转发，Spring WebSocket 管理前端连接，Redis Pub/Sub 订阅评分进度，Spring AI + DeepSeek Flash 做意图分类。

**Tech Stack:** Java 21, Spring Boot 3.x, Spring WebSocket, Spring WebFlux (WebClient), Spring Data Redis, Spring AI, jjwt

**Depends on:** Plan 1 (Docker Compose), Plan 2 (Data Service) 完成

---

## File Structure

```
api-gateway/src/main/java/com/ielts/gateway/
├── GatewayApplication.java              (已存在)
├── config/
│   ├── WebClientConfig.java             (WebClient beans)
│   ├── WebSocketConfig.java             (WebSocket 端点注册)
│   └── RoutingProperties.java           (路由配置属性)
├── filter/
│   ├── JwtAuthFilter.java               (JWT 校验 Filter)
│   └── GuardrailFilter.java             (意图分类 Filter)
├── routing/
│   └── ProxyController.java             (路由转发)
├── websocket/
│   ├── ScoringProgressHandler.java      (WebSocket handler)
│   └── RedisSubscriberConfig.java       (Redis Pub/Sub 订阅)
└── util/
    └── JwtValidator.java                (JWT 解析，不签发)

api-gateway/src/test/java/com/ielts/gateway/
├── filter/
│   ├── JwtAuthFilterTest.java
│   └── GuardrailFilterTest.java
└── util/
    └── JwtValidatorTest.java
```

---

## Task 1: JWT Validator + 配置属性

**Files:**
- Create: `api-gateway/src/main/java/com/ielts/gateway/util/JwtValidator.java`
- Create: `api-gateway/src/main/java/com/ielts/gateway/config/RoutingProperties.java`
- Test: `api-gateway/src/test/java/com/ielts/gateway/util/JwtValidatorTest.java`

- [ ] **Step 1: Write failing test for JwtValidator**

```java
package com.ielts.gateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidatorTest {

    private JwtValidator jwtValidator;
    private SecretKey key;
    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";

    @BeforeEach
    void setUp() {
        jwtValidator = new JwtValidator(SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validate_validToken_returnsUserId() {
        String token = Jwts.builder()
                .subject("42")
                .claim("email", "test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        var result = jwtValidator.validate(token);
        assertTrue(result.isPresent());
        assertEquals(42L, result.get());
    }

    @Test
    void validate_expiredToken_returnsEmpty() {
        String token = Jwts.builder()
                .subject("42")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        var result = jwtValidator.validate(token);
        assertTrue(result.isEmpty());
    }

    @Test
    void validate_invalidToken_returnsEmpty() {
        var result = jwtValidator.validate("garbage.token.here");
        assertTrue(result.isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl api-gateway -Dtest=JwtValidatorTest -am`
Expected: FAIL — JwtValidator not found

- [ ] **Step 3: Implement JwtValidator.java**

```java
package com.ielts.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class JwtValidator {

    private final SecretKey key;

    public JwtValidator(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Long> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl api-gateway -Dtest=JwtValidatorTest -am`
Expected: 3 tests PASS

- [ ] **Step 5: Create RoutingProperties.java**

```java
package com.ielts.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "routing")
public class RoutingProperties {

    private String dataService;
    private String writingService;
    private String speakingService;

    public String getDataService() { return dataService; }
    public void setDataService(String dataService) { this.dataService = dataService; }
    public String getWritingService() { return writingService; }
    public void setWritingService(String writingService) { this.writingService = writingService; }
    public String getSpeakingService() { return speakingService; }
    public void setSpeakingService(String speakingService) { this.speakingService = speakingService; }
}
```

- [ ] **Step 6: Verify compilation**

Run: `mvn compile -pl api-gateway -am`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add api-gateway/src/main/java/com/ielts/gateway/util/ api-gateway/src/main/java/com/ielts/gateway/config/RoutingProperties.java api-gateway/src/test/java/com/ielts/gateway/util/
git commit -m "feat(gateway): add JWT validator and routing properties"
```

---

## Task 2: JWT Auth Filter

**Files:**
- Create: `api-gateway/src/main/java/com/ielts/gateway/filter/JwtAuthFilter.java`
- Test: `api-gateway/src/test/java/com/ielts/gateway/filter/JwtAuthFilterTest.java`

- [ ] **Step 1: Write failing test for JwtAuthFilter**

```java
package com.ielts.gateway.filter;

import com.ielts.gateway.util.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtValidator jwtValidator;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtValidator);
    }

    @Test
    void whitelistedPath_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void validToken_setsUserIdHeader() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtValidator.validate("valid-token")).thenReturn(Optional.of(42L));

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void missingToken_returns401() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtValidator.validate("bad-token")).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(401);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl api-gateway -Dtest=JwtAuthFilterTest -am`
Expected: FAIL — JwtAuthFilter not found

- [ ] **Step 3: Implement JwtAuthFilter.java**

```java
package com.ielts.gateway.filter;

import com.ielts.gateway.util.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@Order(1)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> WHITELIST = Set.of("/auth/login", "/auth/register");

    private final JwtValidator jwtValidator;

    public JwtAuthFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (WHITELIST.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response);
            return;
        }

        String token = authHeader.substring(7);
        Optional<Long> userId = jwtValidator.validate(token);
        if (userId.isEmpty()) {
            sendUnauthorized(response);
            return;
        }

        HttpServletRequest wrappedRequest = new UserIdHeaderWrapper(request, userId.get());
        chain.doFilter(wrappedRequest, response);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":401,\"message\":\"Unauthorized\"}");
    }

    private static class UserIdHeaderWrapper extends HttpServletRequestWrapper {
        private final Long userId;

        UserIdHeaderWrapper(HttpServletRequest request, Long userId) {
            super(request);
            this.userId = userId;
        }

        @Override
        public String getHeader(String name) {
            if ("X-User-Id".equalsIgnoreCase(name)) {
                return String.valueOf(userId);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.add("X-User-Id");
            return Collections.enumeration(names);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl api-gateway -Dtest=JwtAuthFilterTest -am`
Expected: 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add api-gateway/src/main/java/com/ielts/gateway/filter/JwtAuthFilter.java api-gateway/src/test/java/com/ielts/gateway/filter/
git commit -m "feat(gateway): add JWT authentication filter with whitelist"
```

---

## Task 3: 路由转发（Proxy Controller）

**Files:**
- Create: `api-gateway/src/main/java/com/ielts/gateway/config/WebClientConfig.java`
- Create: `api-gateway/src/main/java/com/ielts/gateway/routing/ProxyController.java`

- [ ] **Step 1: Create WebClientConfig.java**

```java
package com.ielts.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
```

- [ ] **Step 2: Create ProxyController.java**

```java
package com.ielts.gateway.routing;

import com.ielts.gateway.config.RoutingProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Enumeration;

@RestController
public class ProxyController {

    private final WebClient webClient;
    private final RoutingProperties routing;

    public ProxyController(WebClient webClient, RoutingProperties routing) {
        this.webClient = webClient;
        this.routing = routing;
    }

    @RequestMapping({"/auth/**", "/data/**"})
    public Mono<ResponseEntity<String>> proxyToDataService(HttpServletRequest request,
                                                            @RequestBody(required = false) String body) {
        return proxy(routing.getDataService(), request, body);
    }

    @RequestMapping("/writing/**")
    public Mono<ResponseEntity<String>> proxyToWritingService(HttpServletRequest request,
                                                              @RequestBody(required = false) String body) {
        return proxy(routing.getWritingService(), request, body);
    }

    @RequestMapping("/speaking/**")
    public Mono<ResponseEntity<String>> proxyToSpeakingService(HttpServletRequest request,
                                                               @RequestBody(required = false) String body) {
        return proxy(routing.getSpeakingService(), request, body);
    }

    private Mono<ResponseEntity<String>> proxy(String baseUrl, HttpServletRequest request, String body) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetUrl = baseUrl + path + (query != null ? "?" + query : "");

        WebClient.RequestBodySpec spec = webClient.method(HttpMethod.valueOf(request.getMethod()))
                .uri(targetUrl)
                .headers(headers -> copyHeaders(request, headers));

        WebClient.RequestHeadersSpec<?> requestSpec = (body != null)
                ? spec.bodyValue(body)
                : spec;

        return requestSpec.retrieve()
                .toEntity(String.class)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(502).body("{\"code\":502,\"message\":\"Service unavailable\"}")));
    }

    private void copyHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                headers.add(name, request.getHeader(name));
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl api-gateway -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add api-gateway/src/main/java/com/ielts/gateway/config/WebClientConfig.java api-gateway/src/main/java/com/ielts/gateway/routing/
git commit -m "feat(gateway): add proxy controller with WebClient routing"
```

---

## Task 4: Guardrail 意图分类 Filter

**Files:**
- Create: `api-gateway/src/main/java/com/ielts/gateway/filter/GuardrailFilter.java`
- Test: `api-gateway/src/test/java/com/ielts/gateway/filter/GuardrailFilterTest.java`

- [ ] **Step 1: Write failing test for GuardrailFilter**

```java
package com.ielts.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardrailFilterTest {

    @Mock private ChatModel chatModel;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private GuardrailFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GuardrailFilter(chatModel);
    }

    @Test
    void nonUgcPath_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/data/writing-topics");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void ugcPath_ieltsRelated_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"essayText\":\"Some people think...\"}")));

        var generation = new Generation("{\"classification\":\"IELTS_RELATED\"}");
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void ugcPath_offTopic_returns400() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"essayText\":\"Tell me a joke\"}")));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        var generation = new Generation("{\"classification\":\"OFF_TOPIC\"}");
        var chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(400);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void ugcPath_classificationFails_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"essayText\":\"test\"}")));
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(any(), any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl api-gateway -Dtest=GuardrailFilterTest -am`
Expected: FAIL — GuardrailFilter not found

- [ ] **Step 3: Implement GuardrailFilter.java**

```java
package com.ielts.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(2)
public class GuardrailFilter extends OncePerRequestFilter {

    private static final Set<String> UGC_PATHS = Set.of("/writing/submit", "/speaking/sessions");
    private static final String CLASSIFICATION_PROMPT = """
            你是一个意图分类器。判断以下用户输入是否与雅思考试相关。
            只输出 JSON: {"classification": "IELTS_RELATED"} 或 {"classification": "OFF_TOPIC"}
            
            用户输入:
            %s""";

    private final ChatModel chatModel;

    public GuardrailFilter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!isUgcRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String body = request.getReader().lines().collect(Collectors.joining("\n"));

        try {
            String prompt = String.format(CLASSIFICATION_PROMPT, body);
            var chatResponse = chatModel.call(new Prompt(prompt));
            String result = chatResponse.getResult().getOutput().getText();

            if (result != null && result.contains("OFF_TOPIC")) {
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":400,\"message\":\"Content not related to IELTS\"}");
                return;
            }
        } catch (Exception e) {
            // Classification failure: fail open
        }

        HttpServletRequest wrappedRequest = new CachedBodyRequestWrapper(request, body);
        chain.doFilter(wrappedRequest, response);
    }

    private boolean isUgcRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return false;
        String uri = request.getRequestURI();
        return UGC_PATHS.stream().anyMatch(uri::startsWith);
    }

    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final String body;

        CachedBodyRequestWrapper(HttpServletRequest request, String body) {
            super(request);
            this.body = body;
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new StringReader(body));
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            byte[] bytes = body.getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            return new jakarta.servlet.ServletInputStream() {
                @Override public int read() { return bais.read(); }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(jakarta.servlet.ReadListener listener) {}
            };
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl api-gateway -Dtest=GuardrailFilterTest -am`
Expected: 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add api-gateway/src/main/java/com/ielts/gateway/filter/GuardrailFilter.java api-gateway/src/test/java/com/ielts/gateway/filter/GuardrailFilterTest.java
git commit -m "feat(gateway): add Guardrail intent classification filter"
```

---

## Task 5: WebSocket 进度推送

**Files:**
- Create: `api-gateway/src/main/java/com/ielts/gateway/config/WebSocketConfig.java`
- Create: `api-gateway/src/main/java/com/ielts/gateway/websocket/ScoringProgressHandler.java`
- Create: `api-gateway/src/main/java/com/ielts/gateway/websocket/RedisSubscriberConfig.java`

- [ ] **Step 1: Create WebSocketConfig.java**

```java
package com.ielts.gateway.config;

import com.ielts.gateway.websocket.ScoringProgressHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ScoringProgressHandler scoringProgressHandler;

    public WebSocketConfig(ScoringProgressHandler scoringProgressHandler) {
        this.scoringProgressHandler = scoringProgressHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(scoringProgressHandler, "/ws/scoring/{submissionId}")
                .setAllowedOrigins("*");
    }
}
```

- [ ] **Step 2: Create ScoringProgressHandler.java**

```java
package com.ielts.gateway.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScoringProgressHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String submissionId = extractSubmissionId(session);
        if (submissionId != null) {
            sessions.put(submissionId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String submissionId = extractSubmissionId(session);
        if (submissionId != null) {
            sessions.remove(submissionId);
        }
    }

    public void sendProgress(String submissionId, String message) {
        WebSocketSession session = sessions.get(submissionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                sessions.remove(submissionId);
            }
        }
    }

    private String extractSubmissionId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}
```

- [ ] **Step 3: Create RedisSubscriberConfig.java**

```java
package com.ielts.gateway.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisSubscriberConfig {

    private final ScoringProgressHandler scoringProgressHandler;

    public RedisSubscriberConfig(ScoringProgressHandler scoringProgressHandler) {
        this.scoringProgressHandler = scoringProgressHandler;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter(), new PatternTopic("scoring.progress:*"));
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter((org.springframework.data.redis.connection.MessageListener)
                (message, pattern) -> {
                    String channel = new String(message.getChannel());
                    String submissionId = channel.replace("scoring.progress:", "");
                    String body = new String(message.getBody());
                    scoringProgressHandler.sendProgress(submissionId, body);
                });
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -pl api-gateway -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add api-gateway/src/main/java/com/ielts/gateway/config/WebSocketConfig.java api-gateway/src/main/java/com/ielts/gateway/websocket/
git commit -m "feat(gateway): add WebSocket scoring progress with Redis Pub/Sub"
```

---

## Summary

完成后 api-gateway 提供：
- JWT 校验 Filter（白名单 /auth/** 放行）
- 路由转发到 data-service、writing-service、speaking-service
- Guardrail 意图分类（仅 UGC 请求，deepseek-v4-flash，失败放行）
- WebSocket /ws/scoring/{submissionId} 端点，订阅 Redis Pub/Sub 推送评分进度
