<template>
  <div class="login-page">
    <section class="intro-panel">
      <p class="eyebrow">HandoffOS</p>
      <h1>进入团队交接 AI 控制台。</h1>
      <p class="intro-copy">
        聚合飞书文档、群聊资料和 Dify Knowledge，把项目经验蒸馏成可问答、可审计的交接 Skill。
      </p>

      <div class="feature-list">
        <article>
          <strong>资料接入</strong>
          <span>同步飞书文档和群聊，也支持手动补充交接材料。</span>
        </article>
        <article>
          <strong>Skill 蒸馏</strong>
          <span>把角色边界、工作原则、检查清单和风险点结构化。</span>
        </article>
        <article>
          <strong>RAG 问答</strong>
          <span>基于知识库回答新人问题，并保留引用、反馈和作业日志。</span>
        </article>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <div class="auth-header">
          <h2>{{ isLogin ? '登录账号' : '创建账号' }}</h2>
          <p>{{ isLogin ? '继续进入 Skill 工作台。' : '创建演示账号后开始搭建交接助手。' }}</p>
        </div>

        <form class="form-area" @submit.prevent="handleSubmit">
          <label>
            <span>用户名</span>
            <input
              v-model="form.username"
              type="text"
              placeholder="请输入用户名"
              required
            />
          </label>

          <label v-if="!isLogin">
            <span>邮箱</span>
            <input
              v-model="form.email"
              type="email"
              placeholder="用于接收账号信息"
            />
          </label>

          <label>
            <span>密码</span>
            <input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              required
            />
          </label>

          <div v-if="errorMessage" class="feedback" :class="{ success: !isLogin && errorMessage.includes('注册成功') }">
            {{ errorMessage }}
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '提交中...' : (isLogin ? '登录' : '注册') }}
          </button>
        </form>

        <button type="button" class="switch-btn" @click="toggleMode">
          {{ isLogin ? '没有账号？去注册' : '已有账号？去登录' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'

const router = useRouter()

const isLogin = ref(true)
const loading = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  email: '',
  password: ''
})

const resetForm = () => {
  form.username = ''
  form.email = ''
  form.password = ''
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
  errorMessage.value = ''
  resetForm()
}

const handleSubmit = async () => {
  if (!form.username || !form.password) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    if (isLogin.value) {
      const loginResult = await userApi.login({
        username: form.username,
        password: form.password
      })
      const token = typeof loginResult === 'string' ? loginResult : loginResult?.token
      if (!token) {
        throw new Error('登录响应缺少 token')
      }
      localStorage.setItem('lifeos_token', token)
      router.push('/skill')
      return
    }

    await userApi.register({
      username: form.username,
      password: form.password,
      email: form.email
    })
    isLogin.value = true
    errorMessage.value = '注册成功，请登录。'
    form.password = ''
  } catch (error) {
    errorMessage.value = error.message || '操作失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}
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
  color: #0b5f6f;
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 12px;
  font-weight: 700;
}

.intro-panel h1 {
  margin: 0;
  max-width: 640px;
  font-size: 52px;
  line-height: 1.08;
  color: #102033;
}

.intro-copy {
  margin: 0;
  max-width: 560px;
  color: #5f7389;
  font-size: 18px;
}

.feature-list {
  display: grid;
  gap: 14px;
  width: min(100%, 580px);
}

.feature-list article {
  padding: 18px 20px;
  border-radius: var(--radius);
  background: var(--panel);
  border: 1px solid var(--border);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.feature-list strong {
  display: block;
  margin-bottom: 6px;
  color: #102033;
  font-size: 18px;
}

.feature-list span {
  color: #61778d;
}

.auth-panel {
  justify-content: center;
}

.auth-card {
  width: min(100%, 460px);
  padding: 34px;
  border-radius: var(--radius);
  background: var(--panel);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}

.auth-header {
  margin-bottom: 26px;
}

.auth-header h2 {
  margin: 0 0 8px;
  font-size: 30px;
  color: #102033;
}

.auth-header p {
  margin: 0;
  color: #61778d;
}

.form-area {
  display: grid;
  gap: 18px;
}

.form-area label {
  display: grid;
  gap: 8px;
  color: #475b71;
  font-size: 14px;
  font-weight: 600;
}

.form-area input {
  width: 100%;
  border: 1px solid #cfdae4;
  background: #f8fbfd;
  color: #102033;
  border-radius: 16px;
  padding: 13px 14px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-area input:focus {
  border-color: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.1);
}

.feedback {
  padding: 12px 14px;
  border-radius: 14px;
  color: #b42318;
  background: rgba(180, 35, 24, 0.08);
}

.feedback.success {
  color: #0f6e53;
  background: rgba(15, 118, 110, 0.08);
}

.submit-btn,
.switch-btn {
  width: 100%;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn {
  margin-top: 6px;
  padding: 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, #0f766e, #0f4c81);
  color: white;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0 18px 30px rgba(15, 76, 129, 0.18);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.switch-btn {
  margin-top: 16px;
  padding: 12px 0 0;
  background: transparent;
  color: #0b5f6f;
  font-weight: 700;
}

@media (max-width: 1024px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .intro-panel {
    padding-bottom: 20px;
  }

  .auth-panel {
    padding-top: 0;
  }
}

@media (max-width: 720px) {
  .intro-panel,
  .auth-panel {
    padding: 24px;
  }

  .intro-panel h1 {
    font-size: 34px;
  }

  .intro-copy {
    font-size: 16px;
  }

  .auth-card {
    padding: 24px;
  }
}
</style>
