<template>
    <div class="detail-main">
      <div v-if="messages.length > 0" class="d-flex justify-end mb-3">
        <v-btn variant="text" size="small" @click="markAllRead">全部已读</v-btn>
      </div>
      <div v-for="m in messages" :key="m.id" class="msg-card" :class="{ 'unread': m.status === 0 }">
        <div class="d-flex align-center mb-1">
          <v-icon :color="m.status===0?'var(--paper-accent)':''" size="16" class="mr-2">{{ iconForType(m.type) }}</v-icon>
          <span style="font-size:14px;font-weight:500;color:var(--paper-text)">{{ m.title }}</span>
          <span style="font-size:12px;color:var(--paper-text2);margin-left:auto">{{ formatTime(m.createTime) }}</span>
        </div>
        <div style="font-size:13px;color:var(--paper-text2);line-height:1.6">
          <router-link v-if="m.fromId" :to="`/users/${m.fromId}`" class="msg-user-link">{{ userName(m) }}</router-link><template v-if="m.fromId">{{ ' ' + contentAfterName(m) }}</template><template v-else>{{ m.content }}</template>
        </div>
      </div>
      <div v-if="messages.length===0 && !loading" class="text-center py-16" style="color:var(--paper-text2)">暂无通知</div>
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

const messages = ref<any[]>([])
const loading = ref(true)
const page = ref(1)
const pageSize = 30
const hasMore = ref(false)
const loadingMore = ref(false)

onMounted(() => loadItems())

async function loadItems(reset = false) {
  if (reset) page.value = 1
  try {
    const { data } = await client.get(`/users/${auth.user?.id}/messages`, { params: { page: page.value, pageSize } })
    if (data.code===0) {
      const newItems = data.data || []
      messages.value = page.value === 1 ? newItems : [...messages.value, ...newItems]
      hasMore.value = newItems.length === pageSize
    }
  } catch { console.error('api error') }
  loading.value = false
}

async function loadMore() { page.value++; loadingMore.value = true; await loadItems(); loadingMore.value = false }

async function markAllRead() {
  await client.post(`/users/${auth.user?.id}/messages/read`, { ids: [] })
  messages.value.forEach(m => (m.status = 1))
}

function iconForType(type: number): string {
  return ['mdi-comment-outline', 'mdi-heart-outline', 'mdi-account-plus-outline', 'mdi-at'][type] || 'mdi-bell-outline'
}

function formatTime(ts: number) { return ts ? new Date(ts).toLocaleString('zh-CN') : '' }
function userName(m: any) { const space = (m.content || '').indexOf(' '); return space > 0 ? m.content.substring(0, space) : (m.content || '') }
function contentAfterName(m: any) { const space = (m.content || '').indexOf(' '); return space > 0 ? m.content.substring(space + 1) : '' }
</script>

<style scoped>
.msg-card { background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 14px 16px; margin-bottom: 8px; transition: border-color .15s; }
.msg-card:hover { border-color: var(--paper-accent); }
.unread { border-left: 2px solid var(--paper-accent); }
.msg-user-link { color: var(--paper-text); font-weight: 500; text-decoration: none; }
.msg-user-link:hover { color: var(--paper-accent); text-decoration: underline; }
</style>
