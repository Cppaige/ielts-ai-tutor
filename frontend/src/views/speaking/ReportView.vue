<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getReport } from '@/api/speaking'

const route = useRoute()
const router = useRouter()

const report = ref(null)
const loading = ref(true)

const dimensions = [
  { key: 'fluencyScore', label: '流利度', abbr: 'Fluency', color: 'bg-blue-500' },
  { key: 'vocabularyScore', label: '词汇', abbr: 'Vocabulary', color: 'bg-green-500' },
  { key: 'grammarScore', label: '语法', abbr: 'Grammar', color: 'bg-yellow-500' },
  { key: 'pronunciationScore', label: '发音', abbr: 'Pronunciation', color: 'bg-purple-500' },
]

onMounted(async () => {
  try {
    report.value = await getReport(route.params.sessionId)
  } catch {
    // mock 数据
    report.value = {
      sessionId: route.params.sessionId,
      topicTitle: 'Describe your hometown',
      part: 'Part1',
      overallScore: 6.5,
      fluencyScore: 7.0,
      vocabularyScore: 6.5,
      grammarScore: 6.0,
      pronunciationScore: 6.5,
      feedback: 'You demonstrated good fluency throughout the session with minimal hesitation. Your vocabulary range is adequate but could be expanded with more topic-specific words. Grammar was generally accurate with occasional errors in complex structures. Pronunciation was clear and easy to understand. Focus on using more idiomatic expressions and varied sentence structures to push towards Band 7.',
      transcript: [],
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-5 max-w-3xl">
    <div class="flex items-center gap-3">
      <button class="text-sm text-gray-500 hover:text-blue-600" @click="router.back()">← 返回</button>
      <h1 class="text-xl font-semibold text-gray-800">口语评分报告</h1>
    </div>

    <div v-if="loading" class="text-center py-16 text-gray-400 text-sm">加载中...</div>

    <template v-else-if="report">
      <!-- 总分卡片 -->
      <div class="bg-white rounded-xl shadow-sm p-6 flex items-center gap-6">
        <div class="text-center">
          <div class="text-5xl font-bold text-blue-600">{{ report.overallScore }}</div>
          <div class="text-xs text-gray-400 mt-1">Overall Band</div>
        </div>
        <div class="flex-1">
          <p class="text-sm text-gray-600">{{ report.topicTitle }}</p>
          <span class="text-xs text-gray-400 mt-1 inline-block">{{ report.part }}</span>
        </div>
      </div>

      <!-- 四维分数 -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div
          v-for="dim in dimensions"
          :key="dim.key"
          class="bg-white rounded-xl shadow-sm p-4 text-center"
        >
          <div class="text-2xl font-bold text-gray-800">{{ report[dim.key] }}</div>
          <div class="text-xs font-medium text-gray-500 mt-1">{{ dim.label }}</div>
          <div class="text-xs text-gray-400 mt-0.5">{{ dim.abbr }}</div>
          <div class="mt-2 h-1 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all"
              :class="dim.color"
              :style="{ width: `${(report[dim.key] / 9) * 100}%` }"
            />
          </div>
        </div>
      </div>

      <!-- 综合反馈 -->
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h2 class="text-sm font-semibold text-gray-700 mb-3">综合反馈</h2>
        <p class="text-sm text-gray-600 leading-relaxed whitespace-pre-line">{{ report.feedback }}</p>
      </div>

      <!-- 操作按钮 -->
      <div class="flex gap-3">
        <button
          class="bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-5 py-2 text-sm font-medium"
          @click="router.push('/speaking')"
        >
          继续练习
        </button>
        <button
          class="border border-gray-300 text-gray-600 hover:bg-gray-50 rounded-lg px-5 py-2 text-sm"
          @click="router.push('/speaking/history')"
        >
          查看历史
        </button>
      </div>
    </template>
  </div>
</template>
