<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'
import { useAuthStore } from '../stores/auth'

const tools = ref({ logCommands: [] })
const authStore = useAuthStore()
const canResetData = computed(() => authStore.hasPermission('ops:reset-data'))

async function load() {
  tools.value = await adminApi.getTools()
}

async function resetData() {
  if (!window.confirm('确认重置测试数据吗？这会清空当前业务数据。')) return
  await adminApi.resetTestData()
  window.alert('已触发重置测试数据')
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <div class="card">
      <h1>Ops Tools</h1>
      <p>访问入口、日志命令和测试数据维护。</p>
      <ul class="links">
        <li><a :href="tools.frontendUrl" target="_blank" rel="noreferrer">Frontend</a></li>
        <li><a :href="tools.adminUrl" target="_blank" rel="noreferrer">Admin</a></li>
        <li><a :href="tools.gatewayUrl" target="_blank" rel="noreferrer">Gateway</a></li>
        <li><a :href="tools.swaggerUrl" target="_blank" rel="noreferrer">Swagger</a></li>
        <li><a :href="tools.nacosUrl" target="_blank" rel="noreferrer">Nacos</a></li>
      </ul>
      <button v-if="canResetData" @click="resetData">重置测试数据</button>
    </div>
    <div class="card">
      <h2>Log Commands</h2>
      <pre>{{ (tools.logCommands || []).join('\n') }}</pre>
    </div>
  </section>
</template>

<style scoped>
.panel { display: grid; gap: 16px; }
.card { background: #fff; border-radius: 16px; padding: 18px; box-shadow: 0 10px 30px rgba(15,23,42,0.08); }
h1, h2 { margin: 0 0 8px; } p { margin: 0 0 12px; color: #486581; }
.links { display: grid; gap: 8px; padding-left: 18px; }
button { margin-top: 14px; background: #c2410c; color: #fff; border: none; border-radius: 10px; padding: 10px 12px; cursor: pointer; }
pre { background: #102a43; color: #d9e2ec; border-radius: 12px; padding: 14px; overflow: auto; }
a { color: #0f766e; }
</style>
