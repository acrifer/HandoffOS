<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { getDeviceId } from '@/utils/deviceAuth'
import { userApi } from '@/api/user'

const route = useRoute()

const navItems = [
  { to: '/skill', label: 'Skill 工作台', hint: '核心演示' },
  { to: '/dashboard', label: '交接总览', hint: '指标概览' },
  { to: '/ai', label: 'AI 作业', hint: '审计记录' },
  { to: '/admin/ai', label: '质量统计', hint: '反馈分析' },
  { to: '/note', label: '资料暂存区', hint: '补充材料' },
  { to: '/task', label: '交接行动', hint: '待办闭环' },
  { to: '/user', label: '设置', hint: '设备' }
]

const isLogin = computed(() => route.path === '/login')
const quota = ref(null)
let quotaTimer = null

const displayDeviceId = computed(() => getDeviceId().slice(0, 8))
const quotaText = computed(() => {
  if (!quota.value) return '额度加载中'
  if (quota.value.whitelisted) return '白名单不限额'
  return `${formatNumber(quota.value.remaining)} / ${formatNumber(quota.value.limit)} token`
})

const quotaPercent = computed(() => {
  if (!quota.value || quota.value.whitelisted || !quota.value.limit) return 100
  return Math.max(0, Math.min(100, Math.round((quota.value.remaining / quota.value.limit) * 100)))
})

const refreshQuota = async () => {
  if (isLogin.value) return
  try {
    quota.value = await userApi.currentQuota()
  } catch (error) {
    quota.value = null
  }
}

const formatNumber = (value) => Number(value || 0).toLocaleString('zh-CN')

onMounted(() => {
  refreshQuota()
  quotaTimer = window.setInterval(refreshQuota, 30000)
})

onUnmounted(() => {
  if (quotaTimer) window.clearInterval(quotaTimer)
})
</script>

<template>
  <div class="app-shell" :class="{ 'auth-shell': isLogin }">
    <aside v-if="!isLogin" class="sidebar">
      <RouterLink to="/skill" class="brand">
        <span class="brand-mark">H</span>
        <span>
          <strong>HandoffOS</strong>
          <small>团队交接 AI 控制台</small>
        </span>
      </RouterLink>

      <nav class="side-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
        >
          <span>{{ item.label }}</span>
          <small>{{ item.hint }}</small>
        </RouterLink>
      </nav>
    </aside>

    <div class="content-shell">
      <header v-if="!isLogin" class="topbar">
        <div>
          <strong>交接 Skill Control Plane</strong>
          <span>Spring Boot 管控制面，Dify 托管知识库与工作流</span>
        </div>
        <div class="topbar-right">
          <div class="quota-pill" :class="{ whitelist: quota?.whitelisted }">
            <span>设备 {{ displayDeviceId }}</span>
            <strong>{{ quotaText }}</strong>
            <i :style="{ width: `${quotaPercent}%` }"></i>
          </div>
          <RouterLink to="/skill" class="topbar-action">进入演示链路</RouterLink>
        </div>
      </header>

      <main class="app-main" :class="{ 'full-screen': isLogin }">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--sidebar-width) 1fr;
  background: var(--bg);
}

.auth-shell {
  display: block;
}

.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 18px;
  background: #0f172a;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: var(--radius);
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: var(--radius);
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--blue);
  font-weight: 900;
}

.brand strong,
.brand small,
.topbar strong,
.topbar span {
  display: block;
}

.brand small,
.topbar span,
.nav-link small {
  color: #94a3b8;
}

.side-nav {
  display: grid;
  gap: 6px;
}

.nav-link {
  display: grid;
  gap: 2px;
  padding: 11px 12px;
  border-radius: var(--radius);
  color: #cbd5e1;
  transition: background-color 180ms ease, color 180ms ease;
}

.nav-link:hover,
.nav-link.router-link-active {
  background: rgba(37, 99, 235, 0.18);
  color: #fff;
}

.content-shell {
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  min-height: 68px;
  padding: 14px 24px;
  background: rgba(248, 250, 252, 0.92);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.topbar-action {
  border: 1px solid #bfdbfe;
  border-radius: var(--radius);
  padding: 9px 12px;
  color: #1d4ed8;
  background: var(--blue-soft);
  font-weight: 700;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quota-pill {
  position: relative;
  min-width: 240px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 7px 10px;
  background: var(--panel);
}

.quota-pill span,
.quota-pill strong {
  position: relative;
  z-index: 1;
  display: block;
}

.quota-pill span {
  color: var(--text-muted);
  font-size: 12px;
}

.quota-pill strong {
  color: var(--text);
  font-size: 13px;
}

.quota-pill i {
  position: absolute;
  inset: auto 0 0 0;
  height: 3px;
  display: block;
  background: var(--blue);
}

.quota-pill.whitelist i {
  background: var(--green);
}

.app-main {
  min-height: calc(100vh - 68px);
}

.app-main.full-screen {
  min-height: 100vh;
}

@media (max-width: 980px) {
  .app-shell {
    display: block;
  }

  .sidebar {
    position: static;
    height: auto;
    padding: 12px;
  }

  .side-nav {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .topbar {
    display: none;
  }
}

@media (max-width: 640px) {
  .side-nav {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
