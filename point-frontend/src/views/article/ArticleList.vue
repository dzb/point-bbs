<template>
    <div class="home-feed">
      <div v-for="a in articles" :key="a.id" class="article-item" @click="$router.push(`/articles/${a.id}`)">
        <div class="d-flex">
          <div class="flex-grow-1">
            <div class="mb-1" style="font-size:16px;font-weight:500;color:var(--paper-text);line-height:1.4">{{ a.title }}</div>
            <div style="font-size:13px;color:var(--paper-text2);line-height:1.6;margin-bottom:6px">
              {{ a.summary || stripHtml(a.content).substring(0, 120) }}
            </div>
            <div style="font-size:12px;color:var(--paper-text2)">
              {{ a.viewCount }} 阅读 · {{ a.commentCount }} 评论 · {{ fmt(a.createTime) }}
            </div>
          </div>
          <div v-if="a.cover" class="ml-3 flex-shrink-0" style="width:80px;height:60px;border-radius:6px;overflow:hidden">
            <img :src="a.cover" style="width:100%;height:100%;object-fit:cover" />
          </div>
        </div>
      </div>
      <div v-if="articles.length===0 && !loading" class="text-center py-16" style="color:var(--paper-text2)">暂无文章</div>
      <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" color="var(--paper-accent)" />
      <div v-if="hasMore" class="text-center mt-4 mb-2">
        <v-btn variant="text" :loading="loadingMore" @click="loadMore" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)">显示更多</v-btn>
      </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import client from '@/api/client'

const articles = ref<any[]>([])
const loading = ref(true)
const page = ref(1)
const pageSize = 30
const hasMore = ref(false)
const loadingMore = ref(false)

onMounted(() => loadItems())

async function loadItems(reset = false) {
  if (reset) page.value = 1
  try {
    const { data } = await client.get('/articles', { params: { page: page.value, pageSize } })
    if (data.code === 0) {
      const newItems = data.data || []
      articles.value = page.value === 1 ? newItems : [...articles.value, ...newItems]
      hasMore.value = newItems.length === pageSize
    }
  } catch { /* */ }
  loading.value = false
}

async function loadMore() { page.value++; loadingMore.value = true; await loadItems(); loadingMore.value = false }

function stripHtml(html: string) { return html ? html.replace(/<[^>]*>/g, '').replace(/[#*>`_~]/g, '') : '' }
function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.article-item { padding: 16px 0; border-bottom: 1px solid var(--paper-border); cursor: pointer; transition: background .15s; }
.article-item:hover { background: rgba(0,0,0,.01); }
</style>
