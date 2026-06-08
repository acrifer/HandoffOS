<template>
  <div class="page">
    <section class="hero">
      <div>
        <p class="eyebrow">Skill 工作台</p>
        <h1>团队交接 AI 控制台</h1>
        <p>创建 Skill，接入飞书和本地资料，同步 Dify Knowledge，完成蒸馏、问答、引用和反馈闭环。</p>
      </div>
      <button class="primary-btn" @click="createSkill">保存左侧 Skill</button>
    </section>

    <section class="workflow-strip">
      <article><strong>1</strong><span>创建 Skill</span></article>
      <article><strong>2</strong><span>接入资料</span></article>
      <article><strong>3</strong><span>蒸馏规则</span></article>
      <article><strong>4</strong><span>RAG 问答</span></article>
      <article><strong>5</strong><span>引用反馈</span></article>
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
            <code v-if="activeSkill.difyDatasetId" class="dataset-id">Dify Dataset: {{ activeSkill.difyDatasetId }}</code>
          </div>
          <div class="metric-grid">
            <article><strong>{{ activeSkill.sourceCount || 0 }}</strong><span>来源</span></article>
            <article><strong>{{ activeSkill.documentSourceCount || 0 }}</strong><span>文档</span></article>
            <article><strong>{{ activeSkill.chatSourceCount || 0 }}</strong><span>群聊</span></article>
            <article><strong>{{ indexingReadyCount }}</strong><span>已入库</span></article>
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
            <small v-if="activeJob">
              当前作业：{{ jobTypeLabel(activeJob.jobType) }} / {{ jobStatusLabel(activeJob.status) }}
              <span v-if="activeJob.difyWorkflowRunId"> / {{ activeJob.difyWorkflowRunId }}</span>
            </small>
          </article>
        </section>

        <section class="tabs">
          <button v-for="tab in tabs" :key="tab.id" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id">
            {{ tab.label }}
          </button>
        </section>

        <section v-if="distillTabIds.includes(activeTab)" class="result-panel">
          <div v-if="resultItems.length" class="result-list">
            <article v-for="item in resultItems" :key="item" class="result-item">{{ item }}</article>
          </div>
          <p v-else class="muted">蒸馏完成后会在这里展示结构化结果。</p>
        </section>

        <section v-else-if="activeTab === 'knowledge'" class="knowledge-panel">
          <div class="row">
            <div>
              <p class="section-label">知识库管理</p>
              <h3>上传、切分和同步 Dify Knowledge</h3>
            </div>
            <button class="secondary-btn" :disabled="summarizing" @click="summarizeSkill">
              {{ summarizing ? '生成中...' : '生成摘要' }}
            </button>
          </div>
          <div class="document-form">
            <input v-model="documentForm.title" type="text" placeholder="资料标题，例如：支付发布流程" />
            <input v-model="documentForm.sourceUrl" type="text" placeholder="来源链接或说明，可选" />
            <textarea v-model="documentForm.content" placeholder="粘贴 Markdown/TXT/飞书导出的交接资料文本"></textarea>
            <button class="primary-btn" :disabled="documentBusy || !documentForm.title.trim()" @click="createDocument">
              {{ documentBusy ? '处理中...' : '上传并解析' }}
            </button>
          </div>
          <div class="document-list">
            <article v-for="doc in documents" :key="doc.id" class="document-item">
              <div class="source-meta">
                <span>{{ doc.sourceType }}</span>
                <em :class="['indexing', indexingClass(doc.status)]">{{ doc.status }}</em>
              </div>
              <strong>{{ doc.title }}</strong>
              <p v-if="doc.summary">{{ doc.summary }}</p>
              <small>{{ doc.chunkCount || 0 }} 个 chunk <span v-if="doc.difyDocumentId">/ {{ doc.difyDocumentId }}</span></small>
              <div class="inline-actions">
                <button class="ghost-btn" @click="parseDocument(doc.id)">重新切分</button>
                <button class="secondary-btn" @click="vectorizeDocument(doc.id)">同步向量库</button>
              </div>
            </article>
            <p v-if="!documents.length" class="muted">还没有本地知识文档。同步飞书来源或手动上传后会出现在这里。</p>
          </div>
        </section>

        <section v-else-if="activeTab === 'search'" class="knowledge-panel">
          <div class="row">
            <div>
              <p class="section-label">权限隔离检索</p>
              <h3>只搜索当前 Skill 的知识片段</h3>
            </div>
            <button class="secondary-btn" @click="searchKnowledge">检索</button>
          </div>
          <div class="ask-box">
            <input v-model="searchQuery" type="text" placeholder="输入要检索的流程、负责人、风险点" @keyup.enter="searchKnowledge" />
            <button class="primary-btn" @click="searchKnowledge">搜索</button>
          </div>
          <div class="result-list">
            <article v-for="result in searchResults" :key="result.chunkId" class="result-item">
              <strong>{{ result.sourceTitle }} · {{ result.sourceLocator }}</strong>
              <p>{{ result.contentPreview }}</p>
              <small>匹配分：{{ result.score }}</small>
            </article>
            <p v-if="!searchResults.length" class="muted">输入问题后会展示命中的知识片段。</p>
          </div>
        </section>

        <section v-else-if="activeTab === 'history'" class="knowledge-panel">
          <div class="row">
            <div>
              <p class="section-label">问答日志</p>
              <h3>质量评估和知识缺口追踪</h3>
            </div>
            <button class="ghost-btn" @click="loadQaHistory">刷新记录</button>
          </div>
          <div class="chat-list">
            <article v-for="log in qaHistory" :key="log.id" class="chat-item">
              <strong>{{ log.question }}</strong>
              <p>{{ log.answer || '这次调用没有返回答案。' }}</p>
              <small v-if="log.citations?.length">引用：{{ log.citations.join(' / ') }}</small>
              <div class="inline-actions">
                <button class="ghost-btn" @click="sendFeedback(log.id, 5, 'HELPFUL')">有帮助</button>
                <button class="ghost-btn" @click="sendFeedback(log.id, 1, 'NEEDS_FIX')">需修正</button>
              </div>
            </article>
            <p v-if="!qaHistory.length" class="muted">还没有问答日志。</p>
          </div>
        </section>

        <section v-else-if="activeTab === 'ask'" class="ask-panel">
          <div v-if="recommendedQuestions.length" class="question-chips">
            <button v-for="item in recommendedQuestions" :key="item.question" class="ghost-btn" @click="question = item.question">
              {{ item.question }}
            </button>
          </div>
          <div class="ask-box">
            <input v-model="question" type="text" placeholder="问一个交接问题，例如：新人第一天应该先看什么？" @keyup.enter="askSkill" />
            <button class="primary-btn" :disabled="asking || !question.trim()" @click="askSkill">
              {{ asking ? '回答中...' : '提问' }}
            </button>
          </div>
          <div class="chat-list">
            <article v-for="chat in qaHistory" :key="chat.id" class="chat-item">
              <strong>{{ chat.question }}</strong>
              <p>{{ chat.answer || '等待 AI 回答写入。' }}</p>
              <small v-if="chat.citations?.length">引用：{{ chat.citations.join(' / ') }}</small>
              <div class="inline-actions">
                <button class="ghost-btn" @click="sendFeedback(chat.id, 5, 'HELPFUL')">有帮助</button>
                <button class="ghost-btn" @click="sendFeedback(chat.id, 1, 'NEEDS_FIX')">需修正</button>
              </div>
            </article>
            <p v-if="!qaHistory.length" class="muted">蒸馏完成后，可以在这里向交接助手提问。</p>
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
              <div class="source-meta">
                <span>{{ sourceTypeLabel(source.sourceType) }}</span>
                <em :class="['indexing', indexingClass(source.indexingStatus)]">{{ indexingLabel(source.indexingStatus) }}</em>
              </div>
              <strong>{{ source.title || source.externalId }}</strong>
              <p>{{ source.contentPreview }}</p>
              <code v-if="source.difyDocumentId">{{ source.difyDocumentId }}</code>
            </article>
          </div>
        </section>

        <section class="bot-panel">
          <div class="row">
            <div>
              <p class="section-label">飞书群入口</p>
              <h3>群内 @机器人协作</h3>
            </div>
            <button class="ghost-btn" @click="loadBotForSkill">刷新</button>
          </div>
          <div v-if="botBindings.length" class="bot-binding-list">
            <article v-for="binding in botBindings" :key="binding.id" class="bot-binding-item">
              <strong>{{ binding.chatName || binding.chatId }}</strong>
              <p>{{ binding.chatId }}</p>
              <span :class="['status', binding.enabled ? 'ok' : 'pending']">{{ binding.enabled ? '已绑定' : '已停用' }}</span>
            </article>
          </div>
          <p v-else class="muted">当前 Skill 还没有绑定飞书群。到设置页绑定后，群内 @机器人即可问答、同步资料和创建任务。</p>
          <div class="bot-command-list">
            <button v-for="command in botCommands" :key="command" class="ghost-btn" @click="copyBotCommand(command)">
              {{ command }}
            </button>
          </div>
          <div class="bot-event-list">
            <article v-for="event in botEvents" :key="event.id" class="bot-event-item">
              <div>
                <strong>{{ event.commandType }}</strong>
                <span :class="['status', jobStatusClass(event.status)]">{{ event.status }}</span>
              </div>
              <p>{{ event.requestText }}</p>
              <small v-if="event.errorMessage" class="error-text">{{ event.errorMessage }}</small>
            </article>
            <p v-if="!botEvents.length" class="muted">暂无机器人事件。</p>
          </div>
        </section>

        <section class="audit-panel">
          <div class="row">
            <div>
              <p class="section-label">作业审计</p>
              <h3>同步、蒸馏和问答记录</h3>
            </div>
            <button class="ghost-btn" @click="loadSkillJobs(activeSkill.id)">刷新作业</button>
          </div>
          <div class="job-list">
            <article v-for="job in activeJobs" :key="job.id" class="job-item">
              <div>
                <strong>{{ jobTypeLabel(job.jobType) }}</strong>
                <span :class="['status', jobStatusClass(job.status)]">{{ jobStatusLabel(job.status) }}</span>
              </div>
              <p v-if="job.result?.datasetId">Dataset: <code>{{ job.result.datasetId }}</code></p>
              <p v-if="job.difyWorkflowRunId">Dify Run: <code>{{ job.difyWorkflowRunId }}</code></p>
              <p v-if="job.errorMessage" class="error-text">{{ job.errorMessage }}</p>
              <small>{{ formatTime(job.createTime) }}</small>
            </article>
            <p v-if="!activeJobs.length" class="muted">还没有作业记录。</p>
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
const summarizing = ref(false)
const documentBusy = ref(false)
const activeJob = ref(null)
const activeJobs = ref([])
const botBindings = ref([])
const botEvents = ref([])
const statusText = ref('')
const documents = ref([])
const qaHistory = ref([])
const recommendedQuestions = ref([])
const searchQuery = ref('')
const searchResults = ref([])
let pollTimer = null

const draft = reactive({ name: '', roleDescription: '' })
const sourceForm = reactive({ documentRefs: '', chatId: '', startTime: '', endTime: '' })
const documentForm = reactive({ title: '', sourceUrl: '', content: '' })
const botCommands = [
  '/帮助',
  '/同步 最近20条',
  '/蒸馏',
  '/资料 标题 | 内容',
  '/任务 新建 标题 | 说明',
  '/统计'
]

const tabs = [
  { id: 'roleBoundaries', label: '角色边界' },
  { id: 'workPrinciples', label: '工作原则' },
  { id: 'decisionRules', label: '决策规则' },
  { id: 'workflowChecklists', label: '检查清单' },
  { id: 'communicationStyle', label: '沟通风格' },
  { id: 'riskWarnings', label: '风险提示' },
  { id: 'handoffQuestions', label: '交接问题' },
  { id: 'knowledge', label: '知识库' },
  { id: 'search', label: '检索' },
  { id: 'history', label: '问答日志' },
  { id: 'ask', label: '问答' }
]
const distillTabIds = ['roleBoundaries', 'workPrinciples', 'decisionRules', 'workflowChecklists', 'communicationStyle', 'riskWarnings', 'handoffQuestions']

const resultItems = computed(() => activeSkill.value?.distillResult?.[activeTab.value] || [])
const indexingReadyCount = computed(() =>
  activeSkill.value?.sources?.filter(source => ['completed', 'success'].includes(String(source.indexingStatus || '').toLowerCase())).length || 0
)

const fetchSkills = async () => {
  skills.value = await skillApi.list()
  if (!activeSkill.value && skills.value.length) {
    await selectSkill(skills.value[0].id)
  }
}

const selectSkill = async (skillId) => {
  stopPoll()
  activeSkill.value = await skillApi.getDetail(skillId)
  await loadDocuments()
  await loadQaHistory()
  await loadRecommendedQuestions()
  await loadSkillJobs(skillId)
  await loadBotForSkill()
  activeJob.value = activeJobs.value[0] || null
  if (activeJob.value && ['PENDING', 'PROCESSING'].includes(activeJob.value.status)) {
    pollJob(activeJob.value.id)
  }
}

const refreshActive = async () => {
  if (!activeSkill.value) return
  activeSkill.value = await skillApi.getDetail(activeSkill.value.id)
  await loadDocuments()
  await loadQaHistory()
  await loadRecommendedQuestions()
  await loadSkillJobs(activeSkill.value.id)
  await loadBotForSkill()
  await fetchSkills()
}

const loadSkillJobs = async (skillId) => {
  activeJobs.value = await skillApi.getJobs(skillId, { limit: 10 })
}

const loadBotForSkill = async () => {
  if (!activeSkill.value) return
  const [bindings, events] = await Promise.all([
    skillApi.botBindings({ skillId: activeSkill.value.id }).catch(() => []),
    skillApi.botEvents({ skillId: activeSkill.value.id, limit: 8 }).catch(() => [])
  ])
  botBindings.value = bindings || []
  botEvents.value = events || []
  if (!sourceForm.chatId && botBindings.value[0]?.chatId) {
    sourceForm.chatId = botBindings.value[0].chatId
  }
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
    statusText.value = `同步失败：${error.message || '请确认飞书凭证、权限和输入的文档/群聊 ID。'}`
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
  try {
    const answer = await skillApi.askKnowledge(activeSkill.value.id, { question: question.value.trim() })
    question.value = ''
    statusText.value = answer.noAnswer ? '知识库暂时没有足够信息，建议补充资料。' : '已生成回答并记录日志。'
    await loadQaHistory()
  } catch (error) {
    statusText.value = `问答失败：${error.message || 'AI 接口调用失败。'}`
  } finally {
    asking.value = false
  }
}

const loadDocuments = async () => {
  if (!activeSkill.value) return
  documents.value = await skillApi.listDocuments(activeSkill.value.id)
}

const createDocument = async () => {
  if (!activeSkill.value || !documentForm.title.trim()) return
  documentBusy.value = true
  try {
    const doc = await skillApi.createDocument(activeSkill.value.id, {
      title: documentForm.title.trim(),
      sourceType: 'MANUAL_TEXT',
      sourceUrl: documentForm.sourceUrl.trim(),
      content: documentForm.content.trim()
    })
    await skillApi.parseDocument(activeSkill.value.id, doc.id, { parseMode: 'paragraph' })
    await skillApi.vectorizeDocument(activeSkill.value.id, doc.id, { embeddingModel: 'dify-managed' })
    documentForm.title = ''
    documentForm.sourceUrl = ''
    documentForm.content = ''
    await loadDocuments()
    statusText.value = '资料已上传、切分并同步到 Dify Knowledge。'
  } catch (error) {
    statusText.value = `资料入库失败：${error.message || 'Dify Knowledge 调用失败。'}`
  } finally {
    documentBusy.value = false
  }
}

const parseDocument = async (documentId) => {
  if (!activeSkill.value) return
  try {
    await skillApi.parseDocument(activeSkill.value.id, documentId, { parseMode: 'paragraph' })
    await loadDocuments()
  } catch (error) {
    statusText.value = `切分失败：${error.message || '文档解析失败。'}`
  }
}

const vectorizeDocument = async (documentId) => {
  if (!activeSkill.value) return
  try {
    await skillApi.vectorizeDocument(activeSkill.value.id, documentId, { embeddingModel: 'dify-managed' })
    await loadDocuments()
  } catch (error) {
    statusText.value = `向量化失败：${error.message || 'Dify Knowledge 调用失败。'}`
  }
}

const summarizeSkill = async () => {
  if (!activeSkill.value) return
  summarizing.value = true
  try {
    await skillApi.summarize(activeSkill.value.id)
    await loadDocuments()
    statusText.value = '已生成结构化摘要。'
  } catch (error) {
    statusText.value = `摘要生成失败：${error.message || 'AI 接口调用失败。'}`
  } finally {
    summarizing.value = false
  }
}

const searchKnowledge = async () => {
  if (!activeSkill.value) return
  searchResults.value = await skillApi.searchKnowledge(activeSkill.value.id, {
    query: searchQuery.value.trim(),
    limit: 8
  })
}

const loadQaHistory = async () => {
  if (!activeSkill.value) return
  qaHistory.value = await skillApi.getQaHistory(activeSkill.value.id, { page: 0, size: 20 })
}

const loadRecommendedQuestions = async () => {
  if (!activeSkill.value) return
  recommendedQuestions.value = await skillApi.getRecommendedQuestions(activeSkill.value.id)
}

const sendFeedback = async (qaLogId, rating, feedbackType) => {
  await skillApi.feedback(qaLogId, { rating, feedbackType })
  statusText.value = rating >= 4 ? '已记录正向反馈。' : '已记录纠错反馈，管理员统计会纳入分析。'
}

const copyBotCommand = async (command) => {
  try {
    await navigator.clipboard.writeText(command)
    statusText.value = `已复制飞书指令：${command}`
  } catch (error) {
    statusText.value = command
  }
}

const pollJob = (jobId) => {
  stopPoll()
  const runner = async () => {
    try {
      const job = await noteApi.getJob(jobId)
      activeJob.value = job
      if (job.status === 'SUCCESS' || job.status === 'FAILED') {
        distilling.value = false
        asking.value = false
        if (job.status === 'FAILED') {
          statusText.value = `作业失败：${job.errorMessage || '外部 API 调用失败。'}`
        }
        await refreshActive()
        stopPoll()
        return
      }
      pollTimer = window.setTimeout(runner, 2000)
    } catch (error) {
      distilling.value = false
      asking.value = false
      statusText.value = `作业状态查询失败：${error.message || '无法获取作业状态。'}`
      stopPoll()
    }
  }
  runner()
}

const stopPoll = () => {
  if (pollTimer) {
    window.clearTimeout(pollTimer)
    pollTimer = null
  }
}

const serializeDateTime = (value) => value || null
const statusLabel = (value) => ({ DRAFT: '草稿', SYNCING: '同步中', SOURCES_READY: '已同步', DISTILLING: '蒸馏中', DISTILLED: '已蒸馏', FAILED: '失败' }[value] || value || '草稿')
const statusClass = (value) => value === 'DISTILLED' ? 'ok' : value === 'FAILED' ? 'bad' : 'pending'
const sourceTypeLabel = (value) => ({ FEISHU_DOC: '飞书文档', FEISHU_CHAT: '飞书群聊' }[value] || value)
const jobTypeLabel = (value) => ({ SKILL_DISTILL: 'Skill 蒸馏', SKILL_ASK: 'Skill 问答' }[value] || value)
const jobStatusLabel = (value) => ({ PENDING: '排队中', PROCESSING: '处理中', SUCCESS: '已完成', FAILED: '失败' }[value] || value)
const jobStatusClass = (value) => value === 'SUCCESS' ? 'ok' : value === 'FAILED' ? 'bad' : 'pending'
const formatTime = (value) => value ? new Date(value).toLocaleString() : ''
const indexingLabel = (value) => ({
  pending: '待入库',
  indexing: '索引中',
  completed: '已入库',
  success: '已入库',
  failed: '入库失败'
}[String(value || '').toLowerCase()] || value || '未入库')
const indexingClass = (value) => {
  const normalized = String(value || '').toLowerCase()
  if (['completed', 'success'].includes(normalized)) return 'ok'
  if (normalized.includes('failed')) return 'bad'
  return 'pending'
}

onMounted(fetchSkills)
onBeforeUnmount(stopPoll)
</script>

<style scoped>
.page { max-width: 1480px; margin: 0 auto; padding: 24px; }
.hero, .summary-band, .panel, .result-panel, .ask-panel, .knowledge-panel, .source-panel, .bot-panel, .audit-panel, .skill-list, .workspace.empty, .workflow-strip { background: var(--panel); border: 1px solid var(--border); border-radius: var(--radius); box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04); }
.hero { padding: 24px; margin-bottom: 12px; display: flex; justify-content: space-between; gap: 24px; color: var(--text); }
.workflow-strip { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 0; margin-bottom: 16px; overflow: hidden; }
.workflow-strip article { padding: 14px 16px; border-right: 1px solid var(--border); display: flex; align-items: center; gap: 10px; }
.workflow-strip article:last-child { border-right: 0; }
.workflow-strip strong { width: 24px; height: 24px; border-radius: 999px; display: grid; place-items: center; background: var(--blue-soft); color: var(--blue); font-size: 12px; }
.workflow-strip span { color: var(--text-muted); font-weight: 700; }
.eyebrow, .section-label { margin: 0 0 8px; font-size: 12px; text-transform: uppercase; color: var(--blue); letter-spacing: 0.08em; font-weight: 800; }
.eyebrow.dark, .section-label { color: var(--blue); }
.hero h1 { margin: 0 0 10px; font-size: 30px; line-height: 1.18; letter-spacing: 0; }
.hero p, .muted, .source-item p, .chat-item p, .summary-band p { color: #607487; line-height: 1.7; }
.hero p { color: var(--text-muted); }
.layout { display: grid; grid-template-columns: 300px 1fr; gap: 16px; align-items: start; }
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
.workspace { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 16px; align-items: start; }
.workspace.empty { padding: 28px; color: #607487; }
.summary-band { grid-column: 1 / -1; padding: 18px; display: flex; justify-content: space-between; gap: 18px; }
.summary-band h2, .panel h3, .source-panel h3 { margin: 0 0 6px; color: #102033; }
.metric-grid, .tools-grid, .source-grid { display: grid; gap: 12px; }
.metric-grid { grid-template-columns: repeat(4, 96px); }
.metric-grid article { background: #f8fbfd; border: 1px solid #e1eaf1; border-radius: 8px; padding: 14px; display: grid; gap: 4px; }
.metric-grid strong { font-size: 24px; color: #102033; }
.dataset-id { display: inline-block; margin-top: 10px; color: #0f5f67; background: #eff9f9; border: 1px solid #cce7e7; border-radius: 6px; padding: 6px 8px; font-size: 12px; }
.tools-grid { grid-column: 1 / -1; grid-template-columns: 1.2fr 0.8fr; }
.panel, .result-panel, .ask-panel, .knowledge-panel, .source-panel, .bot-panel, .audit-panel { padding: 18px; }
.tabs, .result-panel, .ask-panel, .knowledge-panel { grid-column: 1; }
.source-panel { grid-column: 2; grid-row: 3 / span 2; }
.bot-panel { grid-column: 2; }
.audit-panel { grid-column: 2; }
.document-form { display: grid; gap: 10px; margin-bottom: 14px; }
.document-list { display: grid; gap: 12px; }
.document-item { border: 1px solid #e1eaf1; background: #f8fbfd; border-radius: 8px; padding: 14px; display: grid; gap: 8px; }
.document-item strong { color: #102033; }
.document-item p { margin: 0; color: #52667d; white-space: pre-line; line-height: 1.6; }
.inline-actions, .question-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.question-chips { margin-bottom: 12px; }
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
.source-meta { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.source-item span { color: #0f766e; font-size: 12px; font-weight: 700; }
.source-item code { display: inline-block; margin-top: 8px; color: #4b6175; font-size: 12px; word-break: break-all; }
.indexing { border-radius: 999px; padding: 4px 8px; font-size: 11px; font-style: normal; }
.indexing.ok { background: #dcfce7; color: #166534; }
.indexing.warn { background: #fff7ed; color: #9a3412; }
.indexing.bad { background: #fee2e2; color: #b91c1c; }
.indexing.pending { background: #eaf0f6; color: #31516c; }
.source-item strong { display: block; margin: 7px 0; color: #102033; }
.job-list { display: grid; gap: 10px; }
.job-item { border: 1px solid #e1eaf1; background: #f8fbfd; border-radius: 8px; padding: 13px; display: grid; gap: 8px; }
.job-item div { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.job-item p { margin: 0; color: #52667d; word-break: break-all; }
.job-item code { color: #0f5f67; }
.error-text { color: #b91c1c !important; }
.bot-binding-list,
.bot-command-list,
.bot-event-list { display: grid; gap: 10px; }
.bot-binding-list { margin-bottom: 12px; }
.bot-command-list { grid-template-columns: 1fr; margin: 12px 0; }
.bot-binding-item,
.bot-event-item { border: 1px solid #e1eaf1; background: #f8fbfd; border-radius: 8px; padding: 13px; display: grid; gap: 6px; }
.bot-binding-item strong,
.bot-event-item strong { color: #102033; }
.bot-binding-item p,
.bot-event-item p { margin: 0; color: #52667d; word-break: break-all; line-height: 1.6; }
.bot-event-item div { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
small { color: #607487; }
@media (max-width: 1080px) {
  .layout, .workspace, .tools-grid, .summary-band, .form-grid { grid-template-columns: 1fr; }
  .summary-band, .tools-grid, .tabs, .result-panel, .ask-panel, .knowledge-panel, .source-panel, .bot-panel, .audit-panel { grid-column: 1; grid-row: auto; }
  .summary-band { display: grid; }
  .metric-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .workflow-strip { grid-template-columns: 1fr; }
  .workflow-strip article { border-right: 0; border-bottom: 1px solid var(--border); }
}
@media (max-width: 720px) {
  .page { padding: 20px; }
  .hero { flex-direction: column; }
  .ask-box { grid-template-columns: 1fr; }
}
</style>
