<template>
  <div class="page-shell">
    <section class="page-header">
      <div>
        <p class="eyebrow">质量统计</p>
        <h1>用问答日志和反馈发现知识库缺口。</h1>
        <p>面向演示和面试说明：AI 回答不是黑盒调用，而是可观测、可反馈、可迭代。</p>
      </div>
      <div class="row">
        <button class="ghost-btn" :disabled="loading" @click="refresh">{{ loading ? '刷新中...' : '刷新统计' }}</button>
        <button class="primary-btn" :disabled="analyzing" @click="analyze">{{ analyzing ? '分析中...' : '生成日志分析' }}</button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card"><span>问答次数</span><strong>{{ stats.usage || 0 }}</strong></article>
      <article class="metric-card"><span>无答案</span><strong>{{ stats.noAnswerCount || 0 }}</strong></article>
      <article class="metric-card"><span>失败调用</span><strong>{{ stats.failedCount || 0 }}</strong></article>
      <article class="metric-card"><span>无答案率</span><strong>{{ percent(stats.noAnswerRate) }}</strong></article>
      <article class="metric-card"><span>机器人触发</span><strong>{{ stats.botEventCount || 0 }}</strong></article>
      <article class="metric-card"><span>机器人失败</span><strong>{{ stats.botFailedEventCount || 0 }}</strong></article>
      <article class="metric-card"><span>群聊绑定</span><strong>{{ stats.botBindingCount || 0 }}</strong></article>
    </section>

    <section class="quality-layout">
      <article class="panel">
        <p class="section-label">高频问题</p>
        <h2>新人反复卡在哪里</h2>
        <div class="data-list">
          <div v-for="question in stats.topQuestions" :key="question" class="list-row">
            <strong>{{ question }}</strong>
          </div>
          <p v-if="!stats.topQuestions?.length" class="empty-state">还没有足够问答日志。</p>
        </div>
      </article>

      <article class="panel">
        <p class="section-label">知识缺口</p>
        <h2>{{ analysis.summary || '等待生成分析' }}</h2>
        <div class="gap-list">
          <article v-for="item in analysis.knowledgeGaps" :key="item">{{ item }}</article>
          <p v-if="!analysis.knowledgeGaps?.length" class="muted">无答案问题会在这里沉淀为补资料清单。</p>
        </div>
      </article>

      <article class="panel">
        <p class="section-label">机器人命令</p>
        <h2>飞书群触发分布</h2>
        <div class="data-list command-distribution">
          <div v-for="item in commandDistribution" :key="item.type" class="list-row">
            <span>
              <strong>{{ item.type }}</strong>
              <small>来自飞书群 @机器人入口</small>
            </span>
            <em class="badge success">{{ item.count }}</em>
          </div>
          <p v-if="!commandDistribution.length" class="empty-state">还没有机器人触发记录。</p>
        </div>
      </article>

      <article class="panel wide">
        <p class="section-label">Prompt 与运营建议</p>
        <h2>下一轮优化动作</h2>
        <div class="suggestion-grid">
          <article v-for="item in analysis.promptSuggestions" :key="item">{{ item }}</article>
          <article v-if="!analysis.promptSuggestions?.length">继续积累问答与反馈后再生成分析。</article>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { skillApi } from '@/api/skill'

const loading = ref(false)
const analyzing = ref(false)
const stats = reactive({ usage: 0, noAnswerCount: 0, failedCount: 0, noAnswerRate: 0, topQuestions: [] })
const analysis = reactive({ summary: '', knowledgeGaps: [], promptSuggestions: [] })
const commandDistribution = computed(() =>
  Object.entries(stats.botCommandDistribution || {})
    .map(([type, count]) => ({ type, count }))
    .sort((a, b) => b.count - a.count)
)

const refresh = async () => {
  loading.value = true
  try {
    Object.assign(stats, await skillApi.adminStats())
  } finally {
    loading.value = false
  }
}

const analyze = async () => {
  analyzing.value = true
  try {
    Object.assign(analysis, await skillApi.analyzeLogs({}))
  } finally {
    analyzing.value = false
  }
}

const percent = (value) => `${Math.round((value || 0) * 100)}%`

onMounted(refresh)
</script>

<style scoped>
.quality-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.wide {
  grid-column: 1 / -1;
}

.gap-list,
.suggestion-grid {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.gap-list article,
.suggestion-grid article {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 13px;
  color: var(--text-muted);
}

.suggestion-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 900px) {
  .quality-layout,
  .suggestion-grid {
    grid-template-columns: 1fr;
  }

  .wide {
    grid-column: auto;
  }
}
</style>
