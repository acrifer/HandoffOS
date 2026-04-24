<template>
  <div class="insights-page">
    <div class="insights-container">
      <div class="insights-header">
        <h2>📊 学习洞察</h2>
        <p class="subtitle">智能分析您的学习模式</p>
        <div class="period-selector">
          <button
            :class="['period-btn', { active: period === 'weekly' }]"
            @click="loadReport('weekly')"
          >
            本周
          </button>
          <button
            :class="['period-btn', { active: period === 'monthly' }]"
            @click="loadReport('monthly')"
          >
            本月
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <p>正在分析您的学习数据...</p>
      </div>

      <div v-else-if="report" class="insights-content">
        <!-- Overall Score -->
        <div class="score-card">
          <div class="score-circle">
            <svg width="120" height="120">
              <circle cx="60" cy="60" r="50" fill="none" stroke="#e0e0e0" stroke-width="10" />
              <circle
                cx="60"
                cy="60"
                r="50"
                fill="none"
                :stroke="getScoreColor(report.overallScore)"
                stroke-width="10"
                :stroke-dasharray="`${report.overallScore * 314} 314`"
                transform="rotate(-90 60 60)"
              />
            </svg>
            <div class="score-text">
              <span class="score-value">{{ (report.overallScore * 100).toFixed(0) }}</span>
              <span class="score-label">分</span>
            </div>
          </div>
          <div class="score-info">
            <h3>{{ report.period }}学习评分</h3>
            <p class="summary">{{ report.summary }}</p>
          </div>
        </div>

        <!-- Statistics -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">📝</div>
            <div class="stat-value">{{ report.statistics.totalNotes }}</div>
            <div class="stat-label">笔记数量</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">✍️</div>
            <div class="stat-value">{{ formatNumber(report.statistics.totalWords) }}</div>
            <div class="stat-label">总字数</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">📄</div>
            <div class="stat-value">{{ report.statistics.avgWordsPerNote }}</div>
            <div class="stat-label">平均每篇</div>
          </div>
        </div>

        <!-- Topic Clusters -->
        <div class="section">
          <h3>🎯 主题分布</h3>
          <div class="topic-clusters">
            <div
              v-for="(cluster, index) in report.topicClusters.slice(0, 5)"
              :key="index"
              class="cluster-item"
            >
              <div class="cluster-header">
                <span class="cluster-name">{{ cluster.topicName }}</span>
                <span class="cluster-count">{{ cluster.noteCount }} 篇</span>
              </div>
              <div class="cluster-bar">
                <div
                  class="cluster-fill"
                  :style="{ width: (cluster.percentage * 100) + '%' }"
                ></div>
              </div>
              <div class="cluster-percentage">{{ (cluster.percentage * 100).toFixed(1) }}%</div>
            </div>
          </div>
        </div>

        <!-- Learning Patterns -->
        <div class="section">
          <h3>📈 学习模式</h3>
          <div class="patterns-grid">
            <div
              v-for="(pattern, index) in report.patterns"
              :key="index"
              class="pattern-card"
            >
              <div class="pattern-header">
                <h4>{{ pattern.description }}</h4>
                <span :class="['pattern-score', getScoreClass(pattern.score)]">
                  {{ (pattern.score * 100).toFixed(0) }} 分
                </span>
              </div>
              <ul class="pattern-insights">
                <li v-for="(insight, i) in pattern.insights" :key="i">{{ insight }}</li>
              </ul>
              <div class="pattern-recommendation">
                💡 {{ pattern.recommendation }}
              </div>
            </div>
          </div>
        </div>

        <!-- Recommendations -->
        <div v-if="report.recommendations.length > 0" class="section">
          <h3>💡 个性化建议</h3>
          <div class="recommendations">
            <div
              v-for="(rec, index) in report.recommendations"
              :key="index"
              class="recommendation-item"
            >
              <span class="rec-icon">✨</span>
              <span class="rec-text">{{ rec }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <p>📊 暂无学习数据</p>
        <p class="hint">请先创建一些笔记</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'

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
        const response = await axios.get(endpoint)

        if (response.data.code === 200) {
          report.value = response.data.data
        } else {
          console.error('Failed to load report:', response.data.message)
        }
      } catch (error) {
        console.error('Failed to load report:', error)
      } finally {
        loading.value = false
      }
    }

    const getScoreColor = (score) => {
      if (score >= 0.8) return '#43e97b'
      if (score >= 0.6) return '#4facfe'
      if (score >= 0.4) return '#ffc107'
      return '#f093fb'
    }

    const getScoreClass = (score) => {
      if (score >= 0.8) return 'excellent'
      if (score >= 0.6) return 'good'
      if (score >= 0.4) return 'fair'
      return 'poor'
    }

    const formatNumber = (num) => {
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + 'w'
      }
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
      getScoreColor,
      getScoreClass,
      formatNumber
    }
  }
}
</script>

<style scoped>
.insights-page {
  padding: 20px;
  background: #f8f9fa;
  min-height: 100vh;
}

.insights-container {
  max-width: 1200px;
  margin: 0 auto;
}

.insights-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 32px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.insights-header h2 {
  margin: 0 0 8px 0;
}

.subtitle {
  margin: 0 0 20px 0;
  opacity: 0.9;
}

.period-selector {
  display: flex;
  gap: 12px;
}

.period-btn {
  padding: 8px 24px;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.period-btn.active {
  background: white;
  color: #667eea;
}

.loading {
  text-align: center;
  padding: 60px 20px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.insights-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.score-card {
  background: white;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 32px;
}

.score-circle {
  position: relative;
  width: 120px;
  height: 120px;
}

.score-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.score-value {
  font-size: 32px;
  font-weight: 700;
  color: #333;
}

.score-label {
  font-size: 14px;
  color: #999;
}

.score-info h3 {
  margin: 0 0 12px 0;
  font-size: 24px;
}

.summary {
  color: #666;
  line-height: 1.6;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.stat-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  display: block;
  margin-bottom: 8px;
}

.stat-label {
  color: #999;
  font-size: 14px;
}

.section {
  background: white;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.section h3 {
  margin: 0 0 20px 0;
  font-size: 20px;
}

.topic-clusters {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cluster-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.cluster-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.cluster-name {
  font-weight: 600;
  color: #333;
}

.cluster-count {
  color: #999;
  font-size: 14px;
}

.cluster-bar {
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.cluster-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.5s;
}

.cluster-percentage {
  text-align: right;
  font-size: 14px;
  color: #667eea;
  font-weight: 600;
}

.patterns-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.pattern-card {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border-left: 4px solid #667eea;
}

.pattern-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.pattern-header h4 {
  margin: 0;
  font-size: 16px;
}

.pattern-score {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}

.pattern-score.excellent {
  background: #d4edda;
  color: #155724;
}

.pattern-score.good {
  background: #d1ecf1;
  color: #0c5460;
}

.pattern-score.fair {
  background: #fff3cd;
  color: #856404;
}

.pattern-score.poor {
  background: #f8d7da;
  color: #721c24;
}

.pattern-insights {
  margin: 12px 0;
  padding-left: 20px;
}

.pattern-insights li {
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.pattern-recommendation {
  margin-top: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  font-size: 14px;
  color: #667eea;
}

.recommendations {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #43e97b;
}

.rec-icon {
  font-size: 20px;
}

.rec-text {
  flex: 1;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.hint {
  font-size: 14px;
  margin-top: 8px;
}
</style>
