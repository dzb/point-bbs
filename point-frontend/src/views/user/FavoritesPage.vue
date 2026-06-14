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
    <PageAside />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import PageAside from '@/components/PageAside.vue'

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
.detail-main { flex: 1; max-width: 680px; min-width: 0; padding-right: 32px; border-right: 1px solid var(--paper-border); transition: padding .2s ease; }
@media (max-width: 1300px) { .detail-main { padding-right: 24px; } }
@media (max-width: 1200px) { .detail-main { padding-right: 20px; } }
@media (max-width: 1100px) { .detail-main { border-right: none; padding-right: 0; } }
@media (max-width: 900px)  { .detail-main { padding-right: 16px; } }
</style>
