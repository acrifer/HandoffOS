<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'
import { useAuthStore } from '../stores/auth'

const keyword = ref('')
const status = ref('')
const jobType = ref('')
const result = ref({ records: [] })
const authStore = useAuthStore()
const canRetry = computed(() => authStore.hasPermission('ai-jobs:retry'))
const canCancel = computed(() => authStore.hasPermission('ai-jobs:cancel'))

async function load() {
  result.value = await adminApi.listAiJobs({
    page: 1,
    size: 20,
    keyword: keyword.value || undefined,
    status: status.value || undefined,
    jobType: jobType.value || undefined
  })
}

async function retry(item) {
  await adminApi.retryAiJob(item.id)
  await load()
}

async function cancel(item) {
  await adminApi.cancelAiJob(item.id)
  await load()
}

onMounted(load)
</script>

<template>
  <section class="table-card">
    <header class="toolbar">
      <div><h1>AI Jobs</h1><p>失败作业重试、处理中作业取消</p></div>
      <div class="actions">
        <input v-model="keyword" placeholder="用户 / 笔记" @keyup.enter="load" />
        <input v-model="status" placeholder="status" @keyup.enter="load" />
        <input v-model="jobType" placeholder="job type" @keyup.enter="load" />
        <button @click="load">查询</button>
      </div>
    </header>
    <table>
      <thead><tr><th>ID</th><th>User</th><th>Note</th><th>Type</th><th>Status</th><th>Error</th><th>Actions</th></tr></thead>
      <tbody>
        <tr v-for="item in result.records" :key="item.id">
          <td>{{ item.id }}</td><td>{{ item.username }}</td><td>{{ item.noteTitle || item.noteId || '-' }}</td><td>{{ item.jobType }}</td><td>{{ item.status }}</td><td>{{ item.errorMessage || '-' }}</td>
          <td class="row-actions">
            <button v-if="canRetry" @click="retry(item)">重试</button>
            <button v-if="canCancel" @click="cancel(item)">取消</button>
            <span v-if="!canRetry && !canCancel">只读</span>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.table-card { background: #fff; border-radius: 16px; padding: 18px; box-shadow: 0 10px 30px rgba(15,23,42,0.08); }
.toolbar { display: flex; justify-content: space-between; gap: 16px; align-items: end; margin-bottom: 14px; }
.toolbar h1 { margin: 0; } .toolbar p { margin: 4px 0 0; color: #486581; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
input, button { border-radius: 10px; padding: 10px 12px; border: 1px solid #bcccdc; }
button { background: #0f766e; color: #fff; border: none; cursor: pointer; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 12px 10px; border-bottom: 1px solid #e6edf4; text-align: left; font-size: 14px; }
.row-actions { display: flex; gap: 8px; }
</style>
