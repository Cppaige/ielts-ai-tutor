<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTopics } from '@/api/writing'

const router = useRouter()

const activeTask = ref('Task2')
const activeCategory = ref('all')
const topics = ref([])
const loading = ref(false)

const categories = ['all', '社会', '教育', '科技', '环境', '健康']

async function fetchTopics() {
  loading.value = true
  try {
    const params = { taskType: activeTask.value }
    if (activeCategory.value !== 'all') params.category = activeCategory.value
    const res = await getTopics(params)
    topics.value = res.content || res || []
  } catch {
    // mock 数据
    topics.value = [
      { id: 1, title: 'Some people think that the best way to reduce crime is to give longer prison sentences. Others, however, believe there are better alternative ways of reducing crime. Discuss both views and give your opinion.', taskType: 'Task2', category: '社会' },
      { id: 2, title: 'In many countries, the proportion of older people is steadily increasing. Does this trend have more positive or negative effects on society?', taskType: 'Task2', category: '社会' },
      { id: 3, title: 'The graph below shows the number of university graduates in Canada from 1992 to 2007.', taskType: 'Task1', category: '教育' },
      { id: 4, title: 'Some people believe that it is best to accept a bad situation, such as an unsatisfactory job or shortage of money. Others argue that it is better to try and improve such situations. Discuss both views and give your opinion.', taskType: 'Task2', category: '社会' },
      { id: 5, title: 'The chart below shows the percentage of households in owned and rented accommodation in England and Wales between 1918 and 2011.', taskType: 'Task1', category: '社会' },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(fetchTopics)

function switchTask(task) {
  activeTask.value = task
  fetchTopics()
}

function switchCategory(cat) {
  activeCategory.value = cat
  fetchTopics()
}
</script>

<template>
  <div class="space-y-5">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-semibold text-gray-800">写作题库</h1>
    </div>

    <!-- Task 切换 -->
    <div class="flex gap-2">
      <button
        v-for="t in ['Task1', 'Task2']"
        :key="t"
        class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors"
        :class="activeTask === t ? 'bg-blue-600 text-white' : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50'"
        @click="switchTask(t)"
      >
        {{ t }}
      </button>
    </div>

    <!-- 分类筛选 -->
    <div class="flex flex-wrap gap-2">
      <button
        v-for="cat in categories"
        :key="cat"
        class="px-3 py-1 rounded-full text-xs font-medium transition-colors"
        :class="activeCategory === cat ? 'bg-blue-100 text-blue-700' : 'bg-white border border-gray-200 text-gray-500 hover:bg-gray-50'"
        @click="switchCategory(cat)"
      >
        {{ cat === 'all' ? '全部' : cat }}
      </button>
    </div>

    <!-- 题目列表 -->
    <div v-if="loading" class="text-center py-12 text-gray-400 text-sm">加载中...</div>
    <div v-else class="space-y-3">
      <div
        v-for="topic in topics"
        :key="topic.id"
        class="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-shadow"
        @click="router.push(`/writing/practice/${topic.id}`)"
      >
        <div class="flex items-start justify-between gap-4">
          <p class="text-sm text-gray-700 leading-relaxed line-clamp-3">{{ topic.title }}</p>
          <span class="shrink-0 text-xs bg-blue-50 text-blue-600 rounded px-2 py-0.5">{{ topic.taskType }}</span>
        </div>
        <div class="mt-3 flex items-center gap-2">
          <span class="text-xs text-gray-400 bg-gray-100 rounded px-2 py-0.5">{{ topic.category }}</span>
          <span class="text-xs text-blue-500 ml-auto">开始练习 →</span>
        </div>
      </div>
      <p v-if="!topics.length" class="text-center py-12 text-gray-400 text-sm">暂无题目</p>
    </div>
  </div>
</template>
