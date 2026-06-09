<template>
  <div class="page-shell account-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">演示设置</p>
        <h1>设备额度与飞书机器人。</h1>
        <p>当前环境不需要账号。每台登录设备拥有独立数据空间和 AI token 额度。</p>
      </div>
      <button class="ghost-btn danger" @click="resetSession">重置当前设备登录</button>
    </section>

    <section class="metric-grid">
      <article class="metric-card"><span>设备 ID</span><strong>{{ shortDeviceId }}</strong></article>
      <article class="metric-card"><span>额度上限</span><strong>{{ quota.whitelisted ? '不限额' : formatNumber(quota.limit) }}</strong></article>
      <article class="metric-card"><span>已用 token</span><strong>{{ formatNumber(quota.used) }}</strong></article>
      <article class="metric-card"><span>剩余 token</span><strong>{{ quota.whitelisted ? '白名单' : formatNumber(quota.remaining) }}</strong></article>
    </section>

    <section class="content-grid">
      <article class="panel-card">
        <div class="panel-header row">
          <div>
            <h3>设备演示态</h3>
            <p>管理员可在质量统计页通过白名单和额度调节控制当前设备。</p>
          </div>
          <button class="ghost-btn" :disabled="quotaLoading" @click="fetchQuota">{{ quotaLoading ? '刷新中...' : '刷新额度' }}</button>
        </div>
        <div class="device-box">
          <span>完整设备 ID</span>
          <code>{{ deviceId }}</code>
        </div>
        <div class="quota-bar">
          <i :style="{ width: `${quotaPercent}%` }"></i>
        </div>
        <p class="muted">
          {{ quota.whitelisted ? '当前设备处于管理员白名单，不消耗配额。' : `当前剩余额度 ${formatNumber(quota.remaining)} token。` }}
        </p>
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
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearDeviceToken, getDeviceId } from '@/utils/deviceAuth'
import { skillApi } from '@/api/skill'
import { userApi } from '@/api/user'

const router = useRouter()
const deviceId = ref(getDeviceId())
const quota = reactive({ limit: 0, used: 0, remaining: 0, whitelisted: false, enabled: true })
const quotaLoading = ref(false)
const skills = ref([])
const botBindings = ref([])
const botLoading = ref(false)
const botSaving = ref(false)
const botMessage = ref('')
const botSuccess = ref(false)
const botStatus = reactive({ credentialsConfigured: false, botEnabled: false, longConnectionEnabled: false })
const botForm = reactive({ chatId: '', chatName: '', skillId: null })

const shortDeviceId = computed(() => deviceId.value.slice(0, 8))
const quotaPercent = computed(() => {
  if (quota.whitelisted) return 100
  if (!quota.limit) return 0
  return Math.max(0, Math.min(100, Math.round((quota.remaining / quota.limit) * 100)))
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

const fetchQuota = async () => {
  quotaLoading.value = true
  try {
    Object.assign(quota, await userApi.currentQuota())
  } finally {
    quotaLoading.value = false
  }
}

const fetchBotData = async () => {
  botLoading.value = true
  try {
    const [skillList, status, bindings] = await Promise.all([
      skillApi.list().catch(() => []),
      skillApi.botStatus(),
      skillApi.botBindings().catch(() => [])
    ])
    skills.value = skillList || []
    Object.assign(botStatus, status || {})
    botBindings.value = bindings || []
  } catch (error) {
    botMessage.value = error.message || '飞书机器人状态加载失败。'
    botSuccess.value = false
  } finally {
    botLoading.value = false
  }
}

const bindFeishuChat = async () => {
  botSaving.value = true
  botMessage.value = ''
  botSuccess.value = false
  try {
    await skillApi.createBotBinding({ ...botForm, enabled: true })
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
  } catch (error) {
    botMessage.value = command
  }
  botSuccess.value = true
}

const resetSession = () => {
  clearDeviceToken()
  router.push('/login')
}

const formatNumber = (value) => Number(value || 0).toLocaleString('zh-CN')

onMounted(() => {
  fetchQuota()
  fetchBotData()
})
</script>

<style scoped>
.content-grid {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 18px;
}

.panel-card {
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  padding: 24px;
}

.panel-header {
  margin-bottom: 18px;
}

.panel-header h3 {
  margin: 0 0 6px;
  color: var(--text);
}

.panel-header p {
  margin: 0;
  color: var(--text-muted);
}

.device-box {
  display: grid;
  gap: 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 14px;
}

.device-box span {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.device-box code {
  word-break: break-all;
}

.quota-bar {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
  margin: 16px 0;
}

.quota-bar i {
  display: block;
  height: 100%;
  background: var(--blue);
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

.form-grid input,
.form-grid select {
  width: 100%;
  border: 1px solid var(--border);
  background: var(--panel);
  color: var(--text);
  border-radius: var(--radius);
  padding: 10px 11px;
}

.bot-status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.bot-status-grid article,
.binding-item {
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

.binding-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.binding-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.binding-item p {
  margin: 4px 0 0;
  color: var(--text-muted);
  word-break: break-all;
}

.binding-actions,
.command-help {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.command-help {
  margin-top: 18px;
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

.danger {
  color: var(--red);
}

@media (max-width: 980px) {
  .content-grid,
  .bot-status-grid {
    grid-template-columns: 1fr;
  }

  .binding-item {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
