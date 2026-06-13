<template>
  <div class="home-layout">
    <div class="home-feed">
      <div class="moments-list">
        <MomentCard v-for="m in moments" :key="m.id" :moment="m" @toggle-like="toggleLike" />
      </div>
      <div v-if="moments.length===0" class="text-center py-16" style="color:var(--paper-text2)">暂无随想</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import client from '@/api/client'
import MomentCard from '@/components/MomentCard.vue'

const moments = ref<any[]>([])

onMounted(async () => {
  const { data } = await client.get('/topics/moments', { params: { pageSize: 100 } })
  if (data.code===0) moments.value = (data.data||[]).map((m:any)=>({...m,liked:false}))
})

async function toggleLike(m: any) {
  if (m.liked) {
    await client.post(`/topics/${m.id}/unlike`); m.liked=false; m.likeCount--
  } else {
    await client.post(`/topics/${m.id}/like`); m.liked=true; m.likeCount++
  }
}
</script>

<style scoped>
.home-layout { display: flex; gap: 0; }
.home-feed { flex: 1; max-width: 680px; min-width: 0; padding-right: 12px; }
.moments-list { display: flex; flex-direction: column; gap: 10px; }
</style>
