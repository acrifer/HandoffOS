<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'
import { useAuthStore } from '../stores/auth'

const keyword = ref('')
const status = ref('')
const result = ref({ records: [] })
const authStore = useAuthStore()
const canUpdateStatus = computed(() => authStore.hasPermission('tasks:update-status'))
const canDelete = computed(() => authStore.hasPermission('tasks:delete'))

async function load() {
  result.value = await adminApi.listTasks({ page: 1, size: 20, keyword: keyword.value || undefined, status: status.value || undefined })
}

async function changeStatus(item) {
  const next = window.prompt('输入任务状态 0/1/2', String(item.status))
  if (next === null) return
  await adminApi.updateTaskStatus(item.id, Number(next))
  await load()
}

async function removeTask(taskId) {
  if (!window.confirm(`确认删除任务 ${taskId} 吗？`)) return
  await adminApi.deleteTask(taskId)
  await load()
}

onMounted(load)
</script>

<template>
  <section class="table-card">
    <header class="toolbar">
      <div><h1>Tasks</h1><p>全局任务查询、改状态、删除</p></div>
      <div class="actions">
        <input v-model="keyword" placeholder="标题 / 用户" @keyup.enter="load" />
        <input v-model="status" placeholder="status" @keyup.enter="load" />
        <button @click="load">查询</button>
      </div>
    </header>
    <table>
      <thead><tr><th>ID</th><th>User</th><th>Title</th><th>Status</th><th>Deadline</th><th>Source Note</th><th>Actions</th></tr></thead>
      <tbody>
        <tr v-for="item in result.records" :key="item.id">
          <td>{{ item.id }}</td><td>{{ item.username }}</td><td>{{ item.title }}</td><td>{{ item.status }}</td><td>{{ item.deadline || '-' }}</td><td>{{ item.sourceNoteId || '-' }}</td>
          <td class="row-actions">
            <button v-if="canUpdateStatus" @click="changeStatus(item)">改状态</button>
            <button v-if="canDelete" @click="removeTask(item.id)">删除</button>
            <span v-if="!canUpdateStatus && !canDelete">只读</span>
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
.actions { display: flex; gap: 10px; }
input, button { border-radius: 10px; padding: 10px 12px; border: 1px solid #bcccdc; }
button { background: #0f766e; color: #fff; border: none; cursor: pointer; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 12px 10px; border-bottom: 1px solid #e6edf4; text-align: left; font-size: 14px; }
.row-actions { display: flex; gap: 8px; }
</style>
