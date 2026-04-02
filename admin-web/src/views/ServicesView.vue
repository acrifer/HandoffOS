<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'

const services = ref([])

async function load() {
  services.value = await adminApi.listServices()
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <header class="toolbar"><div><h1>Ops Services</h1><p>容器内关键服务 TCP 状态</p></div><button @click="load">刷新</button></header>
    <div class="cards">
      <article v-for="item in services" :key="item.name" class="card">
        <div class="headline"><strong>{{ item.name }}</strong><span :class="item.status === 'UP' ? 'ok' : 'down'">{{ item.status }}</span></div>
        <p>{{ item.category }} · {{ item.host }}:{{ item.port }}</p>
        <a v-if="item.accessUrl" :href="item.accessUrl" target="_blank" rel="noreferrer">{{ item.accessUrl }}</a>
        <small>{{ item.detail }}</small>
      </article>
    </div>
  </section>
</template>

<style scoped>
.panel { display: grid; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: end; }
.toolbar h1 { margin: 0; } .toolbar p { margin: 4px 0 0; color: #486581; }
.cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; }
.card { background: #fff; border-radius: 16px; padding: 18px; box-shadow: 0 10px 30px rgba(15,23,42,0.08); display: grid; gap: 10px; }
.headline { display: flex; justify-content: space-between; gap: 12px; }
.ok { color: #087f5b; } .down { color: #b42318; }
button { background: #0f766e; color: #fff; border: none; border-radius: 10px; padding: 10px 12px; cursor: pointer; }
a { color: #0f766e; }
</style>
