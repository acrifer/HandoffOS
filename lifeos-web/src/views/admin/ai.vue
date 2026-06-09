<template>
  <div class="page-shell">
    <section class="page-header">
      <div>
        <p class="eyebrow">质量统计</p>
        <h1>AI 质量、知识缺口与设备额度。</h1>
        <p>面试演示环境按设备隔离数据，并用白名单和 token 额度控制外部 AI 成本。</p>
      </div>
      <div class="row">
        <button class="ghost-btn" :disabled="loading" @click="refresh">{{ loading ? '刷新中...' : '刷新统计' }}</button>
        <button class="primary-btn" :disabled="analyzing" @click="analyze">{{ analyzing ? '分析中...' : '生成日志分析' }}</button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card"><span>问答次数</span><strong>{{ stats.usage || 0 }}</strong></article>
      <article class="metric-card"><span>无答案</span><strong>{{ stats.noAnswerCount || 0 }}</strong></article>
      <article class="metric-card"><span>失败调用</span><strong>{{ stats.failedCount || 0 }}</strong></article>
      <article class="metric-card"><span>无答案率</span><strong>{{ percent(stats.noAnswerRate) }}</strong></article>
      <article class="metric-card"><span>机器人触发</span><strong>{{ stats.botEventCount || 0 }}</strong></article>
      <article class="metric-card"><span>群聊绑定</span><strong>{{ stats.botBindingCount || 0 }}</strong></article>
    </section>

    <section class="quality-layout">
      <article class="panel quota-panel wide">
        <div class="panel-top">
          <div>
            <p class="section-label">设备白名单</p>
            <h2>按登录设备调节 AI token 额度</h2>
          </div>
          <div class="admin-key-box">
            <input v-model.trim="adminKey" type="password" placeholder="ADMIN_CONSOLE_KEY" @keyup.enter="saveAdminKey" />
            <button class="ghost-btn" @click="saveAdminKey">保存 Key</button>
            <button class="primary-btn" :disabled="quotaLoading" @click="loadDevices">{{ quotaLoading ? '加载中...' : '加载设备' }}</button>
          </div>
        </div>

        <p v-if="quotaMessage" class="inline-message" :class="{ error: quotaError }">{{ quotaMessage }}</p>

        <div class="quota-table">
          <article v-for="device in filteredDevices" :key="device.deviceId" class="device-row">
            <div class="device-main">
              <strong>{{ device.displayName || device.deviceName || '演示设备' }}</strong>
              <code>{{ device.deviceId }}</code>
              <small>用户 #{{ device.userId || '-' }} · 最近访问 {{ formatDate(device.lastSeenAt) }}</small>
            </div>
            <div class="quota-progress">
              <span>{{ device.whitelistEnabled ? '白名单不限额' : `${formatNumber(device.remaining)} / ${formatNumber(device.quotaLimit)} token` }}</span>
              <i :style="{ width: `${quotaPercent(device)}%` }"></i>
            </div>
            <label class="compact-field">
              <span>额度</span>
              <input v-model.number="device.quotaLimit" type="number" min="0" />
            </label>
            <label class="toggle-field">
              <input v-model="device.whitelistEnabled" type="checkbox" />
              <span>白名单</span>
            </label>
            <label class="toggle-field">
              <input v-model="device.enabled" type="checkbox" />
              <span>启用</span>
            </label>
            <div class="device-actions">
              <button class="ghost-btn" @click="saveDevice(device)">保存</button>
              <button class="ghost-btn danger" @click="resetDevice(device)">重置</button>
              <button class="ghost-btn" @click="selectDevice(device)">日志</button>
            </div>
          </article>
          <p v-if="!filteredDevices.length" class="empty-state">输入 Admin Key 后加载设备额度列表。</p>
        </div>
      </article>

      <article class="panel">
        <p class="section-label">高频问题</p>
        <h2>新人反复卡在哪里</h2>
        <div class="data-list">
          <div v-for="question in stats.topQuestions" :key="question" class="list-row">
            <strong>{{ question }}</strong>
          </div>
          <p v-if="!stats.topQuestions?.length" class="empty-state">还没有足够问答日志。</p>
        </div>
      </article>

      <article class="panel">
        <p class="section-label">知识缺口</p>
        <h2>{{ analysis.summary || '等待生成分析' }}</h2>
        <div class="gap-list">
          <article v-for="item in analysis.knowledgeGaps" :key="item">{{ item }}</article>
          <p v-if="!analysis.knowledgeGaps?.length" class="muted">无答案问题会在这里沉淀为补资料清单。</p>
        </div>
      </article>

      <article class="panel wide">
        <p class="section-label">设备用量日志</p>
        <h2>{{ selectedDevice?.displayName || selectedDevice?.deviceName || '选择设备查看最近调用' }}</h2>
        <div class="data-list">
          <div v-for="log in selectedDevice?.usageLogs || []" :key="log.id" class="list-row">
            <span>
              <strong>{{ log.operationType }} · {{ log.status }}</strong>
              <small>{{ formatDate(log.createTime) }} · {{ log.estimated ? '估算' : '真实' }}</small>
            </span>
            <em class="badge">{{ formatNumber((log.requestTokens || 0) + (log.responseTokens || 0)) }}</em>
          </div>
          <p v-if="!(selectedDevice?.usageLogs || []).length" class="empty-state">暂无设备调用日志。</p>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { skillApi } from '@/api/skill'
import { userApi } from '@/api/user'

const loading = ref(false)
const analyzing = ref(false)
const quotaLoading = ref(false)
const stats = reactive({ usage: 0, noAnswerCount: 0, failedCount: 0, noAnswerRate: 0, topQuestions: [] })
const analysis = reactive({ summary: '', knowledgeGaps: [], promptSuggestions: [] })
const adminKey = ref(localStorage.getItem('handoff_admin_key') || '')
const devices = ref([])
const selectedDevice = ref(null)
const quotaMessage = ref('')
const quotaError = ref(false)

const filteredDevices = computed(() => devices.value)

const refresh = async () => {
  loading.value = true
  try {
    Object.assign(stats, await skillApi.adminStats())
  } finally {
    loading.value = false
  }
}

const analyze = async () => {
  analyzing.value = true
  try {
    Object.assign(analysis, await skillApi.analyzeLogs({}))
  } finally {
    analyzing.value = false
  }
}

const saveAdminKey = () => {
  localStorage.setItem('handoff_admin_key', adminKey.value)
  quotaMessage.value = 'Admin Key 已保存到当前浏览器。'
  quotaError.value = false
}

const loadDevices = async () => {
  saveAdminKey()
  quotaLoading.value = true
  quotaMessage.value = ''
  try {
    devices.value = await userApi.adminDevices()
    quotaMessage.value = `已加载 ${devices.value.length} 台设备。`
    quotaError.value = false
  } catch (error) {
    quotaMessage.value = error.message || '设备额度加载失败。'
    quotaError.value = true
  } finally {
    quotaLoading.value = false
  }
}

const saveDevice = async (device) => {
  try {
    const updated = await userApi.updateDeviceQuota(device.deviceId, {
      displayName: device.displayName || device.deviceName,
      quotaLimit: device.quotaLimit,
      whitelistEnabled: device.whitelistEnabled,
      enabled: device.enabled
    })
    Object.assign(device, updated)
    quotaMessage.value = '设备额度已保存。'
    quotaError.value = false
  } catch (error) {
    quotaMessage.value = error.message || '保存失败。'
    quotaError.value = true
  }
}

const resetDevice = async (device) => {
  try {
    const quota = await userApi.resetDeviceQuota(device.deviceId)
    device.quotaUsed = quota.used
    device.remaining = quota.remaining
    quotaMessage.value = '设备用量已重置。'
    quotaError.value = false
  } catch (error) {
    quotaMessage.value = error.message || '重置失败。'
    quotaError.value = true
  }
}

const selectDevice = async (device) => {
  try {
    selectedDevice.value = await userApi.adminDeviceDetail(device.deviceId)
  } catch (error) {
    selectedDevice.value = device
    quotaMessage.value = error.message || '用量日志加载失败。'
    quotaError.value = true
  }
}

const quotaPercent = (device) => {
  if (device.whitelistEnabled) return 100
  if (!device.quotaLimit) return 0
  return Math.max(0, Math.min(100, Math.round(((device.quotaLimit - device.quotaUsed) / device.quotaLimit) * 100)))
}

const percent = (value) => `${Math.round((value || 0) * 100)}%`
const formatNumber = (value) => Number(value || 0).toLocaleString('zh-CN')
const formatDate = (date) => date ? new Date(date).toLocaleString('zh-CN') : '-'

onMounted(() => {
  refresh()
  if (adminKey.value) loadDevices()
})
</script>

<style scoped>
.quality-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.wide {
  grid-column: 1 / -1;
}

.panel-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.admin-key-box {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.admin-key-box input,
.compact-field input {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel);
  padding: 9px 10px;
  color: var(--text);
}

.inline-message {
  margin: 0 0 12px;
  color: var(--green);
}

.inline-message.error {
  color: var(--red);
}

.quota-table {
  display: grid;
  gap: 10px;
}

.device-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) minmax(180px, 1fr) 120px auto auto auto;
  gap: 12px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 12px;
}

.device-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.device-main code,
.device-main small {
  color: var(--text-muted);
  word-break: break-all;
}

.quota-progress {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel);
  padding: 9px 10px;
}

.quota-progress span {
  position: relative;
  z-index: 1;
  font-size: 13px;
  color: var(--text);
}

.quota-progress i {
  position: absolute;
  inset: auto 0 0 0;
  height: 3px;
  background: var(--blue);
}

.compact-field,
.toggle-field {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.compact-field {
  flex-direction: column;
  align-items: stretch;
}

.device-actions {
  display: flex;
  gap: 6px;
}

.ghost-btn.danger {
  color: var(--red);
}

.gap-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.gap-list article {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 13px;
  color: var(--text-muted);
}

@media (max-width: 1100px) {
  .quality-layout,
  .device-row {
    grid-template-columns: 1fr;
  }

  .wide {
    grid-column: auto;
  }

  .panel-top,
  .admin-key-box,
  .device-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
