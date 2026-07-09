<template>
    <div class="home-feed">
      <div v-if="auth.isLoggedIn">
        <div class="moments-list">
          <MomentCard v-for="m in moments" :key="m.id" :moment="m" @toggle-like="toggleLike" @delete-moment="removeMoment" />
        </div>
        <div v-if="moments.length===0 && !loading" class="text-center py-16" style="color:var(--paper-text2)">关注更多用户，这里会显示他们的随想</div>
        <v-progress-circular v-if="loading" indeterminate color="var(--paper-accent)" class="d-block mx-auto mt-8" />
        <LoadMore :has-more="hasMore" :loading="loadingMore" @load-more="loadMore" />
      </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import MomentCard from '@/components/MomentCard.vue'
import LoadMore from '@/components/LoadMore.vue'

import { useRouter } from 'vue-router'

const router = useRouter()
const auth = useAuthStore()
if (!auth.isLoggedIn) { router.replace('/login') }

const moments = ref<any[]>([])
const loading = ref(true)
const page = ref(1)
const pageSize = 30
const hasMore = ref(false)
const loadingMore = ref(false)

onMounted(() => { if (auth.isLoggedIn) loadItems(); else loading.value = false })

async function loadItems(reset = false) {
  if (reset) page.value = 1
  try {
    const { data } = await client.get('/topics/following', { params: { page: page.value, pageSize } })
    if (data.code===0) {
      const newItems = (data.data||[]).map((m:any)=>({...m,liked:false}))
      moments.value = page.value === 1 ? newItems : [...moments.value, ...newItems]
      hasMore.value = newItems.length === pageSize
    }
  } catch { console.error('api error') }
  loading.value = false
}

async function loadMore() { page.value++; loadingMore.value = true; await loadItems(); loadingMore.value = false }

async function toggleLike(m: any) {
  if (m.liked) { await client.post(`/topics/${m.id}/unlike`); m.liked=false; m.likeCount-- }
  else { await client.post(`/topics/${m.id}/like`); m.liked=true; m.likeCount++ }
}

function removeMoment(id: number) { moments.value = moments.value.filter(m => m.id !== id) }
</script>

<style scoped>
</style>
