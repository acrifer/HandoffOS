<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'

const keyword = ref('')
const actionType = ref('')
const result = ref({ records: [] })

async function load() {
  result.value = await adminApi.listBehaviors({ page: 1, size: 20, keyword: keyword.value || undefined, actionType: actionType.value || undefined })
}

onMounted(load)
</script>

<template>
  <section class="table-card">
    <header class="toolbar">
      <div><h1>Behaviors</h1><p>行为明细与筛选</p></div>
      <div class="actions">
        <input v-model="keyword" placeholder="用户 / eventId" @keyup.enter="load" />
        <input v-model="actionType" placeholder="action type" @keyup.enter="load" />
        <button @click="load">查询</button>
      </div>
    </header>
    <table>
      <thead><tr><th>ID</th><th>User</th><th>Action</th><th>Target</th><th>Event ID</th><th>Time</th></tr></thead>
      <tbody>
        <tr v-for="item in result.records" :key="item.id">
          <td>{{ item.id }}</td><td>{{ item.username }}</td><td>{{ item.actionType }}</td><td>{{ item.targetId || '-' }}</td><td>{{ item.eventId }}</td><td>{{ item.createTime }}</td>
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
</style>
