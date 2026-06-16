<template>
    <div class="detail-main">
      <div v-if="messages.length > 0" class="d-flex justify-end mb-3">
        <v-btn variant="text" size="small" @click="markAllRead">全部已读</v-btn>
      </div>
      <div v-for="m in messages" :key="m.id" class="msg-card" :class="{ unread: m.status === 0 }" @click="openEntity(m)">
        <div class="d-flex align-center mb-2">
          <v-icon :color="m.status===0?'var(--paper-accent)':''" size="16" class="mr-2 flex-shrink-0">{{ iconForType(m.type) }}</v-icon>
          <span style="font-size:14px;font-weight:500;color:var(--paper-text)" class="text-truncate">{{ titleFor(m) }}</span>
          <span style="font-size:12px;color:var(--paper-text2);margin-left:auto;flex-shrink:0" class="ml-2">{{ time(m.createTime) }}</span>
        </div>
        <div v-if="m.quoteContent" style="font-size:13px;color:var(--paper-text2);line-height:1.6;margin-bottom:8px;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden">
          {{ m.quoteContent }}
        </div>
        <div class="d-flex align-center" style="gap:8px" @click.stop>
          <v-btn v-if="canReply(m)" variant="text" size="x-small" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)" @click="toggleReply(m)">
            <v-icon size="14" class="mr-1">mdi-reply-outline</v-icon>回复
          </v-btn>
          <v-spacer />
          <span style="font-size:11px;color:var(--paper-text2)">{{ entityLabel(m) }}</span>
        </div>
        <!-- Inline reply box -->
        <div v-if="replyId === m.id" class="mt-2">
          <MentionTextarea v-model="replyText" placeholder="输入回复..." rows="1" auto-grow density="compact" hide-details variant="outlined" class="mb-2" />
          <div class="d-flex justify-end" style="gap:8px">
            <v-btn variant="text" size="x-small" @click.stop="replyId=0" style="text-transform:none;letter-spacing:0">取消</v-btn>
            <v-btn variant="flat" size="x-small" :loading="replying" @click.stop="doReply(m)" style="background:var(--paper-accent);color:#fff;text-transform:none;letter-spacing:0;border-radius:14px">回复</v-btn>
          </div>
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
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import MentionTextarea from '@/components/MentionTextarea.vue'

const router = useRouter()
const auth = useAuthStore()
if (!auth.isLoggedIn) { router.replace('/login') }

const messages = ref<any[]>([])
const loading = ref(true)
const page = ref(1); const pageSize = 30
const hasMore = ref(false); const loadingMore = ref(false)
const replyId = ref(0)
const replyText = ref('')
const replying = ref(false)

onMounted(() => loadItems())

async function loadItems(reset = false) {
  if (reset) page.value = 1
  try {
    const { data } = await client.get(`/users/${auth.user?.id}/messages`, { params: { page: page.value, pageSize } })
    if (data.code===0) {
      const items = data.data || []
      messages.value = page.value === 1 ? items : [...messages.value, ...items]
      hasMore.value = items.length === pageSize
    }
  } catch { /* ignore */ }
  loading.value = false
}

async function loadMore() { page.value++; loadingMore.value = true; await loadItems(); loadingMore.value = false }
async function markAllRead() {
  await client.post(`/users/${auth.user?.id}/messages/read`, { ids: [] })
  messages.value.forEach(m => (m.status = 1))
}

function parseExtra(m: any): { type: string; id: string } | null {
  if (!m.extraData) return null
  const [t, id] = m.extraData.split(':')
  return t && id ? { type: t, id } : null
}

function openEntity(m: any) {
  const e = parseExtra(m)
  if (!e) return
  if (m.status === 0) client.post(`/users/${auth.user?.id}/messages/read`, { ids: [m.id] }).catch(()=>{})
  m.status = 1
  if (e.type === 'topic') router.push(`/topics/${e.id}`)
  else if (e.type === 'article') router.push(`/articles/${e.id}`)
  else if (e.type === 'user') router.push(`/users/${e.id}`)
}

function entityLabel(m: any): string {
  const e = parseExtra(m)
  if (!e) return ''
  return e.type === 'topic' ? '查看帖子' : e.type === 'article' ? '查看文章' : e.type === 'user' ? '查看用户' : ''
}

function titleFor(m: any): string {
  if (m.type === 3) return '提到了你'
  if (m.type === 1) return '赞了你'
  if (m.type === 2) return '关注了你'
  return m.title || '新评论'
}

function canReply(m: any): boolean {
  const e = parseExtra(m)
  return !!(e && (e.type === 'topic' || e.type === 'article'))
}

function toggleReply(m: any) {
  replyId.value = replyId.value === m.id ? 0 : m.id
  replyText.value = ''
}

async function doReply(m: any) {
  if (!replyText.value.trim()) return
  replying.value = true
  try {
    const e = parseExtra(m)
    if (!e) return
    await client.post(`/api/${e.type}s/${e.id}/comments`, {
      content: replyText.value,
      contentType: 'markdown'
    })
    replyText.value = ''
    replyId.value = 0
  } catch { /* ignore */ }
  replying.value = false
}

function iconForType(type: number): string {
  return ['mdi-comment-outline', 'mdi-heart-outline', 'mdi-account-plus-outline', 'mdi-at'][type] || 'mdi-bell-outline'
}

function time(ts: number) {
  if (!ts) return ''
  const diff = Date.now() - ts
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff/60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff/3600000) + '小时前'
  return new Date(ts).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.msg-card { background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 14px 16px; margin-bottom: 8px; cursor: pointer; transition: border-color .15s; }
.msg-card:hover { border-color: var(--paper-accent); }
.unread { border-left: 2px solid var(--paper-accent); }
</style>
