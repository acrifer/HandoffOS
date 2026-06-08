<template>
  <div class="page-shell rag-chat-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">自研 RAG 对比演示</p>
        <h1>验证本地向量检索链路的回答效果。</h1>
        <p>主线使用 Dify Knowledge；这里保留自研 RAG 查询，用于面试时对比 pgvector / embedding / Top-K 检索。</p>
      </div>
      <div class="coverage-card">
        <span>向量化覆盖率</span>
        <strong>{{ (stats.embeddingCoverage * 100).toFixed(1) }}%</strong>
      </div>
    </section>

    <section class="chat-console">
      <div ref="messagesContainer" class="chat-messages">
        <div v-if="messages.length === 0" class="empty-state">
          <strong>可以用样例问题快速演示检索与引用。</strong>
          <div class="example-queries">
            <button @click="askExample('有哪些交接资料提到了 Redis？')">有哪些交接资料提到了 Redis？</button>
            <button @click="askExample('如何解决缓存穿透问题？')">如何解决缓存穿透问题？</button>
            <button @click="askExample('总结 Spring Boot 相关交接材料')">总结 Spring Boot 相关交接材料</button>
          </div>
        </div>

        <article v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(msg.content)"></div>

            <div v-if="msg.sources && msg.sources.length > 0" class="sources">
              <p class="sources-title">来源资料 ({{ msg.sources.length }})</p>
              <div class="source-list">
                <button
                  v-for="source in msg.sources"
                  :key="source.noteId"
                  class="source-item"
                  @click="viewNote(source.noteId)"
                >
                  <div class="source-header">
                    <strong>{{ source.title }}</strong>
                    <span>{{ (source.relevanceScore * 100).toFixed(0) }}% 相关</span>
                  </div>
                  <p>{{ source.excerpt }}</p>
                  <small v-if="source.tags">{{ source.tags }}</small>
                </button>
              </div>
            </div>

            <div v-if="msg.responseTime" class="meta">
              响应 {{ msg.responseTime }}ms · {{ msg.model }}
            </div>
          </div>
        </article>

        <article v-if="loading" class="message assistant">
          <div class="message-content">正在检索并生成回答...</div>
        </article>
      </div>

      <div class="chat-input">
        <textarea
          v-model="query"
          placeholder="输入一个需要本地 RAG 检索的问题"
          rows="3"
          @keydown.enter.prevent="handleEnter"
        ></textarea>
        <button class="primary-btn" :disabled="!query.trim() || loading" @click="sendQuery">
          {{ loading ? '生成中...' : '发送' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script>
import { nextTick, onMounted, ref } from 'vue'
import request from '@/api/request'

export default {
  name: 'RagChat',
  setup() {
    const query = ref('')
    const messages = ref([])
    const loading = ref(false)
    const stats = ref({ embeddingCoverage: 0 })
    const messagesContainer = ref(null)

    const loadStats = async () => {
      try {
        stats.value = await request.get('/ai/rag/stats')
      } catch (error) {
        console.error('Failed to load stats:', error)
      }
    }

    const sendQuery = async () => {
      if (!query.value.trim() || loading.value) return

      const userQuery = query.value.trim()
      query.value = ''
      messages.value.push({ role: 'user', content: userQuery })
      scrollToBottom()
      loading.value = true

      try {
        const result = await request.post('/ai/rag/query', {
          query: userQuery,
          topK: 5,
          includeContent: true
        })

        messages.value.push({
          role: 'assistant',
          content: result.answer,
          sources: result.sources,
          responseTime: result.responseTimeMs,
          model: result.model
        })
      } catch (error) {
        messages.value.push({
          role: 'assistant',
          content: `查询失败：${error.message}`
        })
      } finally {
        loading.value = false
        scrollToBottom()
      }
    }

    const askExample = (exampleQuery) => {
      query.value = exampleQuery
      sendQuery()
    }

    const handleEnter = (event) => {
      if (!event.shiftKey) sendQuery()
    }

    const formatMessage = (content) =>
      content
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code>$1</code>')

    const viewNote = (noteId) => {
      window.location.href = `/#/note?noteId=${noteId}`
    }

    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      })
    }

    onMounted(loadStats)

    return {
      query,
      messages,
      loading,
      stats,
      messagesContainer,
      sendQuery,
      askExample,
      handleEnter,
      formatMessage,
      viewNote
    }
  }
}
</script>

<style scoped>
.rag-chat-page {
  display: grid;
  gap: 16px;
}

.coverage-card {
  min-width: 180px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 14px;
  display: grid;
  gap: 6px;
}

.coverage-card span {
  color: var(--text-muted);
  font-size: 13px;
}

.coverage-card strong {
  font-size: 28px;
  color: var(--navy);
}

.chat-console {
  min-height: calc(100vh - 230px);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel);
  overflow: hidden;
  display: grid;
  grid-template-rows: 1fr auto;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.chat-messages {
  overflow-y: auto;
  padding: 18px;
  background: var(--panel-soft);
}

.example-queries {
  display: grid;
  gap: 10px;
  max-width: 520px;
  margin: 18px auto 0;
}

.example-queries button {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel);
  padding: 12px;
  color: var(--text);
}

.message {
  display: flex;
  margin-bottom: 14px;
}

.message.user {
  justify-content: flex-end;
}

.message-content {
  width: min(760px, 78%);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel);
  padding: 14px;
  color: var(--text);
}

.message.user .message-content {
  background: var(--blue);
  border-color: var(--blue);
  color: #fff;
}

.message-text {
  line-height: 1.7;
  word-break: break-word;
}

.sources {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--border);
}

.sources-title {
  margin: 0 0 10px;
  color: var(--text);
  font-weight: 700;
}

.source-list {
  display: grid;
  gap: 10px;
}

.source-item {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 12px;
  text-align: left;
}

.source-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.source-header span {
  color: var(--blue);
  font-size: 12px;
  font-weight: 700;
}

.source-item p,
.source-item small,
.meta {
  color: var(--text-muted);
}

.chat-input {
  border-top: 1px solid var(--border);
  padding: 14px;
  background: var(--panel);
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.chat-input textarea {
  min-height: 82px;
  resize: vertical;
}

@media (max-width: 720px) {
  .chat-input {
    grid-template-columns: 1fr;
  }

  .message-content {
    width: 100%;
  }
}
</style>
