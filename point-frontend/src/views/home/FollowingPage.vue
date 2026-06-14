<template>
  <div class="home-layout">
    <div class="home-feed">
      <div v-if="!auth.isLoggedIn" class="text-center py-16" style="color:var(--paper-text2)">请先登录</div>
      <div v-else>
        <div class="moments-list">
          <MomentCard v-for="m in moments" :key="m.id" :moment="m" @toggle-like="toggleLike" />
        </div>
        <div v-if="moments.length===0 && !loading" class="text-center py-16" style="color:var(--paper-text2)">关注更多用户，这里会显示他们的随想</div>
        <v-progress-circular v-if="loading" indeterminate color="#c43d3d" class="d-block mx-auto mt-8" />
      </div>
    </div>
    <PageAside />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import MomentCard from '@/components/MomentCard.vue'
import PageAside from '@/components/PageAside.vue'

const auth = useAuthStore()
const moments = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  if (!auth.isLoggedIn) { loading.value = false; return }
  try {
    const { data } = await client.get('/topics/following', { params: { pageSize: 50 } })
    if (data.code===0) moments.value = (data.data||[]).map((m:any)=>({...m,liked:false}))
  } catch { /* */ }
  loading.value = false
})

async function toggleLike(m: any) {
  if (m.liked) { await client.post(`/topics/${m.id}/unlike`); m.liked=false; m.likeCount-- }
  else { await client.post(`/topics/${m.id}/like`); m.liked=true; m.likeCount++ }
}
</script>

<style scoped>
.home-layout { display: flex; }
.home-feed { flex: 1; max-width: 680px; min-width: 0; padding-right: 32px; border-right: 1px solid var(--paper-border); transition: padding .2s ease; }
.moments-list { display: flex; flex-direction: column; gap: 10px; }
@media (max-width: 1300px) { .home-feed { padding-right: 24px; } }
@media (max-width: 1200px) { .home-feed { padding-right: 20px; } }
@media (max-width: 1100px) { .home-feed { border-right: none; padding-right: 0; } }
@media (max-width: 900px)  { .home-feed { padding-right: 16px; } }
</style>
