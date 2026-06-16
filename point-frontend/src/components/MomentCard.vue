<template>
  <div class="mc-link" @click="goToTopic">
    <div class="d-flex">
      <router-link :to="`/users/${moment.userId}`" class="flex-shrink-0 mr-3" @click.stop>
        <UserAvatar :src="moment.user?.avatar" :name="moment.user?.nickname" :size="36" />
      </router-link>
      <div class="flex-grow-1" style="min-width:0">
        <div class="d-flex align-center mb-1">
          <router-link :to="`/users/${moment.userId}`" class="mc-name" @click.stop>{{ moment.user?.nickname }}<span class="mc-username">@{{ moment.user?.username || moment.userId }}</span></router-link>
          <span class="mc-time">{{ fmt(moment.createTime) }}</span>
          <span v-if="isOwner && !confirming" class="mc-delete" @click.stop="confirming = true">
            <v-icon size="14">mdi-trash-can-outline</v-icon>
          </span>
          <span v-if="confirming" class="mc-delete-confirm" @click.stop>
            <span class="confirm-text">删除？</span>
            <span class="confirm-yes" @click.stop="confirmDelete">确认</span>
            <span class="confirm-no" @click.stop="confirming = false">取消</span>
          </span>
        </div>
        <div v-html="rendered" class="mc-body" @click="onBodyClick" />
        <div class="d-flex mt-2 mc-actions">
          <span @click.stop="showReply = !showReply"><v-icon size="14">mdi-comment-outline</v-icon>{{ commentCount }}</span>
          <span @click.stop="$emit('toggle-like', moment)">
            <v-icon size="14" :color="moment.liked ? 'var(--paper-accent)' : ''">{{ moment.liked ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>{{ moment.likeCount || 0 }}
          </span>
        </div>

        <!-- Inline reply box -->
        <div v-if="showReply" class="reply-box" @click.stop>
          <div class="d-flex mt-3">
            <MentionTextarea v-model="replyText" placeholder="写下你的回复..." rows="2" density="compact" hide-details variant="outlined" class="mr-2" style="font-size:13px" @keydown.enter.ctrl="doReply" />
            <v-btn variant="flat" size="small" :loading="replying" @click="doReply"
              style="background:var(--paper-accent);color:#fff;text-transform:none;letter-spacing:0;border-radius:20px;padding:0 16px;align-self:flex-end">回复</v-btn>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Image Viewer — Twitter-style -->
  <Teleport to="body">
    <div v-if="viewer" class="viewer-overlay" @click.self="closeViewer" @keydown.esc="closeViewer">
      <v-btn icon="mdi-close" variant="text" class="viewer-close-btn" @click="closeViewer" size="36" aria-label="关闭" />

      <div class="viewer-layout">
        <!-- Left: Image -->
        <div class="viewer-image-side">
          <v-btn v-if="images.length>1" icon="mdi-chevron-left" variant="text" class="viewer-arrow left" @click="prevImage" />
          <img :src="images[viewer-1]" class="viewer-main-img" />
          <v-btn v-if="images.length>1" icon="mdi-chevron-right" variant="text" class="viewer-arrow right" @click="nextImage" />
          <div v-if="images.length>1" class="viewer-counter">{{ viewer }} / {{ images.length }}</div>
        </div>

        <!-- Right: Post context -->
        <div class="viewer-context">
          <div class="d-flex align-center mb-3">
            <router-link :to="`/users/${moment.userId}`" @click.stop>
              <UserAvatar :src="moment.user?.avatar" :name="moment.user?.nickname" :size="36" />
            </router-link>
            <div class="ml-3">
              <div style="font-size:14px;font-weight:600;color:var(--paper-text)">{{ moment.user?.nickname }}<span style="font-size:12px;font-weight:400;color:var(--paper-text2)"> @{{ moment.user?.username || moment.userId }}</span></div>
            </div>
          </div>
          <div v-html="textOnly" class="viewer-text" />
          <div class="viewer-divider" />
          <div class="d-flex viewer-actions-row mb-3">
            <span class="d-flex align-center" style="gap:4px"><v-icon size="16">mdi-comment-outline</v-icon>{{ commentCount }}</span>
            <span class="d-flex align-center" style="gap:4px;cursor:pointer" @click.stop="toggleLike">
              <v-icon size="16" :color="liked?'var(--paper-accent)':''">{{ liked?'mdi-heart':'mdi-heart-outline' }}</v-icon>{{ likeCount }}
            </span>
          </div>
          <!-- Comments -->
          <div style="border-top:1px solid var(--paper-border);padding-top:12px;overflow-y:auto;flex:1">
            <v-progress-circular v-if="loadingComments" indeterminate size="20" class="d-block mx-auto my-4" color="var(--paper-accent)" />
            <div v-else>
            <div class="viewer-composer">
              <MentionTextarea v-model="newComment" placeholder="发表评论..." rows="1" auto-grow density="compact" hide-details variant="plain" />
              <div style="display:flex;justify-content:flex-end;margin-top:6px">
                <v-btn v-if="newComment.trim()" variant="flat" size="x-small" :loading="posting" @click.stop="postComment" class="viewer-composer-btn">发布</v-btn>
              </div>
            </div>
            <div v-for="(c, i) in comments" :key="c.id" class="mb-2">
              <div v-if="i > 0 && comments[i-1].user?.id !== c.user?.id && (c.user?.id === moment.userId || comments[i-1].user?.id === moment.userId)" class="op-connector" />
              <div class="d-flex">
                <UserAvatar :src="c.user?.avatar" :name="c.user?.nickname" :size="24" class="mr-2 flex-shrink-0" />
                <div>
                  <span style="font-size:13px;font-weight:500;color:var(--paper-text);margin-right:6px">{{ c.user?.nickname }}<span style="font-size:12px;font-weight:400;color:var(--paper-text2)"> @{{ c.user?.username || c.user?.id }}</span></span>
                  <span style="font-size:13px;color:var(--paper-text2)">{{ c.content }}</span>
                </div>
              </div>
            </div>
            <div v-if="hasMoreComments" class="text-center mt-2">
              <v-btn variant="text" size="x-small" :loading="loadingMoreComments" @click.stop="loadMoreComments" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)">更多评论</v-btn>
            </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from './UserAvatar.vue'
import client from '@/api/client'
import { renderMarkdown, md } from '@/utils/markdown'
import MentionTextarea from './MentionTextarea.vue'
import type { Topic, Comment } from '@/types'

const router = useRouter()
const auth = useAuthStore()
const props = defineProps<{ moment: Topic }>()
const emit = defineEmits<{ 'toggle-like': [moment: Topic]; 'delete-moment': [id: number] }>()
const isOwner = computed(() => auth.user?.id === props.moment.userId)
const confirming = ref(false)
const deleting = ref(false)

function goToTopic() { router.push(`/topics/${props.moment.id}`) }

const viewer = ref(0)
const comments = ref<Comment[]>([])
const newComment = ref('')
const posting = ref(false)
const liked = ref(false)
const likeCount = ref(0)
const commentCount = ref(props.moment?.commentCount || 0)
const showReply = ref(false)
const replyText = ref('')
const replying = ref(false)
const commentPage = ref(1)
const commentPageSize = 30
const hasMoreComments = ref(false)
const loadingMoreComments = ref(false)
const loadingComments = ref(false)

function onKey(e: KeyboardEvent) { if (e.key === 'Escape') viewer.value = 0 }
onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))

const rendered = computed(() => {
  const c = props.moment?.content || ''
  let html = renderMarkdown(c)
  if (html.length > 600) html = html.substring(0, 600) + '…'
  return html
})

const textOnly = computed(() => {
  const c = props.moment?.content || ''
  return md.render(c.replace(/!\[.*?\]\(.+?\)/g, ''))
})

const images = computed(() => {
  const matches = props.moment?.content?.matchAll(/!\[.*?\]\((.+?)\)/g) || []
  return Array.from(matches, (m: any) => m[1])
})

function onBodyClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    e.preventDefault()
    e.stopPropagation()
    const src = target.getAttribute('src')
    if (src) {
      const idx = images.value.indexOf(src)
      viewer.value = idx >= 0 ? idx + 1 : 1
    }
  }
  // Non-image clicks: let the router-link handle navigation naturally
}

function closeViewer() { viewer.value = 0 }
function prevImage() { if (viewer.value > 1) viewer.value-- }
function nextImage() { if (viewer.value < images.value.length) viewer.value++ }

watch(viewer, async (v) => {
  if (v > 0 && props.moment?.id) {
    liked.value = props.moment.liked || false
    likeCount.value = props.moment.likeCount || 0
    commentCount.value = props.moment.commentCount || 0
    showReply.value = false
    replyText.value = ''
    await loadComments(true)
  }
})

async function loadComments(reset = false) {
  if (reset) { commentPage.value = 1; loadingComments.value = true }
  if (!props.moment?.id) return
  try {
    const { data } = await client.get(`/topics/${props.moment.id}/comments`, { params: { page: commentPage.value, pageSize: commentPageSize } })
    if (data.code === 0) {
      const newItems = data.data || []
      comments.value = commentPage.value === 1 ? newItems : [...comments.value, ...newItems]
      hasMoreComments.value = newItems.length === commentPageSize
    }
  } catch { console.error('api error'); comments.value = [] }
  loadingComments.value = false
}

async function loadMoreComments() { commentPage.value++; loadingMoreComments.value = true; await loadComments(); loadingMoreComments.value = false }

async function toggleLike() {
  try {
    if (liked.value) { liked.value = false; likeCount.value-- }
    else { liked.value = true; likeCount.value++ }
    emit('toggle-like', props.moment)
  } catch { console.error('api error') }
}

async function doReply() {
  if (!replyText.value.trim()) return
  replying.value = true
  try {
    await client.post(`/topics/${props.moment.id}/comments`, { content: replyText.value, contentType: 'markdown' })
    replyText.value = ''
    commentCount.value++
    showReply.value = false
  } catch { console.error('api error') }
  replying.value = false
}

async function postComment() {
  if (!newComment.value.trim()) return
  posting.value = true
  try {
    await client.post(`/topics/${props.moment.id}/comments`, { content: newComment.value, contentType: 'markdown' })
    newComment.value = ''
    commentCount.value++
    await loadComments(true)
  } catch { console.error('api error') }
  posting.value = false
}

function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }

async function confirmDelete() {
  if (deleting.value) return
  deleting.value = true
  try {
    await client.post(`/topics/delete/${props.moment.id}`)
    emit('delete-moment', props.moment.id)
  } catch { console.error('api error') }
  finally { deleting.value = false; confirming.value = false }
}
</script>

<style scoped>
.mc-link { display: block; background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 16px 18px; text-decoration: none; }
.mc-link:hover { border-color: var(--paper-accent); }
.mc-name { text-decoration: none; color: var(--paper-text); font-weight: 500; font-size: 14px; }
.mc-username { font-size: 12px; color: var(--paper-text2); font-weight: 400; }
.mc-time { font-size: 12px; color: var(--paper-text2); margin-left: auto; }
.mc-body { font-size: 15px; color: var(--paper-text); line-height: 1.7; word-break: break-word; }
.mc-body :deep(p) { margin: .3em 0; }
.mc-body :deep(img) { max-width: 100%; max-height: 400px; border-radius: 8px; cursor: pointer; vertical-align: top; }
.mc-body :deep(.img-grid img) { width: 100%; height: 100%; object-fit: cover; aspect-ratio: 1; max-height: none; border-radius: 0; }
/* Multi-image grid via container */
.mc-actions { gap: 20px; font-size: 12px; color: var(--paper-text2); }
.mc-actions span { cursor: pointer; display: flex; align-items: center; gap: 3px; }
.mc-delete { opacity: 0; margin-left: auto; cursor: pointer; color: var(--paper-text2); transition: opacity .15s; display: flex; align-items: center; }
.mc-link:hover .mc-delete { opacity: 1; }
.mc-delete:hover { color: #c62828; }
.mc-delete-confirm { margin-left: auto; display: flex; align-items: center; gap: 8px; font-size: 12px; }
.confirm-text { color: var(--paper-text2); }
.confirm-yes { color: #c62828; cursor: pointer; font-weight: 500; }
.confirm-yes:hover { text-decoration: underline; }
.confirm-no { color: var(--paper-text2); cursor: pointer; }
.confirm-no:hover { color: var(--paper-text); }
</style>

<style>
.viewer-overlay { position: fixed; inset: 0; z-index: 9999; background: rgba(0,0,0,.94); display: flex; align-items: stretch; }
.viewer-close-btn { position: fixed; top: 12px; left: 12px; z-index: 10; color: #fff !important; background: rgba(255,255,255,.12) !important; border-radius: 50%; }
.viewer-layout { display: flex; width: 100%; height: 100%; }
.viewer-image-side { flex: 1; display: flex; align-items: center; justify-content: center; position: relative; min-width: 0; }
.viewer-main-img { max-width: 95%; max-height: 95vh; object-fit: contain; border-radius: 4px; }
.viewer-arrow { position: absolute; top: 50%; transform: translateY(-50%); color: rgba(255,255,255,.7) !important; }
.viewer-arrow.left { left: 8px; }
.viewer-arrow.right { right: 8px; }
.viewer-counter { position: absolute; bottom: 16px; left: 50%; transform: translateX(-50%); color: rgba(255,255,255,.5); font-size: 13px; }
.viewer-context { width: 360px; flex-shrink: 0; background: var(--paper-bg); display: flex; flex-direction: column; padding: 48px 24px 24px; }
.viewer-text { font-size: 15px; color: var(--paper-text); line-height: 1.7; word-break: break-word; max-height: 200px; overflow-y: auto; }
.viewer-text :deep(p) { margin: .3em 0; }
.viewer-divider { height: 1px; background: var(--paper-border); margin: 20px 0; }
.viewer-actions-row { gap: 24px; font-size: 13px; color: var(--paper-text2); }
.op-connector { width: 2px; height: 20px; background: var(--paper-border); margin-left: 12px; }
.viewer-composer { margin-bottom: 12px; }
.viewer-composer :deep(.v-field__field) { padding: 0 !important; }
.viewer-composer-btn { background: var(--paper-accent) !important; color: #fff !important; text-transform: none; letter-spacing: 0; border-radius: 16px; padding: 2px 14px; font-size: 11px; }
@media (max-width: 800px) { .viewer-context { display: none; } }
</style>
