<template>
  <div class="page">
    <section class="hero card">
      <div>
        <p class="eyebrow">AI 工作台</p>
        <h1>把 AI 放进真实工作流，而不是单独开一个聊天页。</h1>
        <p>这里集中展示待整理笔记、复习队列、周复盘入口和完整的 AI 历史记录。</p>
      </div>
      <div class="stats">
        <article>
          <strong>{{ dashboard.aiInboxCount || 0 }}</strong>
          <span>待整理</span>
        </article>
        <article>
          <strong>{{ dashboard.notesToReviewCount || 0 }}</strong>
          <span>待复习</span>
        </article>
        <article>
          <strong>{{ historyStats.failed }}</strong>
          <span>失败作业</span>
        </article>
      </div>
    </section>

    <section class="shortcut-grid">
      <button class="card shortcut" @click="router.push({ path: '/note', query: { view: 'organize' } })">
        <strong>打开待整理笔记</strong>
        <p>处理缺少摘要或标签的内容。</p>
      </button>
      <button class="card shortcut" @click="router.push({ path: '/note', query: { view: 'review' } })">
        <strong>打开复习队列</strong>
        <p>查看该回看的笔记和常青内容。</p>
      </button>
      <button class="card shortcut" @click="router.push('/task?view=derived')">
        <strong>打开知识任务</strong>
        <p>查看从笔记里提取出的行动项。</p>
      </button>
    </section>

    <section class="top-grid">
      <article class="card panel">
        <div class="row">
          <div>
            <p class="section-label">周复盘</p>
            <h2>{{ weeklyReview?.headline || '还没有本周复盘' }}</h2>
          </div>
          <button class="primary-btn" :disabled="weeklyBusy" @click="createWeeklyReview">
            {{ weeklyBusy ? '处理中...' : '生成本周复盘' }}
          </button>
        </div>
        <template v-if="weeklyReview">
          <p class="summary">{{ weeklyReview.summary }}</p>
          <div class="group">
            <strong>亮点</strong>
            <ul>
              <li v-for="item in weeklyReview.highlights" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div class="group">
            <strong>接下来关注</strong>
            <ul>
              <li v-for="item in weeklyReview.focusAreas" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div class="group">
            <strong>下一步</strong>
            <ul>
              <li v-for="item in weeklyReview.nextActions" :key="item">{{ item }}</li>
            </ul>
          </div>
        </template>
        <p v-else class="summary">提交异步复盘任务后，会在这里展示本周知识沉淀总结。</p>
        <small v-if="weeklyJob">当前状态：{{ jobStatusLabel(weeklyJob.status) }}</small>
      </article>

      <article class="card panel">
        <div class="row">
          <div>
            <p class="section-label">待办入口</p>
            <h2>先处理最有价值的内容</h2>
          </div>
          <button class="ghost-btn" @click="fetchQueues">刷新</button>
        </div>
        <div class="queue-stack">
          <div class="queue-card">
            <strong>待整理笔记</strong>
            <span>{{ organizeNotes.length }} 篇</span>
            <p>{{ organizeNotes[0]?.title || '当前没有待整理笔记。' }}</p>
          </div>
          <div class="queue-card">
            <strong>复习队列</strong>
            <span>{{ reviewNotes.length }} 篇</span>
            <p>{{ reviewNotes[0]?.title || '当前没有待复习内容。' }}</p>
          </div>
        </div>
      </article>
    </section>

    <section class="history-panel card">
      <div class="row history-header">
        <div>
          <p class="section-label">AI 历史记录</p>
          <h2>按时间回看 AI 做过什么</h2>
        </div>
        <button class="ghost-btn" @click="fetchJobs">刷新</button>
      </div>

      <div class="filters">
        <select v-model="jobTypeFilter">
          <option value="all">全部类型</option>
          <option value="SUMMARY">摘要</option>
          <option value="ORGANIZE">整理</option>
          <option value="EXTRACT_TASKS">提取任务</option>
          <option value="WEEKLY_REVIEW">周复盘</option>
          <option value="SKILL_DISTILL">Skill 蒸馏</option>
          <option value="SKILL_ASK">Skill 问答</option>
        </select>
        <select v-model="jobStatusFilter">
          <option value="all">全部状态</option>
          <option value="SUCCESS">已完成</option>
          <option value="FAILED">失败</option>
          <option value="PROCESSING">处理中</option>
          <option value="PENDING">排队中</option>
        </select>
      </div>

      <div class="history-stats">
        <article>
          <strong>{{ historyStats.total }}</strong>
          <span>总作业数</span>
        </article>
        <article>
          <strong>{{ historyStats.success }}</strong>
          <span>成功</span>
        </article>
        <article>
          <strong>{{ historyStats.failed }}</strong>
          <span>失败</span>
        </article>
      </div>

      <div class="history-grid">
        <div class="history-list">
          <button
            v-for="job in filteredJobs"
            :key="job.id"
            class="history-item"
            :class="{ active: activeJob?.id === job.id }"
            @click="selectJob(job)"
          >
            <div class="history-main">
              <strong>{{ job.noteTitle || job.skillName || jobTypeLabel(job.jobType) }}</strong>
              <p>{{ jobTypeLabel(job.jobType) }} · {{ formatDate(job.createTime) }}</p>
            </div>
            <span class="badge" :class="statusClass(job.status)">{{ jobStatusLabel(job.status) }}</span>
          </button>
          <p v-if="!filteredJobs.length" class="muted">筛选条件下没有 AI 历史记录。</p>
        </div>

        <div class="history-detail card inner-card">
          <template v-if="activeJob">
            <div class="row">
              <div>
                <p class="section-label">作业详情</p>
                <h3>{{ activeJob.noteTitle || activeJob.skillName || jobTypeLabel(activeJob.jobType) }}</h3>
              </div>
              <span class="badge" :class="statusClass(activeJob.status)">{{ jobStatusLabel(activeJob.status) }}</span>
            </div>
            <p class="meta-line">
              {{ jobTypeLabel(activeJob.jobType) }} · {{ formatDate(activeJob.createTime) }}
              <span v-if="activeJob.finishedTime">· 完成于 {{ formatDate(activeJob.finishedTime) }}</span>
            </p>

            <div class="detail-actions">
              <button v-if="activeJob.noteId" class="ghost-btn" @click="openRelatedNote(activeJob)">打开关联笔记</button>
              <button v-if="activeJob.skillId" class="ghost-btn" @click="router.push('/skill')">打开 Skill 工作台</button>
              <button v-if="activeJob.jobType === 'WEEKLY_REVIEW'" class="ghost-btn" @click="router.push('/dashboard')">回到总览页</button>
            </div>

            <template v-if="activeJob.status === 'FAILED'">
              <div class="result-box failed-box">
                <strong>失败原因</strong>
                <p>{{ activeJob.errorMessage || '没有返回具体错误。' }}</p>
              </div>
            </template>

            <template v-else-if="activeJob.jobType === 'SUMMARY'">
              <div class="result-box">
                <strong>摘要结果</strong>
                <p>{{ activeJob.result?.summary || '暂无摘要内容。' }}</p>
              </div>
            </template>

            <template v-else-if="activeJob.jobType === 'ORGANIZE'">
              <div class="result-box">
                <strong>整理建议</strong>
                <p><b>标题：</b>{{ activeJob.result?.suggestedTitle || '未生成' }}</p>
                <p><b>标签：</b>{{ activeJob.result?.suggestedTags || '未生成' }}</p>
                <p><b>摘要：</b>{{ activeJob.result?.summary || '未生成' }}</p>
              </div>
            </template>

            <template v-else-if="activeJob.jobType === 'EXTRACT_TASKS'">
              <div class="result-box">
                <strong>提取出的任务</strong>
                <div v-if="activeJob.result?.tasks?.length" class="task-list">
                  <div v-for="task in activeJob.result.tasks" :key="task.title + task.description" class="task-item">
                    <strong>{{ task.title }}</strong>
                    <p>{{ task.description || '暂无说明' }}</p>
                  </div>
                </div>
                <p v-else>没有提取到任务。</p>
              </div>
            </template>

            <template v-else-if="activeJob.jobType === 'WEEKLY_REVIEW'">
              <div class="result-box">
                <strong>{{ activeJob.result?.weeklyReview?.headline || '周复盘' }}</strong>
                <p>{{ activeJob.result?.weeklyReview?.summary || '暂无复盘摘要。' }}</p>
                <div class="group">
                  <strong>亮点</strong>
                  <ul>
                    <li v-for="item in activeJob.result?.weeklyReview?.highlights || []" :key="item">{{ item }}</li>
                  </ul>
                </div>
                <div class="group">
                  <strong>下一步</strong>
                  <ul>
                    <li v-for="item in activeJob.result?.weeklyReview?.nextActions || []" :key="item">{{ item }}</li>
                  </ul>
                </div>
              </div>
            </template>

            <template v-else-if="activeJob.jobType === 'SKILL_DISTILL'">
              <div class="result-box">
                <strong>Skill 蒸馏结果</strong>
                <div class="group">
                  <strong>角色边界</strong>
                  <ul>
                    <li v-for="item in activeJob.result?.handoffSkill?.roleBoundaries || []" :key="item">{{ item }}</li>
                  </ul>
                </div>
                <div class="group">
                  <strong>检查清单</strong>
                  <ul>
                    <li v-for="item in activeJob.result?.handoffSkill?.workflowChecklists || []" :key="item">{{ item }}</li>
                  </ul>
                </div>
              </div>
            </template>

            <template v-else-if="activeJob.jobType === 'SKILL_ASK'">
              <div class="result-box">
                <strong>Skill 问答</strong>
                <p>{{ activeJob.result?.skillAnswer || '暂无回答。' }}</p>
                <div v-if="activeJob.result?.citations?.length" class="group">
                  <strong>引用</strong>
                  <ul>
                    <li v-for="item in activeJob.result.citations" :key="item">{{ item }}</li>
                  </ul>
                </div>
              </div>
            </template>
          </template>
          <p v-else class="muted">从左侧选择一条作业，查看它的完整结果和上下文。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { behaviorApi } from '@/api/behavior'
import { noteApi } from '@/api/note'

const router = useRouter()
const route = useRoute()
const dashboard = ref({})
const jobs = ref([])
const organizeNotes = ref([])
const reviewNotes = ref([])
const weeklyReview = ref(null)
const weeklyJob = ref(null)
const weeklyBusy = ref(false)
const activeJobId = ref(route.query.history || null)
const jobTypeFilter = ref('all')
const jobStatusFilter = ref('all')
let weeklyTimer = null

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
  failed: jobs.value.filter(job => job.status === 'FAILED').length
}))

const fetchDashboard = async () => {
  dashboard.value = await behaviorApi.getDashboard()
}

const fetchQueues = async () => {
  const [needsAi, reviewQueue] = await Promise.all([
    noteApi.search({ needsOrganization: true, sort: 'updated' }),
    noteApi.search({ reviewState: 'REVIEWABLE', sort: 'review' })
  ])
  organizeNotes.value = needsAi.slice(0, 5)
  reviewNotes.value = reviewQueue.slice(0, 5)
}

const fetchJobs = async () => {
  jobs.value = await noteApi.getJobs({ limit: 30 })
  const latestWeekly = jobs.value.find(item => item.jobType === 'WEEKLY_REVIEW')
  if (latestWeekly) {
    weeklyJob.value = latestWeekly
    weeklyReview.value = latestWeekly.result?.weeklyReview || null
    if (latestWeekly.status === 'PENDING' || latestWeekly.status === 'PROCESSING') {
      weeklyBusy.value = true
      pollWeeklyJob(latestWeekly.id)
    }
  }
  if (!activeJobId.value && jobs.value.length) {
    activeJobId.value = jobs.value[0].id
  }
}

const createWeeklyReview = async () => {
  weeklyJob.value = await noteApi.createWeeklyReview()
  weeklyBusy.value = true
  activeJobId.value = weeklyJob.value.id
  pollWeeklyJob(weeklyJob.value.id)
  await fetchJobs()
}

const pollWeeklyJob = (jobId) => {
  stopWeeklyPoll()
  const runner = async () => {
    weeklyJob.value = await noteApi.getJob(jobId)
    if (weeklyJob.value.status === 'SUCCESS') {
      weeklyReview.value = weeklyJob.value.result?.weeklyReview || null
      weeklyBusy.value = false
      activeJobId.value = weeklyJob.value.id
      await fetchJobs()
      stopWeeklyPoll()
      return
    }
    if (weeklyJob.value.status === 'FAILED') {
      weeklyBusy.value = false
      activeJobId.value = weeklyJob.value.id
      await fetchJobs()
      stopWeeklyPoll()
      return
    }
    weeklyTimer = window.setTimeout(runner, 2000)
  }
  runner()
}

const stopWeeklyPoll = () => {
  if (weeklyTimer) {
    window.clearTimeout(weeklyTimer)
    weeklyTimer = null
  }
}

const selectJob = (job) => {
  activeJobId.value = job.id
  router.replace({ path: '/ai', query: { ...route.query, history: job.id } })
}

const openRelatedNote = (job) => {
  if (!job.noteId) return
  router.push({ path: '/note', query: { noteId: job.noteId } })
}

const formatDate = (value) =>
  value
    ? new Date(value).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
    : '未设置时间'

const jobTypeLabel = (value) => ({ SUMMARY: '摘要', ORGANIZE: '整理', EXTRACT_TASKS: '提取任务', WEEKLY_REVIEW: '周复盘', SKILL_DISTILL: 'Skill 蒸馏', SKILL_ASK: 'Skill 问答' }[value] || value)
const jobStatusLabel = (value) => ({ PENDING: '排队中', PROCESSING: '处理中', SUCCESS: '已完成', FAILED: '失败' }[value] || '未知状态')
const statusClass = (value) => ({
  success: value === 'SUCCESS',
  failed: value === 'FAILED'
})

watch(() => route.query.history, value => {
  activeJobId.value = value || activeJob.value?.id || null
})

watch(filteredJobs, (items) => {
  if (!items.length) {
    activeJobId.value = null
    return
  }
  if (!items.some(job => String(job.id) === String(activeJobId.value))) {
    activeJobId.value = items[0].id
  }
})

onMounted(async () => {
  await Promise.all([fetchDashboard(), fetchQueues(), fetchJobs()])
})

onBeforeUnmount(() => {
  stopWeeklyPoll()
})
</script>

<style scoped>
.page { max-width: 1280px; margin: 0 auto; padding: 32px; }
.card { background: #fff; border: 1px solid #d8e3ec; border-radius: 24px; box-shadow: 0 18px 32px rgba(15, 23, 42, 0.06); }
.hero { padding: 32px; margin-bottom: 18px; display: flex; justify-content: space-between; gap: 24px; background: linear-gradient(135deg, #0f4c81, #0f766e); color: #fff; }
.eyebrow, .section-label { margin: 0 0 10px; font-size: 12px; letter-spacing: 0.18em; text-transform: uppercase; color: rgba(255,255,255,0.72); }
.section-label { color: #5e7388; }
.hero h1 { margin: 0 0 12px; font-size: 38px; line-height: 1.08; }
.hero p, .summary, .meta-line, .muted, .task-item p, .queue-card p { margin: 0; line-height: 1.8; color: #607487; }
.stats, .shortcut-grid, .top-grid, .history-grid, .queue-stack, .filters, .history-stats { display: grid; gap: 16px; }
.stats { grid-template-columns: repeat(3, minmax(0, 1fr)); min-width: 420px; }
.stats article { background: rgba(255,255,255,0.12); border: 1px solid rgba(255,255,255,0.18); border-radius: 18px; padding: 18px; display: grid; gap: 8px; }
.stats strong { font-size: 32px; }
.shortcut-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); margin-bottom: 18px; }
.shortcut { padding: 20px; text-align: left; border: none; cursor: pointer; }
.top-grid { grid-template-columns: 1.2fr 0.8fr; margin-bottom: 18px; }
.panel, .history-panel { padding: 20px; }
.row { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 12px; }
.actions, .detail-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.primary-btn, .ghost-btn, .badge { border: none; border-radius: 14px; padding: 10px 12px; font: inherit; }
.primary-btn { background: #103d5d; color: #fff; cursor: pointer; }
.primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.ghost-btn { background: #f5f9fc; color: #375068; cursor: pointer; }
.group + .group { margin-top: 12px; }
.group ul, .result-box ul { margin: 8px 0 0; padding-left: 18px; color: #607487; line-height: 1.7; }
.queue-card, .result-box, .task-item, .history-item { padding: 16px; border-radius: 18px; border: 1px solid #e1eaf1; background: #f8fbfd; }
.queue-card { display: grid; gap: 6px; }
.filters { grid-template-columns: repeat(2, minmax(0, 220px)); margin-bottom: 16px; }
.filters select { border: 1px solid #d7e2eb; border-radius: 14px; background: #f8fbfd; padding: 12px 14px; font: inherit; color: #102033; }
.history-stats { grid-template-columns: repeat(3, minmax(0, 1fr)); margin-bottom: 16px; }
.history-stats article { padding: 14px; border-radius: 18px; border: 1px solid #e1eaf1; background: #f8fbfd; display: grid; gap: 6px; }
.history-stats strong { font-size: 28px; color: #102033; }
.history-grid { grid-template-columns: 0.9fr 1.1fr; align-items: start; }
.history-list { display: grid; gap: 12px; }
.history-item { width: 100%; border: 1px solid #e1eaf1; display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; cursor: pointer; text-align: left; }
.history-item.active { border-color: #0f766e; box-shadow: inset 0 0 0 1px rgba(15, 118, 110, 0.2); }
.history-main { display: grid; gap: 6px; }
.inner-card { padding: 20px; }
.badge { background: #eaf0f6; color: #31516c; align-self: center; }
.badge.success { background: #dcfce7; color: #166534; }
.badge.failed { background: #fee2e2; color: #b91c1c; }
.failed-box { background: #fff1f2; border-color: #fecdd3; }
@media (max-width: 1100px) {
  .stats, .shortcut-grid, .top-grid, .history-grid, .filters, .history-stats { grid-template-columns: 1fr; min-width: 0; }
}
@media (max-width: 768px) {
  .page { padding: 20px; }
  .hero { flex-direction: column; }
}
</style>
