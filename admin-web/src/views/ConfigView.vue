<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'

const configs = ref([])

onMounted(async () => {
  configs.value = await adminApi.listConfigs()
})
</script>

<template>
  <section class="table-card">
    <header class="toolbar"><div><h1>Ops Config</h1><p>关键配置脱敏只读展示</p></div></header>
    <table>
      <thead><tr><th>Key</th><th>Value</th><th>Masked</th></tr></thead>
      <tbody>
        <tr v-for="item in configs" :key="item.key">
          <td>{{ item.key }}</td><td>{{ item.value }}</td><td>{{ item.masked ? 'Y' : 'N' }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.table-card { background: #fff; border-radius: 16px; padding: 18px; box-shadow: 0 10px 30px rgba(15,23,42,0.08); }
.toolbar h1 { margin: 0; } .toolbar p { margin: 4px 0 14px; color: #486581; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 12px 10px; border-bottom: 1px solid #e6edf4; text-align: left; font-size: 14px; }
</style>
