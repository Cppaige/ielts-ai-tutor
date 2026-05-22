<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTopics, submitEssay } from '@/api/writing'
import { useToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const topic = ref(null)
const essay = ref('')
const loading = ref(false)
const submitting = ref(false)

const wordCount = computed(() => {
  const text = essay.value.trim()
  if (!text) return 0
  return text.split(/\s+/).filter(Boolean).length
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getTopics({ id: route.params.id })
    topic.value = Array.isArray(res) ? res[0] : res
  } catch {
    // mock
    topic.value = {
      id: route.params.id,
      title: 'Some people think that the best way to reduce crime is to give longer prison sentences. Others, however, believe there are better alternative ways of reducing crime. Discuss both views and give your opinion.',
      taskType: 'Task2',
      category: '社会',
      requirement: 'Write at least 250 words.',
    }
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (wordCount.value < 50) {
    toast.error('作文内容太短，请继续写作')
    return
  }
  submitting.value = true
  try {
    const res = await submitEssay({ topicId: route.params.id, content: essay.value })
    toast.success('提交成功，正在评分...')
    router.push(`/writing/waiting/${res.submissionId}`)
  } catch {
    toast.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="h-full flex flex-col">
    <div v-if="loading" class="flex-1 flex items-center justify-center text-gray-400 text-sm">
      加载题目中...
    </div>

    <div v-else-if="topic" class="flex-1 flex flex-col lg:flex-row gap-5 min-h-0">
      <!-- 左侧：题目 -->
      <div class="lg:w-2/5 bg-white rounded-xl shadow-sm p-6 flex flex-col gap-3">
        <div class="flex items-center gap-2">
          <span class="text-xs bg-blue-50 text-blue-600 rounded px-2 py-0.5">{{ topic.taskType }}</span>
          <span class="text-xs bg-gray-100 text-gray-500 rounded px-2 py-0.5">{{ topic.category }}</span>
        </div>
        <p class="text-sm text-gray-700 leading-relaxed">{{ topic.title }}</p>
        <p v-if="topic.requirement" class="text-xs text-gray-400 mt-auto pt-3 border-t border-gray-100">
          {{ topic.requirement }}
        </p>
      </div>

      <!-- 右侧：作文输入 -->
      <div class="lg:w-3/5 flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-gray-700">你的作文</h2>
          <span
            class="text-xs"
            :class="wordCount >= 250 ? 'text-green-600' : 'text-gray-400'"
          >
            {{ wordCount }} 词{{ topic.taskType === 'Task2' ? ' / 建议 250+' : ' / 建议 150+' }}
          </span>
        </div>
        <textarea
          v-model="essay"
          placeholder="在此输入你的作文..."
          class="flex-1 min-h-64 lg:min-h-0 lg:h-full border border-gray-300 rounded-lg px-4 py-3 text-sm text-gray-800 leading-relaxed focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
        />
        <div class="flex justify-end">
          <button
            :disabled="submitting || wordCount < 10"
            class="bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-6 py-2 text-sm font-medium disabled:opacity-50"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中...' : '提交评分' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
