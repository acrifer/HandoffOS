<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../api/admin'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const username = ref('admin')
const password = ref('AdminPass123456')
const loading = ref(false)
const errorMessage = ref('')

async function submit() {
  loading.value = true
  errorMessage.value = ''
  try {
    const token = await adminApi.login({ username: username.value, password: password.value })
    localStorage.setItem('lifeos_admin_token', token)
    await authStore.fetchCurrentUser()
    router.push('/dashboard')
  } catch (error) {
    errorMessage.value = error.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <form class="card" @submit.prevent="submit">
      <h1>LifeOS Admin</h1>
      <p>独立管理员后台</p>
      <label>用户名<input v-model="username" autocomplete="username" /></label>
      <label>密码<input v-model="password" type="password" autocomplete="current-password" /></label>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <button :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
    </form>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; display: grid; place-items: center; background: radial-gradient(circle at top, #d9e2ec, #bcccdc); }
.card { width: min(420px, 92vw); background: #fff; border-radius: 18px; padding: 28px; box-shadow: 0 30px 80px rgba(16,42,67,0.2); display: grid; gap: 14px; }
h1 { margin: 0; color: #102a43; }
p { margin: 0; color: #486581; }
label { display: grid; gap: 6px; color: #243b53; font-size: 14px; }
input { border: 1px solid #bcccdc; border-radius: 10px; padding: 12px; font-size: 14px; }
button { border: none; border-radius: 10px; padding: 12px; background: #0f766e; color: #fff; cursor: pointer; }
.error { color: #b42318; }
</style>
