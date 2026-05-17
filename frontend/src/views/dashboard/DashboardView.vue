<template>
  <div class="dashboard">
    <h1 class="page-title">欢迎回来</h1>
    <p class="page-subtitle">选择一项练习开始你的雅思备考之旅</p>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon writing-icon">
          <el-icon :size="24"><EditPen /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">写作练习</span>
          <span class="stat-value">{{ stats.writingCount }} 次</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon speaking-icon">
          <el-icon :size="24"><Microphone /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">口语练习</span>
          <span class="stat-value">{{ stats.speakingCount }} 次</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon band-icon">
          <el-icon :size="24"><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">平均分数</span>
          <span class="stat-value">{{ stats.avgBand || '--' }}</span>
        </div>
      </div>
    </div>

    <div class="practice-cards">
      <div class="practice-card" @click="$router.push('/writing')">
        <div class="card-content">
          <h3>写作练习</h3>
          <p>提交作文，获得 AI 评分和详细反馈</p>
          <ul>
            <li>Task Response 任务回应</li>
            <li>Coherence & Cohesion 连贯与衔接</li>
            <li>Lexical Resource 词汇丰富度</li>
            <li>Grammatical Range 语法多样性</li>
          </ul>
        </div>
        <el-button type="primary" size="large">开始写作</el-button>
      </div>
      <div class="practice-card" @click="$router.push('/speaking')">
        <div class="card-content">
          <h3>口语练习</h3>
          <p>与 AI 考官实时对话，模拟真实考试</p>
          <ul>
            <li>Fluency 流利度</li>
            <li>Lexical Resource 词汇资源</li>
            <li>Grammar 语法准确性</li>
            <li>Pronunciation 发音</li>
          </ul>
        </div>
        <el-button type="primary" size="large">开始口语</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { EditPen, Microphone, TrendCharts } from '@element-plus/icons-vue'
import { getPracticeRecords } from '../../api'

const stats = ref({
  writingCount: 0,
  speakingCount: 0,
  avgBand: ''
})

onMounted(async () => {
  try {
    const { data } = await getPracticeRecords()
    const records = data.data || []
    stats.value.writingCount = records.filter((r: any) => r.type === 'WRITING').length
    stats.value.speakingCount = records.filter((r: any) => r.type === 'SPEAKING').length
    const bands = records.map((r: any) => r.overallBand).filter(Boolean)
    if (bands.length > 0) {
      stats.value.avgBand = (bands.reduce((a: number, b: number) => a + b, 0) / bands.length).toFixed(1)
    }
  } catch {
    // silently fail on dashboard load
  }
})
</script>

<style scoped>
.dashboard {
  max-width: 900px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.page-subtitle {
  color: #64748b;
  margin-bottom: 32px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 40px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.writing-icon { background: var(--color-primary); }
.speaking-icon { background: var(--color-accent); }
.band-icon { background: var(--color-secondary); }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-foreground);
}

.practice-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.practice-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.practice-card:hover {
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.1);
  transform: translateY(-2px);
}

.practice-card h3 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--color-foreground);
}

.practice-card p {
  color: #64748b;
  margin-bottom: 16px;
  font-size: 14px;
}

.practice-card ul {
  list-style: none;
  padding: 0;
  margin-bottom: 24px;
}

.practice-card ul li {
  padding: 4px 0;
  font-size: 13px;
  color: #475569;
}

.practice-card ul li::before {
  content: '•';
  color: var(--color-primary);
  margin-right: 8px;
}
</style>
