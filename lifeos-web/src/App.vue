<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { to: '/skill', label: 'Skill 工作台', hint: '核心演示' },
  { to: '/dashboard', label: '交接总览', hint: '指标概览' },
  { to: '/ai', label: 'AI 作业', hint: '审计记录' },
  { to: '/admin/ai', label: '质量统计', hint: '反馈分析' },
  { to: '/note', label: '资料暂存区', hint: '补充材料' },
  { to: '/task', label: '交接行动', hint: '待办闭环' },
  { to: '/user', label: '设置', hint: '账号' }
]

const isLogin = computed(() => route.path === '/login')
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
        <RouterLink to="/skill" class="topbar-action">进入演示链路</RouterLink>
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
