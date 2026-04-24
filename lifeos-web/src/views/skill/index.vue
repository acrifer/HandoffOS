<template>
  <div class="page">
    <section class="hero">
      <div>
        <p class="eyebrow">团队交接 Skill</p>
        <h1>把飞书里的项目经验蒸馏成交接助手。</h1>
        <p>首版面向项目/角色交接，聚合飞书文档和群聊上下文，生成可问答的工作规则与检查清单。</p>
      </div>
      <button class="primary-btn" @click="createSkill">新建 Skill</button>
    </section>

    <section class="layout">
      <aside class="skill-list">
        <div class="new-skill">
          <input v-model="draft.name" type="text" placeholder="Skill 名称，例如：支付后端交接助手" />
          <textarea v-model="draft.roleDescription" placeholder="项目/角色边界、负责事项、交接目标"></textarea>
        </div>

        <button
          v-for="item in skills"
          :key="item.id"
          class="skill-item"
          :class="{ active: activeSkill?.id === item.id }"
          @click="selectSkill(item.id)"
        >
          <div>
            <strong>{{ item.name }}</strong>
            <p>{{ item.roleDescription || '还没有角色描述' }}</p>
          </div>
          <span :class="['status', statusClass(item.status)]">{{ statusLabel(item.status) }}</span>
        </button>
        <p v-if="!skills.length" class="muted">还没有交接 Skill，先创建一个项目/角色助手。</p>
      </aside>

      <main v-if="activeSkill" class="workspace">
        <section class="summary-band">
          <div>
            <p class="eyebrow dark">当前 Skill</p>
            <h2>{{ activeSkill.name }}</h2>
            <p>{{ activeSkill.roleDescription || '这个助手还缺少角色边界说明。' }}</p>
          </div>
          <div class="metric-grid">
            <article><strong>{{ activeSkill.sourceCount || 0 }}</strong><span>来源</span></article>
            <article><strong>{{ activeSkill.documentSourceCount || 0 }}</strong><span>文档</span></article>
            <article><strong>{{ activeSkill.chatSourceCount || 0 }}</strong><span>群聊</span></article>
          </div>
        </section>

        <section class="tools-grid">
          <article class="panel">
            <div class="row">
              <div>
                <p class="section-label">飞书来源</p>
                <h3>同步文档和群聊</h3>
              </div>
              <button class="secondary-btn" :disabled="syncing" @click="syncSources">
                {{ syncing ? '同步中...' : '同步来源' }}
              </button>
            </div>
            <textarea v-model="sourceForm.documentRefs" placeholder="飞书文档链接或 ID，每行一个"></textarea>
            <div class="form-grid">
              <input v-model="sourceForm.chatId" type="text" placeholder="飞书群 chat_id" />
              <input v-model="sourceForm.startTime" type="datetime-local" />
              <input v-model="sourceForm.endTime" type="datetime-local" />
            </div>
            <small>{{ statusText }}</small>
          </article>

          <article class="panel">
            <div class="row">
              <div>
                <p class="section-label">蒸馏</p>
                <h3>生成交接助手</h3>
              </div>
              <button class="primary-btn" :disabled="distilling || !activeSkill.sourceCount" @click="distillSkill">
                {{ distilling ? '蒸馏中...' : '开始蒸馏' }}
              </button>
            </div>
            <p class="muted">输出角色边界、工作原则、决策规则、检查清单、沟通风格、风险提示和交接问题。</p>
            <small v-if="activeJob">当前作业：{{ jobTypeLabel(activeJob.jobType) }} / {{ jobStatusLabel(activeJob.status) }}</small>
          </article>
        </section>

        <section class="tabs">
          <button v-for="tab in tabs" :key="tab.id" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id">
            {{ tab.label }}
          </button>
        </section>

        <section v-if="activeTab !== 'ask'" class="result-panel">
          <div v-if="resultItems.length" class="result-list">
            <article v-for="item in resultItems" :key="item" class="result-item">{{ item }}</article>
          </div>
          <p v-else class="muted">蒸馏完成后会在这里展示结构化结果。</p>
        </section>

        <section v-else class="ask-panel">
          <div class="ask-box">
            <input v-model="question" type="text" placeholder="问一个交接问题，例如：新人第一天应该先看什么？" @keyup.enter="askSkill" />
            <button class="primary-btn" :disabled="asking || !question.trim()" @click="askSkill">
              {{ asking ? '回答中...' : '提问' }}
            </button>
          </div>
          <div class="chat-list">
            <article v-for="chat in activeSkill.chats" :key="chat.id" class="chat-item">
              <strong>{{ chat.question }}</strong>
              <p>{{ chat.answer || '等待 AI 回答写入。' }}</p>
              <small v-if="chat.citations?.length">引用：{{ chat.citations.join(' / ') }}</small>
            </article>
            <p v-if="!activeSkill.chats?.length" class="muted">蒸馏完成后，可以在这里向交接助手提问。</p>
          </div>
        </section>

        <section class="source-panel">
          <div class="row">
            <div>
              <p class="section-label">来源预览</p>
              <h3>最近同步的材料</h3>
            </div>
            <button class="ghost-btn" @click="refreshActive">刷新</button>
          </div>
          <div class="source-grid">
            <article v-for="source in activeSkill.sources" :key="source.id" class="source-item">
              <span>{{ sourceTypeLabel(source.sourceType) }}</span>
              <strong>{{ source.title || source.externalId }}</strong>
              <p>{{ source.contentPreview }}</p>
            </article>
          </div>
        </section>
      </main>

      <main v-else class="workspace empty">选择或创建一个交接 Skill。</main>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { skillApi } from '@/api/skill'
import { noteApi } from '@/api/note'

const skills = ref([])
const activeSkill = ref(null)
const activeTab = ref('roleBoundaries')
const question = ref('')
const syncing = ref(false)
const distilling = ref(false)
const asking = ref(false)
const activeJob = ref(null)
const statusText = ref('')
let pollTimer = null

const draft = reactive({ name: '', roleDescription: '' })
const sourceForm = reactive({ documentRefs: '', chatId: '', startTime: '', endTime: '' })

const tabs = [
  { id: 'roleBoundaries', label: '角色边界' },
  { id: 'workPrinciples', label: '工作原则' },
  { id: 'decisionRules', label: '决策规则' },
  { id: 'workflowChecklists', label: '检查清单' },
  { id: 'communicationStyle', label: '沟通风格' },
  { id: 'riskWarnings', label: '风险提示' },
  { id: 'handoffQuestions', label: '交接问题' },
  { id: 'ask', label: '问答' }
]

const resultItems = computed(() => activeSkill.value?.distillResult?.[activeTab.value] || [])

const fetchSkills = async () => {
  skills.value = await skillApi.list()
  if (!activeSkill.value && skills.value.length) {
    await selectSkill(skills.value[0].id)
  }
}

const selectSkill = async (skillId) => {
  stopPoll()
  activeSkill.value = await skillApi.getDetail(skillId)
  const jobs = await skillApi.getJobs(skillId, { limit: 5 })
  activeJob.value = jobs[0] || null
  if (activeJob.value && ['PENDING', 'PROCESSING'].includes(activeJob.value.status)) {
    pollJob(activeJob.value.id)
  }
}

const refreshActive = async () => {
  if (!activeSkill.value) return
  activeSkill.value = await skillApi.getDetail(activeSkill.value.id)
  await fetchSkills()
}

const createSkill = async () => {
  if (!draft.name.trim()) {
    statusText.value = '请先填写 Skill 名称。'
    return
  }
  const created = await skillApi.create({
    name: draft.name.trim(),
    roleDescription: draft.roleDescription.trim()
  })
  draft.name = ''
  draft.roleDescription = ''
  await fetchSkills()
  await selectSkill(created.id)
}

const syncSources = async () => {
  if (!activeSkill.value) return
  syncing.value = true
  statusText.value = '正在从飞书同步来源...'
  try {
    activeSkill.value = await skillApi.syncSources(activeSkill.value.id, {
      documentRefs: sourceForm.documentRefs.split(/\r?\n/).map(item => item.trim()).filter(Boolean),
      chatId: sourceForm.chatId.trim(),
      startTime: serializeDateTime(sourceForm.startTime),
      endTime: serializeDateTime(sourceForm.endTime),
      limit: 80
    })
    statusText.value = '来源同步完成。'
    await fetchSkills()
  } catch (error) {
    console.error(error)
    statusText.value = '同步失败：请确认飞书凭证、权限和输入的文档/群聊 ID。'
  } finally {
    syncing.value = false
  }
}

const distillSkill = async () => {
  if (!activeSkill.value) return
  distilling.value = true
  activeJob.value = await skillApi.distill(activeSkill.value.id)
  pollJob(activeJob.value.id)
}

const askSkill = async () => {
  if (!activeSkill.value || !question.value.trim()) return
  asking.value = true
  activeJob.value = await skillApi.ask(activeSkill.value.id, { question: question.value.trim() })
  question.value = ''
  pollJob(activeJob.value.id)
}

const pollJob = (jobId) => {
  stopPoll()
  const runner = async () => {
    const job = await noteApi.getJob(jobId)
    activeJob.value = job
    if (job.status === 'SUCCESS' || job.status === 'FAILED') {
      distilling.value = false
      asking.value = false
      await refreshActive()
      stopPoll()
      return
    }
    pollTimer = window.setTimeout(runner, 2000)
  }
  runner()
}

const stopPoll = () => {
  if (pollTimer) {
    window.clearTimeout(pollTimer)
    pollTimer = null
  }
}

const serializeDateTime = (value) => value ? new Date(value).toISOString() : null
const statusLabel = (value) => ({ DRAFT: '草稿', SOURCES_READY: '已同步', DISTILLING: '蒸馏中', DISTILLED: '已蒸馏', FAILED: '失败' }[value] || value || '草稿')
const statusClass = (value) => value === 'DISTILLED' ? 'ok' : value === 'FAILED' ? 'bad' : 'pending'
const sourceTypeLabel = (value) => ({ FEISHU_DOC: '飞书文档', FEISHU_CHAT: '飞书群聊' }[value] || value)
const jobTypeLabel = (value) => ({ SKILL_DISTILL: 'Skill 蒸馏', SKILL_ASK: 'Skill 问答' }[value] || value)
const jobStatusLabel = (value) => ({ PENDING: '排队中', PROCESSING: '处理中', SUCCESS: '已完成', FAILED: '失败' }[value] || value)

onMounted(fetchSkills)
onBeforeUnmount(stopPoll)
</script>

<style scoped>
.page { max-width: 1320px; margin: 0 auto; padding: 32px; }
.hero, .summary-band, .panel, .result-panel, .ask-panel, .source-panel, .skill-list, .workspace.empty { background: #fff; border: 1px solid #d8e3ec; border-radius: 8px; box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06); }
.hero { padding: 30px; margin-bottom: 18px; display: flex; justify-content: space-between; gap: 24px; background: #113f5f; color: #fff; }
.eyebrow, .section-label { margin: 0 0 8px; font-size: 12px; text-transform: uppercase; color: rgba(255,255,255,0.72); }
.eyebrow.dark, .section-label { color: #5e7388; }
.hero h1 { margin: 0 0 12px; font-size: 36px; line-height: 1.12; letter-spacing: 0; }
.hero p, .muted, .source-item p, .chat-item p, .summary-band p { color: #607487; line-height: 1.7; }
.hero p { color: rgba(255,255,255,0.82); }
.layout { display: grid; grid-template-columns: 340px 1fr; gap: 18px; align-items: start; }
.skill-list { padding: 16px; display: grid; gap: 12px; }
.new-skill { display: grid; gap: 10px; }
input, textarea { width: 100%; border: 1px solid #d7e2eb; border-radius: 8px; background: #f8fbfd; padding: 11px 12px; font: inherit; color: #102033; box-sizing: border-box; }
textarea { min-height: 96px; resize: vertical; }
.skill-item { border: 1px solid #e1eaf1; background: #f8fbfd; border-radius: 8px; padding: 14px; text-align: left; cursor: pointer; display: grid; gap: 10px; }
.skill-item.active { border-color: #0f766e; box-shadow: inset 0 0 0 1px rgba(15,118,110,0.2); }
.skill-item strong { color: #102033; }
.skill-item p { margin: 6px 0 0; color: #607487; line-height: 1.5; }
.status { justify-self: start; border-radius: 999px; padding: 5px 9px; font-size: 12px; }
.status.ok { background: #dcfce7; color: #166534; }
.status.bad { background: #fee2e2; color: #b91c1c; }
.status.pending { background: #eaf0f6; color: #31516c; }
.workspace { display: grid; gap: 16px; }
.workspace.empty { padding: 28px; color: #607487; }
.summary-band { padding: 20px; display: flex; justify-content: space-between; gap: 18px; }
.summary-band h2, .panel h3, .source-panel h3 { margin: 0 0 6px; color: #102033; }
.metric-grid, .tools-grid, .source-grid { display: grid; gap: 12px; }
.metric-grid { grid-template-columns: repeat(3, 100px); }
.metric-grid article { background: #f8fbfd; border: 1px solid #e1eaf1; border-radius: 8px; padding: 14px; display: grid; gap: 4px; }
.metric-grid strong { font-size: 24px; color: #102033; }
.tools-grid { grid-template-columns: 1.2fr 0.8fr; }
.panel, .result-panel, .ask-panel, .source-panel { padding: 18px; }
.row { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 12px; }
.form-grid { display: grid; grid-template-columns: 1.2fr 1fr 1fr; gap: 10px; margin-top: 10px; }
.primary-btn, .secondary-btn, .ghost-btn { border: none; border-radius: 8px; padding: 10px 13px; font: inherit; cursor: pointer; }
.primary-btn { background: #103d5d; color: #fff; }
.secondary-btn { background: #dbf0f0; color: #0f5f67; }
.ghost-btn { background: #f5f9fc; color: #375068; }
button:disabled { opacity: 0.58; cursor: not-allowed; }
.tabs { display: flex; flex-wrap: wrap; gap: 8px; }
.tabs button { border: 1px solid #d8e3ec; background: #fff; color: #52667d; border-radius: 8px; padding: 9px 11px; cursor: pointer; }
.tabs button.active { background: #103d5d; color: #fff; border-color: #103d5d; }
.result-list { display: grid; gap: 10px; }
.result-item, .source-item, .chat-item { border: 1px solid #e1eaf1; background: #f8fbfd; border-radius: 8px; padding: 14px; color: #25364a; line-height: 1.7; }
.ask-box { display: grid; grid-template-columns: 1fr auto; gap: 10px; margin-bottom: 14px; }
.chat-list { display: grid; gap: 12px; }
.source-grid { grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); }
.source-item span { color: #0f766e; font-size: 12px; font-weight: 700; }
.source-item strong { display: block; margin: 7px 0; color: #102033; }
small { color: #607487; }
@media (max-width: 1080px) {
  .layout, .tools-grid, .summary-band, .form-grid { grid-template-columns: 1fr; }
  .summary-band { display: grid; }
  .metric-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .page { padding: 20px; }
  .hero { flex-direction: column; }
  .ask-box { grid-template-columns: 1fr; }
}
</style>
