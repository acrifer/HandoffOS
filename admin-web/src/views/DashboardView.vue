<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'

const dashboard = ref({ recentIssues: [] })

onMounted(async () => {
  dashboard.value = await adminApi.dashboard()
})
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div>
        <h1>Dashboard</h1>
        <p>后台总览和最近异常摘要</p>
      </div>
    </header>

    <div class="cards">
      <article class="card"><strong>{{ dashboard.totalUsers || 0 }}</strong><span>Users</span></article>
      <article class="card"><strong>{{ dashboard.totalNotes || 0 }}</strong><span>Notes</span></article>
      <article class="card"><strong>{{ dashboard.totalTasks || 0 }}</strong><span>Tasks</span></article>
      <article class="card"><strong>{{ dashboard.failedAiJobs || 0 }}</strong><span>Failed AI Jobs</span></article>
      <article class="card"><strong>{{ dashboard.onlineServices || 0 }}/{{ dashboard.totalServices || 0 }}</strong><span>Online Services</span></article>
    </div>

    <div class="table-card">
      <h2>Recent Issues</h2>
      <table>
        <thead><tr><th>Type</th><th>Title</th><th>Message</th><th>Time</th></tr></thead>
        <tbody>
          <tr v-for="item in dashboard.recentIssues || []" :key="`${item.type}-${item.title}-${item.occurredAt}`">
            <td>{{ item.type }}</td>
            <td>{{ item.title }}</td>
            <td>{{ item.message }}</td>
            <td>{{ item.occurredAt || '-' }}</td>
          </tr>
          <tr v-if="!(dashboard.recentIssues || []).length"><td colspan="4">No recent issues</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.panel { display: grid; gap: 18px; }
.panel-header h1, .table-card h2 { margin: 0 0 6px; color: #102a43; }
.panel-header p { margin: 0; color: #486581; }
.cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; }
.card, .table-card { background: #fff; border-radius: 16px; padding: 18px; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08); }
.card strong { font-size: 28px; display: block; color: #0f766e; }
.card span { color: #486581; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 12px 10px; border-bottom: 1px solid #e6edf4; text-align: left; font-size: 14px; }
</style>
