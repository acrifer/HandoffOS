<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'
import { useAuthStore } from '../stores/auth'

const keyword = ref('')
const result = ref({ records: [] })
const authStore = useAuthStore()
const canToggle = computed(() => authStore.hasPermission('users:toggle'))
const canResetPassword = computed(() => authStore.hasPermission('users:reset-password'))

async function load() {
  result.value = await adminApi.listUsers({ page: 1, size: 20, keyword: keyword.value || undefined })
}

async function toggle(user) {
  if (user.enabled) await adminApi.disableUser(user.id)
  else await adminApi.enableUser(user.id)
  await load()
}

async function resetPassword(user) {
  const newPassword = window.prompt(`为 ${user.username} 设置新密码`, 'Pass123456')
  if (!newPassword) return
  await adminApi.resetPassword(user.id, newPassword)
  window.alert('密码已重置')
}

onMounted(load)
</script>

<template>
  <section class="table-card">
    <header class="toolbar">
      <div><h1>Users</h1><p>查看账号、禁用/启用、重置密码</p></div>
      <div class="actions">
        <input v-model="keyword" placeholder="用户名 / 邮箱" @keyup.enter="load" />
        <button @click="load">查询</button>
      </div>
    </header>
    <table>
      <thead><tr><th>ID</th><th>Username</th><th>Email</th><th>Enabled</th><th>Create Time</th><th>Actions</th></tr></thead>
      <tbody>
        <tr v-for="item in result.records" :key="item.id">
          <td>{{ item.id }}</td><td>{{ item.username }}</td><td>{{ item.email || '-' }}</td><td>{{ item.enabled ? 'Y' : 'N' }}</td><td>{{ item.createTime }}</td>
          <td class="row-actions">
            <button v-if="canToggle" @click="toggle(item)">{{ item.enabled ? '禁用' : '启用' }}</button>
            <button v-if="canResetPassword" @click="resetPassword(item)">重置密码</button>
            <span v-if="!canToggle && !canResetPassword">只读</span>
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
