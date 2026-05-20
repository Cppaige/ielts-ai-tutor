# Qdrant RAG 链路排查与修复记录

## 背景

本项目使用 Qdrant 作为向量数据库存储雅思高分范文（`writing_exemplars`），
在 writing-service 评分流程中通过 RAG 检索相似范文，作为 LLM 评分的参考上下文。

---

## 一、`writing_exemplars` 表设计解析

### 表结构

```sql
writing_exemplars (
  id BIGINT PK AUTO_INCREMENT,
  task_type TINYINT NOT NULL,
  category VARCHAR(100),
  band_score DECIMAL(2,1),
  excerpt TEXT NOT NULL,
  examiner_comment TEXT NOT NULL,
  source VARCHAR(100),           -- 来源: Cambridge IELTS 17 Test 1
  created_at TIMESTAMP
)
```

### `excerpt` 字段不是范文全文

依据：

**1. 字段名本身**

`excerpt` 英文意思是"节选、摘录"，不是 `full_text` / `essay`。

**2. 实际用途（RAG 检索喂给 LLM）**

`writing-service/src/main/java/com/ielts/writing/agent/TrCcAgent.java:51-57`：

```java
private String buildExemplarContext(List<QdrantClient.SearchResult> exemplars) {
    if (exemplars.isEmpty()) return "";
    String refs = exemplars.stream()
            .map(e -> "---\n" + e.excerpt() + "\n---")
            .collect(Collectors.joining("\n"));
    return "以下是同类型高分范文供参考:\n" + refs;
}
```

检索回 top-3 个 excerpt，拼成 prompt 喂给评分 LLM 作为参考。

**3. Qdrant 向量化的粒度**

`QdrantClient.java` 中每条 excerpt 在 Qdrant 里对应**一个嵌入向量**。一篇 250 词的雅思作文如果整体压成一个向量，语义会被严重稀释，RAG 检索效果会很差。RAG 的最佳实践是**段落级别 chunking**。

### 建议存什么

| 内容 | 推荐 |
|---|---|
| Task 1 整段开篇概述 | ✅ 适合 |
| Task 2 主体论证段（1-2 段） | ✅ 适合 |
| 完整 250+ 词的整篇作文 | ❌ 不推荐，向量稀释、prompt 过长 |
| 单句金句 | ⚠️ 可以但偏短 |

如果确实想存整篇范文，可考虑：
- 加一个 `full_essay` 字段存全文（仅用于前端展示）
- `excerpt` 仍存段落级片段（用于向量化和 RAG）
- 或者把整篇文章拆成多条 `excerpt` 记录（按段落），共享同一个 `source` 标识来自同一篇

---

## 二、Qdrant Payload 字段对齐校验

### 后端代码期望（`QdrantClient.java`）

| 用途 | 字段名 |
|---|---|
| Filter 过滤 | `task_type` |
| Filter 过滤 | `category` |
| Payload 解析 | `exemplar_id` |
| Payload 解析 | `excerpt` |

### 最小必需字段

| 字段 | 必需性 | 用途 |
|---|---|---|
| `task_type` | ✅ 必需 | filter |
| `category` | ✅ 必需（删除前） | filter |
| `excerpt` | ✅ 必需 | 喂给 LLM 做参考 |
| `exemplar_id` | ✅ 建议保留 | 关联 MySQL，回查详情 |
| `band_score` | 可选 | 可加到 filter 里只检索 7+ 高分文 |
| `topic_name`、`question_text` | 可选 | 调试时方便看 |

### 最终 Payload 示例（对齐后）

```json
{
  "exemplar_id": 2495,
  "excerpt": "The relative importance of natural talent and training is a frequent topic of discussion...",
  "task_type": 2,
  "category": "Education",
  "band_score": 9,
  "topic_name": "Nature vs. Nurture",
  "question_text": "It is generally believed that some people are born with certain talents..."
}
```

---

## 三、Python 导入脚本（`qdrant/read_qdrant_data.py`）

### 关键设计

- 调用阿里云 DashScope `text-embedding-v2` 模型生成 **1536 维** 真实向量
- 按 20 条/批次请求，避免 API 限速
- 重建 collection 时清空旧数据，按真实维度建表
- Payload 字段名严格对齐后端代码

### 字段映射

```python
payload = {
    "exemplar_id": r["topicId"],
    "excerpt": r["essayBody"],
    "task_type": r["taskType"],
    "category": r["categoryName"],
    "band_score": r["bandScore"],
    "topic_name": r["topicName"],
    "question_text": r["questionText"],
}
```

### 已导入数据

- 共 68 条记录
- 向量维度：1536（阿里云 text-embedding-v2 实测维度）
- 距离度量：Cosine

---

## 四、发现的三个致命问题

### 🚨 问题 1：Collection 名称不一致

| 位置 | 值 |
|---|---|
| Python 脚本导入到 | `ielts_essays` |
| 后端配置 `application.yml` 期望 | `writing_exemplars` |

**后果**：后端去查 `writing_exemplars` 这个 collection，根本不存在，搜索失败。

**修复**：把 Qdrant 中的 collection 重命名为 `writing_exemplars`（已完成）。

---

### 🚨 问题 2：后端 embedding 未实现，是空列表

`writing-service/src/main/java/com/ielts/writing/service/DataServiceClient.java`（修复前）：

```java
public List<Float> getEmbedding(String text) {
    // TODO: Call Aliyun embedding API
    // For MVP, return empty list which will skip RAG
    return List.of();
}
```

后端调评分流程时，传给 Qdrant 的 query vector 是**空列表**。然后 `QdrantClient.java:32-34`：

```java
if (queryVector == null || queryVector.isEmpty()) {
    return List.of();
}
```

直接短路返回空，**整个 RAG 完全没工作**，无论 Qdrant 里数据多漂亮都用不上。

**修复**：实现 `DataServiceClient.getEmbedding()`，调阿里云 `text-embedding-v2` 把考生作文转成 1536 维向量。必须保证**和导入数据时用同一个模型**，否则向量空间不一致。

---

### 🚨 问题 3：Category 过滤会全部 miss

`ScoringRequestConsumer.java:25`（修复前）：

```java
scoringPipeline.execute(message.submissionId(), embedding, "general");
```

写死了传 `"general"`。Qdrant filter `category == "general"` 永远匹配不到任何记录（实际值是 `Education`、`Government` 等）。

**修复**：去掉 category filter，只按 task_type 过滤。

---

## 五、修复方案与代码变更

### 5.1 实现 `DataServiceClient.getEmbedding()`（调用阿里云 DashScope）

`writing-service/src/main/java/com/ielts/writing/service/DataServiceClient.java`：

```java
package com.ielts.writing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DataServiceClient {

    private static final String DASHSCOPE_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final String EMBEDDING_MODEL = "text-embedding-v2";

    private final WebClient webClient;
    private final WebClient embeddingClient;
    private final String dashscopeApiKey;
    private final ObjectMapper objectMapper;

    public DataServiceClient(@Value("${data-service.base-url}") String baseUrl,
                             @Value("${dashscope.api-key:}") String dashscopeApiKey,
                             ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.embeddingClient = WebClient.builder().baseUrl(DASHSCOPE_URL).build();
        this.dashscopeApiKey = dashscopeApiKey;
        this.objectMapper = objectMapper;
    }

    public List<Float> getEmbedding(String text) {
        if (text == null || text.isBlank() || dashscopeApiKey == null || dashscopeApiKey.isBlank()) {
            return List.of();
        }

        Map<String, Object> body = Map.of(
                "model", EMBEDDING_MODEL,
                "input", Map.of("texts", List.of(text))
        );

        try {
            String responseBody = embeddingClient.post()
                    .header("Authorization", "Bearer " + dashscopeApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseEmbedding(responseBody);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Float> parseEmbedding(String responseBody) {
        List<Float> embedding = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode embeddings = root.path("output").path("embeddings");
            if (embeddings.isArray() && !embeddings.isEmpty()) {
                JsonNode vector = embeddings.get(0).path("embedding");
                if (vector.isArray()) {
                    for (JsonNode v : vector) {
                        embedding.add(v.floatValue());
                    }
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return embedding;
    }
}
```

### 5.2 删除 Qdrant Category Filter

`writing-service/src/main/java/com/ielts/writing/rag/QdrantClient.java`：

```java
public List<SearchResult> search(List<Float> queryVector, int taskType, int topK) {
    if (queryVector == null || queryVector.isEmpty()) {
        return List.of();
    }

    Map<String, Object> filter = Map.of(
            "must", List.of(
                    Map.of("key", "task_type", "match", Map.of("value", taskType))
            )
    );

    Map<String, Object> body = Map.of(
            "vector", queryVector,
            "filter", filter,
            "limit", topK,
            "score_threshold", 0.7,
            "with_payload", true
    );
    // ... 略
}
```

### 5.3 调用方更新（移除 category 参数）

**`TrCcAgent.java:44`**：

```java
public TrCcResult analyze(String essayText, List<Float> essayEmbedding, int taskType) {
    List<QdrantClient.SearchResult> exemplars = qdrantClient.search(essayEmbedding, taskType, 3);
    String exemplarContext = buildExemplarContext(exemplars);
    String userMessage = exemplarContext + "\n\n<essay_text>" + essayText + "</essay_text>";
    return callWithRetry(userMessage);
}
```

**`ScoringPipeline.java:40`**：

```java
public void execute(Long submissionId, List<Float> essayEmbedding) {
    // ...
    CompletableFuture<TrCcResult> trCcFuture = CompletableFuture.supplyAsync(
            () -> trCcAgent.analyze(submission.getEssayText(), essayEmbedding, submission.getTaskType()));
    // ...
}
```

**`ScoringRequestConsumer.java:25`**：

```java
@KafkaListener(topics = "writing.scoring.request", groupId = "writing-service")
public void consume(ScoringRequestMessage message) {
    List<Float> embedding = dataServiceClient.getEmbedding(message.essayText());
    scoringPipeline.execute(message.submissionId(), embedding);
}
```

### 5.4 配置文件变更

**`writing-service/src/main/resources/application.yml`** — 新增配置项：

```yaml
data-service:
  base-url: ${DATA_SERVICE_URL:http://localhost:8083}

dashscope:
  api-key: ${DASHSCOPE_API_KEY:}
```

**`docker-compose.yml`** — writing-service 加环境变量：

```yaml
writing-service:
  environment:
    DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}
    DASHSCOPE_API_KEY: ${DASHSCOPE_API_KEY}
    DATA_SERVICE_URL: http://data-service:8083
    QDRANT_HOST: qdrant
    QDRANT_PORT: 6333
```

---

## 六、改动文件总览

| 文件 | 改动 |
|---|---|
| `DataServiceClient.java` | 实现 `getEmbedding()`，调用阿里云 DashScope `text-embedding-v2`（与 Python 脚本同款，1536 维） |
| `QdrantClient.java:31` | 移除 `category` 参数，filter 只保留 `task_type` |
| `TrCcAgent.java:44` | 方法签名移除 `category` |
| `ScoringPipeline.java:40` | 方法签名移除 `category` |
| `ScoringRequestConsumer.java:25` | 不再硬编码 `"general"` |
| `application.yml` | 新增 `dashscope.api-key` 配置 |
| `docker-compose.yml` | writing-service 加 `DASHSCOPE_API_KEY` 环境变量 |

---

## 七、启动前 Checklist

### 1. 设置环境变量

在 `.env` 文件或 shell 里加：

```bash
export DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxx
```

（与 Python 脚本使用同一个 key）

### 2. 确认 Qdrant collection 名一致

后端默认读 `writing_exemplars`（见 `writing-service/src/main/resources/application.yml`）。验证：

```bash
curl http://localhost:6333/collections
```

返回应包含 `writing_exemplars`。

### 3. 验证 Qdrant 检索可用

```bash
curl -X POST http://localhost:6333/collections/writing_exemplars/points/search \
  -H "Content-Type: application/json" \
  -d '{
    "vector": [...1536 个浮点数...],
    "filter": {"must": [{"key": "task_type", "match": {"value": 2}}]},
    "limit": 3,
    "score_threshold": 0.7,
    "with_payload": true
  }'
```

应返回 top-3 相似范文。

---

## 八、Category 过滤的设计权衡

### 作用

让 RAG 只检索同类话题的范文（如 Education 类作文只参考 Education 类高分范文，提升语义相关性）。

### 为什么先删掉

- 目前只有 68 条数据，task_type 过滤后剩 30-40 条已经够细
- `ScoringRequestConsumer` 拿不到题目的 category，硬编码 `"general"` 永远 miss
- cosine 相似度本身会优先选语义最接近的范文

### 未来什么时候加回来

数据量到几百条以上、且能从 `topic` 表里查到 category 时，再恢复 filter 会有意义。届时只需：

1. 在 `ScoringRequestMessage` 加 `category` 字段
2. writing-service 提交时从 data-service 查 topic 的 category
3. 一路传到 `QdrantClient.search()` 加回 filter

---

## 九、当前 RAG 链路状态

| 环节 | 状态 |
|---|---|
| Qdrant 数据（向量+payload） | ✅ 正确（1536 维真实阿里云向量） |
| Collection 名匹配 | ✅ 已对齐 `writing_exemplars` |
| 后端生成 query 向量 | ✅ 已实现（DashScope `text-embedding-v2`） |
| Category 过滤参数 | ✅ 已移除 |
| 编译验证 | ✅ `mvn clean compile -pl writing-service -am` 通过 |

---

## 十、参考：完整数据流

```
用户提交作文
   ↓
writing-service: POST /writing/submit
   ↓
保存 WritingSubmission (status: PENDING)
   ↓
发 Kafka 消息: writing.scoring.request
   ↓
ScoringRequestConsumer 消费
   ↓
DataServiceClient.getEmbedding(essayText)  ← 调阿里云 DashScope
   ↓ (返回 1536 维向量)
ScoringPipeline.execute(submissionId, embedding)
   ↓
并行执行：
  ├── LrGraAgent (词汇语法)
  └── TrCcAgent (任务回应 + 衔接)
        ↓
        QdrantClient.search(embedding, taskType, 3)  ← Qdrant 检索 top-3 范文
        ↓
        把范文片段拼进 prompt
        ↓
        DeepSeek LLM 评分
   ↓
MasterAgent 汇总
   ↓
保存评分结果 (status: COMPLETED)
   ↓
WebSocket 推送进度 + Kafka 发结果消息
```
