<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getHistory as getWritingHistory } from '@/api/writing'
import { getHistory as getSpeakingHistory } from '@/api/speaking'

const router = useRouter()
const userStore = useUserStore()

const recentWriting = ref([])
const recentSpeaking = ref([])

onMounted(async () => {
  try {
    const wr = await getWritingHistory({ page: 0, size: 3 })
    recentWriting.value = wr.content || wr || []
  } catch {
    // 后端未就绪时使用 mock 数据
    recentWriting.value = [
      { submissionId: 1, topicTitle: 'Some people think...', totalScore: 6.5, createdAt: '2025-05-20' },
      { submissionId: 2, topicTitle: 'The graph shows...', totalScore: 7.0, createdAt: '2025-05-19' },
    ]
  }
  try {
    const sr = await getSpeakingHistory({ page: 0, size: 3 })
    recentSpeaking.value = sr.content || sr || []
  } catch {
    recentSpeaking.value = [
      { sessionId: 1, topicTitle: 'Describe a place...', overallScore: 6.0, createdAt: '2025-05-18' },
    ]
  }
})

const userName = () => userStore.userInfo?.email?.split('@')[0] || '同学'
</script>

<template>
  <div class="space-y-6">
    <!-- 欢迎语 -->
    <div class="bg-white rounded-xl shadow-sm p-6">
      <h1 class="text-xl font-semibold text-gray-800">你好，{{ userName() }} 👋</h1>
      <p class="text-sm text-gray-500 mt-1">继续你的备考之旅，距离目标分数又近了一步</p>
    </div>

    <!-- Band 分数趋势占位 -->
    <div class="bg-white rounded-xl shadow-sm p-6">
      <h2 class="text-base font-semibold text-gray-700 mb-4">Band 分数趋势</h2>
      <div class="h-40 bg-gray-50 rounded-lg flex items-center justify-center text-gray-400 text-sm border border-dashed border-gray-200">
        图表区域（接入数据后渲染折线图）
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <button
        class="bg-blue-600 text-white hover:bg-blue-700 rounded-xl p-5 text-left"
        @click="router.push('/writing')"
      >
        <div class="text-2xl mb-2">✍️</div>
        <div class="font-semibold">开始写作练习</div>
        <div class="text-sm text-blue-100 mt-1">Task 1 / Task 2 题库</div>
      </button>
      <button
        class="bg-white border border-gray-200 hover:bg-gray-50 rounded-xl p-5 text-left shadow-sm"
        @click="router.push('/speaking')"
      >
        <div class="text-2xl mb-2">🎙️</div>
        <div class="font-semibold text-gray-800">开始口语练习</div>
        <div class="text-sm text-gray-500 mt-1">Part 1 / 2 / 3 模拟考官</div>
      </button>
    </div>

    <!-- 最近写作记录 -->
    <div class="bg-white rounded-xl shadow-sm p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-semibold text-gray-700">最近写作</h2>
        <button class="text-sm text-blue-600 hover:underline" @click="router.push('/writing/history')">
          查看全部
        </button>
      </div>
      <div v-if="recentWriting.length" class="space-y-3">
        <div
          v-for="item in recentWriting"
          :key="item.submissionId"
          class="flex items-center justify-between py-2 border-b border-gray-100 last:border-0 cursor-pointer hover:bg-gray-50 rounded px-2"
          @click="router.push(`/writing/result/${item.submissionId}`)"
        >
          <span class="text-sm text-gray-700 truncate max-w-xs">{{ item.topicTitle }}</span>
          <span class="text-sm font-medium text-blue-600 ml-4">{{ item.totalScore }}</span>
        </div>
      </div>
      <p v-else class="text-sm text-gray-400">暂无写作记录</p>
    </div>

    <!-- 最近口语记录 -->
    <div class="bg-white rounded-xl shadow-sm p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-semibold text-gray-700">最近口语</h2>
        <button class="text-sm text-blue-600 hover:underline" @click="router.push('/speaking/history')">
          查看全部
        </button>
      </div>
      <div v-if="recentSpeaking.length" class="space-y-3">
        <div
          v-for="item in recentSpeaking"
          :key="item.sessionId"
          class="flex items-center justify-between py-2 border-b border-gray-100 last:border-0 cursor-pointer hover:bg-gray-50 rounded px-2"
          @click="router.push(`/speaking/report/${item.sessionId}`)"
        >
          <span class="text-sm text-gray-700 truncate max-w-xs">{{ item.topicTitle }}</span>
          <span class="text-sm font-medium text-blue-600 ml-4">{{ item.overallScore }}</span>
        </div>
      </div>
      <p v-else class="text-sm text-gray-400">暂无口语记录</p>
    </div>
  </div>
</template>
