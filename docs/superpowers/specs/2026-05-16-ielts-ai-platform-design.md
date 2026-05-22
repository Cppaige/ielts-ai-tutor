# IELTS AI 练习平台 — 架构设计文档 v1.1

> **版本说明**：v1.0 为初始设计稿；v1.1 基于第一版实际落地情况更新，记录了实现偏差、已修复的关键 Bug 及已知技术债。

---

## 第一节：系统架构演进

### 1.1 服务清单与端口

| 服务 | 端口 | 职责 |
|------|------|------|
| `api-gateway` | 8080 | JWT 校验（Servlet Filter）、反向代理（RestClient）、Guardrail 意图分类（DeepSeek）、WebSocket 评分进度推送 |
| `data-service` | 8083 | 用户注册/登录/JWT 签发、题库 CRUD、练习记录索引（Kafka 消费写入）、Redis Cache-Aside |
| `writing-service` | 8081 | 作文提交 → Kafka 入队 → 三 Agent 流水线评分 → Redis Pub/Sub 进度通知 → Kafka 摘要 |
| `speaking-service` | 8082 | 口语会话状态机（Redis Hash + Lua）、ASR/LLM/TTS 编排、Kafka 报告生成 |

### 1.2 服务间通信拓扑

| 路径 | 方式 | 实现说明 |
|------|------|----------|
| 前端 → gateway | HTTP REST + WebSocket | 唯一对外入口，端口 8080 |
| gateway → 各下游服务 | HTTP（Spring `RestClient`，同步阻塞） | **实现偏差**：设计稿用 WebClient（响应式），实际落地用 RestClient（Servlet 栈） |
| writing/speaking → data-service | HTTP REST（WebClient，响应式） | 题库数据获取，调用方侧 Cache-Aside |
| writing-service → gateway | Redis Pub/Sub | 评分进度通知，channel: `scoring.progress:{submissionId}` |
| writing/speaking → data-service | Kafka | 评分/报告摘要异步推送 |

### 1.3 架构图（当前实现）

```
┌─────────────────────────────────────────────────────┐
│                      Frontend                        │
│                  (Vite + React, :5173)               │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP + WebSocket
                       ▼
┌─────────────────────────────────────────────────────┐
│                   api-gateway (:8080)                │
│  Filter Chain: JwtAuthFilter(Order=1)                │
│               → GuardrailFilter(Order=2)             │
│               → ProxyController (RestClient)         │
│  WebSocket: /ws/scoring/{submissionId}               │
│             ← Redis Pub/Sub subscriber               │
└──────────┬──────────────┬──────────────┬────────────┘
           │ REST          │ REST          │ REST
           ▼               ▼               ▼
  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
  │ data-service │ │writing-service│ │speaking-svc  │
  │    (:8083)   │ │   (:8081)    │ │   (:8082)    │
  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
         │                │                 │
         ▼                ▼                 ▼
      MySQL            MySQL             MySQL
    (ielts_data)   (ielts_writing)   (ielts_speaking)

共享基础设施（Docker backend 网络）:
  Redis  ← Cache-Aside + Pub/Sub + 状态机 Hash
  Kafka  ← 评分任务队列 + 摘要同步
  Qdrant ← 范文向量检索（writing-service 专用）
```

### 1.4 路由规则（实际实现）

gateway 同时支持带 `/api` 前缀和不带前缀两套路径，`ProxyController` 在转发前统一剥离 `/api` 前缀：

| 外部路径 | 转发目标 |
|----------|----------|
| `/auth/**`, `/api/auth/**` | data-service:8083 |
| `/data/**`, `/api/data/**` | data-service:8083 |
| `/writing/**`, `/api/writing/**` | writing-service:8081 |
| `/speaking/**`, `/api/speaking/**` | speaking-service:8082 |

JWT 白名单：`/auth/login`, `/auth/register`, `/api/auth/login`, `/api/auth/register`，以及所有 `OPTIONS` 预检请求无条件放行。

---

## 第二节：核心技术规格

### 2.1 JWT 鉴权

- data-service 签发，gateway 校验，共享密钥（`JWT_SECRET` 环境变量，≥32 字节）
- Payload：`{userId, email, exp, iat}`，Access Token TTL 2 小时
- gateway 通过 `HttpServletRequestWrapper` 注入 `X-User-Id` header，下游服务直接读取，不重复校验
- 内部服务间信任 Docker 网络隔离，不做 mTLS

### 2.2 Guardrail 意图分类

**触发条件**：仅对 UGC 写入路径（`POST /writing/submit`, `POST /api/writing/submit`）

**实现**：`GuardrailFilter`（Order=2，在 JWT 校验之后）

- 读取请求 body → 调 DeepSeek（`deepseek-v4-flash`）分类 → 判断 `OFF_TOPIC` → 返回 400
- 分类器异常时 **fail open**（放行），避免误伤正常用户
- body 读取后用 `CachedBodyRequestWrapper` 重新包装，保证下游 Controller 仍可读取

**口语侧 Guardrail**：`SessionService.isAbnormalContent()` 在 speaking-service 内部对候选人文本做二次分类（`NORMAL`/`ABNORMAL`），空文本直接放行。

### 2.3 Writing Service 三 Agent 流水线

```
Kafka 消费 writing.scoring.request
        │
        ├── DataServiceClient.getEmbedding(essayText)
        │   └── 阿里云 DashScope text-embedding-v2 → 1536 维向量
        │
        ▼
CompletableFuture.allOf() 并行执行
  ├── LrGraAgent   → 词汇多样性 + 语法准确性 → JSON(lr_score, gra_score, 错误列表)
  └── TrCcAgent    → 任务回应 + 连贯性 + RAG 范文注入 → JSON(tr_score, cc_score, 结构分析)
        │
        ▼
MasterAgent → 交叉校验 + Band Score 计算 + 综合反馈
        │
        ▼
写入 DB (status: COMPLETED) + Redis Pub/Sub 进度通知 + Kafka writing.scoring.result
```

**Agent 安全约束**：用户作文始终包裹在 `<essay_text></essay_text>` 标签内，System Prompt 声明标签内为不可信输入。Agent JSON 输出经 Jackson 反序列化 + 字段校验（分数范围 0-9，必填项），校验失败重试一次，二次失败标记 `FAILED`。

**进度推送节点**：

| 阶段 | 状态值 | Redis Pub/Sub channel |
|------|--------|-----------------------|
| 开始评分 | `SCORING_STARTED` | `scoring.progress:{submissionId}` |
| 语法词汇完成 | `LR_GRA_DONE` | 同上 |
| 逻辑连贯完成 | `TR_CC_DONE` | 同上 |
| 汇总完成 | `COMPLETED` | 同上 |
| 任意阶段异常 | `FAILED` | 同上 |

### 2.4 RAG 范文检索（已修复三个致命问题）

**Qdrant 集合**：`writing_exemplars`，向量维度 1536（阿里云 `text-embedding-v2`），距离度量 Cosine，当前已导入 68 条记录。

**Payload 字段**（与后端代码对齐后）：

```json
{
  "exemplar_id": 2495,
  "excerpt": "段落级范文片段（非全文）",
  "task_type": 2,
  "category": "Education",
  "band_score": 9.0,
  "topic_name": "Nature vs. Nurture",
  "question_text": "..."
}
```

**检索流程**：

1. 用户作文 → `DataServiceClient.getEmbedding()` → 1536 维 query 向量
2. Qdrant filter（仅 `task_type`，已移除 `category` filter）+ vector search，`top_k=3`，`score_threshold=0.7`
3. 返回 `excerpt` 拼入 TrCcAgent prompt
4. 评分完成后用 `exemplar_id` 回查 MySQL 完整范文供前端展示

**降级策略**：Qdrant 不可用 / Embedding API 不可用 / 检索结果为空 → 跳过 RAG，TrCcAgent 正常评分。

**已修复的三个致命 Bug**（详见 `docs/qdrant-rag-fix.md`）：

| # | 问题 | 修复 |
|---|------|------|
| 1 | Python 脚本导入到 `ielts_essays`，后端读 `writing_exemplars`，collection 名不一致 | 重命名 collection 为 `writing_exemplars` |
| 2 | `DataServiceClient.getEmbedding()` 返回空列表，RAG 完全未工作 | 实现调用阿里云 DashScope `text-embedding-v2` |
| 3 | `ScoringRequestConsumer` 硬编码 `category="general"`，Qdrant filter 永远 miss | 移除 category filter，只按 `task_type` 过滤 |

### 2.5 Speaking Service 状态机

**状态流转**：

```
PART1_QA → PART2_INTRO → PART2_CANDIDATE_SPEAKING → PART3_DISCUSSION → SESSION_ENDED
```

**Redis 数据结构**：

```
Key: session:{sessionId}   Type: Hash   TTL: 3600s
Fields: state, userId, topicId, persona,
        part1Index, part1Questions(JSON),
        part2StartedAt, turnCount, createdAt
```

**Lua 脚本原子状态推进**：CAS 语义，`current != expected` 时拒绝推进，防止并发竞争。

**当前实现范围（MVP）**：

- 仅实现 Part 1 完整流程（4 道硬编码问题，不从 data-service 拉取）
- Part 1 问题用完后直接推进到 `SESSION_ENDED`，跳过 Part 2 / Part 3
- 支持文本输入（`textInput`）和音频输入（`audioData`）两条路，ASR 接口已预留但实现为 stub
- TTS 接口已预留，`audioUrl` 字段已在响应中返回

**每轮处理流程**：

```
候选人输入（文本/音频）
  → ASR（音频路径）
  → isAbnormalContent() Guardrail
  → 保存 CANDIDATE turn
  → ExaminerService.generateResponse()（LLM + 考官人设 + 对话历史）
  → TtsService.synthesize()
  → 保存 EXAMINER turn
  → stateMachine.incrementField(part1Index, turnCount)
  → [若问题用完] transition → SESSION_ENDED → endSession() → Kafka speaking.report.request
```

### 2.6 Kafka 消息设计

**Topic 列表**：

| Topic | 生产者 | 消费者 | Partition | Key |
|-------|--------|--------|-----------|-----|
| `writing.scoring.request` | writing-service | writing-service | 3 | userId |
| `writing.scoring.result` | writing-service | data-service | 3 | userId |
| `speaking.report.request` | speaking-service | speaking-service | 3 | sessionId |
| `speaking.session.result` | speaking-service | data-service | 3 | userId |

**消费失败处理**：重试 3 次（backoff 1s/3s/10s），三次失败后进入 DLT（`{topic}.DLT`），MVP 阶段仅日志告警。

### 2.7 Redis Key 规划

| 服务 | 用途 | Key 模式 | TTL |
|------|------|----------|-----|
| data-service | 题库/用户缓存 | `data:topic:writing:{id}`, `data:user:{id}` | 15-30 min |
| writing-service | 题库缓存 | `writing:topic:{id}` | 30 min |
| speaking-service | 题库缓存 | `speaking:topic:{id}` | 30 min |
| speaking-service | 会话状态机 | `session:{sessionId}` | 1 hour |
| writing-service | 进度 Pub/Sub | `scoring.progress:{submissionId}` | N/A |

缓存策略：统一 Cache-Aside。写入时 data-service 删 Redis key，调用方侧缓存依赖 TTL 自然过期。

### 2.8 数据库 Schema（实际落地版）

**与设计稿的差异**：所有 `TINYINT` 字段已改为 `INT`（`task_type`, `part`, `difficulty` 等），`writing_exemplars` 新增 `full_content TEXT NOT NULL` 字段用于前端展示完整范文，`examiner_comment` 改为可空（`NULL`）。

三个独立 MySQL database：`ielts_data`、`ielts_writing`、`ielts_speaking`，跨服务引用通过 ID 关联，不使用外键。

### 2.9 API Gateway 实现细节

**实现栈**：Spring Boot 3.x Servlet 栈（非 WebFlux），`ProxyController` 使用 `RestClient`（同步阻塞）转发请求。

**Header 处理**：转发时复制所有 header，跳过 `Host` 和 `Content-Length`；`X-User-Id` 由 `JwtAuthFilter` 的 `UserIdHeaderWrapper` 注入，外部请求无法伪造（filter 在 proxy 之前执行）。

**CORS**：`application.yml` 中配置允许 `localhost:5173` 和 `localhost:5174`（Vite 开发服务器），`OPTIONS` 预检请求在 `JwtAuthFilter` 中无条件放行。

---

## 第三节：已知局限与技术债

### 3.1 高优先级（影响功能完整性）

**Speaking Service 仅实现 Part 1**
- 当前：4 道硬编码问题，Part 1 结束即 `SESSION_ENDED`
- 缺失：Part 2（话题卡 + 独白计时）、Part 3（动态追问逻辑）
- 缺失：从 data-service 动态拉取题目（`startSession` 中 `questions` 为硬编码列表）

**ASR / TTS 为 Stub**
- `AsrService` 和 `TtsService` 接口已定义，实现为占位符（返回空字符串或固定 URL）
- 音频输入路径（`audioData`）实际未经过真实 ASR，`textInput` 是当前唯一可用输入方式

**RAG Category 过滤已移除**
- 当前仅按 `task_type` 过滤，语义相关性依赖 cosine 相似度
- 数据量（68 条）不足以支撑 category 细分过滤
- 恢复条件：数据量 ≥ 数百条 + `ScoringRequestMessage` 携带 `category` 字段

### 3.2 中优先级（架构妥协）

**Gateway 同步阻塞代理**
- `ProxyController` 使用 `RestClient`（同步），gateway 线程在等待下游响应期间被阻塞
- 高并发场景下 Tomcat 线程池会成为瓶颈
- 迭代方向：迁移到 Spring Cloud Gateway（WebFlux 响应式）或 Virtual Threads（Java 21）

**Embedding API 同步调用**
- `DataServiceClient.getEmbedding()` 使用 `WebClient.block()`，在 Kafka 消费线程中同步等待阿里云 API 响应
- 阿里云 API 延迟直接影响评分吞吐量
- 迭代方向：改为异步，或在 Kafka 消费前预计算 embedding

**缓存失效策略不一致**
- data-service 写入时删 Redis key（主动失效）
- 调用方侧（writing/speaking-service）缓存依赖 TTL 过期（被动失效）
- 题库数据更新后，调用方侧最多有 30 分钟的脏读窗口

**单 Redis 实例**
- Cache-Aside、Pub/Sub、状态机 Hash 共用同一 Redis 实例
- 无 Sentinel / Cluster，单点故障会同时影响缓存、评分进度推送和口语会话状态

### 3.3 低优先级（MVP 已知简化）

**无 Refresh Token**
- Access Token TTL 2 小时，过期后需重新登录
- 迭代方向：实现 Refresh Token + Token 轮换

**内部服务无鉴权**
- 依赖 Docker 网络隔离，内部服务信任 `X-User-Id` header
- 迭代方向：内部服务间 mTLS 或 Service Account Token

**DLT 仅日志告警**
- Kafka DLT 消息无自动重处理机制，需人工介入
- 迭代方向：DLT 消费者 + 告警 + 可重放机制

**Qdrant 数据入库为手动脚本**
- `qdrant/read_qdrant_data.py` 需手动执行，不集成到服务启动流程
- API Key 硬编码在脚本中（`sk-29d9a46a...`），存在安全风险，需迁移到环境变量

**输入校验不完整**
- `speaking_topics` 的 `part` 参数传入不存在的值（如 `part=5`）会成功返回空列表而非 400
- `writing_topics` 的 `task_type=3` 同样会成功返回空列表
- 迭代方向：Controller 层加枚举校验，返回明确的 400 错误

---

## 附录：环境变量清单

| 变量 | 服务 | 说明 |
|------|------|------|
| `JWT_SECRET` | gateway, data-service | 共享密钥，≥32 字节 |
| `DEEPSEEK_API_KEY` | gateway, writing-service, speaking-service | DeepSeek API Key |
| `DEEPSEEK_BASE_URL` | gateway | 默认 `https://api.deepseek.com` |
| `DASHSCOPE_API_KEY` | writing-service | 阿里云 DashScope，用于 Embedding |
| `DATA_SERVICE_URL` | gateway, writing-service, speaking-service | 默认 `http://localhost:8083` |
| `WRITING_SERVICE_URL` | gateway | 默认 `http://localhost:8081` |
| `SPEAKING_SERVICE_URL` | gateway | 默认 `http://localhost:8082` |
| `REDIS_HOST` / `REDIS_PORT` | 所有服务 | 默认 `localhost:6379` |
| `QDRANT_HOST` / `QDRANT_PORT` | writing-service | 默认 `localhost:6333` |
