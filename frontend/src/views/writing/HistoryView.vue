<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHistory } from '@/api/writing'

const router = useRouter()

const history = ref([])
const loading = ref(true)
const page = ref(0)
const hasMore = ref(true)

async function fetchHistory() {
  try {
    const res = await getHistory({ page: page.value, size: 10 })
    const items = res.content || res || []
    history.value.push(...items)
    hasMore.value = items.length === 10
  } catch {
    // mock
    history.value = [
      { submissionId: 1, topicTitle: 'Some people think that the best way to reduce crime is to give longer prison sentences.', totalScore: 6.5, createdAt: '2025-05-20T10:30:00' },
      { submissionId: 2, topicTitle: 'The graph below shows the number of university graduates in Canada from 1992 to 2007.', totalScore: 7.0, createdAt: '2025-05-19T14:20:00' },
      { submissionId: 3, topicTitle: 'In many countries, the proportion of older people is steadily increasing.', totalScore: 6.0, createdAt: '2025-05-18T09:15:00' },
    ]
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

onMounted(fetchHistory)

function loadMore() {
  page.value++
  fetchHistory()
}

function formatDate(dateStr) {
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function scoreColor(score) {
  if (score >= 7) return 'text-green-600'
  if (score >= 6) return 'text-blue-600'
  return 'text-yellow-600'
}
</script>

<template>
  <div class="space-y-5 max-w-3xl">
    <h1 class="text-xl font-semibold text-gray-800">写作历史</h1>

    <div v-if="loading" class="text-center py-16 text-gray-400 text-sm">加载中...</div>

    <div v-else-if="history.length" class="space-y-3">
      <div
        v-for="item in history"
        :key="item.submissionId"
        class="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-shadow"
        @click="router.push(`/writing/result/${item.submissionId}`)"
      >
        <div class="flex items-start justify-between gap-4">
          <p class="text-sm text-gray-700 leading-relaxed line-clamp-2 flex-1">
            {{ item.topicTitle }}
          </p>
          <span class="text-2xl font-bold shrink-0" :class="scoreColor(item.totalScore)">
            {{ item.totalScore }}
          </span>
        </div>
        <div class="mt-3 flex items-center justify-between">
          <span class="text-xs text-gray-400">{{ formatDate(item.createdAt) }}</span>
          <span class="text-xs text-blue-500">查看详情 →</span>
        </div>
      </div>

      <div v-if="hasMore" class="text-center pt-2">
        <button
          class="text-sm text-blue-600 hover:underline"
          @click="loadMore"
        >
          加载更多
        </button>
      </div>
    </div>

    <div v-else class="bg-white rounded-xl shadow-sm p-12 text-center">
      <p class="text-gray-400 text-sm mb-4">还没有写作记录</p>
      <button
        class="bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-5 py-2 text-sm"
        @click="router.push('/writing')"
      >
        开始练习
      </button>
    </div>
  </div>
</template>
