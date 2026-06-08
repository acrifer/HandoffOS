<template>
  <div class="page-shell graph-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">知识图谱</p>
        <h1>查看交接知识中的实体、关系和冲突。</h1>
        <p>辅助展示 Skill 资料中的负责人、项目、流程和概念关系，用于发现交接材料的结构缺口。</p>
      </div>
      <div class="graph-stats">
        <article><strong>{{ stats.entityCount }}</strong><span>实体</span></article>
        <article><strong>{{ stats.relationCount }}</strong><span>关系</span></article>
        <article><strong>{{ conflicts.length }}</strong><span>冲突</span></article>
      </div>
    </section>

    <section class="graph-layout">
      <article class="graph-canvas panel">
        <svg :width="canvasWidth" :height="canvasHeight" viewBox="0 0 800 600" preserveAspectRatio="xMidYMid meet">
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

          <g class="entities">
            <g
              v-for="(entity, index) in visualEntities"
              :key="'entity-' + index"
              :transform="`translate(${entity.x}, ${entity.y})`"
              class="entity-node"
              @click="selectEntity(entity)"
            >
              <circle
                :r="30"
                :fill="getEntityColor(entity.type)"
                :opacity="entity.confidence"
                stroke="#fff"
                stroke-width="2"
              />
              <text text-anchor="middle" dy="5" class="entity-label" fill="#fff">
                {{ entity.name.substring(0, 4) }}
              </text>
            </g>
          </g>
        </svg>

        <div v-if="!graphData.entities || graphData.entities.length === 0" class="empty-state">
          暂无知识图谱数据。请先接入并解析交接资料。
        </div>
      </article>

      <aside class="graph-sidebar">
        <article class="panel">
          <p class="section-label">图例</p>
          <div class="legend">
            <div v-for="item in legendItems" :key="item.label" class="legend-item">
              <span class="legend-color" :style="{ background: item.color }"></span>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </article>

        <article v-if="selectedEntity" class="panel">
          <p class="section-label">实体详情</p>
          <h2>{{ selectedEntity.entityName }}</h2>
          <p><strong>类型：</strong>{{ selectedEntity.entityType }}</p>
          <p><strong>描述：</strong>{{ selectedEntity.description || '暂无描述' }}</p>
          <p><strong>置信度：</strong>{{ (selectedEntity.confidence * 100).toFixed(0) }}%</p>
        </article>

        <article v-if="conflicts.length > 0" class="panel">
          <p class="section-label">冲突检测</p>
          <div class="conflict-list">
            <article v-for="(conflict, index) in conflicts" :key="index">
              <div class="conflict-header">
                <span :class="['conflict-type', conflict.conflictType.toLowerCase()]">
                  {{ conflict.conflictType }}
                </span>
                <span>严重度 {{ (conflict.severity * 100).toFixed(0) }}%</span>
              </div>
              <p>{{ conflict.description }}</p>
              <small>{{ conflict.recommendation }}</small>
            </article>
          </div>
        </article>
      </aside>
    </section>
  </div>
</template>

<script>
import { computed, onMounted, ref } from 'vue'
import request from '@/api/request'

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

    const entityColors = {
      PERSON: '#2563eb',
      PROJECT: '#0f766e',
      PROCESS: '#b45309',
      CONCEPT: '#475569'
    }

    const relationColors = {
      RESPONSIBLE_FOR: '#2563eb',
      DEPENDS_ON: '#b45309',
      PREREQUISITE: '#0f766e',
      RELATED_TO: '#64748b'
    }

    const legendItems = [
      { label: '人员 PERSON', color: entityColors.PERSON },
      { label: '项目 PROJECT', color: entityColors.PROJECT },
      { label: '流程 PROCESS', color: entityColors.PROCESS },
      { label: '概念 CONCEPT', color: entityColors.CONCEPT }
    ]

    const stats = computed(() => ({
      entityCount: graphData.value.entities?.length || 0,
      relationCount: graphData.value.relations?.length || 0
    }))

    const visualEntities = computed(() => {
      if (!graphData.value.entities) return []

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
      }).filter(Boolean)
    })

    const loadGraph = async () => {
      try {
        graphData.value = await request.get(`/ai/knowledge/graph/${props.skillId}`)
      } catch (error) {
        console.error('Failed to load knowledge graph:', error)
      }
    }

    const getEntityColor = (type) => entityColors[type] || '#64748b'
    const getRelationColor = (type) => relationColors[type] || '#94a3b8'
    const getRelationLabel = (type) => ({
      RESPONSIBLE_FOR: '负责',
      DEPENDS_ON: '依赖',
      PREREQUISITE: '前置',
      RELATED_TO: '相关'
    }[type] || type)

    const selectEntity = (entity) => {
      selectedEntity.value = entity
    }

    onMounted(loadGraph)

    return {
      graphData,
      conflicts,
      selectedEntity,
      canvasWidth,
      canvasHeight,
      stats,
      visualEntities,
      visualRelations,
      legendItems,
      getEntityColor,
      getRelationColor,
      getRelationLabel,
      selectEntity
    }
  }
}
</script>

<style scoped>
.graph-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.graph-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(90px, 1fr));
  gap: 10px;
}

.graph-stats article {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 13px;
}

.graph-stats strong {
  display: block;
  font-size: 24px;
  color: var(--navy);
}

.graph-stats span {
  color: var(--text-muted);
  font-size: 13px;
}

.graph-canvas {
  position: relative;
  min-height: 640px;
  overflow: hidden;
}

.graph-canvas svg {
  width: 100%;
  height: min(620px, 70vh);
  display: block;
  background: var(--panel-soft);
}

.entity-node {
  cursor: pointer;
}

.entity-node:hover circle {
  filter: brightness(1.08);
}

.entity-label {
  font-size: 12px;
  font-weight: 700;
  pointer-events: none;
}

.relation-label {
  font-size: 11px;
  fill: var(--text-muted);
  pointer-events: none;
}

.graph-sidebar {
  display: grid;
  gap: 16px;
  align-content: start;
}

.legend {
  display: grid;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-muted);
}

.legend-color {
  width: 18px;
  height: 18px;
  border-radius: 999px;
}

.conflict-list {
  display: grid;
  gap: 10px;
}

.conflict-list article {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--panel-soft);
  padding: 12px;
}

.conflict-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: var(--text-muted);
  font-size: 12px;
}

.conflict-type {
  border-radius: 999px;
  padding: 3px 8px;
  background: #fef3c7;
  color: var(--amber);
  font-weight: 700;
}

.conflict-list p {
  margin: 10px 0;
}

.conflict-list small {
  color: var(--text-muted);
}

@media (max-width: 980px) {
  .graph-layout {
    grid-template-columns: 1fr;
  }

  .graph-stats {
    grid-template-columns: 1fr;
  }
}
</style>
