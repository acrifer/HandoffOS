<template>
  <div class="login-page">
    <section class="intro-panel">
      <p class="eyebrow">HandoffOS Demo</p>
      <h1>无需账号，按设备进入交接 AI 控制台。</h1>
      <p class="intro-copy">
        当前项目用于面试展示。系统会为每台浏览器设备生成独立演示空间，并按设备统计 Dify / AI token 额度。
      </p>

      <div class="feature-list">
        <article>
          <strong>设备独立数据</strong>
          <span>不同浏览器设备创建的 Skill、资料、问答和任务互相隔离。</span>
        </article>
        <article>
          <strong>额度可控</strong>
          <span>AI 调用前检查 token 额度，管理员可在白名单面板调整。</span>
        </article>
        <article>
          <strong>真实链路</strong>
          <span>飞书、Dify、RAG 和机器人失败即报错，不再使用 mock fallback。</span>
        </article>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <div class="auth-header">
          <h2>进入演示控制台</h2>
          <p>设备 ID 会保存在当前浏览器 localStorage 中。</p>
        </div>

        <div class="device-box">
          <span>当前设备</span>
          <code>{{ deviceId }}</code>
        </div>

        <div v-if="errorMessage" class="feedback">{{ errorMessage }}</div>

        <button type="button" class="submit-btn" :disabled="loading" @click="enterDemo">
          {{ loading ? '进入中...' : '进入 HandoffOS' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ensureDeviceSession, getDeviceId } from '@/utils/deviceAuth'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const deviceId = ref(getDeviceId())

const enterDemo = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    await ensureDeviceSession()
    router.push('/skill')
  } catch (error) {
    errorMessage.value = error.message || '设备演示登录失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  enterDemo()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  background: var(--bg);
}

.intro-panel,
.auth-panel {
  padding: 56px;
  display: flex;
  align-items: center;
}

.intro-panel {
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 24px;
}

.eyebrow {
  margin: 0;
  color: var(--blue);
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 12px;
  font-weight: 800;
}

.intro-panel h1 {
  margin: 0;
  max-width: 680px;
  font-size: 52px;
  line-height: 1.08;
  color: var(--text);
}

.intro-copy {
  margin: 0;
  max-width: 580px;
  color: var(--text-muted);
  font-size: 18px;
}

.feature-list {
  display: grid;
  gap: 14px;
  width: min(100%, 580px);
}

.feature-list article,
.auth-card,
.device-box {
  border-radius: var(--radius);
  background: var(--panel);
  border: 1px solid var(--border);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.feature-list article {
  padding: 18px 20px;
}

.feature-list strong {
  display: block;
  margin-bottom: 6px;
  color: var(--text);
  font-size: 18px;
}

.feature-list span {
  color: var(--text-muted);
}

.auth-panel {
  justify-content: center;
}

.auth-card {
  width: min(100%, 460px);
  padding: 34px;
}

.auth-header {
  margin-bottom: 22px;
}

.auth-header h2 {
  margin: 0 0 8px;
  font-size: 30px;
  color: var(--text);
}

.auth-header p {
  margin: 0;
  color: var(--text-muted);
}

.device-box {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
  padding: 14px;
  background: var(--panel-soft);
}

.device-box span {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.device-box code {
  word-break: break-all;
  color: var(--text);
}

.feedback {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: var(--radius);
  color: var(--red);
  background: #fef2f2;
}

.submit-btn {
  width: 100%;
  border: 1px solid var(--blue);
  cursor: pointer;
  padding: 14px;
  border-radius: var(--radius);
  background: var(--blue);
  color: white;
  font-size: 16px;
  font-weight: 800;
}

.submit-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .intro-panel,
  .auth-panel {
    padding: 28px;
  }

  .intro-panel h1 {
    font-size: 34px;
  }
}
</style>
