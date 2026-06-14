<template>
  <div class="detail-layout">
    <div class="detail-main">
      <div v-if="messages.length > 0" class="d-flex justify-end mb-3">
        <v-btn variant="text" size="small" @click="markAllRead">全部已读</v-btn>
      </div>
      <div v-for="m in messages" :key="m.id" class="py-3" style="border-bottom:1px solid var(--paper-border)" :class="{ 'unread': m.status === 0 }">
        <div class="d-flex align-center mb-1">
          <v-icon :color="m.status===0?'#c43d3d':''" size="16" class="mr-2">{{ iconForType(m.type) }}</v-icon>
          <span style="font-size:14px;font-weight:500;color:var(--paper-text)">{{ m.title }}</span>
          <span style="font-size:12px;color:var(--paper-text2);margin-left:auto">{{ formatTime(m.createTime) }}</span>
        </div>
        <div style="font-size:13px;color:var(--paper-text2);line-height:1.6">
          <router-link v-if="m.fromId" :to="`/users/${m.fromId}`" class="msg-user-link">{{ userName(m) }}</router-link><template v-if="m.fromId">{{ ' ' + contentAfterName(m) }}</template><template v-else>{{ m.content }}</template>
        </div>
      </div>
      <div v-if="messages.length===0 && !loading" class="text-center py-16" style="color:var(--paper-text2)">暂无通知</div>
      <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" color="#c43d3d" />
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
const messages = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await client.get(`/users/${auth.user?.id}/messages`, { params: { page: 1, pageSize: 50 } })
    if (data.code===0) messages.value = data.data || []
  } catch { /* ignore */ }
  loading.value = false
})

async function markAllRead() {
  await client.post(`/users/${auth.user?.id}/messages/read`, { ids: [] })
  messages.value.forEach(m => (m.status = 1))
}

function iconForType(type: number): string {
  return ['mdi-comment-outline', 'mdi-heart-outline', 'mdi-account-plus-outline'][type] || 'mdi-bell-outline'
}

function formatTime(ts: number) { return ts ? new Date(ts).toLocaleString('zh-CN') : '' }
function userName(m: any) { const space = (m.content || '').indexOf(' '); return space > 0 ? m.content.substring(0, space) : (m.content || '') }
function contentAfterName(m: any) { const space = (m.content || '').indexOf(' '); return space > 0 ? m.content.substring(space + 1) : '' }
</script>

<style scoped>
.detail-layout { display: flex; }
.detail-main { flex: 1; max-width: 680px; min-width: 0; padding-right: 32px; border-right: 1px solid var(--paper-border); transition: padding .2s ease; }
.unread { background: rgba(196,61,61,.03); }
.msg-user-link { color: var(--paper-text); font-weight: 500; text-decoration: none; }
.msg-user-link:hover { color: #c43d3d; text-decoration: underline; }
@media (max-width: 1300px) { .detail-main { padding-right: 24px; } }
@media (max-width: 1200px) { .detail-main { padding-right: 20px; } }
@media (max-width: 1100px) { .detail-main { border-right: none; padding-right: 0; } }
@media (max-width: 900px)  { .detail-main { padding-right: 16px; } }
</style>
