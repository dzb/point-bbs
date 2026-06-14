<template>
    <div class="detail-main">
      <div v-if="favorites.length">
        <div v-for="f in favorites" :key="f.id" class="py-3" style="border-bottom:1px solid var(--paper-border);cursor:pointer"
          @click="$router.push(`/topics/${f.entityId}`)">
          <span style="font-size:15px;color:var(--paper-text)">{{ f.entityType === 'topic' ? '帖子' : '文章' }} #{{ f.entityId }}</span>
          <span style="font-size:12px;color:var(--paper-text2);margin-left:12px">{{ fmt(f.createTime) }}</span>
        </div>
      </div>
      <div v-else-if="!loading" class="text-center py-16" style="color:var(--paper-text2)">暂无收藏</div>
      <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" color="var(--paper-accent)" />
      <div v-if="hasMore" class="text-center mt-4 mb-2">
        <v-btn variant="text" :loading="loadingMore" @click="loadMore" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)">显示更多</v-btn>
      </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'

import { useRouter } from 'vue-router'

const router = useRouter()
const auth = useAuthStore()
if (!auth.isLoggedIn) { router.replace('/login') }

const favorites = ref<any[]>([])
const loading = ref(true)
const page = ref(1)
const pageSize = 30
const hasMore = ref(false)
const loadingMore = ref(false)

onMounted(() => loadItems())

async function loadItems(reset = false) {
  if (reset) page.value = 1
  const uid = auth.user?.id
  if (!uid) { loading.value = false; return }
  try {
    const { data } = await client.get(`/users/${uid}/favorites`, { params: { page: page.value, pageSize } })
    if (data.code===0) {
      const newItems = data.data || []
      favorites.value = page.value === 1 ? newItems : [...favorites.value, ...newItems]
      hasMore.value = newItems.length === pageSize
    }
  } catch { console.error('api error') }
  loading.value = false
}

async function loadMore() { page.value++; loadingMore.value = true; await loadItems(); loadingMore.value = false }

function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
</style>
