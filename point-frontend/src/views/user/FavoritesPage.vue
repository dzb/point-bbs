<template>
  <div class="detail-layout">
    <div class="detail-main">
      <div v-if="favorites.length">
        <div v-for="f in favorites" :key="f.id" class="py-3" style="border-bottom:1px solid var(--paper-border);cursor:pointer"
          @click="$router.push(`/topics/${f.entityId}`)">
          <span style="font-size:15px;color:var(--paper-text)">{{ f.entityType === 'topic' ? '帖子' : '文章' }} #{{ f.entityId }}</span>
          <span style="font-size:12px;color:var(--paper-text2);margin-left:12px">{{ fmt(f.createTime) }}</span>
        </div>
      </div>
      <div v-else class="text-center py-16" style="color:var(--paper-text2)">暂无收藏</div>
    </div>
    <aside class="detail-aside">
      <p class="aside-tagline">记录思考，分享见闻</p>
      <div class="aside-card"><div class="aside-card-title">社区公告</div><div class="aside-card-text">point 正在建设中。</div></div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'

const auth = useAuthStore()
const favorites = ref<any[]>([])

onMounted(async () => {
  const uid = auth.user?.id
  if (!uid) return
  const { data } = await client.get(`/users/${uid}/favorites`, { params: { pageSize: 50 } })
  if (data.code===0) favorites.value = data.data || []
})

function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.detail-layout { display: flex; }
.detail-main { flex: 1; max-width: 680px; min-width: 0; padding-right: 32px; border-right: 1px solid var(--paper-border); }
.detail-aside { width: 366px; flex-shrink: 0; padding-left: 32px; }
.aside-tagline { font-size: 14px; color: var(--paper-text2); line-height: 1.8; margin-bottom: 20px; }
.aside-card { border: 1px solid var(--paper-border); border-radius: 6px; padding: 12px; margin-bottom: 12px; }
.aside-card-title { font-size: 16px; color: var(--paper-text); font-weight: 500; margin-bottom: 2px; }
.aside-card-text { font-size: 14px; color: var(--paper-text2); line-height: 1.6; }
@media (max-width: 1200px) { .detail-main { padding-right: 20px; } .detail-aside { width: 280px; padding-left: 20px; } }
@media (max-width: 1100px) { .detail-aside { display: none; } .detail-main { border-right: none; padding-right: 0; } }
</style>
