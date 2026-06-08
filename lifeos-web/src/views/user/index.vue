<template>
  <div class="page-shell account-page">
    <section class="page-header">
      <div class="hero-copy">
        <p class="eyebrow">设置</p>
        <h1>管理控制台账号和演示状态。</h1>
        <p>查看当前登录账号、修改基础资料，并确认这个账号下的交接资料和行动项规模。</p>
      </div>

      <div class="hero-stats">
        <article>
          <strong>{{ dashboard.noteCount || 0 }}</strong>
          <span>交接资料</span>
        </article>
        <article>
          <strong>{{ dashboard.pendingTaskCount || 0 }}</strong>
          <span>待处理行动</span>
        </article>
        <article>
          <strong>{{ dashboard.weekCompletedTaskCount || 0 }}</strong>
          <span>本周完成</span>
        </article>
      </div>
    </section>

    <section v-if="loading" class="state-card">
      <div class="spinner"></div>
      <p>正在加载账户信息...</p>
    </section>

    <section v-else-if="user" class="content-grid">
      <article class="profile-card">
        <div class="profile-head">
          <div class="avatar">{{ user.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
          <div>
            <h2>{{ user.username }}</h2>
            <p>{{ user.email || '未填写邮箱' }}</p>
          </div>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <span>用户 ID</span>
            <strong>#{{ user.id }}</strong>
          </div>
          <div class="meta-item">
            <span>注册时间</span>
            <strong>{{ formatDate(user.createTime) }}</strong>
          </div>
          <div class="meta-item">
            <span>当前状态</span>
            <strong>已登录</strong>
          </div>
        </div>

        <div class="helper-card">
          <h3>管理建议</h3>
          <ul>
            <li>修改用户名后，当前登录态会自动刷新。</li>
            <li>建议补全邮箱，便于后续扩展通知或找回能力。</li>
            <li>修改密码后，当前会话会继续保持登录，但会刷新为新令牌。</li>
          </ul>
        </div>

        <button class="danger-btn" :disabled="loggingOut" @click="handleLogout">
          {{ loggingOut ? '退出中...' : '退出登录' }}
        </button>
      </article>

      <div class="manage-column">
        <article class="panel-card">
          <div class="panel-header">
            <div>
              <h3>基础资料</h3>
              <p>更新当前账号可编辑的信息。</p>
            </div>
          </div>

          <form class="form-grid" @submit.prevent="submitProfile">
            <label>
              <span>用户名</span>
              <input v-model="profileForm.username" type="text" placeholder="当前环境不支持修改用户名" disabled />
            </label>
            <label>
              <span>邮箱</span>
              <input v-model="profileForm.email" type="email" placeholder="请输入邮箱" />
            </label>

            <div v-if="profileMessage" class="message" :class="{ success: profileSuccess }">
              {{ profileMessage }}
            </div>

            <div class="form-actions">
              <button type="button" class="ghost-btn" @click="resetProfileForm">重置</button>
              <button type="submit" class="primary-btn" :disabled="savingProfile">
                {{ savingProfile ? '保存中...' : '保存资料' }}
              </button>
            </div>
          </form>
        </article>

        <article class="panel-card">
          <div class="panel-header">
            <div>
              <h3>演示环境说明</h3>
              <p>当前后端只开放了登录、注册、邮箱更新和退出接口。</p>
            </div>
          </div>
          <div class="helper-card">
            <ul>
              <li>密码修改接口尚未在当前 HandoffOS 后端兼容层开放。</li>
              <li>如果需要完整账号管理，优先补齐 `/user/password` 与任务兼容接口。</li>
              <li>本页仍可用于演示登录态、资料规模和邮箱更新能力。</li>
            </ul>
          </div>
        </article>

        <article class="panel-card">
          <div class="panel-header row">
            <div>
              <h3>飞书机器人</h3>
              <p>把飞书群 chat_id 绑定到 Skill，群内 @机器人即可同步资料、问答和创建行动项。</p>
            </div>
            <button class="ghost-btn" type="button" :disabled="botLoading" @click="fetchBotData">
              {{ botLoading ? '刷新中...' : '刷新' }}
            </button>
          </div>

          <div class="bot-status-grid">
            <article>
              <span>飞书凭证</span>
              <strong :class="botStatus.credentialsConfigured ? 'ok' : 'bad'">
                {{ botStatus.credentialsConfigured ? '已配置' : '缺失' }}
              </strong>
            </article>
            <article>
              <span>机器人开关</span>
              <strong :class="botStatus.botEnabled ? 'ok' : 'bad'">
                {{ botStatus.botEnabled ? '启用' : '关闭' }}
              </strong>
            </article>
            <article>
              <span>长连接</span>
              <strong :class="botStatus.longConnectionEnabled ? 'ok' : 'bad'">
                {{ botStatus.longConnectionEnabled ? '启用' : '关闭' }}
              </strong>
            </article>
          </div>

          <form class="form-grid bot-form" @submit.prevent="bindFeishuChat">
            <label>
              <span>chat_id</span>
              <input v-model.trim="botForm.chatId" type="text" placeholder="oc_xxx" />
            </label>
            <label>
              <span>群名称</span>
              <input v-model.trim="botForm.chatName" type="text" placeholder="例如：支付交接群" />
            </label>
            <label>
              <span>绑定 Skill</span>
              <select v-model.number="botForm.skillId">
                <option :value="null">选择 Skill</option>
                <option v-for="skill in skills" :key="skill.id" :value="skill.id">{{ skill.name }}</option>
              </select>
            </label>
            <div v-if="botMessage" class="message" :class="{ success: botSuccess }">{{ botMessage }}</div>
            <div class="form-actions">
              <button type="submit" class="primary-btn" :disabled="botSaving || !botForm.chatId || !botForm.skillId">
                {{ botSaving ? '绑定中...' : '绑定飞书群' }}
              </button>
            </div>
          </form>

          <div class="binding-list">
            <article v-for="binding in botBindings" :key="binding.id" class="binding-item">
              <div>
                <strong>{{ binding.chatName || binding.chatId }}</strong>
                <p>{{ binding.chatId }} · {{ binding.skillName || `Skill #${binding.skillId}` }}</p>
              </div>
              <div class="binding-actions">
                <span :class="['badge', binding.enabled ? 'success' : 'warning']">{{ binding.enabled ? '启用' : '停用' }}</span>
                <button class="ghost-btn" type="button" @click="disableBinding(binding.id)">停用</button>
              </div>
            </article>
            <p v-if="!botBindings.length" class="muted">还没有飞书群绑定。</p>
          </div>

          <div class="command-help">
            <p class="section-label">群内演示指令</p>
            <button v-for="command in botCommands" :key="command" type="button" class="command-chip" @click="copyCommand(command)">
              {{ command }}
            </button>
          </div>

          <div class="event-list">
            <p class="section-label">最近事件</p>
            <article v-for="event in botEvents" :key="event.id" class="event-item">
              <strong>{{ event.commandType }}</strong>
              <span :class="['badge', event.status === 'SUCCESS' ? 'success' : event.status === 'FAILED' ? 'danger' : 'warning']">
                {{ event.status }}
              </span>
              <p>{{ event.requestText }}</p>
              <small v-if="event.errorMessage">{{ event.errorMessage }}</small>
            </article>
            <p v-if="!botEvents.length" class="muted">暂无机器人事件。</p>
          </div>
        </article>
      </div>
    </section>

    <section v-else class="state-card error-card">
      <p>账户信息加载失败，请刷新后重试。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { behaviorApi } from '@/api/behavior'
import { skillApi } from '@/api/skill'
import { userApi } from '@/api/user'

const router = useRouter()

const user = ref(null)
const dashboard = ref({})
const loading = ref(true)
const savingProfile = ref(false)
const loggingOut = ref(false)
const profileMessage = ref('')
const profileSuccess = ref(false)
const skills = ref([])
const botBindings = ref([])
const botEvents = ref([])
const botLoading = ref(false)
const botSaving = ref(false)
const botMessage = ref('')
const botSuccess = ref(false)
const botStatus = reactive({
  credentialsConfigured: false,
  botEnabled: false,
  longConnectionEnabled: false,
  bindingCount: 0,
  eventCount: 0,
  failedEventCount: 0
})

const profileForm = reactive({
  username: '',
  email: ''
})

const botForm = reactive({
  chatId: '',
  chatName: '',
  skillId: null
})

const botCommands = [
  '/帮助',
  '/同步 最近20条',
  '/同步 文档 <飞书文档链接>',
  '/蒸馏',
  '/资料 发布检查 | 上线前确认灰度、回滚、负责人和值班群。',
  '/任务 新建 补充发布手册 | 缺少回滚步骤说明',
  '/任务 列表',
  '/统计'
]

const syncProfileForm = () => {
  profileForm.username = user.value?.username || ''
  profileForm.email = user.value?.email || ''
}

const decodeTokenPayload = (token) => {
  try {
    const [, payload] = token.split('.')
    if (!payload) return null
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map(char => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join('')
    )
    return JSON.parse(json)
  } catch (error) {
    return null
  }
}

const fetchPageData = async () => {
  loading.value = true
  try {
    const [userInfo, dashboardStats] = await Promise.all([
      userApi.getInfo(),
      behaviorApi.getDashboard().catch(() => ({}))
    ])
    user.value = userInfo
    dashboard.value = dashboardStats || {}
    syncProfileForm()
  } catch (error) {
    const token = localStorage.getItem('lifeos_token')
    const payload = token ? decodeTokenPayload(token) : null
    if (payload) {
      user.value = {
        id: payload.userId,
        username: payload.username || payload.sub || 'unknown',
        email: '',
        createTime: null
      }
      syncProfileForm()
    } else {
      console.error('Failed to fetch account page data', error)
    }
  } finally {
    loading.value = false
  }
  await fetchBotData()
}

const fetchBotData = async () => {
  botLoading.value = true
  try {
    const [skillList, status, bindings, events] = await Promise.all([
      skillApi.list().catch(() => []),
      skillApi.botStatus(),
      skillApi.botBindings().catch(() => []),
      skillApi.botEvents({ limit: 8 }).catch(() => [])
    ])
    skills.value = skillList || []
    Object.assign(botStatus, status || {})
    botBindings.value = bindings || []
    botEvents.value = events || []
  } catch (error) {
    botMessage.value = error.message || '飞书机器人状态加载失败。'
    botSuccess.value = false
  } finally {
    botLoading.value = false
  }
}

const resetProfileForm = () => {
  profileMessage.value = ''
  profileSuccess.value = false
  syncProfileForm()
}

const submitProfile = async () => {
  savingProfile.value = true
  profileMessage.value = ''
  profileSuccess.value = false

  try {
    const response = await userApi.updateProfile({
      username: profileForm.username,
      email: profileForm.email
    })
    user.value = {
      ...(user.value || {}),
      ...(response || {}),
      username: user.value?.username || profileForm.username
    }
    syncProfileForm()
    profileMessage.value = '资料已更新。'
    profileSuccess.value = true
  } catch (error) {
    profileMessage.value = error.message || '资料更新失败。'
  } finally {
    savingProfile.value = false
  }
}

const bindFeishuChat = async () => {
  botSaving.value = true
  botMessage.value = ''
  botSuccess.value = false
  try {
    await skillApi.createBotBinding({
      chatId: botForm.chatId,
      chatName: botForm.chatName,
      skillId: botForm.skillId,
      enabled: true
    })
    botForm.chatId = ''
    botForm.chatName = ''
    botForm.skillId = null
    botMessage.value = '飞书群已绑定。'
    botSuccess.value = true
    await fetchBotData()
  } catch (error) {
    botMessage.value = error.message || '绑定失败。'
  } finally {
    botSaving.value = false
  }
}

const disableBinding = async (bindingId) => {
  try {
    await skillApi.deleteBotBinding(bindingId)
    botMessage.value = '绑定已停用。'
    botSuccess.value = true
    await fetchBotData()
  } catch (error) {
    botMessage.value = error.message || '停用失败。'
    botSuccess.value = false
  }
}

const copyCommand = async (command) => {
  try {
    await navigator.clipboard.writeText(command)
    botMessage.value = '指令已复制。'
    botSuccess.value = true
  } catch (error) {
    botMessage.value = command
    botSuccess.value = true
  }
}

const handleLogout = async () => {
  loggingOut.value = true
  try {
    await userApi.logout()
  } catch (error) {
    console.error('Failed to logout', error)
  } finally {
    localStorage.removeItem('lifeos_token')
    loggingOut.value = false
    router.push('/login')
  }
}

const formatDate = (dateString) => {
  if (!dateString) {
    return '刚刚'
  }
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

onMounted(() => {
  fetchPageData()
})
</script>

<style scoped>
.profile-card,
.panel-card,
.state-card {
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-stats article {
  padding: 16px;
  border-radius: var(--radius);
  background: var(--panel-soft);
  border: 1px solid var(--border);
}

.hero-stats strong {
  display: block;
  margin-bottom: 8px;
  font-size: 28px;
  color: var(--navy);
}

.hero-stats span {
  color: var(--text-muted);
  font-size: 14px;
}

.content-grid {
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  gap: 18px;
}

.profile-card {
  padding: 26px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 800;
  color: white;
  background: var(--blue);
}

.profile-head h2,
.panel-header h3,
.helper-card h3 {
  margin: 0 0 6px;
  color: var(--text);
}

.profile-head p,
.panel-header p,
.helper-card li {
  margin: 0;
  color: var(--text-muted);
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  padding: 16px;
  border-radius: var(--radius);
  background: var(--panel-soft);
  border: 1px solid var(--border);
}

.meta-item span {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-muted);
}

.meta-item strong {
  color: var(--text);
  font-size: 16px;
}

.helper-card {
  padding: 18px 20px;
  border-radius: var(--radius);
  background: var(--panel-soft);
  border: 1px solid var(--border);
}

.helper-card ul {
  margin: 0;
  padding-left: 18px;
}

.helper-card li + li {
  margin-top: 10px;
}

.manage-column {
  display: grid;
  gap: 18px;
}

.panel-card {
  padding: 24px;
}

.panel-header {
  margin-bottom: 18px;
}

.form-grid {
  display: grid;
  gap: 16px;
}

.form-grid label {
  display: grid;
  gap: 8px;
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 600;
}

.form-grid input {
  width: 100%;
  border: 1px solid var(--border);
  background: var(--panel);
  color: var(--text);
  border-radius: var(--radius);
  padding: 10px 11px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-grid input:focus {
  border-color: var(--blue);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.message {
  padding: 12px 14px;
  border-radius: var(--radius);
  color: var(--red);
  background: #fef2f2;
}

.message.success {
  color: var(--green);
  background: #dcfce7;
}

.bot-status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.bot-status-grid article {
  padding: 13px;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: var(--panel-soft);
}

.bot-status-grid span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
  margin-bottom: 5px;
}

.bot-status-grid strong.ok {
  color: var(--green);
}

.bot-status-grid strong.bad {
  color: var(--red);
}

.bot-form {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.binding-list,
.event-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.binding-item,
.event-item {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 13px;
}

.binding-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.binding-item strong,
.event-item strong {
  color: var(--text);
}

.binding-item p,
.event-item p,
.event-item small {
  margin: 4px 0 0;
  color: var(--text-muted);
  word-break: break-all;
}

.binding-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.command-help {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.command-help .section-label {
  width: 100%;
  margin-bottom: 0;
}

.command-chip {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  color: #334155;
  padding: 8px 10px;
  font-size: 13px;
}

.event-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 6px 10px;
}

.event-item p,
.event-item small {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.primary-btn,
.ghost-btn,
.danger-btn {
  border: 1px solid transparent;
  cursor: pointer;
  transition: background-color 180ms ease, border-color 180ms ease, color 180ms ease;
}

.primary-btn {
  padding: 10px 14px;
  border-radius: var(--radius);
  background: var(--blue);
  border-color: var(--blue);
  color: white;
  font-weight: 700;
}

.ghost-btn {
  padding: 10px 14px;
  border-radius: var(--radius);
  background: var(--panel-soft);
  border-color: var(--border);
  color: #334155;
  font-weight: 700;
}

.danger-btn {
  margin-top: auto;
  padding: 10px 14px;
  border-radius: var(--radius);
  background: #fef2f2;
  border-color: #fecaca;
  color: var(--red);
  font-weight: 700;
}

.state-card {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 14px;
  color: var(--text-muted);
}

.error-card {
  color: var(--red);
}

.spinner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 3px solid rgba(37, 99, 235, 0.18);
  border-top-color: var(--blue);
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 980px) {
  .content-grid,
  .meta-grid,
  .hero-stats,
  .bot-status-grid {
    grid-template-columns: 1fr;
  }

  .binding-item {
    align-items: stretch;
    flex-direction: column;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
