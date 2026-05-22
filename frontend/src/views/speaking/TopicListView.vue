<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTopics } from '@/api/speaking'

const router = useRouter()

const activePart = ref('Part1')
const topics = ref([])
const loading = ref(false)
const selectedTopic = ref(null)
const selectedPersona = ref('encouraging')

const personas = [
  { key: 'encouraging', label: '鼓励型', desc: '友善耐心，给予正向反馈' },
  { key: 'strict', label: '严格型', desc: '专业严谨，接近真实考场' },
]

async function fetchTopics() {
  loading.value = true
  try {
    const res = await getTopics({ part: activePart.value })
    topics.value = res.content || res || []
  } catch {
    // mock 数据
    topics.value = [
      { id: 1, title: 'Describe your hometown', part: 'Part1' },
      { id: 2, title: 'Talk about your hobbies', part: 'Part1' },
      { id: 3, title: 'Describe a memorable journey you have taken', part: 'Part2' },
      { id: 4, title: 'Describe a person who has influenced you', part: 'Part2' },
      { id: 5, title: 'Do you think technology has changed the way people communicate?', part: 'Part3' },
      { id: 6, title: 'What are the advantages and disadvantages of living in a big city?', part: 'Part3' },
    ].filter(t => t.part === activePart.value)
  } finally {
    loading.value = false
  }
}

onMounted(fetchTopics)

function switchPart(part) {
  activePart.value = part
  selectedTopic.value = null
  fetchTopics()
}

function startSession() {
  if (!selectedTopic.value) return
  router.push({
    path: '/speaking/session',
    query: { topicId: selectedTopic.value.id, persona: selectedPersona.value, part: activePart.value },
  })
}
</script>

<template>
  <div class="space-y-5 max-w-3xl">
    <h1 class="text-xl font-semibold text-gray-800">口语话题</h1>

    <!-- Part 切换 -->
    <div class="flex gap-2">
      <button
        v-for="p in ['Part1', 'Part2', 'Part3']"
        :key="p"
        class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors"
        :class="activePart === p ? 'bg-blue-600 text-white' : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50'"
        @click="switchPart(p)"
      >
        {{ p }}
      </button>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- 话题列表 -->
      <div class="lg:col-span-2 space-y-3">
        <div v-if="loading" class="text-center py-10 text-gray-400 text-sm">加载中...</div>
        <div
          v-for="topic in topics"
          :key="topic.id"
          class="bg-white rounded-xl shadow-sm p-4 cursor-pointer transition-all"
          :class="selectedTopic?.id === topic.id ? 'ring-2 ring-blue-500' : 'hover:shadow-md'"
          @click="selectedTopic = topic"
        >
          <p class="text-sm text-gray-700">{{ topic.title }}</p>
          <span class="text-xs text-gray-400 mt-1 inline-block">{{ topic.part }}</span>
        </div>
        <p v-if="!loading && !topics.length" class="text-center py-10 text-gray-400 text-sm">暂无话题</p>
      </div>

      <!-- 右侧：考官人设 + 开始按钮 -->
      <div class="space-y-4">
        <div class="bg-white rounded-xl shadow-sm p-4">
          <h3 class="text-sm font-semibold text-gray-700 mb-3">选择考官风格</h3>
          <div class="space-y-2">
            <label
              v-for="p in personas"
              :key="p.key"
              class="flex items-start gap-3 p-3 rounded-lg border cursor-pointer transition-colors"
              :class="selectedPersona === p.key ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:bg-gray-50'"
            >
              <input
                v-model="selectedPersona"
                type="radio"
                :value="p.key"
                class="mt-0.5 accent-blue-600"
              />
              <div>
                <div class="text-sm font-medium text-gray-800">{{ p.label }}</div>
                <div class="text-xs text-gray-500 mt-0.5">{{ p.desc }}</div>
              </div>
            </label>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm p-4">
          <p v-if="selectedTopic" class="text-xs text-gray-500 mb-3 line-clamp-2">
            已选：{{ selectedTopic.title }}
          </p>
          <p v-else class="text-xs text-gray-400 mb-3">请先选择一个话题</p>
          <button
            :disabled="!selectedTopic"
            class="w-full bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-4 py-2 text-sm font-medium disabled:opacity-40"
            @click="startSession"
          >
            开始口语练习
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
