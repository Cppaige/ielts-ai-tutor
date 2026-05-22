<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createSession, submitTurn } from '@/api/speaking'
import { useToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const sessionId = ref(null)
const messages = ref([])
const currentPart = ref(route.query.part || 'Part1')
const partProgress = ref({ current: 1, total: 3 })
const loading = ref(true)
const recording = ref(false)
const uploading = ref(false)

// 文本输入模式
const inputMode = ref('audio') // 'audio' | 'text'
const textInput = ref('')

// Part2 倒计时（纯前端，60 秒准备时间）
const part2Timer = ref(0)
const part2Active = ref(false)
let countdownInterval = null

// MediaRecorder 相关
let mediaRecorder = null
let audioChunks = []

onMounted(async () => {
  try {
    const res = await createSession({
      topicId: route.query.topicId,
      persona: route.query.persona || 'encouraging',
      part: currentPart.value,
    })
    sessionId.value = res.sessionId
    if (res.firstQuestion) {
      messages.value.push({ role: 'examiner', text: res.firstQuestion, audioUrl: res.audioUrl })
    }
  } catch {
    // mock 会话
    sessionId.value = 'mock-session-001'
    messages.value.push({
      role: 'examiner',
      text: "Good morning! Let's start with Part 1. Can you tell me about your hometown?",
      audioUrl: null,
    })
    if (currentPart.value === 'Part2') startPart2Timer()
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  stopRecording()
  if (countdownInterval) clearInterval(countdownInterval)
})

function startPart2Timer() {
  part2Active.value = true
  part2Timer.value = 60
  countdownInterval = setInterval(() => {
    part2Timer.value--
    if (part2Timer.value <= 0) {
      clearInterval(countdownInterval)
      part2Active.value = false
    }
  }, 1000)
}

// ── 音频模式 ──────────────────────────────────────────────
async function startRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioChunks = []
    // TODO: 根据后端需要调整 mimeType（如 audio/webm 或 audio/ogg）
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    mediaRecorder.onstop = handleRecordingStop
    mediaRecorder.start()
    recording.value = true
  } catch {
    toast.error('无法访问麦克风，请检查权限')
  }
}

function stopRecording() {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
    mediaRecorder.stream.getTracks().forEach((t) => t.stop())
  }
  recording.value = false
}

async function handleRecordingStop() {
  const blob = new Blob(audioChunks, { type: 'audio/webm' })
  const localUrl = URL.createObjectURL(blob)
  messages.value.push({ role: 'user', text: '（语音回答）', audioUrl: localUrl })

  // 音频转 base64 后放进 JSON body 的 audioData 字段
  const arrayBuffer = await blob.arrayBuffer()
  const uint8 = new Uint8Array(arrayBuffer)
  const base64 = btoa(String.fromCharCode(...uint8))

  await doSubmit({ audioData: base64 })
}

function toggleRecording() {
  if (recording.value) {
    stopRecording()
  } else {
    startRecording()
  }
}

// ── 文本模式 ──────────────────────────────────────────────
async function submitText() {
  const text = textInput.value.trim()
  if (!text) return
  messages.value.push({ role: 'user', text, audioUrl: null })
  textInput.value = ''
  await doSubmit({ textInput: text })
}

// ── 公共提交逻辑（文本和音频都走这里，后端统一做意图识别）──
async function doSubmit(payload) {
  uploading.value = true
  try {
    const res = await submitTurn(sessionId.value, payload)
    const examinerText = res.examinerResponse || res.nextQuestion
    if (examinerText) {
      messages.value.push({ role: 'examiner', text: examinerText, audioUrl: res.audioUrl || null })
    }
    if (res.progress) partProgress.value = res.progress
    if (res.sessionEnded || res.finished) {
      toast.success('口语练习完成！')
      setTimeout(() => router.push(`/speaking/report/${sessionId.value}`), 1000)
    }
  } catch (err) {
    if (err.response?.status === 400) {
      toast.error('回答内容不符合雅思口语要求，请重新作答')
      // 移除刚加入的用户气泡
      messages.value.pop()
    } else {
      // mock 下一个问题（后端未就绪时）
      messages.value.push({
        role: 'examiner',
        text: "That's interesting! Now, do you prefer living in a city or the countryside? Why?",
        audioUrl: null,
      })
    }
  } finally {
    uploading.value = false
  }
}

function endSession() {
  router.push(`/speaking/report/${sessionId.value || 'mock-session-001'}`)
}

const timerColor = computed(() => {
  if (part2Timer.value > 30) return 'text-green-600'
  if (part2Timer.value > 10) return 'text-yellow-600'
  return 'text-red-600'
})
</script>

<template>
  <div class="flex flex-col h-full max-w-2xl mx-auto space-y-4">
    <!-- 顶部状态栏 -->
    <div class="bg-white rounded-xl shadow-sm px-5 py-3 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <span class="text-sm font-medium text-gray-700">{{ currentPart }}</span>
        <span class="text-xs text-gray-400">问题 {{ partProgress.current }} / {{ partProgress.total }}</span>
      </div>
      <div v-if="part2Active" class="flex items-center gap-2">
        <span class="text-xs text-gray-500">准备时间</span>
        <span class="text-lg font-bold" :class="timerColor">{{ part2Timer }}s</span>
      </div>
      <button class="text-xs text-gray-400 hover:text-red-500" @click="endSession">
        结束练习
      </button>
    </div>

    <!-- 对话区域 -->
    <div v-if="loading" class="flex-1 flex items-center justify-center text-gray-400 text-sm">
      正在连接考官...
    </div>

    <div v-else class="flex-1 bg-white rounded-xl shadow-sm p-5 overflow-y-auto space-y-4 min-h-64">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="flex"
        :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
      >
        <div
          class="max-w-[80%] rounded-xl px-4 py-3 text-sm leading-relaxed"
          :class="msg.role === 'examiner' ? 'bg-gray-100 text-gray-800' : 'bg-blue-600 text-white'"
        >
          <p>{{ msg.text }}</p>
          <audio v-if="msg.audioUrl" :src="msg.audioUrl" controls class="mt-2 w-full h-8" />
        </div>
      </div>

      <div v-if="uploading" class="flex justify-start">
        <div class="bg-gray-100 rounded-xl px-4 py-3 text-sm text-gray-400 animate-pulse">
          考官正在思考...
        </div>
      </div>
    </div>

    <!-- 输入模式切换 + 控制区 -->
    <div class="bg-white rounded-xl shadow-sm px-5 py-4 space-y-3">
      <!-- 模式切换 -->
      <div class="flex gap-2 justify-center">
        <button
          class="px-3 py-1 rounded-full text-xs font-medium transition-colors"
          :class="inputMode === 'audio' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'"
          @click="inputMode = 'audio'"
        >
          🎙 语音
        </button>
        <button
          class="px-3 py-1 rounded-full text-xs font-medium transition-colors"
          :class="inputMode === 'text' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'"
          @click="inputMode = 'text'"
        >
          ⌨️ 文字
        </button>
      </div>

      <!-- 音频模式 -->
      <div v-if="inputMode === 'audio'" class="flex items-center justify-center gap-4">
        <button
          :disabled="uploading || loading"
          class="flex items-center gap-2 rounded-full px-6 py-3 text-sm font-medium transition-all disabled:opacity-40"
          :class="recording
            ? 'bg-red-500 text-white hover:bg-red-600 animate-pulse'
            : 'bg-blue-600 text-white hover:bg-blue-700'"
          @click="toggleRecording"
        >
          {{ recording ? '⏹ 停止录音' : '🎙 开始录音' }}
        </button>
        <p class="text-xs text-gray-400">
          {{ recording ? '录音中，点击停止并上传' : '点击开始回答' }}
        </p>
      </div>

      <!-- 文字模式 -->
      <div v-else class="flex gap-2">
        <input
          v-model="textInput"
          type="text"
          placeholder="用英文输入你的回答..."
          :disabled="uploading || loading"
          class="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-40"
          @keydown.enter="submitText"
        />
        <button
          :disabled="uploading || loading || !textInput.trim()"
          class="bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-4 py-2 text-sm font-medium disabled:opacity-40"
          @click="submitText"
        >
          发送
        </button>
      </div>
    </div>
  </div>
</template>
