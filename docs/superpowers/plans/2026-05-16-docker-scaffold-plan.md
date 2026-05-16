# Docker Compose 脚手架 + Maven 多模块项目骨架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建完整的 Maven 多模块项目结构 + Docker Compose 编排，使所有基础设施和服务容器可一键启动。

**Architecture:** Maven parent POM 管理四个子模块（api-gateway、data-service、writing-service、speaking-service）+ 一个 common 模块。Docker Compose 编排 MySQL、Redis、Kafka（KRaft）、Qdrant 基础设施容器及四个业务服务容器。

**Tech Stack:** Java 21, Spring Boot 3.x, Maven, Docker, Docker Compose, MySQL 8, Redis 7, Kafka 3.x (KRaft), Qdrant

---

## File Structure

```
IELTS/
├── pom.xml                          (parent POM)
├── docker-compose.yml               (全量编排)
├── .env.example                     (环境变量模板)
├── .gitignore
├── init-sql/
│   └── init.sql                     (MySQL 建库建表)
├── ielts-common/
│   ├── pom.xml
│   └── src/main/java/com/ielts/common/
│       ├── dto/                     (共享 DTO)
│       └── exception/               (共享异常)
├── api-gateway/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/ielts/gateway/
│       │   └── GatewayApplication.java
│       └── resources/application.yml
├── data-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/ielts/data/
│       │   └── DataServiceApplication.java
│       └── resources/application.yml
├── writing-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/ielts/writing/
│       │   └── WritingServiceApplication.java
│       └── resources/application.yml
└── speaking-service/
    ├── pom.xml
    ├── Dockerfile
    └── src/main/
        ├── java/com/ielts/speaking/
        │   └── SpeakingServiceApplication.java
        └── resources/application.yml
```

---

## Task 1: Parent POM + .gitignore

**Files:**
- Create: `pom.xml`
- Create: `.gitignore`

- [ ] **Step 1: Create parent pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
        <relativePath/>
    </parent>

    <groupId>com.ielts</groupId>
    <artifactId>ielts-platform</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>IELTS AI Platform</name>

    <modules>
        <module>ielts-common</module>
        <module>api-gateway</module>
        <module>data-service</module>
        <module>writing-service</module>
        <module>speaking-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-ai.version>1.0.0-M4</spring-ai.version>
        <jjwt.version>0.12.6</jjwt.version>
        <testcontainers.version>1.20.4</testcontainers.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.ielts</groupId>
                <artifactId>ielts-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <url>https://repo.spring.io/milestone</url>
        </repository>
    </repositories>
</project>
```

- [ ] **Step 2: Create .gitignore**

```
target/
!.mvn/wrapper/maven-wrapper.jar
*.iml
.idea/
*.class
*.jar
*.war
*.log
.env
.DS_Store
```

- [ ] **Step 3: Verify Maven structure parses**

Run: `mvn validate -N`
Expected: BUILD SUCCESS (parent-only validation, modules don't exist yet)

- [ ] **Step 4: Commit**

```bash
git add pom.xml .gitignore
git commit -m "feat: add parent POM with dependency management and .gitignore"
```

---

## Task 2: ielts-common 模块

**Files:**
- Create: `ielts-common/pom.xml`
- Create: `ielts-common/src/main/java/com/ielts/common/dto/ApiResponse.java`
- Create: `ielts-common/src/main/java/com/ielts/common/exception/BusinessException.java`

- [ ] **Step 1: Create ielts-common/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ielts</groupId>
        <artifactId>ielts-platform</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>ielts-common</artifactId>
    <name>IELTS Common</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create ApiResponse.java**

```java
package com.ielts.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

- [ ] **Step 3: Create BusinessException.java**

```java
package com.ielts.common.exception;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -pl ielts-common`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add ielts-common/
git commit -m "feat: add ielts-common module with shared DTO and exception"
```

---

## Task 3: data-service 模块骨架

**Files:**
- Create: `data-service/pom.xml`
- Create: `data-service/src/main/java/com/ielts/data/DataServiceApplication.java`
- Create: `data-service/src/main/resources/application.yml`
- Create: `data-service/Dockerfile`

- [ ] **Step 1: Create data-service/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ielts</groupId>
        <artifactId>ielts-platform</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>data-service</artifactId>
    <name>IELTS Data Service</name>

    <dependencies>
        <dependency>
            <groupId>com.ielts</groupId>
            <artifactId>ielts-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>kafka</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create DataServiceApplication.java**

```java
package com.ielts.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataServiceApplication.class, args);
    }
}
```

- [ ] **Step 3: Create data-service/src/main/resources/application.yml**

```yaml
server:
  port: 8083

spring:
  application:
    name: data-service
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/ielts_data?useSSL=false&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root123}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: data-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.ielts.*

jwt:
  secret: ${JWT_SECRET:my-super-secret-key-for-development-only-change-in-prod}
  expiration: 7200000
```

- [ ] **Step 4: Create data-service/Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl data-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add data-service/
git commit -m "feat: add data-service module skeleton"
```

---

## Task 4: writing-service 模块骨架

**Files:**
- Create: `writing-service/pom.xml`
- Create: `writing-service/src/main/java/com/ielts/writing/WritingServiceApplication.java`
- Create: `writing-service/src/main/resources/application.yml`
- Create: `writing-service/Dockerfile`

- [ ] **Step 1: Create writing-service/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ielts</groupId>
        <artifactId>ielts-platform</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>writing-service</artifactId>
    <name>IELTS Writing Service</name>

    <dependencies>
        <dependency>
            <groupId>com.ielts</groupId>
            <artifactId>ielts-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-deepseek</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>kafka</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create WritingServiceApplication.java**

```java
package com.ielts.writing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WritingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WritingServiceApplication.class, args);
    }
}
```

- [ ] **Step 3: Create writing-service/src/main/resources/application.yml**

```yaml
server:
  port: 8081

spring:
  application:
    name: writing-service
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/ielts_writing?useSSL=false&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root123}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: writing-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.ielts.*
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:sk-placeholder}
      chat:
        options:
          model: deepseek-chat

data-service:
  base-url: http://${DATA_SERVICE_HOST:localhost}:${DATA_SERVICE_PORT:8083}

qdrant:
  host: ${QDRANT_HOST:localhost}
  port: ${QDRANT_PORT:6333}
  collection: writing_exemplars
```

- [ ] **Step 4: Create writing-service/Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl writing-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add writing-service/
git commit -m "feat: add writing-service module skeleton"
```

---

## Task 5: speaking-service 模块骨架

**Files:**
- Create: `speaking-service/pom.xml`
- Create: `speaking-service/src/main/java/com/ielts/speaking/SpeakingServiceApplication.java`
- Create: `speaking-service/src/main/resources/application.yml`
- Create: `speaking-service/Dockerfile`

- [ ] **Step 1: Create speaking-service/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ielts</groupId>
        <artifactId>ielts-platform</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>speaking-service</artifactId>
    <name>IELTS Speaking Service</name>

    <dependencies>
        <dependency>
            <groupId>com.ielts</groupId>
            <artifactId>ielts-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-deepseek</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>kafka</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create SpeakingServiceApplication.java**

```java
package com.ielts.speaking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpeakingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpeakingServiceApplication.class, args);
    }
}
```

- [ ] **Step 3: Create speaking-service/src/main/resources/application.yml**

```yaml
server:
  port: 8082

spring:
  application:
    name: speaking-service
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/ielts_speaking?useSSL=false&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root123}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: speaking-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.ielts.*
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:sk-placeholder}
      chat:
        options:
          model: deepseek-chat

data-service:
  base-url: http://${DATA_SERVICE_HOST:localhost}:${DATA_SERVICE_PORT:8083}

aliyun:
  nls:
    access-key-id: ${ALIYUN_ACCESS_KEY_ID:placeholder}
    access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET:placeholder}
    app-key: ${ALIYUN_NLS_APP_KEY:placeholder}
```

- [ ] **Step 4: Create speaking-service/Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl speaking-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add speaking-service/
git commit -m "feat: add speaking-service module skeleton"
```

---

## Task 6: api-gateway 模块骨架

**Files:**
- Create: `api-gateway/pom.xml`
- Create: `api-gateway/src/main/java/com/ielts/gateway/GatewayApplication.java`
- Create: `api-gateway/src/main/resources/application.yml`
- Create: `api-gateway/Dockerfile`

- [ ] **Step 1: Create api-gateway/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ielts</groupId>
        <artifactId>ielts-platform</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>api-gateway</artifactId>
    <name>IELTS API Gateway</name>

    <dependencies>
        <dependency>
            <groupId>com.ielts</groupId>
            <artifactId>ielts-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-deepseek</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create GatewayApplication.java**

```java
package com.ielts.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- [ ] **Step 3: Create api-gateway/src/main/resources/application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:sk-placeholder}
      chat:
        options:
          model: deepseek-chat

jwt:
  secret: ${JWT_SECRET:my-super-secret-key-for-development-only-change-in-prod}

routing:
  data-service: http://${DATA_SERVICE_HOST:localhost}:${DATA_SERVICE_PORT:8083}
  writing-service: http://${WRITING_SERVICE_HOST:localhost}:${WRITING_SERVICE_PORT:8081}
  speaking-service: http://${SPEAKING_SERVICE_HOST:localhost}:${SPEAKING_SERVICE_PORT:8082}
```

- [ ] **Step 4: Create api-gateway/Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl api-gateway -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add api-gateway/
git commit -m "feat: add api-gateway module skeleton"
```

---

## Task 7: MySQL 初始化脚本

**Files:**
- Create: `init-sql/init.sql`

- [ ] **Step 1: Create init-sql/init.sql**

```sql
-- Create databases
CREATE DATABASE IF NOT EXISTS ielts_data CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ielts_writing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ielts_speaking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ielts_data tables
USE ielts_data;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    target_band DECIMAL(2,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE writing_topics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type TINYINT NOT NULL,
    title TEXT NOT NULL,
    chart_type VARCHAR(50),
    chart_description TEXT,
    category VARCHAR(100),
    difficulty TINYINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE speaking_topics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    part TINYINT NOT NULL,
    question TEXT NOT NULL,
    cue_card TEXT,
    follow_up_questions JSON,
    category VARCHAR(100),
    season VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE practice_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('WRITING','SPEAKING') NOT NULL,
    topic_id BIGINT NOT NULL,
    service_record_id BIGINT NOT NULL,
    overall_band DECIMAL(2,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_type (user_id, type, created_at DESC)
);

-- ielts_writing tables
USE ielts_writing;

CREATE TABLE writing_exemplars (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type TINYINT NOT NULL,
    category VARCHAR(100),
    band_score DECIMAL(2,1),
    excerpt TEXT NOT NULL,
    examiner_comment TEXT NOT NULL,
    source VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE writing_submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    task_type TINYINT NOT NULL,
    essay_text TEXT NOT NULL,
    chart_type VARCHAR(50),
    chart_description TEXT,
    status ENUM('PENDING','SCORING','COMPLETED','FAILED') DEFAULT 'PENDING',
    tr_score DECIMAL(2,1),
    cc_score DECIMAL(2,1),
    lr_score DECIMAL(2,1),
    gra_score DECIMAL(2,1),
    overall_band DECIMAL(2,1),
    lr_gra_detail JSON,
    tr_cc_detail JSON,
    master_feedback JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    scored_at TIMESTAMP NULL,
    INDEX idx_user (user_id, created_at DESC)
);

-- ielts_speaking tables
USE ielts_speaking;

CREATE TABLE speaking_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    examiner_persona ENUM('ENCOURAGING','STRICT') DEFAULT 'ENCOURAGING',
    status ENUM('IN_PROGRESS','COMPLETED','ABANDONED') DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    INDEX idx_user (user_id, started_at DESC)
);

CREATE TABLE session_turns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    part TINYINT NOT NULL,
    turn_order INT NOT NULL,
    role ENUM('EXAMINER','CANDIDATE') NOT NULL,
    content TEXT NOT NULL,
    audio_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id, turn_order)
);

CREATE TABLE speaking_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT UNIQUE NOT NULL,
    fluency_score DECIMAL(2,1),
    lexical_score DECIMAL(2,1),
    grammar_score DECIMAL(2,1),
    pronunciation_score DECIMAL(2,1),
    overall_band DECIMAL(2,1),
    detail JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 2: Commit**

```bash
git add init-sql/
git commit -m "feat: add MySQL initialization script for all databases"
```

---

## Task 8: .env.example + Docker Compose

**Files:**
- Create: `.env.example`
- Create: `docker-compose.yml`

- [ ] **Step 1: Create .env.example**

```env
# MySQL
MYSQL_ROOT_PASSWORD=root123
MYSQL_USER=root
MYSQL_PASSWORD=root123
MYSQL_HOST=mysql
MYSQL_PORT=3306

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Qdrant
QDRANT_HOST=qdrant
QDRANT_PORT=6333

# JWT
JWT_SECRET=my-super-secret-key-for-development-only-change-in-prod

# DeepSeek
DEEPSEEK_API_KEY=sk-your-deepseek-api-key

# Aliyun NLS
ALIYUN_ACCESS_KEY_ID=your-access-key-id
ALIYUN_ACCESS_KEY_SECRET=your-access-key-secret
ALIYUN_NLS_APP_KEY=your-nls-app-key

# Service hosts (for Docker internal networking)
DATA_SERVICE_HOST=data-service
DATA_SERVICE_PORT=8083
WRITING_SERVICE_HOST=writing-service
WRITING_SERVICE_PORT=8081
SPEAKING_SERVICE_HOST=speaking-service
SPEAKING_SERVICE_PORT=8082
```

- [ ] **Step 2: Create docker-compose.yml**

```yaml
services:
  # Infrastructure
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-sql:/docker-entrypoint-initdb.d
    networks:
      - backend
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - backend
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  kafka:
    image: apache/kafka:3.7.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
    volumes:
      - kafka-data:/var/lib/kafka/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1"]
      interval: 10s
      timeout: 10s
      retries: 5

  qdrant:
    image: qdrant/qdrant:v1.12.1
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant-data:/qdrant/storage
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:6333/healthz || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Business Services
  data-service:
    build:
      context: ./data-service
      dockerfile: Dockerfile
    ports:
      - "8083:8083"
    environment:
      MYSQL_HOST: ${MYSQL_HOST}
      MYSQL_PORT: ${MYSQL_PORT}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: ${REDIS_HOST}
      REDIS_PORT: ${REDIS_PORT}
      KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS}
      JWT_SECRET: ${JWT_SECRET}
    networks:
      - backend
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy

  writing-service:
    build:
      context: ./writing-service
      dockerfile: Dockerfile
    ports:
      - "8081:8081"
    environment:
      MYSQL_HOST: ${MYSQL_HOST}
      MYSQL_PORT: ${MYSQL_PORT}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: ${REDIS_HOST}
      REDIS_PORT: ${REDIS_PORT}
      KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS}
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}
      DATA_SERVICE_HOST: ${DATA_SERVICE_HOST}
      DATA_SERVICE_PORT: ${DATA_SERVICE_PORT}
      QDRANT_HOST: ${QDRANT_HOST}
      QDRANT_PORT: ${QDRANT_PORT}
    networks:
      - backend
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
      qdrant:
        condition: service_healthy

  speaking-service:
    build:
      context: ./speaking-service
      dockerfile: Dockerfile
    ports:
      - "8082:8082"
    environment:
      MYSQL_HOST: ${MYSQL_HOST}
      MYSQL_PORT: ${MYSQL_PORT}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: ${REDIS_HOST}
      REDIS_PORT: ${REDIS_PORT}
      KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS}
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}
      DATA_SERVICE_HOST: ${DATA_SERVICE_HOST}
      DATA_SERVICE_PORT: ${DATA_SERVICE_PORT}
      ALIYUN_ACCESS_KEY_ID: ${ALIYUN_ACCESS_KEY_ID}
      ALIYUN_ACCESS_KEY_SECRET: ${ALIYUN_ACCESS_KEY_SECRET}
      ALIYUN_NLS_APP_KEY: ${ALIYUN_NLS_APP_KEY}
    networks:
      - backend
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy

  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      REDIS_HOST: ${REDIS_HOST}
      REDIS_PORT: ${REDIS_PORT}
      JWT_SECRET: ${JWT_SECRET}
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}
      DATA_SERVICE_HOST: ${DATA_SERVICE_HOST}
      DATA_SERVICE_PORT: ${DATA_SERVICE_PORT}
      WRITING_SERVICE_HOST: ${WRITING_SERVICE_HOST}
      WRITING_SERVICE_PORT: ${WRITING_SERVICE_PORT}
      SPEAKING_SERVICE_HOST: ${SPEAKING_SERVICE_HOST}
      SPEAKING_SERVICE_PORT: ${SPEAKING_SERVICE_PORT}
    networks:
      - frontend
      - backend
    depends_on:
      - data-service
      - writing-service
      - speaking-service

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge

volumes:
  mysql-data:
  redis-data:
  kafka-data:
  qdrant-data:
```

- [ ] **Step 3: Verify docker-compose syntax**

Run: `docker compose config --quiet`
Expected: No output (valid syntax)

- [ ] **Step 4: Commit**

```bash
git add .env.example docker-compose.yml
git commit -m "feat: add Docker Compose orchestration and env template"
```

---

## Task 9: 验证全量构建 + 基础设施启动

**Files:**
- No new files

- [ ] **Step 1: Full Maven build**

Run: `mvn clean package -DskipTests`
Expected: BUILD SUCCESS for all 5 modules

- [ ] **Step 2: Start infrastructure containers only**

Run: `docker compose up -d mysql redis kafka qdrant`
Expected: All 4 containers healthy

- [ ] **Step 3: Verify MySQL initialization**

Run: `docker compose exec mysql mysql -uroot -proot123 -e "SHOW DATABASES;"`
Expected: Output includes ielts_data, ielts_writing, ielts_speaking

- [ ] **Step 4: Verify MySQL tables**

Run: `docker compose exec mysql mysql -uroot -proot123 ielts_data -e "SHOW TABLES;"`
Expected: users, writing_topics, speaking_topics, practice_records

- [ ] **Step 5: Verify Redis**

Run: `docker compose exec redis redis-cli ping`
Expected: PONG

- [ ] **Step 6: Verify Kafka**

Run: `docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list`
Expected: Empty list (no topics yet, will be created by services)

- [ ] **Step 7: Verify Qdrant**

Run: `curl -s http://localhost:6333/healthz`
Expected: `{"title":"qdrant - vector search engine","version":"..."}`

- [ ] **Step 8: Stop infrastructure**

Run: `docker compose down`
Expected: All containers stopped

- [ ] **Step 9: Commit (if any fixes were needed)**

```bash
git status
# If changes exist:
git add -A
git commit -m "fix: adjust configuration for successful infrastructure startup"
```

---

## Task 10: Kafka Topic 自动创建配置

**Files:**
- Create: `writing-service/src/main/java/com/ielts/writing/config/KafkaTopicConfig.java`
- Create: `speaking-service/src/main/java/com/ielts/speaking/config/KafkaTopicConfig.java`

- [ ] **Step 1: Create writing-service KafkaTopicConfig.java**

```java
package com.ielts.writing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic writingScoringRequestTopic() {
        return TopicBuilder.name("writing.scoring.request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic writingScoringResultTopic() {
        return TopicBuilder.name("writing.scoring.result")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
```

- [ ] **Step 2: Create speaking-service KafkaTopicConfig.java**

```java
package com.ielts.speaking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic speakingReportRequestTopic() {
        return TopicBuilder.name("speaking.report.request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic speakingSessionResultTopic() {
        return TopicBuilder.name("speaking.session.result")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl writing-service,speaking-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add writing-service/src/main/java/com/ielts/writing/config/KafkaTopicConfig.java speaking-service/src/main/java/com/ielts/speaking/config/KafkaTopicConfig.java
git commit -m "feat: add Kafka topic auto-creation config for writing and speaking services"
```

---

## Task 11: 全量集成验证

**Files:**
- No new files

- [ ] **Step 1: Full build**

Run: `mvn clean package -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: Start all infrastructure**

Run: `docker compose up -d mysql redis kafka qdrant`
Expected: All healthy

- [ ] **Step 3: Start data-service locally to verify connectivity**

Run: `cd data-service && mvn spring-boot:run`
Expected: Application starts on port 8083, connects to MySQL and Redis without errors

- [ ] **Step 4: Stop data-service (Ctrl+C), verify writing-service**

Run: `cd writing-service && mvn spring-boot:run`
Expected: Application starts on port 8081, Kafka topics created automatically

- [ ] **Step 5: Verify Kafka topics were created**

Run: `docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list`
Expected: writing.scoring.request, writing.scoring.result listed

- [ ] **Step 6: Stop all, final commit if needed**

Run: `docker compose down`

```bash
git status
# If any fixes:
git add -A
git commit -m "fix: resolve integration issues from full-stack verification"
```

---

## Summary

完成后你将拥有：
- Maven 多模块项目，5 个子模块全部可编译
- Docker Compose 一键启动 MySQL + Redis + Kafka + Qdrant
- 所有数据库和表自动初始化
- Kafka topic 自动创建
- 每个服务可独立启动并连接基础设施
- 为后续 Plan 2（Data Service 业务逻辑）做好准备
