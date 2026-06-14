<template>
  <div>
    <h1 class="text-h5 mb-4">搜索结果: {{ query }}</h1>
    <v-card v-for="t in topics" :key="t.id" class="mb-3" @click="$router.push(`/topics/${t.id}`)">
      <v-card-item>
        <v-card-title class="text-body-1">{{ t.title }}</v-card-title>
        <v-card-subtitle>
          {{ t.user?.nickname }} · {{ formatTime(t.createTime) }}
          <span class="ml-2">评论 {{ t.commentCount }}</span>
        </v-card-subtitle>
      </v-card-item>
    </v-card>
    <div v-if="topics.length === 0 && !loading" class="text-center py-8 text-grey">没有找到相关帖子</div>
    <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" />
    <div v-if="hasMore" class="text-center mt-4 mb-2">
      <v-btn variant="text" :loading="loadingMore" @click="loadMore" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)">显示更多</v-btn>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import client from '@/api/client'

const route = useRoute()
const query = ref(route.query.q as string || '')
const topics = ref<any[]>([])
const loading = ref(true)
const page = ref(1)
const pageSize = 30
const hasMore = ref(false)
const loadingMore = ref(false)

onMounted(() => loadItems())

async function loadItems(reset = false) {
  if (reset) page.value = 1
  if (!query.value) { loading.value = false; return }
  try {
    const { data } = await client.get('/topics/search', { params: { q: query.value, page: page.value, pageSize } })
    if (data.code === 0) {
      const newItems = data.data?.items || []
      topics.value = page.value === 1 ? newItems : [...topics.value, ...newItems]
      hasMore.value = newItems.length === pageSize
    }
  } catch { /* */ }
  loading.value = false
}

async function loadMore() { page.value++; loadingMore.value = true; await loadItems(); loadingMore.value = false }

function formatTime(ts: number) {
  return ts ? new Date(ts).toLocaleDateString('zh-CN') : ''
}
</script>
