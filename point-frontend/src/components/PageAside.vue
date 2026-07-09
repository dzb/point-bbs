<template>
  <aside class="page-aside">
    <p class="aside-tagline">记录思考，分享见闻</p>
    <div class="aside-card"><div class="aside-card-title">社区公告</div>
      <div class="aside-card-text">欢迎来到 point — 一个安静的思考角落。记录随想，发表文章，关注有趣的人。</div>
    </div>
    <div class="aside-card">
      <div class="aside-card-title">推荐阅读</div>
      <template v-if="loading">
        <div class="aside-card-text">加载中…</div>
      </template>
      <template v-else-if="items.length">
        <router-link
          v-for="t in items"
          :key="t.id"
          :to="`/topics/${t.id}`"
          class="rec-item"
        >{{ t.title || '无标题' }}</router-link>
      </template>
      <div v-else class="aside-card-text">暂无推荐</div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import client from '@/api/client'

const items = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await client.get('/topics/recommended', { params: { limit: 5 } })
    if (data.code === 0 && Array.isArray(data.data)) items.value = data.data
  } catch { /* ignore */ }
  loading.value = false
})
</script>

<style scoped>
.page-aside { padding: 20px 24px; }
.rec-item {
  display: block;
  padding: 6px 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--paper-text2);
  text-decoration: none;
  border-bottom: 1px solid var(--paper-border);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rec-item:hover { color: var(--paper-accent); }
</style>
