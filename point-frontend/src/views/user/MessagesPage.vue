<template>
    <div class="detail-main">
      <v-tabs v-model="activeTab" density="compact" color="var(--paper-accent)" class="mb-4" style="border-bottom:1px solid var(--paper-border)">
        <v-tab value="all" style="font-size:14px;text-transform:none;letter-spacing:0">全部</v-tab>
        <v-tab value="1" style="font-size:14px;text-transform:none;letter-spacing:0">赞</v-tab>
        <v-tab value="2" style="font-size:14px;text-transform:none;letter-spacing:0">关注</v-tab>
        <v-tab value="3" style="font-size:14px;text-transform:none;letter-spacing:0">提及</v-tab>
        <v-tab value="0" style="font-size:14px;text-transform:none;letter-spacing:0">评论</v-tab>
      </v-tabs>
      <div v-if="filtered.length > 0" class="d-flex justify-end mb-3">
        <v-btn variant="text" size="small" @click="markAllRead">全部已读</v-btn>
      </div>
      <div v-for="m in filtered" :key="m.id" class="msg-card" :class="{ unread: m.status === 0 }">
        <div class="d-flex align-center mb-2">
          <v-icon :color="m.status===0?'var(--paper-accent)':''" size="16" class="mr-2 flex-shrink-0">{{ iconForType(m.type) }}</v-icon>
          <span style="font-size:13px;font-weight:600;color:var(--paper-text)">{{ sender(m) }}</span>
          <span style="font-size:13px;color:var(--paper-text2)"> · {{ actionText(m) }}</span>
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
          <div v-if="canOpen(m)">
            <v-btn variant="text" size="x-small" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)" @click.stop="openEntity(m)">
              查看{{ entityLabel(m) }}<v-icon size="12" class="ml-1">mdi-arrow-right</v-icon>
            </v-btn>
          </div>
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
      <div v-if="filtered.length===0 && !loading" class="text-center py-16" style="color:var(--paper-text2)">暂无通知</div>
      <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" color="var(--paper-accent)" />
      <LoadMore :has-more="hasMore" :loading="loadingMore" @load-more="loadMore" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import MentionTextarea from '@/components/MentionTextarea.vue'
import LoadMore from '@/components/LoadMore.vue'

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
const activeTab = ref('all')

const filtered = computed(() => {
  if (activeTab.value === 'all') return messages.value
  const t = Number(activeTab.value)
  return messages.value.filter((m: any) => m.type === t)
})

onMounted(() => loadItems())

async function loadItems(reset = false) {
  if (reset) page.value = 1
  try {
    const { data } = await client.get(`/users/${auth.user?.id}/messages`, { params: { page: page.value, pageSize } })
    if (data.code===0) {
      const payload = data.data || {}
      const items = payload.items || []
      messages.value = page.value === 1 ? items : [...messages.value, ...items]
      hasMore.value = (payload.items || []).length < (payload.total ?? 0)
    }
  } catch { /* ignore */ }
  loading.value = false
}

async function loadMore() { page.value++; loadingMore.value = true; await loadItems(); loadingMore.value = false }
async function markAllRead() {
  await client.post(`/users/${auth.user?.id}/messages/read`, { ids: [] })
  messages.value.forEach(m => (m.status = 1))
}

function parseExtra(m: any): { type: string; id: string; parentType?: string; parentId?: string } | null {
  if (!m.extraData) return null
  const parts = m.extraData.split(':')
  // "topic:142" or "comment:108:topic:142"
  if (parts.length === 2) return { type: parts[0], id: parts[1] }
  if (parts.length === 4) return { type: parts[0], id: parts[1], parentType: parts[2], parentId: parts[3] }
  return null
}

function canOpen(m: any): boolean {
  const e = parseExtra(m); if (!e) return false
  // For comment, use parent entity for navigation
  const t = e.parentType || e.type
  return t === 'topic' || t === 'article' || t === 'user'
}

function openEntity(m: any) {
  const e = parseExtra(m); if (!e) return
  if (m.status === 0) client.post(`/users/${auth.user?.id}/messages/read`, { ids: [m.id] }).catch(()=>{})
  m.status = 1
  const t = e.parentType || e.type; const id = e.parentId || e.id
  if (t === 'topic') router.push(`/topics/${id}`)
  else if (t === 'article') router.push(`/articles/${id}`)
  else if (t === 'user') router.push(`/users/${id}`)
}

function entityLabel(m: any): string {
  const e = parseExtra(m); if (!e) return ''
  const t = e.parentType || e.type
  return t === 'topic' ? '帖子' : t === 'article' ? '文章' : '用户'
}

function sender(m: any): string {
  const c = m.content || ''; const space = c.indexOf(' ')
  return space > 0 ? c.substring(0, space) : (m.fromId ? '用户' + m.fromId : '有人')
}

function actionText(m: any): string {
  if (m.type === 1) return '赞了你'
  if (m.type === 2) return '关注了你'
  if (m.type === 3) return '提到了你'
  return m.title || '评论了你'
}

function canReply(m: any): boolean {
  const e = parseExtra(m); if (!e) return false
  const t = e.parentType || e.type
  return t === 'topic' || t === 'article'
}

function toggleReply(m: any) {
  replyId.value = replyId.value === m.id ? 0 : m.id
  replyText.value = ''
}

async function doReply(m: any) {
  if (!replyText.value.trim()) return
  replying.value = true
  try {
    const e = parseExtra(m); if (!e) return
    const t = e.parentType || e.type; const id = e.parentId || e.id
    await client.post(`/${t}s/${id}/comments`, {
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
