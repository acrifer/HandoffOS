<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { to: '/dashboard', label: '总览' },
  { to: '/note', label: '笔记库' },
  { to: '/task', label: '任务' },
  { to: '/ai', label: 'AI 工作台' },
  { to: '/skill', label: '交接 Skill' },
  { to: '/user', label: '我的' }
]

const isLogin = computed(() => route.path === '/login')
</script>

<template>
  <div class="app-shell">
    <header v-if="!isLogin" class="app-header">
      <div class="brand-block">
        <div class="brand-mark">L</div>
        <div class="brand-copy">
          <strong>LifeOS</strong>
          <span>个人知识与行动系统</span>
        </div>
      </div>

      <nav class="nav-bar">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </header>

    <main class="app-main" :class="{ 'full-screen': isLogin }">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  --app-header-height: 96px;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 28px;
  margin: 0 auto;
  backdrop-filter: blur(18px);
  background: rgba(247, 250, 252, 0.84);
  border-bottom: 1px solid rgba(191, 206, 219, 0.8);
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 800;
  color: #f8fafc;
  background: linear-gradient(135deg, #0f766e, #0f4c81);
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.2);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-size: 18px;
  color: #112033;
  letter-spacing: 0.02em;
}

.brand-copy span {
  font-size: 12px;
  color: #667b91;
}

.nav-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.nav-link {
  padding: 10px 16px;
  border-radius: 999px;
  color: #52667d;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.nav-link:hover {
  color: #112033;
  background: rgba(214, 226, 237, 0.65);
}

.nav-link.router-link-exact-active {
  color: #0b5f6f;
  background: rgba(15, 118, 110, 0.12);
  box-shadow: inset 0 0 0 1px rgba(15, 118, 110, 0.16);
}

.app-main {
  min-height: calc(100vh - 79px);
}

.app-main.full-screen {
  min-height: 100vh;
}

@media (max-width: 900px) {
  .app-shell {
    --app-header-height: 132px;
  }

  .app-header {
    padding: 16px 18px;
    align-items: flex-start;
    flex-direction: column;
  }

  .nav-bar {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
