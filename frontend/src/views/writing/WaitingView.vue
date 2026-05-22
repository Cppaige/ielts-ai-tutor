<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 评分阶段定义
const stages = [
  { key: 'SCORING_STARTED', label: '开始评分' },
  { key: 'LR_GRA_DONE', label: '词汇语法分析完成' },
  { key: 'TR_CC_DONE', label: '任务回应连贯分析完成' },
  { key: 'COMPLETED', label: '评分完成' },
]

const currentStage = ref(0)
const failed = ref(false)
let ws = null
let mockTimer = null

function connectWebSocket() {
  const submissionId = route.params.submissionId

  // TODO: 后端就绪后替换为真实 WebSocket 连接
  // ws = new WebSocket(`ws://localhost:8080/ws/scoring/${submissionId}`)
  // ws.onmessage = (event) => {
  //   const data = JSON.parse(event.data)
  //   const idx = stages.findIndex(s => s.key === data.status)
  //   if (idx !== -1) currentStage.value = idx
  //   if (data.status === 'COMPLETED') {
  //     setTimeout(() => router.push(`/writing/result/${submissionId}`), 800)
  //   }
  //   if (data.status === 'FAILED') failed.value = true
  // }

  // 模拟进度推进（后端就绪后删除此段）
  let step = 0
  mockTimer = setInterval(() => {
    step++
    currentStage.value = step
    if (step >= stages.length - 1) {
      clearInterval(mockTimer)
      setTimeout(() => router.push(`/writing/result/${submissionId}`), 800)
    }
  }, 1800)
}

onMounted(connectWebSocket)

onUnmounted(() => {
  if (ws) ws.close()
  if (mockTimer) clearInterval(mockTimer)
})
</script>

<template>
  <div class="flex items-center justify-center min-h-[60vh]">
    <div class="bg-white rounded-xl shadow-sm p-10 w-full max-w-md text-center">
      <div v-if="failed">
        <div class="text-4xl mb-4">❌</div>
        <h2 class="text-lg font-semibold text-gray-800 mb-2">评分失败</h2>
        <p class="text-sm text-gray-500 mb-6">请稍后重试或联系客服</p>
        <button
          class="bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-5 py-2 text-sm"
          @click="router.push('/writing')"
        >
          返回题库
        </button>
      </div>

      <div v-else>
        <div class="text-4xl mb-4 animate-spin inline-block">⏳</div>
        <h2 class="text-lg font-semibold text-gray-800 mb-1">AI 正在评分中</h2>
        <p class="text-sm text-gray-400 mb-8">请稍候，通常需要 10-30 秒</p>

        <!-- 5 阶段进度条 -->
        <div class="space-y-3">
          <div
            v-for="(stage, idx) in stages"
            :key="stage.key"
            class="flex items-center gap-3"
          >
            <!-- 状态图标 -->
            <div
              class="w-6 h-6 rounded-full flex items-center justify-center text-xs shrink-0"
              :class="{
                'bg-blue-600 text-white': idx < currentStage,
                'bg-blue-100 text-blue-600 animate-pulse': idx === currentStage,
                'bg-gray-100 text-gray-400': idx > currentStage,
              }"
            >
              <span v-if="idx < currentStage">✓</span>
              <span v-else-if="idx === currentStage">●</span>
              <span v-else>○</span>
            </div>
            <!-- 阶段标签 -->
            <span
              class="text-sm"
              :class="{
                'text-gray-800 font-medium': idx <= currentStage,
                'text-gray-400': idx > currentStage,
              }"
            >
              {{ stage.label }}
            </span>
          </div>
        </div>

        <!-- 整体进度条 -->
        <div class="mt-6 h-1.5 bg-gray-100 rounded-full overflow-hidden">
          <div
            class="h-full bg-blue-600 rounded-full transition-all duration-700"
            :style="{ width: `${(currentStage / (stages.length - 1)) * 100}%` }"
          />
        </div>
      </div>
    </div>
  </div>
</template>
