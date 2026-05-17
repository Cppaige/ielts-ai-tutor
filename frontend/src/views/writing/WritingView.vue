<template>
  <div class="writing-page">
    <h1 class="page-title">写作练习</h1>
    <p class="page-subtitle">提交你的作文，AI 将从四个维度为你评分</p>

    <div v-if="!submitted" class="writing-form">
      <el-form label-position="top">
        <el-form-item label="题目类型">
          <el-radio-group v-model="taskType">
            <el-radio :value="1">Task 1 (图表描述)</el-radio>
            <el-radio :value="2">Task 2 (议论文)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="作文内容">
          <el-input
            v-model="essayText"
            type="textarea"
            :rows="14"
            placeholder="请在此输入你的作文..."
            show-word-limit
            :maxlength="3000"
          />
        </el-form-item>
        <div class="word-count">
          词数: {{ wordCount }}
          <span v-if="wordCount < 150 && taskType === 1" class="warning">（Task 1 建议至少 150 词）</span>
          <span v-if="wordCount < 250 && taskType === 2" class="warning">（Task 2 建议至少 250 词）</span>
        </div>
        <el-button type="primary" size="large" :loading="submitting" :disabled="!essayText.trim()" @click="handleSubmit">
          提交评分
        </el-button>
      </el-form>
    </div>

    <div v-else class="result-section">
      <div v-if="scoring" class="scoring-progress">
        <el-icon class="scoring-spinner" :size="48"><Loading /></el-icon>
        <h3>AI 正在评分中...</h3>
        <p>{{ progressText }}</p>
        <el-progress :percentage="progress" :stroke-width="8" color="var(--color-primary)" />
      </div>

      <div v-else-if="result" class="scoring-result">
        <div class="overall-band">
          <div class="band-circle">
            <span class="band-number">{{ result.overallBand }}</span>
          </div>
          <span class="band-label">Overall Band</span>
        </div>

        <div class="score-grid">
          <div class="score-item">
            <span class="score-name">Task Response</span>
            <el-progress :percentage="(result.trScore / 9) * 100" :format="() => result.trScore" color="#2563EB" />
          </div>
          <div class="score-item">
            <span class="score-name">Coherence & Cohesion</span>
            <el-progress :percentage="(result.ccScore / 9) * 100" :format="() => result.ccScore" color="#7C3AED" />
          </div>
          <div class="score-item">
            <span class="score-name">Lexical Resource</span>
            <el-progress :percentage="(result.lrScore / 9) * 100" :format="() => result.lrScore" color="#059669" />
          </div>
          <div class="score-item">
            <span class="score-name">Grammatical Range</span>
            <el-progress :percentage="(result.graScore / 9) * 100" :format="() => result.graScore" color="#D97706" />
          </div>
        </div>

        <div v-if="result.masterFeedback" class="feedback-section">
          <h3>详细反馈</h3>
          <div class="feedback-content">{{ result.masterFeedback }}</div>
        </div>

        <el-button type="primary" size="large" @click="resetForm">再写一篇</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { submitWriting, getWritingSubmission } from '../../api'

const taskType = ref(2)
const essayText = ref('')
const submitting = ref(false)
const submitted = ref(false)
const scoring = ref(false)
const progress = ref(0)
const progressText = ref('正在分析文章结构...')
const result = ref<any>(null)

const wordCount = computed(() => {
  return essayText.value.trim().split(/\s+/).filter(Boolean).length
})

async function handleSubmit() {
  submitting.value = true
  try {
    const { data } = await submitWriting(1, taskType.value, essayText.value)
    const submissionId = data.data
    submitted.value = true
    scoring.value = true
    pollResult(submissionId)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function pollResult(submissionId: number) {
  const stages = [
    { pct: 20, text: '正在分析词汇和语法...' },
    { pct: 45, text: '正在评估任务回应和连贯性...' },
    { pct: 70, text: '正在生成综合评价...' },
    { pct: 90, text: '即将完成...' }
  ]

  let stageIdx = 0
  const timer = setInterval(() => {
    if (stageIdx < stages.length) {
      progress.value = stages[stageIdx].pct
      progressText.value = stages[stageIdx].text
      stageIdx++
    }
  }, 3000)

  const maxAttempts = 60
  for (let i = 0; i < maxAttempts; i++) {
    await new Promise(resolve => setTimeout(resolve, 2000))
    try {
      const { data } = await getWritingSubmission(submissionId)
      const submission = data.data
      if (submission.status === 'COMPLETED') {
        clearInterval(timer)
        progress.value = 100
        result.value = submission
        scoring.value = false
        return
      } else if (submission.status === 'FAILED') {
        clearInterval(timer)
        scoring.value = false
        ElMessage.error('评分失败，请重试')
        return
      }
    } catch {
      // continue polling
    }
  }
  clearInterval(timer)
  scoring.value = false
  ElMessage.error('评分超时，请稍后查看结果')
}

function resetForm() {
  submitted.value = false
  scoring.value = false
  result.value = null
  essayText.value = ''
  progress.value = 0
}
</script>

<style scoped>
.writing-page {
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

.writing-form {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.word-count {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 20px;
}

.word-count .warning {
  color: var(--color-secondary);
}

.scoring-progress {
  background: #fff;
  border-radius: 16px;
  padding: 64px 32px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.scoring-spinner {
  color: var(--color-primary);
  animation: spin 1.5s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.scoring-progress h3 {
  font-size: 20px;
  margin-bottom: 8px;
}

.scoring-progress p {
  color: #64748b;
  margin-bottom: 24px;
}

.scoring-result {
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.overall-band {
  text-align: center;
  margin-bottom: 36px;
}

.band-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563EB, #7C3AED);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.band-number {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
}

.band-label {
  font-size: 14px;
  color: #64748b;
}

.score-grid {
  display: grid;
  gap: 20px;
  margin-bottom: 32px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-name {
  width: 180px;
  font-size: 14px;
  font-weight: 500;
  flex-shrink: 0;
}

.score-item .el-progress {
  flex: 1;
}

.feedback-section {
  border-top: 1px solid var(--color-border);
  padding-top: 24px;
  margin-bottom: 24px;
}

.feedback-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.feedback-content {
  font-size: 14px;
  line-height: 1.8;
  color: #475569;
  white-space: pre-wrap;
}
</style>
