<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'
import { useAuthStore } from '../stores/auth'

const keyword = ref('')
const reviewState = ref('')
const result = ref({ records: [] })
const authStore = useAuthStore()
const canDelete = computed(() => authStore.hasPermission('notes:delete'))
const canReview = computed(() => authStore.hasPermission('notes:review'))

async function load() {
  result.value = await adminApi.listNotes({ page: 1, size: 20, keyword: keyword.value || undefined, reviewState: reviewState.value || undefined })
}

async function removeNote(noteId) {
  if (!window.confirm(`确认删除笔记 ${noteId} 吗？`)) return
  await adminApi.deleteNote(noteId)
  await load()
}

async function updateReview(item) {
  const nextState = window.prompt('输入新的 review state', item.reviewState || 'REVIEW')
  if (!nextState) return
  await adminApi.updateNoteReview(item.id, { reviewState: nextState })
  await load()
}

onMounted(load)
</script>

<template>
  <section class="table-card">
    <header class="toolbar">
      <div><h1>Notes</h1><p>全局笔记查询、删除、强制调整复习状态</p></div>
      <div class="actions">
        <input v-model="keyword" placeholder="标题 / 标签 / 用户" @keyup.enter="load" />
        <input v-model="reviewState" placeholder="review state" @keyup.enter="load" />
        <button @click="load">查询</button>
      </div>
    </header>
    <table>
      <thead><tr><th>ID</th><th>User</th><th>Title</th><th>Tags</th><th>Review</th><th>Updated</th><th>Actions</th></tr></thead>
      <tbody>
        <tr v-for="item in result.records" :key="item.id">
          <td>{{ item.id }}</td><td>{{ item.username }}</td><td>{{ item.title }}</td><td>{{ item.tags || '-' }}</td><td>{{ item.reviewState }}</td><td>{{ item.updateTime }}</td>
          <td class="row-actions">
            <button v-if="canReview" @click="updateReview(item)">改复习状态</button>
            <button v-if="canDelete" @click="removeNote(item.id)">删除</button>
            <span v-if="!canReview && !canDelete">只读</span>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.table-card, table { width: 100%; }
.table-card { background: #fff; border-radius: 16px; padding: 18px; box-shadow: 0 10px 30px rgba(15,23,42,0.08); }
.toolbar { display: flex; justify-content: space-between; gap: 16px; align-items: end; margin-bottom: 14px; }
.toolbar h1 { margin: 0; } .toolbar p { margin: 4px 0 0; color: #486581; }
.actions { display: flex; gap: 10px; }
input, button { border-radius: 10px; padding: 10px 12px; border: 1px solid #bcccdc; }
button { background: #0f766e; color: #fff; border: none; cursor: pointer; }
table { border-collapse: collapse; }
th, td { padding: 12px 10px; border-bottom: 1px solid #e6edf4; text-align: left; font-size: 14px; }
.row-actions { display: flex; gap: 8px; }
</style>
