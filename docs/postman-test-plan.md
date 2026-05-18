# IELTS Backend Postman 测试计划

## 环境配置

在 Postman 中创建一个 Environment，设置以下变量：

| 变量 | 值 |
|---|---|
| `base_url` | `http://localhost:8080` |
| `token` | 登录后填入 |
| `userId` | 登录后填入 |

所有接口统一前缀 `{{base_url}}`。认证接口之后的所有请求都需添加 Header：`Authorization: Bearer {{token}}`。

---

## 第一步：认证测试（无需 Token）

### 1.1 注册

> 邮箱号有正确格式，密码要大小写中英文，修改后端部分

> ```json
> // 邮箱有人注
> {
>     "code": 409,
>     "message": "Email already registered"
> }
> ```
>
> 

| 项目 | 值 |
|---|---|
| Method | `POST` |
| URL | `http://localhost:8080/auth/register` |
| Headers | `Content-Type: application/json` |
| Body (JSON) | `{"email": "test@example.com", "password": "123456", "nickname": "TestUser"}` |

**预期响应：**
```json
{"code": 200, "message": "success", "data": 1}
```

**测试用例：**
- 正常注册 → 返回新用户 ID
- 重复邮箱注册 → 返回业务错误（code != 200）

---

### 1.2 登录

| 项目 | 值 |
|---|---|
| Method | `POST` |
| URL | `http://localhost:8080/auth/login` |
| Headers | `Content-Type: application/json` |
| Body (JSON) | `{"email": "test@example.com", "password": "123456"}` |

**预期响应：**
```json
{"code": 200, "message": "success", "data": {"token": "eyJ...", "userId": 1, "email": "test@example.com"}}
```

**测试用例：**
- 正确邮箱密码 → 返回 token、userId、email
- 错误密码 → 返回业务错误

> 拿到 token 后，更新 Postman 环境变量 `token` 和 `userId`。

---

## 第二步：认证边界测试

| 测试场景 | Method | URL | Headers | 预期结果 |
|---|---|---|---|---|
| 不带 Token | `GET` | `http://localhost:8080/data/writing-topics` | 无 Authorization | 401 Unauthorized |
| 无效 Token | `GET` | `http://localhost:8080/data/writing-topics` | `Authorization: Bearer invalidtoken` | 401 Unauthorized |
| 有效 Token | `GET` | `http://localhost:8080/data/writing-topics` | `Authorization: Bearer {{token}}` | 200 正常返回 |

---

## 第三步：口语话题查询

### 3.1 按 Part 查询口语话题列表

| 项目 | 值 |
|---|---|
| Method | `GET` |
| URL | `http://localhost:8080/data/speaking-topics` |
| Headers | `Authorization: Bearer {{token}}` |
| Query Params | `part`（必填，1/2/3），`category`（可选） |

**测试用例：**

| 场景 | URL | 预期 |
|---|---|---|
| Part 1 话题 | `http://localhost:8080/data/speaking-topics?part=1` | 返回 Part 1 题目列表 |
| Part 2 话题（含 cue card） | `http://localhost:8080/data/speaking-topics?part=2` | 返回 Part 2 题目列表 |
| Part 3 按分类过滤 | `http://localhost:8080/data/speaking-topics?part=3&category=Education` | 返回 Education 分类的 Part 3 题目 |
| 不传 part | `http://localhost:8080/data/speaking-topics` | 400 或业务错误 |

---

### 3.2 获取单个口语话题详情

| 项目 | 值 |
|---|---|
| Method | `GET` |
| URL | `http://localhost:8080/data/speaking-topics/1` |
| Headers | `Authorization: Bearer {{token}}` |

**测试用例：**
- `id=1` → 返回对应话题详情
- `id=99999`（不存在）→ 抛出异常

---

## 第四步：写作话题查询

### 4.1 获取写作话题列表

| 项目 | 值 |
|---|---|
| Method | `GET` |
| URL | `http://localhost:8080/data/writing-topics` |
| Headers | `Authorization: Bearer {{token}}` |
| Query Params | `taskType`（可选，1=Task1 图表题，2=Task2 议论文） |

**测试用例：**

| 场景 | URL | 预期 |
|---|---|---|
| 查询全部 | `http://localhost:8080/data/writing-topics` | 返回所有写作话题 |
| Task 1 图表题 | `http://localhost:8080/data/writing-topics?taskType=1` | 只返回 Task 1（条形图、折线图、饼图等） |
| Task 2 议论文 | `http://localhost:8080/data/writing-topics?taskType=2` | 只返回 Task 2（观点论证、原因/解决方案等） |

---

### 4.2 获取单个写作话题详情

| 项目 | 值 |
|---|---|
| Method | `GET` |
| URL | `http://localhost:8080/data/writing-topics/1` |
| Headers | `Authorization: Bearer {{token}}` |

---

## 第五步：写作提交与查询

### 5.1 提交写作（Task 1 — 图表题）

| 项目 | 值 |
|---|---|
| Method | `POST` |
| URL | `http://localhost:8080/writing/submit` |
| Headers | `Authorization: Bearer {{token}}` |
| Body (JSON) | 见下方 |

```json
{
  "topicId": 1,
  "taskType": 1,
  "essayText": "The bar chart illustrates the number of tourists visiting three countries between 2000 and 2020. Overall, all three countries experienced an upward trend...",
  "chartType": "bar",
  "chartDescription": "Tourist visits by country, 2000-2020"
}
```

**预期响应：**
```json
{"code": 200, "message": "success", "data": 1}
```

---

### 5.2 提交写作（Task 2 — 议论文）

| 项目 | 值 |
|---|---|
| Method | `POST` |
| URL | `http://localhost:8080/writing/submit` |
| Headers | `Authorization: Bearer {{token}}` |
| Body (JSON) | 见下方 |

```json
{
  "topicId": 6,
  "taskType": 2,
  "essayText": "Some people believe that university education should be free for everyone. In my opinion, while free education has clear benefits, a completely tuition-free system presents significant challenges...",
  "chartType": null,
  "chartDescription": null
}
```

> **注意**：`chartType` 和 `chartDescription` 在 Task 2 时为 `null`。

---

### 5.3 查询写作提交结果

| 项目 | 值 |
|---|---|
| Method | `GET` |
| URL | `http://localhost:8080/writing/submissions/{submissionId}` |
| Headers | `Authorization: Bearer {{token}}` |

> 评分是异步的（Kafka 消息驱动），提交后可能需要等待数秒。关注响应中 `status` 字段的状态流转：`PENDING` → `SCORING` → `COMPLETED`（或 `FAILED`）。

**COMPLETED 状态时的响应包含评分详情：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "userId": 1,
    "topicId": 1,
    "taskType": 1,
    "essayText": "...",
    "status": "COMPLETED",
    "trScore": 6.5,
    "ccScore": 7.0,
    "lrScore": 6.0,
    "graScore": 6.5,
    "overallBand": 6.5,
    "masterFeedback": { ... },
    "createdAt": "...",
    "scoredAt": "..."
  }
}
```

---

### 5.4 Guardrail 内容审核测试

| 场景 | Body 示例 | 预期结果 |
|---|---|---|
| 雅思相关内容 | `"essayText": "The chart shows the percentage of..."` | 正常返回 submissionId |
| 无关内容 | `"essayText": "今天天气真好，我们去春游吧"` | 400 OFF_TOPIC |

---

## 第六步：口语会话

### 6.1 开始口语会话

| 项目 | 值 |
|---|---|
| Method | `POST` |
| URL | `http://localhost:8080/speaking/sessions` |
| Headers | `Authorization: Bearer {{token}}` |
| Body (JSON) | 见下方 |

```json
{
  "topicId": 1,
  "persona": "ENCOURAGING"
}
```

`persona` 可选值：
- `ENCOURAGING` — 鼓励型考官，语气友好
- `STRICT` — 严格型考官，语气正式

**预期响应：**
```json
{"code": 200, "message": "success", "data": 1}
```

---

### 6.2 提交对话轮次（跳过语音识别）

| 项目 | 值 |
|---|---|
| Method | `POST` |
| URL | `http://localhost:8080/speaking/sessions/{sessionId}/turns` |
| Headers | `Authorization: Bearer {{token}}` |
| Body (JSON) | 见下方 |

```json
{
  "audioData": null,
  "transcriptOverride": "I'm from Beijing. I work as a software engineer at a tech company."
}
```

> `transcriptOverride` 非空时直接使用文本作为考生回答（跳过 ASR）。`audioData` 传 `null`。

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "candidateTranscript": "I'm from Beijing. I work as a software engineer at a tech company.",
    "examinerResponse": "That's interesting! Could you tell me more about what your job involves?",
    "examinerAudioUrl": "https://...",
    "currentState": "PART1",
    "sessionEnded": false
  }
}
```

**字段说明：**

| 字段 | 说明 |
|---|---|
| `candidateTranscript` | 最终使用的考生回答文本 |
| `examinerResponse` | 考官（LLM）生成的回复文本 |
| `examinerAudioUrl` | 考官回复的 TTS 音频地址 |
| `currentState` | 当前阶段：`PART1` → `PART2` → `PART3` → `COMPLETED` |
| `sessionEnded` | 会话是否已结束 |

---

### 6.3 完整多轮对话测试

连续调用 `/speaking/sessions/{sessionId}/turns` 多次，每次传入不同的考生回答，观察状态流转：

| 轮次 | 输入示例 | 预期状态 |
|---|---|---|
| 第 1 轮 | `"transcriptOverride": "My name is Li Ming. I come from Shanghai."` | `PART1`, `sessionEnded: false` |
| 第 2 轮 | `"transcriptOverride": "I work as a teacher. I've been teaching English for five years."` | `PART1`, `sessionEnded: false` |
| 第 3 轮 | `"transcriptOverride": "Shanghai is a vibrant city with a rich history and modern skyline."` | `PART1` 或 `PART2` |
| ...继续直到 | `sessionEnded` 变为 `true`，`currentState` 变为 `COMPLETED` |

> 测试重点：验证状态机按 `PART1 → PART2 → PART3 → COMPLETED` 顺序流转，考官回复与考生回答语义相关。

---

### 6.4 Guardrail 内容审核测试（口语）

| 场景 | Body | 预期结果 |
|---|---|---|
| 雅思口试相关回答 | `"transcriptOverride": "I enjoy reading books in my free time..."` | 正常进入对话 |
| 完全无关内容 | 测试开始会话时 `topicId` 指向无效话题或内容 | 可能触发 400 |

---

## 第七步：练习记录查询

### 7.1 查询全部练习记录

| 项目 | 值 |
|---|---|
| Method | `GET` |
| URL | `http://localhost:8080/data/practice-records` |
| Headers | `Authorization: Bearer {{token}}` |

**预期响应：** 分页返回用户的所有练习记录（口语 + 写作）。

---

### 7.2 按类型分页查询

| 场景 | URL | 预期 |
|---|---|---|
| 只查写作记录 | `http://localhost:8080/data/practice-records?type=WRITING&page=0&size=10` | 返回写作记录分页 |
| 只查口语记录 | `http://localhost:8080/data/practice-records?type=SPEAKING&page=0&size=10` | 返回口语记录分页 |

---

## 第八步：WebSocket 测试（评分进度推送）

| 项目 | 值 |
|---|---|
| 连接 URL | `ws://localhost:8080/ws/scoring/{submissionId}` |

**测试步骤：**

1. 在 Postman 中新建 WebSocket 请求，连接 `ws://localhost:8080/ws/scoring/1`
2. 连接成功后，在另一个 Tab 提交一篇写作（POST `http://localhost:8080/writing/submit`）
3. 观察 WebSocket 是否收到评分进度消息

> 注意：需要先知道 submissionId，可以在提交成功后用返回的 ID 连接。

---

## 完整测试流程总结

```
注册 → 登录（获取 token）
    │
    ├── 认证边界测试（无 token / 无效 token）
    │
    ├── 口语话题查询（/data/speaking-topics?part=1,2,3）
    │
    ├── 写作话题查询（/data/writing-topics?taskType=1,2）
    │
    ├── 写作流程
    │   ├── POST /writing/submit（Task 1 / Task 2）
    │   ├── GET /writing/submissions/{id}（轮询直到 COMPLETED）
    │   └── Guardrail 测试（雅思 vs 无关内容）
    │
    ├── 口语流程
    │   ├── POST /speaking/sessions（开始会话）
    │   ├── POST /speaking/sessions/{id}/turns（多轮对话 × N）
    │   └── 观察 PART1 → PART2 → PART3 → COMPLETED
    │
    ├── 练习记录查询（/data/practice-records）
    │
    └── WebSocket 测试（ws://localhost:8080/ws/scoring/{id}）
```

---

## 常见 HTTP 状态码

| 状态码 | 含义 |
|---|---|
| 200 | 成功（响应体 `code` 也为 200） |
| 400 | Guardrail 拦截（OFF_TOPIC）或请求参数错误 |
| 401 | 未认证（缺少 Token 或 Token 无效） |
| 502 | 下游服务不可用 |
