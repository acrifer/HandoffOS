<template>
  <div class="knowledge-graph-page">
    <div class="graph-container">
      <div class="graph-header">
        <h2>🕸️ 知识图谱</h2>
        <p class="subtitle">实体关系可视化</p>
        <div class="stats">
          <span>实体: {{ stats.entityCount }}</span>
          <span>关系: {{ stats.relationCount }}</span>
          <span v-if="conflicts.length > 0" class="conflict-badge">⚠️ {{ conflicts.length }} 个冲突</span>
        </div>
      </div>

      <div class="graph-content">
        <div class="graph-canvas" ref="graphCanvas">
          <svg :width="canvasWidth" :height="canvasHeight">
            <!-- Relations (edges) -->
            <g class="relations">
              <line
                v-for="(relation, index) in visualRelations"
                :key="'rel-' + index"
                :x1="relation.x1"
                :y1="relation.y1"
                :x2="relation.x2"
                :y2="relation.y2"
                :stroke="getRelationColor(relation.type)"
                stroke-width="2"
                :opacity="relation.confidence"
              />
              <text
                v-for="(relation, index) in visualRelations"
                :key="'rel-text-' + index"
                :x="(relation.x1 + relation.x2) / 2"
                :y="(relation.y1 + relation.y2) / 2"
                class="relation-label"
                text-anchor="middle"
              >
                {{ getRelationLabel(relation.type) }}
              </text>
            </g>

            <!-- Entities (nodes) -->
            <g class="entities">
              <g
                v-for="(entity, index) in visualEntities"
                :key="'entity-' + index"
                :transform="`translate(${entity.x}, ${entity.y})`"
                @click="selectEntity(entity)"
                class="entity-node"
              >
                <circle
                  :r="30"
                  :fill="getEntityColor(entity.type)"
                  :opacity="entity.confidence"
                  stroke="#fff"
                  stroke-width="2"
                />
                <text
                  text-anchor="middle"
                  dy="5"
                  class="entity-label"
                  fill="#fff"
                >
                  {{ entity.name.substring(0, 4) }}
                </text>
              </g>
            </g>
          </svg>

          <div v-if="!graphData.entities || graphData.entities.length === 0" class="empty-state">
            <p>📊 暂无知识图谱数据</p>
            <p class="hint">请先构建知识图谱</p>
          </div>
        </div>

        <div class="graph-sidebar">
          <div class="legend">
            <h3>图例</h3>
            <div class="legend-item">
              <span class="legend-color" style="background: #667eea"></span>
              <span>人员 (PERSON)</span>
            </div>
            <div class="legend-item">
              <span class="legend-color" style="background: #f093fb"></span>
              <span>项目 (PROJECT)</span>
            </div>
            <div class="legend-item">
              <span class="legend-color" style="background: #4facfe"></span>
              <span>流程 (PROCESS)</span>
            </div>
            <div class="legend-item">
              <span class="legend-color" style="background: #43e97b"></span>
              <span>概念 (CONCEPT)</span>
            </div>
          </div>

          <div v-if="selectedEntity" class="entity-detail">
            <h3>实体详情</h3>
            <p><strong>名称:</strong> {{ selectedEntity.entityName }}</p>
            <p><strong>类型:</strong> {{ selectedEntity.entityType }}</p>
            <p><strong>描述:</strong> {{ selectedEntity.description || '无' }}</p>
            <p><strong>置信度:</strong> {{ (selectedEntity.confidence * 100).toFixed(0) }}%</p>
          </div>

          <div v-if="conflicts.length > 0" class="conflicts-panel">
            <h3>⚠️ 冲突检测</h3>
            <div v-for="(conflict, index) in conflicts" :key="index" class="conflict-item">
              <div class="conflict-header">
                <span :class="['conflict-type', conflict.conflictType.toLowerCase()]">
                  {{ conflict.conflictType }}
                </span>
                <span class="conflict-severity">
                  严重度: {{ (conflict.severity * 100).toFixed(0) }}%
                </span>
              </div>
              <p class="conflict-desc">{{ conflict.description }}</p>
              <p class="conflict-recommendation">💡 {{ conflict.recommendation }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'

export default {
  name: 'KnowledgeGraph',
  props: {
    skillId: {
      type: Number,
      required: true
    }
  },
  setup(props) {
    const graphData = ref({ entities: [], relations: [] })
    const conflicts = ref([])
    const selectedEntity = ref(null)
    const canvasWidth = ref(800)
    const canvasHeight = ref(600)

    const stats = computed(() => ({
      entityCount: graphData.value.entities?.length || 0,
      relationCount: graphData.value.relations?.length || 0
    }))

    const visualEntities = computed(() => {
      if (!graphData.value.entities) return []

      // Simple circular layout
      const entities = graphData.value.entities
      const centerX = canvasWidth.value / 2
      const centerY = canvasHeight.value / 2
      const radius = Math.min(canvasWidth.value, canvasHeight.value) / 3

      return entities.map((entity, index) => {
        const angle = (2 * Math.PI * index) / entities.length
        return {
          ...entity,
          x: centerX + radius * Math.cos(angle),
          y: centerY + radius * Math.sin(angle),
          name: entity.entityName,
          type: entity.entityType
        }
      })
    })

    const visualRelations = computed(() => {
      if (!graphData.value.relations || !visualEntities.value.length) return []

      return graphData.value.relations.map(relation => {
        const source = visualEntities.value.find(e => e.id === relation.sourceEntityId)
        const target = visualEntities.value.find(e => e.id === relation.targetEntityId)

        if (!source || !target) return null

        return {
          ...relation,
          x1: source.x,
          y1: source.y,
          x2: target.x,
          y2: target.y,
          type: relation.relationType
        }
      }).filter(r => r !== null)
    })

    const loadGraph = async () => {
      try {
        const response = await axios.get(`/ai/knowledge/graph/${props.skillId}`)
        if (response.data.code === 200) {
          graphData.value = response.data.data
        }
      } catch (error) {
        console.error('Failed to load knowledge graph:', error)
      }
    }

    const getEntityColor = (type) => {
      const colors = {
        PERSON: '#667eea',
        PROJECT: '#f093fb',
        PROCESS: '#4facfe',
        CONCEPT: '#43e97b'
      }
      return colors[type] || '#999'
    }

    const getRelationColor = (type) => {
      const colors = {
        RESPONSIBLE_FOR: '#667eea',
        DEPENDS_ON: '#f093fb',
        PREREQUISITE: '#4facfe',
        RELATED_TO: '#43e97b'
      }
      return colors[type] || '#999'
    }

    const getRelationLabel = (type) => {
      const labels = {
        RESPONSIBLE_FOR: '负责',
        DEPENDS_ON: '依赖',
        PREREQUISITE: '前置',
        RELATED_TO: '相关'
      }
      return labels[type] || type
    }

    const selectEntity = (entity) => {
      selectedEntity.value = entity
    }

    onMounted(() => {
      loadGraph()
    })

    return {
      graphData,
      conflicts,
      selectedEntity,
      canvasWidth,
      canvasHeight,
      stats,
      visualEntities,
      visualRelations,
      getEntityColor,
      getRelationColor,
      getRelationLabel,
      selectEntity
    }
  }
}
</script>

<style scoped>
.knowledge-graph-page {
  padding: 20px;
  background: #f8f9fa;
  min-height: 100vh;
}

.graph-container {
  max-width: 1400px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.graph-header {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.graph-header h2 {
  margin: 0 0 8px 0;
}

.subtitle {
  margin: 0 0 12px 0;
  opacity: 0.9;
}

.stats {
  display: flex;
  gap: 20px;
  font-size: 14px;
}

.conflict-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 12px;
}

.graph-content {
  display: flex;
  height: 700px;
}

.graph-canvas {
  flex: 1;
  position: relative;
  background: #fafafa;
  overflow: hidden;
}

.graph-canvas svg {
  display: block;
  margin: 50px auto;
}

.entity-node {
  cursor: pointer;
  transition: all 0.3s;
}

.entity-node:hover circle {
  r: 35;
  filter: brightness(1.1);
}

.entity-label {
  font-size: 12px;
  font-weight: 600;
  pointer-events: none;
}

.relation-label {
  font-size: 11px;
  fill: #666;
  pointer-events: none;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #999;
}

.hint {
  font-size: 14px;
  margin-top: 8px;
}

.graph-sidebar {
  width: 300px;
  padding: 24px;
  background: white;
  border-left: 1px solid #e0e0e0;
  overflow-y: auto;
}

.legend h3,
.entity-detail h3,
.conflicts-panel h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 14px;
}

.legend-color {
  width: 20px;
  height: 20px;
  border-radius: 50%;
}

.entity-detail {
  margin-top: 32px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.entity-detail p {
  margin: 8px 0;
  font-size: 14px;
}

.conflicts-panel {
  margin-top: 32px;
}

.conflict-item {
  padding: 12px;
  background: #fff3cd;
  border-left: 4px solid #ffc107;
  border-radius: 4px;
  margin-bottom: 12px;
}

.conflict-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.conflict-type {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: uppercase;
}

.conflict-type.contradiction {
  background: #dc3545;
  color: white;
}

.conflict-type.inconsistency {
  background: #ffc107;
  color: #333;
}

.conflict-type.ambiguity {
  background: #17a2b8;
  color: white;
}

.conflict-severity {
  font-size: 12px;
  color: #666;
}

.conflict-desc {
  font-size: 13px;
  margin: 8px 0;
}

.conflict-recommendation {
  font-size: 12px;
  color: #666;
  font-style: italic;
}
</style>
