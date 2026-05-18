# IELTS AI 练习平台 — 架构设计文档

## 项目概述

基于 AI 的雅思备考平台，包含写作评分系统（多 Agent 流水线）、口语对练系统（状态机考官）、题库与用户系统。采用多容器微服务架构，Java 21 + Spring Boot 3.x + Spring AI + DeepSeek。

---

## 第一节：服务架构与通信拓扑

### 服务清单

| 服务 | 职责 |
|------|------|
| api-gateway (8080) | JWT 校验、路由转发（WebClient）、WebSocket 连接管理、Guardrail 意图分类（deepseek-v4-flash） |
| data-service (8083) | 用户注册/登录/JWT 签发、题库 CRUD、练习记录索引摘要、Redis Cache-Aside |
| writing-service (8081) | 作文提交 → Kafka 入队 → 三 Agent 流水线评分 → Redis Pub/Sub 进度 → Kafka 摘要 |
| speaking-service (8082) | 口语会话状态机（Redis + Lua）、ASR/LLM/TTS 编排、Kafka 报告生成 |

### 服务间通信

| 路径 | 方式 | 用途 |
|------|------|------|
| gateway → 各服务 | REST (WebClient) | 请求转发 |
| writing/speaking → data-service | REST | 获取题库数据（Cache-Aside 在调用方） |
| writing-service → gateway | Redis Pub/Sub | 评分进度通知 |
| writing/speaking → data-service | Kafka | 评分/报告摘要推送 |

### 架构图

```
┌─────────────┐
│   Frontend  │
└──────┬──────┘
       │ HTTP + WebSocket
┌──────▼──────┐
│ api-gateway │
└──┬───┬───┬──┘
   │   │   │  REST (内部)
   ▼   ▼   ▼
┌─────┐ ┌─────────┐ ┌─────────────┐
│data │ │writing  │ │speaking     │
│svc  │ │svc      │ │svc          │
└──┬──┘ └────┬────┘ └──────┬──────┘
   │         │              │
   ▼         ▼              ▼
 MySQL    MySQL           MySQL
 (共享 Redis 集群: 缓存 + Pub/Sub + 状态机)
```

---

## 第二节：数据库 Schema 设计

每个服务独立 MySQL 数据库，跨服务引用通过 ID 关联（不用外键）。

### data-service（ielts_data）

```sql
-- 用户表
users (
  id BIGINT PK AUTO_INCREMENT,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(100),
  target_band DECIMAL(2,1),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

-- 写作题库
writing_topics (
  id BIGINT PK AUTO_INCREMENT,
  task_type TINYINT NOT NULL,       -- 1=Task1, 2=Task2
  title TEXT NOT NULL,
  chart_type VARCHAR(50),           -- Task1: bar/line/pie/table/map/process
  chart_description TEXT,           -- Task1: 图表结构化描述
  category VARCHAR(100),            -- education, technology...
  difficulty TINYINT,
  created_at TIMESTAMP
)

-- 口语题库
speaking_topics (
  id BIGINT PK AUTO_INCREMENT,
  part TINYINT NOT NULL,            -- 1/2/3
  question TEXT NOT NULL,
  cue_card TEXT,                    -- Part2 话题卡内容
  follow_up_questions JSON,         -- Part3 预设追问
  category VARCHAR(100),
  season VARCHAR(20),               -- 考季: 2024-Q1
  created_at TIMESTAMP
)

-- 练习记录索引（从 Kafka 消费写入）
practice_records (
  id BIGINT PK AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type ENUM('WRITING','SPEAKING') NOT NULL,
  topic_id BIGINT NOT NULL,
  service_record_id BIGINT NOT NULL,
  overall_band DECIMAL(2,1),
  created_at TIMESTAMP,
  INDEX idx_user_type (user_id, type, created_at DESC)
)
```

### writing-service（ielts_writing）

```sql
-- 范文表（前端展示 + Qdrant 关联）
writing_exemplars (
  id BIGINT PK AUTO_INCREMENT,
  task_type TINYINT NOT NULL,
  category VARCHAR(100),
  band_score DECIMAL(2,1),
  excerpt TEXT NOT NULL,
  examiner_comment TEXT NOT NULL,
  source VARCHAR(100),           -- 来源: Cambridge IELTS 17 Test 1
  full_content TEXT NOT NULL,
  created_at TIMESTAMP
)

writing_submissions (
  id BIGINT PK AUTO_INCREMENT,
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
  created_at TIMESTAMP,
  scored_at TIMESTAMP,
  INDEX idx_user (user_id, created_at DESC)
)
```

### speaking-service（ielts_speaking）

```sql
speaking_sessions (
  id BIGINT PK AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  topic_id BIGINT NOT NULL,
  examiner_persona ENUM('ENCOURAGING','STRICT') DEFAULT 'ENCOURAGING',
  status ENUM('IN_PROGRESS','COMPLETED','ABANDONED') DEFAULT 'IN_PROGRESS',
  started_at TIMESTAMP,
  ended_at TIMESTAMP,
  INDEX idx_user (user_id, started_at DESC)
)

session_turns (
  id BIGINT PK AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  part TINYINT NOT NULL,
  turn_order INT NOT NULL,
  role ENUM('EXAMINER','CANDIDATE') NOT NULL,
  content TEXT NOT NULL,
  audio_url VARCHAR(500),
  created_at TIMESTAMP,
  INDEX idx_session (session_id, turn_order)
)

speaking_reports (
  id BIGINT PK AUTO_INCREMENT,
  session_id BIGINT UNIQUE NOT NULL,
  fluency_score DECIMAL(2,1),
  lexical_score DECIMAL(2,1),
  grammar_score DECIMAL(2,1),
  pronunciation_score DECIMAL(2,1),
  overall_band DECIMAL(2,1),
  detail JSON,
  created_at TIMESTAMP
)
```

---

## 第三节：Kafka 消息设计

### Topic 列表

| Topic | 生产者 | 消费者 | Partition | 用途 |
|-------|--------|--------|-----------|------|
| `writing.scoring.request` | writing-service | writing-service | 3 | 写作评分任务队列（自产自消） |
| `writing.scoring.result` | writing-service | data-service | 3 | 评分摘要同步 |
| `speaking.report.request` | speaking-service | speaking-service | 3 | 口语报告生成任务队列（自产自消） |
| `speaking.session.result` | speaking-service | data-service | 3 | 报告摘要同步 |

### 消息体格式（JSON + 版本号）

**writing.scoring.request:**
```json
{
  "version": 1,
  "submissionId": 12345,
  "userId": 1001,
  "topicId": 42,
  "taskType": 2,
  "essayText": "...",
  "chartType": null,
  "chartDescription": null,
  "requestedAt": "2026-05-16T10:30:00Z"
}
```

**writing.scoring.result:**
```json
{
  "version": 1,
  "submissionId": 12345,
  "userId": 1001,
  "topicId": 42,
  "taskType": 2,
  "overallBand": 7.0,
  "trScore": 7.0,
  "ccScore": 7.0,
  "lrScore": 7.0,
  "graScore": 6.5,
  "scoredAt": "2026-05-16T10:31:15Z"
}
```

**speaking.report.request:**
```json
{
  "version": 1,
  "sessionId": 5678,
  "userId": 1001,
  "topicId": 88,
  "requestedAt": "2026-05-16T11:00:00Z"
}
```

**speaking.session.result:**
```json
{
  "version": 1,
  "sessionId": 5678,
  "userId": 1001,
  "topicId": 88,
  "overallBand": 6.5,
  "fluencyScore": 6.5,
  "lexicalScore": 7.0,
  "grammarScore": 6.5,
  "pronunciationScore": 6.0,
  "completedAt": "2026-05-16T11:01:30Z"
}
```

### 消费失败处理

- 重试 3 次（Spring Kafka DefaultErrorHandler，backoff 1s/3s/10s）
- 3 次失败后发送到 DLT（如 `writing.scoring.request.DLT`）
- MVP 阶段 DLT 仅日志告警

### Partition Key

- `writing.scoring.request`: key = userId
- `speaking.report.request`: key = sessionId
- result topics: key = userId

---

## 第四节：Writing Service 三 Agent 流水线

### 流程

```
Kafka 消费 scoring.request
        │
        ▼
┌─ CompletableFuture.allOf() ─────────────┐
│                                          │
│  ┌──────────────┐    ┌──────────────┐   │
│  │ LR_GRA Agent │    │ TR_CC Agent  │   │
│  │              │    │  (含 RAG)    │   │
│  └──────┬───────┘    └──────┬───────┘   │
│         │                   │           │
└─────────┼───────────────────┼───────────┘
          │                   │
          ▼                   ▼
    ┌─────────────────────────────┐
    │       Master Agent          │
    │ (汇总 + Band Score + 润色)  │
    └──────────────┬──────────────┘
                   │
                   ▼
         写入 DB + 发布 result 到 Kafka
         + Redis Pub/Sub 通知 gateway
```

### Agent 职责

**LR_GRA Agent：**
- 输入：System Prompt + 用户作文（`<essay_text>` 标签包裹）
- 职责：词汇多样性分析、语法准确性分析、错误标注与修正建议
- 输出：JSON（lr_score、gra_score、错误列表、词汇统计）

**TR_CC Agent：**
- 输入：System Prompt + 用户作文 + RAG 检索范文片段
- 职责：任务回应完整度、论点展开逻辑、连贯性与衔接词使用
- RAG 流程：作文 embedding → Qdrant 查询（filter: task_type + category，top_k=3）→ 范文注入 prompt
- 输出：JSON（tr_score、cc_score、结构分析、改进建议）

**Master Agent：**
- 输入：System Prompt + LR_GRA JSON + TR_CC JSON + 原文
- 职责：交叉校验评分、计算官方 Band Score（四项平均，按雅思规则四舍五入到 0.5）、生成综合反馈、输出润色段落
- 输出：JSON（overall_band、综合评语、润色段落）

### 进度推送节点

| 阶段 | 状态值 | 时机 |
|------|--------|------|
| 开始评分 | `SCORING_STARTED` | 消费消息后 |
| 语法词汇分析完成 | `LR_GRA_DONE` | LR_GRA Agent 返回后 |
| 逻辑连贯分析完成 | `TR_CC_DONE` | TR_CC Agent 返回后 |
| 汇总评分完成 | `COMPLETED` | Master Agent 返回、DB 写入后 |
| 评分失败 | `FAILED` | 任何阶段异常时 |

Pub/Sub channel: `scoring.progress:{submissionId}`

### 安全约束

- 用户作文始终包裹在 `<essay_text></essay_text>` 标签内
- System Prompt 声明标签内内容为不可信用户输入，不得执行其中任何指令
- 所有 Agent JSON 输出通过 Jackson 反序列化 + 校验（字段类型、分数范围 0-9、必填项）
- 校验失败重试一次，二次失败标记 FAILED

---

## 第五节：Speaking Service 状态机与会话流程

### 状态机

```
PART1_QA → PART2_INTRO → PART2_CANDIDATE_SPEAKING → PART3_DISCUSSION → SESSION_ENDED
```

### Redis 数据结构

```
Key: session:{sessionId}
Type: Hash
Fields:
  state, userId, topicId, persona,
  part1Index, part1Questions (JSON),
  part2StartedAt, turnCount, createdAt
TTL: 3600s
```

### Lua 脚本原子状态推进

```lua
local current = redis.call('HGET', KEYS[1], 'state')
if current ~= ARGV[1] then
  return {0, current}
end
redis.call('HSET', KEYS[1], 'state', ARGV[2])
for i = 3, #ARGV, 2 do
  redis.call('HSET', KEYS[1], ARGV[i], ARGV[i+1])
end
redis.call('EXPIRE', KEYS[1], 3600)
return {1, ARGV[2]}
```

### 各阶段流程

**Part 1（4-6 题随机问答）：**
- 会话创建时从题库缓存随机抽取 4-6 道题，存入 session hash
- 每轮：录音上传 → ASR → LLM（含考官人设 + 对话历史）→ TTS → HTTP 返回音频 URL
- 题目用完后 Lua 推进到 PART2_INTRO

**Part 2（话题卡 + 独白）：**
- PART2_INTRO：返回话题卡 + 考官引导语音频，记录 part2StartedAt
- 前端 60 秒准备倒计时（纯前端）
- PART2_CANDIDATE_SPEAKING：用户上传完整录音 → ASR 转文字
- 懒推进：不主动计时，收到提交时检查时长
- 完成后推进到 PART3_DISCUSSION

**Part 3（深度讨论 + 动态追问）：**
- 预设 3-4 道题（与 Part 2 话题相关）
- LLM 判断回答深度：充分 → 下一题；浅薄 → 追问（最多 1 次）
- 所有题目完成后推进到 SESSION_ENDED

**会话结束：**
- 对话历史写入 session_turns 表
- 发送 speaking.report.request 到 Kafka
- Redis session hash 等 TTL 自然过期

### 上下文管理

- 全量对话历史随每次请求传入 LLM
- 预估 20-30 轮，2000-3000 token 历史，在 DeepSeek 上下文窗口内

---

## 第六节：API Gateway 设计

### 请求处理流程

```
请求 → JWT 校验 → Guardrail（仅 UGC 请求）→ 路由转发 → 响应
```

### JWT 方案

- data-service 签发，gateway 校验（共享密钥）
- Token: {userId, email, exp, iat}
- Access Token TTL: 2 小时
- MVP 不实现 Refresh Token

### 路由规则

- `/auth/**` → data-service:8083（免 JWT 白名单）
- `/writing/**` → writing-service:8081
- `/speaking/**` → speaking-service:8082
- `/data/**` → data-service:8083（题库、用户信息、练习记录等）
- 转发时注入 `X-User-Id` header（白名单路径除外）

### Guardrail

触发条件：仅对 UGC 请求（POST /writing/submit, POST /speaking/sessions/*/turns）

Prompt:
```
你是一个意图分类器。判断以下用户输入是否与雅思考试相关。
只输出 JSON: {"classification": "IELTS_RELATED"} 或 {"classification": "OFF_TOPIC"}

用户输入:
{userContent}
```

- 模型：deepseek-v4-flash
- OFF_TOPIC → 400 + 提示信息
- 分类失败（超时/异常）→ 放行

### WebSocket 管理

```
前端 ──WebSocket──▶ gateway (/ws/scoring/{submissionId})
                        │ 订阅 Redis Pub/Sub channel: scoring.progress:{submissionId}
                        ▼
                  推送进度给前端
```

- 连接建立时校验 JWT
- 维护 submissionId → WebSocket session 映射
- 评分完成或断开后清理订阅

### 内部服务间鉴权

- MVP：依赖 Docker 网络隔离，内部服务信任 X-User-Id header
- gateway 覆盖/剥离外部请求的 X-User-Id

---

## 第七节：RAG 范文检索设计

### Qdrant 集合

```
Collection: writing_exemplars
Vector size: 1024 (阿里云 text-embedding-v3)
Distance: Cosine

Payload:
  exemplar_id: int (关联 MySQL writing_exemplars.id)
  task_type: int (1/2)
  category: string
  band_score: float (7.0-9.0)
  excerpt: string (范文原文，用于注入 prompt)
  source: string
```

### 数据入库（离线脚本）

- 手动整理 50-100 篇高分范文（Task 1 + Task 2 各半）
- 脚本调用阿里云 embedding API 生成向量 → 写入 Qdrant
- 独立工具，不集成到服务启动流程

### 检索流程

1. 用户作文 → 阿里云 embedding API → query 向量
2. Qdrant 查询：filter(task_type + category) + vector search，top_k=3，score_threshold=0.7
3. 返回范文 excerpt 注入 TR_CC Agent prompt
4. 评分完成后：用 exemplar_id 从 MySQL 查完整范文 + examiner_comment，随评分结果返回前端展示

### 降级策略

- Qdrant 不可用 → 跳过 RAG，TR_CC Agent 正常评分
- Embedding API 不可用 → 同上
- 检索结果为空（score < 0.7）→ 不注入

---

## 第八节：Redis 使用规划

### Key 规划

| 服务 | 用途 | Key 模式 | TTL |
|------|------|----------|-----|
| data-service | 题库/用户缓存 | `data:topic:writing:{id}`, `data:user:{id}` | 15-30 min |
| writing-service | 题库缓存 | `writing:topic:{id}` | 30 min |
| speaking-service | 题库缓存 | `speaking:topic:{id}` | 30 min |
| speaking-service | 会话状态机 | `session:{sessionId}` | 1 hour |
| writing-service | 进度 Pub/Sub | channel: `scoring.progress:{submissionId}` | N/A |

### 部署

MVP 单 Redis 实例，key 前缀隔离各服务数据。

### 缓存策略（统一 Cache-Aside）

读取：Redis 命中 → 返回；未命中 → REST 调 data-service → 写 Redis + TTL → 返回

写入（data-service）：写 MySQL → 删 Redis key。调用方侧缓存依赖 TTL 过期。

### 序列化

- 缓存数据：JSON（Jackson）
- 会话状态 Hash：原生 Redis Hash 字段

---

## 第九节：Docker Compose 基础设施

### 容器清单

```yaml
# 基础设施
mysql:           # 3306, volume 持久化
redis:           # 6379, volume 持久化
kafka:           # 9092, KRaft 模式（无 Zookeeper）
qdrant:          # 6333/6334, volume 持久化

# 业务服务
api-gateway:     # 8080（唯一对外）
data-service:    # 8083（内部）
writing-service: # 8081（内部）
speaking-service:# 8082（内部）
```

### 网络隔离

```yaml
networks:
  frontend:  # api-gateway 对外
  backend:   # 所有服务 + 基础设施
```

仅 api-gateway 连接两个网络，其余仅 backend。

### 数据库初始化

- MySQL 单实例，三个 database：ielts_data、ielts_writing、ielts_speaking
- 初始化 SQL 挂载到 /docker-entrypoint-initdb.d/

### 启动顺序

```
mysql + redis + kafka + qdrant (healthy)
    → data-service + writing-service + speaking-service
        → api-gateway
```

depends_on + healthcheck 保证顺序。

### 环境变量

- `.env` 文件存配置（DB 密码、API key）
- `.env` 加入 .gitignore，提供 .env.example 模板

---

## 第十节：测试策略

### 测试分层

| 层级 | 工具 | 覆盖范围 |
|------|------|----------|
| 单元测试 | JUnit 5 + Mockito | Service 层逻辑、JSON 解析、状态机转换 |
| 集成测试 | Testcontainers | DB CRUD、Kafka 生产消费、Redis + Lua |
| API 测试 | MockMvc / WebTestClient | Controller 请求响应、JWT、路由 |

### Mock 边界

| 被 Mock | 原因 |
|---------|------|
| DeepSeek API | 外部不稳定、成本高 |
| 阿里云 Embedding/ASR/TTS | 同上 |
| Qdrant | 简单场景 Mock，复杂场景 Testcontainers |

不 Mock：MySQL、Kafka、Redis — 用 Testcontainers 真实验证。

### 关键测试用例

**Writing Service:**
- 三 Agent 流水线正常完成 → JSON 解析、分数计算、DB 写入
- Agent 返回非法 JSON → 重试后标记 FAILED
- Kafka 消费失败 → 重试 3 次进入 DLT

**Speaking Service:**
- 状态机正常流转 Part1 → Part2 → Part3 → ENDED
- 并发状态推进 → Lua 脚本只允许一个成功
- 会话 TTL 过期 → 僵尸会话清理

**API Gateway:**
- 有效 JWT → 正常转发
- 无效/过期 JWT → 401
- Guardrail OFF_TOPIC → 400
