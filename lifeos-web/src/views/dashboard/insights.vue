<template>
  <div class="page-shell">
    <section class="page-header">
      <div>
        <p class="eyebrow">知识洞察</p>
        <h1>从交接资料中观察覆盖度和结构风险。</h1>
        <p>复用原洞察接口，但把展示重点从个人学习改成资料覆盖、主题分布和后续补充建议。</p>
      </div>
      <div class="period-selector">
        <button
          :class="['ghost-btn', { active: period === 'weekly' }]"
          @click="loadReport('weekly')"
        >
          本周
        </button>
        <button
          :class="['ghost-btn', { active: period === 'monthly' }]"
          @click="loadReport('monthly')"
        >
          本月
        </button>
      </div>
    </section>

    <section v-if="loading" class="empty-state">正在生成知识洞察...</section>

    <template v-else-if="report">
      <section class="metric-grid">
        <article class="metric-card">
          <span>覆盖评分</span>
          <strong>{{ (report.overallScore * 100).toFixed(0) }}</strong>
        </article>
        <article class="metric-card">
          <span>资料数量</span>
          <strong>{{ report.statistics.totalNotes }}</strong>
        </article>
        <article class="metric-card">
          <span>资料字数</span>
          <strong>{{ formatNumber(report.statistics.totalWords) }}</strong>
        </article>
        <article class="metric-card">
          <span>平均篇幅</span>
          <strong>{{ report.statistics.avgWordsPerNote }}</strong>
        </article>
      </section>

      <section class="insights-grid">
        <article class="panel">
          <p class="section-label">周期摘要</p>
          <h2>{{ report.period }}资料覆盖</h2>
          <p class="summary">{{ report.summary }}</p>
          <div class="score-bar">
            <span :style="{ width: `${Math.round(report.overallScore * 100)}%` }"></span>
          </div>
        </article>

        <article class="panel">
          <p class="section-label">主题分布</p>
          <h2>哪些交接主题已有沉淀</h2>
          <div class="topic-list">
            <article
              v-for="cluster in report.topicClusters.slice(0, 6)"
              :key="cluster.topicName"
            >
              <div class="topic-row">
                <strong>{{ cluster.topicName }}</strong>
                <span>{{ cluster.noteCount }} 篇</span>
              </div>
              <div class="bar">
                <span :style="{ width: `${Math.round(cluster.percentage * 100)}%` }"></span>
              </div>
            </article>
          </div>
        </article>

        <article class="panel">
          <p class="section-label">资料模式</p>
          <h2>结构完整性线索</h2>
          <div class="pattern-list">
            <article v-for="pattern in report.patterns" :key="pattern.description">
              <div class="topic-row">
                <strong>{{ pattern.description }}</strong>
                <em :class="['score-pill', getScoreClass(pattern.score)]">{{ (pattern.score * 100).toFixed(0) }} 分</em>
              </div>
              <ul>
                <li v-for="insight in pattern.insights" :key="insight">{{ insight }}</li>
              </ul>
              <p>{{ pattern.recommendation }}</p>
            </article>
          </div>
        </article>

        <article class="panel">
          <p class="section-label">补充建议</p>
          <h2>下一步资料治理动作</h2>
          <div class="recommendation-list">
            <article v-for="rec in report.recommendations" :key="rec">{{ rec }}</article>
            <p v-if="!report.recommendations.length" class="muted">当前暂无建议，继续补充交接资料后再生成洞察。</p>
          </div>
        </article>
      </section>
    </template>

    <section v-else class="empty-state">
      暂无资料洞察。请先在资料暂存区或 Skill 工作台接入交接资料。
    </section>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue'
import request from '@/api/request'

export default {
  name: 'InsightsView',
  setup() {
    const report = ref(null)
    const loading = ref(false)
    const period = ref('weekly')

    const loadReport = async (selectedPeriod) => {
      period.value = selectedPeriod
      loading.value = true

      try {
        const endpoint = selectedPeriod === 'weekly' ? '/ai/insight/weekly' : '/ai/insight/monthly'
        report.value = await request.get(endpoint)
      } catch (error) {
        console.error('Failed to load report:', error)
      } finally {
        loading.value = false
      }
    }

    const getScoreClass = (score) => {
      if (score >= 0.8) return 'excellent'
      if (score >= 0.6) return 'good'
      if (score >= 0.4) return 'fair'
      return 'poor'
    }

    const formatNumber = (num) => {
      if (num >= 10000) return `${(num / 10000).toFixed(1)}w`
      return num
    }

    onMounted(() => {
      loadReport('weekly')
    })

    return {
      report,
      loading,
      period,
      loadReport,
      getScoreClass,
      formatNumber
    }
  }
}
</script>

<style scoped>
.period-selector {
  display: flex;
  gap: 8px;
}

.period-selector .active {
  background: var(--blue);
  border-color: var(--blue);
  color: #fff;
}

.insights-grid {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 16px;
}

.score-bar,
.bar {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--bg-muted);
}

.score-bar span,
.bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--blue);
}

.topic-list,
.pattern-list,
.recommendation-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.topic-list article,
.pattern-list article,
.recommendation-list article {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 13px;
}

.topic-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.topic-row span,
.pattern-list p,
.pattern-list li {
  color: var(--text-muted);
}

.pattern-list ul {
  margin: 10px 0;
  padding-left: 18px;
}

.score-pill {
  border-radius: 999px;
  padding: 4px 9px;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.score-pill.excellent,
.score-pill.good {
  background: #dcfce7;
  color: var(--green);
}

.score-pill.fair {
  background: #fef3c7;
  color: var(--amber);
}

.score-pill.poor {
  background: #fee2e2;
  color: var(--red);
}

@media (max-width: 960px) {
  .insights-grid {
    grid-template-columns: 1fr;
  }
}
</style>
