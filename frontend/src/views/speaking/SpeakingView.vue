<template>
  <div class="speaking-page">
    <h1 class="page-title">口语练习</h1>
    <p class="page-subtitle">与 AI 考官进行实时对话，模拟真实雅思口语考试</p>

    <div v-if="!sessionStarted" class="start-section">
      <div class="persona-select">
        <h3>选择考官风格</h3>
        <div class="persona-cards">
          <div
            class="persona-card"
            :class="{ active: persona === 'ENCOURAGING' }"
            @click="persona = 'ENCOURAGING'"
          >
            <div class="persona-avatar encouraging">E</div>
            <div class="persona-info">
              <span class="persona-name">鼓励型考官</span>
              <span class="persona-desc">温和友善，适合建立信心</span>
            </div>
          </div>
          <div
            class="persona-card"
            :class="{ active: persona === 'STRICT' }"
            @click="persona = 'STRICT'"
          >
            <div class="persona-avatar strict">S</div>
            <div class="persona-info">
              <span class="persona-name">严格型考官</span>
              <span class="persona-desc">标准严格，模拟真实考试</span>
            </div>
          </div>
        </div>
      </div>
      <el-button type="primary" size="large" :loading="starting" @click="startSession">
        开始口语练习
      </el-button>
    </div>

    <div v-else class="session-section">
      <div class="chat-container" ref="chatContainer">
        <div
          v-for="(turn, idx) in turns"
          :key="idx"
          class="chat-bubble"
          :class="turn.role === 'EXAMINER' ? 'examiner' : 'candidate'"
        >
          <div class="bubble-avatar">
            {{ turn.role === 'EXAMINER' ? 'E' : 'Me' }}
          </div>
          <div class="bubble-content">
            <span class="bubble-role">{{ turn.role === 'EXAMINER' ? '考官' : '你' }}</span>
            <p>{{ turn.content }}</p>
          </div>
        </div>
        <div v-if="waitingResponse" class="chat-bubble examiner">
          <div class="bubble-avatar">E</div>
          <div class="bubble-content">
            <span class="bubble-role">考官</span>
            <p class="typing-indicator">
              <span></span><span></span><span></span>
            </p>
          </div>
        </div>
      </div>

      <div v-if="!sessionEnded" class="input-area">
        <el-input
          v-model="userInput"
          type="textarea"
          :rows="3"
          placeholder="输入你的回答（模拟语音转文字）..."
          @keydown.ctrl.enter="sendTurn"
        />
        <div class="input-actions">
          <span class="input-hint">Ctrl + Enter 发送</span>
          <el-button type="primary" :loading="sending" :disabled="!userInput.trim()" @click="sendTurn">
            发送
          </el-button>
        </div>
      </div>

      <div v-else class="session-ended">
        <div v-if="report" class="report-card">
          <h3>口语评分报告</h3>
          <div class="report-scores">
            <div class="report-overall">
              <div class="band-circle">
                <span class="band-number">{{ report.overallBand }}</span>
              </div>
              <span class="band-label">Overall Band</span>
            </div>
            <div class="report-details">
              <div class="report-item">
                <span>Fluency & Coherence</span>
                <el-progress :percentage="(report.fluencyScore / 9) * 100" :format="() => report.fluencyScore" color="#2563EB" />
              </div>
              <div class="report-item">
                <span>Lexical Resource</span>
                <el-progress :percentage="(report.lexicalScore / 9) * 100" :format="() => report.lexicalScore" color="#7C3AED" />
              </div>
              <div class="report-item">
                <span>Grammar</span>
                <el-progress :percentage="(report.grammarScore / 9) * 100" :format="() => report.grammarScore" color="#059669" />
              </div>
              <div class="report-item">
                <span>Pronunciation</span>
                <el-progress :percentage="(report.pronunciationScore / 9) * 100" :format="() => report.pronunciationScore" color="#D97706" />
              </div>
            </div>
          </div>
        </div>
        <div v-else class="waiting-report">
          <el-icon class="scoring-spinner" :size="32"><Loading /></el-icon>
          <p>正在生成评分报告...</p>
        </div>
        <el-button type="primary" size="large" @click="resetSession">再练一次</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { startSpeakingSession, sendSpeakingTurn } from '../../api'

const persona = ref('ENCOURAGING')
const sessionStarted = ref(false)
const sessionEnded = ref(false)
const starting = ref(false)
const sending = ref(false)
const waitingResponse = ref(false)
const userInput = ref('')
const sessionId = ref(0)
const turns = ref<Array<{ role: string; content: string }>>([])
const report = ref<any>(null)
const chatContainer = ref<HTMLElement>()

async function startSession() {
  starting.value = true
  try {
    const { data } = await startSpeakingSession(1, persona.value)
    sessionId.value = data.data
    sessionStarted.value = true
    // Send initial turn to get examiner's first question
    await getExaminerResponse()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '启动失败')
  } finally {
    starting.value = false
  }
}

async function sendTurn() {
  if (!userInput.value.trim() || sending.value) return

  const text = userInput.value.trim()
  turns.value.push({ role: 'CANDIDATE', content: text })
  userInput.value = ''
  scrollToBottom()

  await getExaminerResponse(text)
}

async function getExaminerResponse(transcript?: string) {
  sending.value = true
  waitingResponse.value = true
  scrollToBottom()

  try {
    const { data } = await sendSpeakingTurn(sessionId.value, transcript || '')
    const response = data.data
    waitingResponse.value = false

    if (response.examinerReply) {
      turns.value.push({ role: 'EXAMINER', content: response.examinerReply })
    }

    if (response.sessionEnded) {
      sessionEnded.value = true
      if (response.report) {
        report.value = response.report
      }
    }

    scrollToBottom()
  } catch (e: any) {
    waitingResponse.value = false
    ElMessage.error(e.response?.data?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

function resetSession() {
  sessionStarted.value = false
  sessionEnded.value = false
  turns.value = []
  report.value = null
  userInput.value = ''
}
</script>

<style scoped>
.speaking-page {
  max-width: 800px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #64748b;
  margin-bottom: 32px;
}

.start-section {
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.persona-select h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.persona-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.persona-card {
  border: 2px solid var(--color-border);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.persona-card:hover {
  border-color: var(--color-primary);
}

.persona-card.active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.persona-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #fff;
  font-size: 18px;
}

.persona-avatar.encouraging { background: #059669; }
.persona-avatar.strict { background: #DC2626; }

.persona-info {
  display: flex;
  flex-direction: column;
}

.persona-name {
  font-weight: 600;
  font-size: 15px;
}

.persona-desc {
  font-size: 13px;
  color: #64748b;
}

.chat-container {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  max-height: 500px;
  overflow-y: auto;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  margin-bottom: 16px;
}

.chat-bubble {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.chat-bubble.candidate {
  flex-direction: row-reverse;
}

.bubble-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.examiner .bubble-avatar {
  background: var(--color-primary);
  color: #fff;
}

.candidate .bubble-avatar {
  background: var(--color-muted);
  color: var(--color-foreground);
}

.bubble-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
}

.examiner .bubble-content {
  background: var(--color-muted);
}

.candidate .bubble-content {
  background: var(--color-primary);
  color: #fff;
}

.bubble-role {
  font-size: 12px;
  font-weight: 500;
  opacity: 0.7;
  display: block;
  margin-bottom: 4px;
}

.bubble-content p {
  font-size: 14px;
  line-height: 1.6;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-4px); }
}

.input-area {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.input-hint {
  font-size: 12px;
  color: #94a3b8;
}

.session-ended {
  text-align: center;
  margin-top: 24px;
}

.report-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
  text-align: left;
}

.report-card h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 24px;
  text-align: center;
}

.report-scores {
  display: flex;
  gap: 40px;
  align-items: center;
}

.report-overall {
  text-align: center;
  flex-shrink: 0;
}

.band-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563EB, #7C3AED);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
}

.band-number {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
}

.band-label {
  font-size: 12px;
  color: #64748b;
}

.report-details {
  flex: 1;
  display: grid;
  gap: 16px;
}

.report-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.report-item span:first-child {
  width: 160px;
  font-size: 13px;
  font-weight: 500;
  flex-shrink: 0;
}

.report-item .el-progress {
  flex: 1;
}

.waiting-report {
  padding: 40px;
}

.scoring-spinner {
  color: var(--color-primary);
  animation: spin 1.5s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.waiting-report p {
  color: #64748b;
  margin-bottom: 24px;
}
</style>
