<template>
  <div class="page-shell">
    <section class="page-header">
      <div>
        <p class="eyebrow">交接总览</p>
        <h1>从资料接入到问答反馈的运行态。</h1>
        <p>这里展示 Skill、资料、AI 作业和问答质量，让演示直接围绕团队交接闭环展开。</p>
      </div>
      <div class="row">
        <button class="ghost-btn" @click="refreshAll">刷新</button>
        <button class="primary-btn" @click="router.push('/skill')">进入 Skill 工作台</button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card"><span>Skill 数</span><strong>{{ skills.length }}</strong></article>
      <article class="metric-card"><span>接入来源</span><strong>{{ sourceTotal }}</strong></article>
      <article class="metric-card"><span>问答次数</span><strong>{{ aiStats.usage || 0 }}</strong></article>
      <article class="metric-card"><span>无答案率</span><strong>{{ percent(aiStats.noAnswerRate) }}</strong></article>
      <article class="metric-card"><span>机器人触发</span><strong>{{ aiStats.botEventCount || 0 }}</strong></article>
      <article class="metric-card"><span>群聊绑定</span><strong>{{ botStatus.bindingCount || 0 }}</strong></article>
    </section>

    <section class="overview-grid">
      <article class="panel">
        <div class="row">
          <div>
            <p class="section-label">演示路径</p>
            <h2>五步完成交接助手</h2>
          </div>
        </div>
        <div class="step-list">
          <button v-for="step in workflowSteps" :key="step.title" @click="router.push(step.to)">
            <strong>{{ step.title }}</strong>
            <span>{{ step.desc }}</span>
          </button>
        </div>
      </article>

      <article class="panel">
        <div class="row">
          <div>
            <p class="section-label">最近 Skill</p>
            <h2>当前可演示对象</h2>
          </div>
          <button class="ghost-btn" @click="router.push('/skill')">管理</button>
        </div>
        <div class="data-list">
          <button v-for="skill in skills.slice(0, 6)" :key="skill.id" class="list-row" @click="router.push('/skill')">
            <span>
              <strong>{{ skill.name }}</strong>
              <small>{{ skill.roleDescription || '暂无角色说明' }}</small>
            </span>
            <em class="badge" :class="statusClass(skill.status)">{{ statusLabel(skill.status) }}</em>
          </button>
          <p v-if="!skills.length" class="empty-state">还没有 Skill，先进入工作台创建一个。</p>
        </div>
      </article>

      <article class="panel">
        <div class="row">
          <div>
            <p class="section-label">演示检查清单</p>
            <h2>网站与飞书双入口是否就绪</h2>
          </div>
          <button class="ghost-btn" @click="refreshAll">重查</button>
        </div>
        <div class="data-list">
          <div class="list-row">
            <span><strong>飞书凭证</strong><small>App ID / Secret</small></span>
            <em class="badge" :class="botStatus.credentialsConfigured ? 'success' : 'danger'">{{ botStatus.credentialsConfigured ? '已配置' : '缺失' }}</em>
          </div>
          <div class="list-row">
            <span><strong>机器人长连接</strong><small>FEISHU_BOT_* 开关</small></span>
            <em class="badge" :class="botStatus.botEnabled && botStatus.longConnectionEnabled ? 'success' : 'warning'">{{ botStatus.botEnabled && botStatus.longConnectionEnabled ? '启用' : '未启用' }}</em>
          </div>
          <div class="list-row">
            <span><strong>Skill</strong><small>至少一个交接助手</small></span>
            <em class="badge" :class="skills.length ? 'success' : 'warning'">{{ skills.length ? '已创建' : '未创建' }}</em>
          </div>
          <div class="list-row">
            <span><strong>飞书群绑定</strong><small>chat_id -> skill_id</small></span>
            <em class="badge" :class="(botStatus.bindingCount || 0) > 0 ? 'success' : 'warning'">{{ botStatus.bindingCount || 0 }}</em>
          </div>
          <div class="list-row">
            <span><strong>最近成功问答</strong><small>用于现场演示</small></span>
            <em class="badge" :class="(aiStats.usage || 0) > 0 ? 'success' : 'warning'">{{ aiStats.usage || 0 }}</em>
          </div>
        </div>
      </article>
    </section>

    <section class="overview-grid">
      <article class="panel">
        <div class="row">
          <div>
            <p class="section-label">AI 作业</p>
            <h2>同步、蒸馏和问答审计</h2>
          </div>
          <button class="ghost-btn" @click="router.push('/ai')">查看全部</button>
        </div>
        <div class="data-list">
          <button v-for="job in jobs.slice(0, 8)" :key="job.id" class="list-row" @click="router.push({ path: '/ai', query: { history: job.id } })">
            <span>
              <strong>{{ job.skillName || job.noteTitle || jobTypeLabel(job.jobType) }}</strong>
              <small>{{ jobTypeLabel(job.jobType) }} · {{ formatDate(job.createTime) }}</small>
            </span>
            <em class="badge" :class="jobStatusClass(job.status)">{{ jobStatusLabel(job.status) }}</em>
          </button>
          <p v-if="!jobs.length" class="empty-state">还没有 AI 作业。</p>
        </div>
      </article>

      <article class="panel">
        <div class="row">
          <div>
            <p class="section-label">质量信号</p>
            <h2>知识库是否够用</h2>
          </div>
          <button class="ghost-btn" @click="router.push('/admin/ai')">质量统计</button>
        </div>
        <div class="quality-grid">
          <article><strong>{{ aiStats.failedCount || 0 }}</strong><span>失败调用</span></article>
          <article><strong>{{ aiStats.noAnswerCount || 0 }}</strong><span>无答案</span></article>
          <article><strong>{{ aiStats.negativeFeedbackCount || 0 }}</strong><span>负反馈</span></article>
        </div>
        <div class="question-list">
          <strong>高频问题</strong>
          <p v-for="question in aiStats.topQuestions || []" :key="question">{{ question }}</p>
          <p v-if="!aiStats.topQuestions?.length" class="muted">积累问答后会显示知识缺口。</p>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { noteApi } from '@/api/note'
import { skillApi } from '@/api/skill'

const router = useRouter()
const skills = ref([])
const jobs = ref([])
const aiStats = reactive({ usage: 0, failedCount: 0, noAnswerCount: 0, negativeFeedbackCount: 0, noAnswerRate: 0, topQuestions: [] })
const botStatus = reactive({ credentialsConfigured: false, botEnabled: false, longConnectionEnabled: false, bindingCount: 0 })

const workflowSteps = [
  { title: '创建 Skill', desc: '定义项目/角色交接边界', to: '/skill' },
  { title: '接入资料', desc: '同步飞书或上传本地文本', to: '/skill' },
  { title: '蒸馏规则', desc: '生成清单、原则和风险点', to: '/skill' },
  { title: '发起问答', desc: '基于知识库回答新人问题', to: '/skill' },
  { title: '查看质量', desc: '分析无答案和反馈', to: '/admin/ai' }
]

const sourceTotal = computed(() => skills.value.reduce((total, skill) => total + (skill.sourceCount || 0), 0))

const refreshAll = async () => {
  const [skillList, jobList, stats] = await Promise.all([
    skillApi.list(),
    noteApi.getJobs({ limit: 20 }),
    skillApi.adminStats()
  ])
  skills.value = skillList
  jobs.value = jobList
  Object.assign(aiStats, stats)
  Object.assign(botStatus, await skillApi.botStatus().catch(() => ({ credentialsConfigured: false, botEnabled: false, longConnectionEnabled: false, bindingCount: 0 })))
}

const percent = (value) => `${Math.round((value || 0) * 100)}%`
const formatDate = (value) => value ? new Date(value).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '未知时间'
const statusLabel = (value) => ({ DRAFT: '草稿', SYNCING: '同步中', SOURCES_READY: '已接入', DISTILLING: '蒸馏中', DISTILLED: '已蒸馏', FAILED: '失败' }[value] || value || '草稿')
const statusClass = (value) => value === 'DISTILLED' ? 'success' : value === 'FAILED' ? 'danger' : 'warning'
const jobTypeLabel = (value) => ({ SKILL_SYNC: '来源同步', SKILL_DISTILL: 'Skill 蒸馏', SKILL_ASK: 'Skill 问答', SUMMARY: '摘要', ORGANIZE: '整理', EXTRACT_TASKS: '提取行动', WEEKLY_REVIEW: '复盘' }[value] || value)
const jobStatusLabel = (value) => ({ PENDING: '排队中', PROCESSING: '处理中', SUCCESS: '已完成', FAILED: '失败' }[value] || value || '未知')
const jobStatusClass = (value) => value === 'SUCCESS' ? 'success' : value === 'FAILED' ? 'danger' : 'warning'

onMounted(refreshAll)
</script>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.step-list {
  display: grid;
  gap: 8px;
}

.step-list button {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 13px;
  text-align: left;
  display: grid;
  gap: 3px;
}

.step-list span,
.list-row small {
  display: block;
  color: var(--text-muted);
}

.quality-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.quality-grid article {
  background: var(--panel-soft);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 14px;
  display: grid;
  gap: 4px;
}

.quality-grid strong {
  font-size: 24px;
}

.question-list {
  margin-top: 16px;
  display: grid;
  gap: 8px;
}

.question-list p {
  margin: 0;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--text-muted);
}

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
