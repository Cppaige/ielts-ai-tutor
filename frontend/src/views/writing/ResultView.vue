<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSubmission } from '@/api/writing'

const route = useRoute()
const router = useRouter()

const result = ref(null)
const loading = ref(true)

const dimensions = [
  { key: 'trScore', label: 'Task Response', abbr: 'TR', color: 'bg-blue-500' },
  { key: 'ccScore', label: 'Coherence & Cohesion', abbr: 'CC', color: 'bg-green-500' },
  { key: 'lrScore', label: 'Lexical Resource', abbr: 'LR', color: 'bg-yellow-500' },
  { key: 'graScore', label: 'Grammatical Range', abbr: 'GRA', color: 'bg-purple-500' },
]

onMounted(async () => {
  try {
    result.value = await getSubmission(route.params.submissionId)
  } catch {
    // mock 数据
    result.value = {
      submissionId: route.params.submissionId,
      topicTitle: 'Some people think that the best way to reduce crime...',
      totalScore: 6.5,
      trScore: 7.0,
      ccScore: 6.5,
      lrScore: 6.5,
      graScore: 6.0,
      feedback: 'Your essay addresses the task well and presents clear arguments on both sides. The structure is logical with a clear introduction, body paragraphs, and conclusion. To improve, focus on using a wider range of vocabulary and more complex grammatical structures. Some sentences are repetitive in structure. Overall, a solid Band 6.5 response.',
      essay: '',
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
      <h1 class="text-xl font-semibold text-gray-800">写作评分结果</h1>
    </div>

    <div v-if="loading" class="text-center py-16 text-gray-400 text-sm">加载中...</div>

    <template v-else-if="result">
      <!-- 总分卡片 -->
      <div class="bg-white rounded-xl shadow-sm p-6 flex items-center gap-6">
        <div class="text-center">
          <div class="text-5xl font-bold text-blue-600">{{ result.totalScore }}</div>
          <div class="text-xs text-gray-400 mt-1">Overall Band</div>
        </div>
        <div class="flex-1">
          <p class="text-sm text-gray-600 line-clamp-2">{{ result.topicTitle }}</p>
        </div>
      </div>

      <!-- 四维分数 -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div
          v-for="dim in dimensions"
          :key="dim.key"
          class="bg-white rounded-xl shadow-sm p-4 text-center"
        >
          <div class="text-2xl font-bold text-gray-800">{{ result[dim.key] }}</div>
          <div class="text-xs font-medium text-gray-500 mt-1">{{ dim.abbr }}</div>
          <div class="text-xs text-gray-400 mt-0.5">{{ dim.label }}</div>
          <!-- 分数条 -->
          <div class="mt-2 h-1 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all"
              :class="dim.color"
              :style="{ width: `${(result[dim.key] / 9) * 100}%` }"
            />
          </div>
        </div>
      </div>

      <!-- 雷达图占位 -->
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h2 class="text-sm font-semibold text-gray-700 mb-3">能力雷达图</h2>
        <div class="h-48 bg-gray-50 rounded-lg flex items-center justify-center text-gray-400 text-sm border border-dashed border-gray-200">
          雷达图占位（接入图表库后渲染）
        </div>
      </div>

      <!-- 综合反馈 -->
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h2 class="text-sm font-semibold text-gray-700 mb-3">综合反馈</h2>
        <p class="text-sm text-gray-600 leading-relaxed whitespace-pre-line">{{ result.feedback }}</p>
      </div>

      <!-- 操作按钮 -->
      <div class="flex gap-3">
        <button
          class="bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-5 py-2 text-sm font-medium"
          @click="router.push('/writing')"
        >
          继续练习
        </button>
        <button
          class="border border-gray-300 text-gray-600 hover:bg-gray-50 rounded-lg px-5 py-2 text-sm"
          @click="router.push('/writing/history')"
        >
          查看历史
        </button>
      </div>
    </template>
  </div>
</template>
