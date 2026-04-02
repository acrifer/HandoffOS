<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { adminApi } from '../../api/admin'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menuItems = computed(() => authStore.menuItems)

async function logout() {
  try {
    await adminApi.logout()
  } finally {
    localStorage.removeItem('lifeos_admin_token')
    authStore.clear()
    router.push('/login')
  }
}
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <strong>LifeOS Admin</strong>
        <span>{{ authStore.currentUser?.displayName || authStore.currentUser?.username }}</span>
      </div>
      <nav class="menu">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="menu-item"
          :class="{ active: route.path === item.path }"
        >
          {{ item.label }}
        </router-link>
      </nav>
      <button class="logout" @click="logout">退出</button>
    </aside>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout { display: grid; grid-template-columns: 240px 1fr; min-height: 100vh; background: #eef3f7; color: #16324f; }
.sidebar { background: linear-gradient(180deg, #102a43 0%, #16324f 100%); color: #fff; padding: 20px 16px; display: flex; flex-direction: column; gap: 18px; }
.brand { display: flex; flex-direction: column; gap: 6px; padding: 4px 8px 12px; border-bottom: 1px solid rgba(255,255,255,0.14); }
.brand span { font-size: 12px; color: rgba(255,255,255,0.74); }
.menu { display: flex; flex-direction: column; gap: 8px; }
.menu-item { padding: 10px 12px; border-radius: 10px; color: rgba(255,255,255,0.9); text-decoration: none; }
.menu-item.active, .menu-item:hover { background: rgba(255,255,255,0.12); }
.logout { margin-top: auto; border: none; background: #f25f5c; color: #fff; border-radius: 10px; padding: 10px 12px; cursor: pointer; }
.content { padding: 28px; }
@media (max-width: 960px) {
  .layout { grid-template-columns: 1fr; }
  .sidebar { position: sticky; top: 0; z-index: 2; }
}
</style>
