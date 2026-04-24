<template>
  <div class="rag-chat-page">
    <div class="chat-container">
      <div class="chat-header">
        <h2>💬 智能问答</h2>
        <p class="subtitle">基于您的笔记，使用 RAG 技术回答问题</p>
        <div class="stats">
          <span>向量化覆盖率: {{ (stats.embeddingCoverage * 100).toFixed(1) }}%</span>
        </div>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <p>👋 您好！我可以帮您查找和总结笔记中的内容。</p>
          <div class="example-queries">
            <p>试试这些问题：</p>
            <button @click="askExample('我写过哪些关于 Redis 的笔记？')">我写过哪些关于 Redis 的笔记？</button>
            <button @click="askExample('如何解决缓存穿透问题？')">如何解决缓存穿透问题？</button>
            <button @click="askExample('总结一下我学习 Spring Boot 的笔记')">总结一下我学习 Spring Boot 的笔记</button>
          </div>
        </div>

        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(msg.content)"></div>

            <div v-if="msg.sources && msg.sources.length > 0" class="sources">
              <p class="sources-title">📚 来源笔记 ({{ msg.sources.length }})</p>
              <div class="source-list">
                <div v-for="source in msg.sources" :key="source.noteId" class="source-item" @click="viewNote(source.noteId)">
                  <div class="source-header">
                    <strong>{{ source.title }}</strong>
                    <span class="relevance">{{ (source.relevanceScore * 100).toFixed(0) }}% 相关</span>
                  </div>
                  <p class="source-excerpt">{{ source.excerpt }}</p>
                  <span v-if="source.tags" class="source-tags">{{ source.tags }}</span>
                </div>
              </div>
            </div>

            <div v-if="msg.responseTime" class="meta">
              响应时间: {{ msg.responseTime }}ms | 模型: {{ msg.model }}
            </div>
          </div>
        </div>

        <div v-if="loading" class="message assistant">
          <div class="message-content">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <textarea
          v-model="query"
          placeholder="问我关于您笔记的任何问题..."
          @keydown.enter.prevent="handleEnter"
          rows="3"
        ></textarea>
        <button @click="sendQuery" :disabled="!query.trim() || loading" class="send-btn">
          {{ loading ? '思考中...' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue'
import axios from 'axios'

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
        const response = await axios.get('/ai/rag/stats')
        if (response.data.code === 200) {
          stats.value = response.data.data
        }
      } catch (error) {
        console.error('Failed to load stats:', error)
      }
    }

    const sendQuery = async () => {
      if (!query.value.trim() || loading.value) return

      const userQuery = query.value.trim()
      query.value = ''

      // Add user message
      messages.value.push({
        role: 'user',
        content: userQuery
      })

      scrollToBottom()
      loading.value = true

      try {
        const response = await axios.post('/ai/rag/query', {
          query: userQuery,
          topK: 5,
          includeContent: true
        })

        if (response.data.code === 200) {
          const result = response.data.data
          messages.value.push({
            role: 'assistant',
            content: result.answer,
            sources: result.sources,
            responseTime: result.responseTimeMs,
            model: result.model
          })
        } else {
          messages.value.push({
            role: 'assistant',
            content: '抱歉，查询失败：' + response.data.message
          })
        }
      } catch (error) {
        messages.value.push({
          role: 'assistant',
          content: '抱歉，查询过程中出现错误：' + error.message
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
      if (!event.shiftKey) {
        sendQuery()
      }
    }

    const formatMessage = (content) => {
      // Simple markdown-like formatting
      return content
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code>$1</code>')
    }

    const viewNote = (noteId) => {
      // Navigate to note detail
      window.location.href = `/#/notes/${noteId}`
    }

    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
        }
      })
    }

    onMounted(() => {
      loadStats()
    })

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
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.chat-container {
  width: 100%;
  max-width: 900px;
  height: 90vh;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.chat-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
}

.subtitle {
  margin: 0 0 12px 0;
  opacity: 0.9;
  font-size: 14px;
}

.stats {
  font-size: 13px;
  opacity: 0.8;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f8f9fa;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.example-queries {
  margin-top: 32px;
}

.example-queries p {
  margin-bottom: 16px;
  font-weight: 500;
}

.example-queries button {
  display: block;
  width: 100%;
  max-width: 400px;
  margin: 12px auto;
  padding: 12px 20px;
  background: white;
  border: 2px solid #667eea;
  border-radius: 8px;
  color: #667eea;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.example-queries button:hover {
  background: #667eea;
  color: white;
  transform: translateY(-2px);
}

.message {
  margin-bottom: 20px;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.user .message-content {
  background: #667eea;
  color: white;
  border-radius: 18px 18px 4px 18px;
}

.message.assistant .message-content {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 18px 18px 18px 4px;
}

.message-content {
  max-width: 75%;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.message-text {
  line-height: 1.6;
  word-wrap: break-word;
}

.sources {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e0e0e0;
}

.sources-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.source-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.source-item {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e0e0e0;
}

.source-item:hover {
  background: #e9ecef;
  transform: translateX(4px);
}

.source-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.relevance {
  font-size: 12px;
  color: #667eea;
  font-weight: 600;
}

.source-excerpt {
  font-size: 13px;
  color: #666;
  margin: 8px 0;
  line-height: 1.5;
}

.source-tags {
  font-size: 12px;
  color: #999;
}

.meta {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
}

.typing-indicator {
  display: flex;
  gap: 4px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

.chat-input {
  padding: 20px;
  background: white;
  border-top: 1px solid #e0e0e0;
  display: flex;
  gap: 12px;
}

.chat-input textarea {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
  resize: none;
  font-family: inherit;
  transition: border-color 0.3s;
}

.chat-input textarea:focus {
  outline: none;
  border-color: #667eea;
}

.send-btn {
  padding: 12px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
