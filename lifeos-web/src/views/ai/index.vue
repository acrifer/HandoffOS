<template>
  <div class="page-shell">
    <section class="page-header">
      <div>
        <p class="eyebrow">AI 作业</p>
        <h1>统一审计同步、蒸馏、问答和资料整理。</h1>
        <p>这里把同步、蒸馏、问答和资料整理都归入可追踪的控制面作业。</p>
      </div>
      <div class="row">
        <button class="ghost-btn" @click="fetchJobs">刷新</button>
        <button class="primary-btn" @click="router.push('/skill')">打开 Skill 工作台</button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card"><span>总作业数</span><strong>{{ historyStats.total }}</strong></article>
      <article class="metric-card"><span>成功</span><strong>{{ historyStats.success }}</strong></article>
      <article class="metric-card"><span>失败</span><strong>{{ historyStats.failed }}</strong></article>
      <article class="metric-card"><span>处理中</span><strong>{{ historyStats.processing }}</strong></article>
    </section>

    <section class="audit-grid">
      <article class="panel">
        <div class="row">
          <div>
            <p class="section-label">过滤器</p>
            <h2>按类型和状态筛选</h2>
          </div>
        </div>
        <div class="filters">
          <select v-model="jobTypeFilter">
            <option value="all">全部类型</option>
            <option value="SKILL_SYNC">来源同步</option>
            <option value="SKILL_DISTILL">Skill 蒸馏</option>
            <option value="SKILL_ASK">Skill 问答</option>
            <option value="SUMMARY">摘要</option>
            <option value="ORGANIZE">整理</option>
            <option value="EXTRACT_TASKS">提取行动</option>
            <option value="WEEKLY_REVIEW">周复盘</option>
          </select>
          <select v-model="jobStatusFilter">
            <option value="all">全部状态</option>
            <option value="SUCCESS">已完成</option>
            <option value="FAILED">失败</option>
            <option value="PROCESSING">处理中</option>
            <option value="PENDING">排队中</option>
          </select>
        </div>
        <div class="data-list">
          <button
            v-for="job in filteredJobs"
            :key="job.id"
            class="list-row"
            :class="{ active: activeJob?.id === job.id }"
            @click="selectJob(job)"
          >
            <span>
              <strong>{{ job.skillName || job.noteTitle || jobTypeLabel(job.jobType) }}</strong>
              <small>{{ jobTypeLabel(job.jobType) }} · {{ formatDate(job.createTime) }}</small>
            </span>
            <em class="badge" :class="statusClass(job.status)">{{ jobStatusLabel(job.status) }}</em>
          </button>
          <p v-if="!filteredJobs.length" class="empty-state">筛选条件下没有作业记录。</p>
        </div>
      </article>

      <article class="panel">
        <template v-if="activeJob">
          <div class="row">
            <div>
              <p class="section-label">作业详情</p>
              <h2>{{ activeJob.skillName || activeJob.noteTitle || jobTypeLabel(activeJob.jobType) }}</h2>
            </div>
            <em class="badge" :class="statusClass(activeJob.status)">{{ jobStatusLabel(activeJob.status) }}</em>
          </div>

          <p class="meta-line">
            {{ jobTypeLabel(activeJob.jobType) }} · {{ formatDate(activeJob.createTime) }}
            <span v-if="activeJob.finishedTime"> · 完成于 {{ formatDate(activeJob.finishedTime) }}</span>
          </p>

          <div class="detail-actions">
            <button v-if="activeJob.skillId" class="ghost-btn" @click="router.push('/skill')">打开关联 Skill</button>
            <button v-if="activeJob.noteId" class="ghost-btn" @click="router.push({ path: '/note', query: { noteId: activeJob.noteId } })">打开资料</button>
          </div>

          <div v-if="activeJob.errorMessage" class="result-box failed-box">
            <strong>失败原因</strong>
            <p>{{ activeJob.errorMessage }}</p>
          </div>

          <div v-else class="result-box">
            <strong>结果载荷</strong>
            <pre>{{ pretty(activeJob.result) }}</pre>
          </div>
        </template>
        <p v-else class="empty-state">从左侧选择一条作业查看详情。</p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { noteApi } from '@/api/note'

const router = useRouter()
const route = useRoute()
const jobs = ref([])
const activeJobId = ref(route.query.history || null)
const jobTypeFilter = ref('all')
const jobStatusFilter = ref('all')

const filteredJobs = computed(() =>
  jobs.value.filter(job => {
    if (jobTypeFilter.value !== 'all' && job.jobType !== jobTypeFilter.value) return false
    if (jobStatusFilter.value !== 'all' && job.status !== jobStatusFilter.value) return false
    return true
  })
)

const activeJob = computed(() =>
  filteredJobs.value.find(job => String(job.id) === String(activeJobId.value)) || filteredJobs.value[0] || null
)

const historyStats = computed(() => ({
  total: jobs.value.length,
  success: jobs.value.filter(job => job.status === 'SUCCESS').length,
  failed: jobs.value.filter(job => job.status === 'FAILED').length,
  processing: jobs.value.filter(job => ['PROCESSING', 'PENDING'].includes(job.status)).length
}))

const fetchJobs = async () => {
  jobs.value = await noteApi.getJobs({ limit: 40 })
  if (!activeJobId.value && jobs.value.length) {
    activeJobId.value = jobs.value[0].id
  }
}

const selectJob = (job) => {
  activeJobId.value = job.id
  router.replace({ path: '/ai', query: { history: job.id } })
}

const pretty = (value) => JSON.stringify(value || {}, null, 2)
const formatDate = (value) => value ? new Date(value).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '未知时间'
const jobTypeLabel = (value) => ({ SKILL_SYNC: '来源同步', SKILL_DISTILL: 'Skill 蒸馏', SKILL_ASK: 'Skill 问答', SUMMARY: '摘要', ORGANIZE: '整理', EXTRACT_TASKS: '提取行动', WEEKLY_REVIEW: '周复盘' }[value] || value)
const jobStatusLabel = (value) => ({ PENDING: '排队中', PROCESSING: '处理中', SUCCESS: '已完成', FAILED: '失败' }[value] || value)
const statusClass = (value) => value === 'SUCCESS' ? 'success' : value === 'FAILED' ? 'danger' : 'warning'

watch(() => route.query.history, value => {
  activeJobId.value = value || null
})

onMounted(fetchJobs)
</script>

<style scoped>
.audit-grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) minmax(0, 1.1fr);
  gap: 16px;
}

.filters {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 14px;
}

.list-row.active {
  background: var(--blue-soft);
}

.meta-line {
  color: var(--text-muted);
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;
}

.result-box {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 14px;
}

.failed-box {
  background: #fff7ed;
  border-color: #fed7aa;
}

pre {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  color: var(--text-muted);
}

@media (max-width: 1100px) {
  .audit-grid,
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
