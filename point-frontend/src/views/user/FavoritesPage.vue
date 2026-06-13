<template>
  <div style="max-width:680px">
    <h1 class="text-h5 mb-4" style="font-family:'Noto Serif SC',Georgia,serif;color:var(--paper-text)">我的收藏</h1>
    <div v-if="favorites.length">
      <div v-for="f in favorites" :key="f.id" class="py-3" style="border-bottom:1px solid var(--paper-border);cursor:pointer"
        @click="$router.push(`/topics/${f.entityId}`)">
        <span style="font-size:15px;color:var(--paper-text)">{{ f.entityType === 'topic' ? '帖子' : '文章' }} #{{ f.entityId }}</span>
        <span style="font-size:12px;color:var(--paper-text2);margin-left:12px">{{ fmt(f.createTime) }}</span>
      </div>
    </div>
    <div v-else class="text-center py-16" style="color:var(--paper-text2)">暂无收藏</div>
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
