<template>
  <div class="history-page">
    <h1 class="page-title">练习记录</h1>
    <p class="page-subtitle">查看你的历史练习和评分</p>

    <div class="filter-bar">
      <el-radio-group v-model="filterType" @change="loadRecords">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="WRITING">写作</el-radio-button>
        <el-radio-button value="SPEAKING">口语</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="records.length === 0" class="empty-state">
      <el-empty description="暂无练习记录">
        <el-button type="primary" @click="$router.push('/writing')">开始练习</el-button>
      </el-empty>
    </div>

    <div v-else class="records-list">
      <div v-for="record in records" :key="record.id" class="record-card">
        <div class="record-type" :class="record.type.toLowerCase()">
          {{ record.type === 'WRITING' ? '写作' : '口语' }}
        </div>
        <div class="record-info">
          <span class="record-date">{{ formatDate(record.createdAt) }}</span>
        </div>
        <div class="record-score">
          <span class="score-value">{{ record.overallBand || '--' }}</span>
          <span class="score-label">Band</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPracticeRecords } from '../../api'

const filterType = ref('')
const records = ref<any[]>([])
const loading = ref(true)

onMounted(() => {
  loadRecords()
})

async function loadRecords() {
  loading.value = true
  try {
    const { data } = await getPracticeRecords(filterType.value || undefined)
    records.value = data.data || []
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
    + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.history-page {
  max-width: 800px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #64748b;
  margin-bottom: 24px;
}

.filter-bar {
  margin-bottom: 24px;
}

.loading-state {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}

.record-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.record-type {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.record-type.writing {
  background: rgba(37, 99, 235, 0.1);
  color: var(--color-primary);
}

.record-type.speaking {
  background: rgba(236, 72, 153, 0.1);
  color: var(--color-accent);
}

.record-info {
  flex: 1;
}

.record-date {
  font-size: 14px;
  color: #475569;
}

.record-score {
  text-align: center;
  flex-shrink: 0;
}

.score-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-foreground);
}

.score-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
}
</style>
