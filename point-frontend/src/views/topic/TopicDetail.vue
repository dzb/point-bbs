<template>
  <div v-if="topic">
      <div class="detail-main">
        <BackButton />
        <!-- Post content card -->
        <div class="post-card mb-4">
          <div class="d-flex align-center mb-3">
            <router-link :to="`/users/${topic.userId}`">
              <UserAvatar :src="topic.user?.avatar" :name="topic.user?.nickname" :size="40" />
            </router-link>
            <div class="ml-3">
              <router-link :to="`/users/${topic.userId}`" class="text-decoration-none" style="color:var(--paper-text);font-weight:600;font-size:15px">
                {{ topic.user?.nickname }}
              </router-link>
              <div style="font-size:12px;color:var(--paper-text2)">{{ formatTime(topic.createTime) }}</div>
            </div>
          </div>

          <div v-if="topic.title" style="font-size:20px;font-weight:600;color:var(--paper-text);margin-bottom:8px;font-family:'Noto Serif SC',Georgia,serif">
            {{ topic.title }}
          </div>
          <div v-html="renderedContent" class="topic-content" @click="onContentClick" />

          <div class="d-flex mt-3" style="gap:20px;font-size:13px;color:var(--paper-text2)">
            <span class="d-flex align-center" style="gap:4px"><v-icon size="16">mdi-eye-outline</v-icon>{{ topic.viewCount }}</span>
            <span class="d-flex align-center" style="gap:4px"><v-icon size="16">mdi-comment-outline</v-icon>{{ topic.commentCount }}</span>
          </div>

          <div class="d-flex mt-2 pt-3" style="border-top:1px solid var(--paper-border);gap:4px">
            <v-btn :color="liked ? 'var(--paper-accent)' : ''" variant="text" size="small"
              :icon="liked ? 'mdi-heart' : 'mdi-heart-outline'" :loading="likeLoading" @click="toggleLike" />
            <v-btn :color="favorited ? 'var(--paper-accent)' : ''" variant="text" size="small"
              :icon="favorited ? 'mdi-bookmark' : 'mdi-bookmark-outline'" :loading="favLoading" @click="toggleFav" />
          </div>
        </div>

        <!-- Comments -->
        <div class="post-card">
          <div v-if="auth.isLoggedIn" class="d-flex mb-4">
            <UserAvatar :src="auth.user?.avatar" :name="auth.user?.nickname" :size="32" class="mr-3 flex-shrink-0" />
            <div class="flex-grow-1">
              <v-textarea v-model="commentText" placeholder="写下你的评论..." rows="2" variant="outlined" density="compact" hide-details class="mb-2" />
              <div class="d-flex justify-end">
                <v-btn variant="flat" size="small" :loading="submitting" @click="postComment"
                  style="background:var(--paper-accent);color:#fff;text-transform:none;letter-spacing:0;border-radius:20px;padding:0 16px">回复</v-btn>
              </div>
            </div>
          </div>

          <div v-for="c in comments" :key="c.id" class="py-3" style="border-top:1px solid var(--paper-border)">
            <div class="d-flex">
              <router-link :to="`/users/${c.user?.id}`" class="flex-shrink-0 mr-3">
                <UserAvatar :src="c.user?.avatar" :name="c.user?.nickname" :size="32" />
              </router-link>
              <div>
                <div class="mb-1">
                  <router-link :to="`/users/${c.user?.id}`" class="text-decoration-none" style="font-size:13px;font-weight:500;color:var(--paper-text)">
                    {{ c.user?.nickname }}
                  </router-link>
                  <span style="font-size:12px;color:var(--paper-text2);margin-left:8px">{{ formatTime(c.createTime) }}</span>
                </div>
                <div v-html="c.content" class="comment-body" />
              </div>
            </div>
          </div>
          <div v-if="hasMoreComments" class="text-center mt-3 mb-2">
            <v-btn variant="text" size="small" :loading="loadingMoreComments" @click="loadMoreComments" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)">更多评论</v-btn>
          </div>
        </div>

      </div>
      <ImageViewer :images="contentImages" v-model="viewerIndex" />
  </div>
  <div v-else class="text-center py-16" style="color:var(--paper-text2)">
    <v-progress-circular v-if="loading" indeterminate color="var(--paper-accent)" />
    <span v-else>帖子不存在</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import UserAvatar from '@/components/UserAvatar.vue'
import BackButton from '@/components/BackButton.vue'
import ImageViewer from '@/components/ImageViewer.vue'
import MarkdownIt from 'markdown-it'
import { groupConsecutiveImages } from '@/utils/markdown'
import { topicAsideState } from '@/composables/useTopicAside'

const md = new MarkdownIt({ html: true, breaks: true, linkify: true })
const route = useRoute(); const auth = useAuthStore()
const topic = ref<any>(null)
const comments = ref<any[]>([])
const commentText = ref('')
const submitting = ref(false); const loading = ref(true)
const commentPage = ref(1); const commentPageSize = 30
const hasMoreComments = ref(false); const loadingMoreComments = ref(false)
const liked = ref(false); const favorited = ref(false); const following = ref(false)
const likeLoading = ref(false); const favLoading = ref(false); const followLoading = ref(false)
const viewerIndex = ref(0)
const renderedContent = computed(() => topic.value?.content ? md.render(groupConsecutiveImages(topic.value.content)) : '')
const contentImages = computed(() => {
  const matches = topic.value?.content?.matchAll(/!\[.*?\]\((.+?)\)/g) || []
  return Array.from(matches, (m: any) => m[1])
})

onMounted(loadAll)
watch(() => route.params.id, loadAll)

async function loadAll() {
  loading.value = true
  const id = route.params.id as string
  const tr = await client.get(`/topics/${id}`)
  if (tr.data.code===0) {
    topic.value = tr.data.data
    syncAsideState()
    if (auth.isLoggedIn) await Promise.all([checkLike(), checkFav(), checkFollow()])
  }
  await loadComments()
  loading.value = false
}

async function loadComments(reset = false) {
  if (reset) commentPage.value = 1
  const id = route.params.id as string
  try {
    const { data } = await client.get(`/topics/${id}/comments`, { params: { page: commentPage.value, pageSize: commentPageSize } })
    if (data.code===0) {
      const newItems = data.data || []
      comments.value = commentPage.value === 1 ? newItems : [...comments.value, ...newItems]
      hasMoreComments.value = newItems.length === commentPageSize
    }
  } catch { /* */ }
}

async function loadMoreComments() { commentPage.value++; loadingMoreComments.value = true; await loadComments(); loadingMoreComments.value = false }

async function checkLike() {
  const { data } = await client.get(`/topics/${route.params.id}/like/status`)
  liked.value = data?.data?.liked || false
}
async function checkFav() {
  const { data } = await client.get(`/topics/${route.params.id}/favorite/status`)
  favorited.value = data?.data?.favorited || false
}
async function checkFollow() {
  if (!topic.value?.userId) return
  const { data } = await client.get(`/users/${topic.value.userId}/follow/status`)
  following.value = data?.data?.following || false
}

async function toggleLike() {
  likeLoading.value = true
  if (liked.value) { await client.post(`/topics/${route.params.id}/unlike`); liked.value=false; topic.value.likeCount-- }
  else { await client.post(`/topics/${route.params.id}/like`); liked.value=true; topic.value.likeCount++ }
  likeLoading.value = false
}
async function toggleFav() {
  favLoading.value = true
  if (favorited.value) { await client.post(`/topics/${route.params.id}/unfavorite`); favorited.value=false }
  else { await client.post(`/topics/${route.params.id}/favorite`); favorited.value=true }
  favLoading.value = false
}
function syncAsideState() {
  if (!topic.value) return
  topicAsideState.author = topic.value.user || null
  topicAsideState.showFollow = auth.isLoggedIn && topic.value.userId !== auth.user?.id
  topicAsideState.following = following.value
  topicAsideState.followLoading = followLoading.value
  topicAsideState.toggleFollow = toggleFollow
}

async function toggleFollow() {
  followLoading.value = true
  topicAsideState.followLoading = true
  try {
    if (following.value) { await client.post(`/users/${topic.value.userId}/unfollow`); following.value=false }
    else { await client.post(`/users/${topic.value.userId}/follow`); following.value=true }
    topicAsideState.following = following.value
  } catch(e) { console.error('follow error', e) }
  finally { followLoading.value = false; topicAsideState.followLoading = false }
}
async function postComment() {
  if (!commentText.value.trim()) return
  submitting.value = true
  await client.post(`/topics/${route.params.id}/comments`, { content: commentText.value, contentType: 'markdown' })
  commentText.value = ''
  await loadComments(true)
  submitting.value = false
}

function formatTime(ts: number) { return ts ? new Date(ts).toLocaleString('zh-CN') : '' }

function onContentClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    const src = target.getAttribute('src')
    if (src) {
      const idx = contentImages.value.indexOf(src)
      viewerIndex.value = idx >= 0 ? idx + 1 : 1
    }
  }
}
</script>

<style scoped>
.post-card { background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 20px; margin-bottom: 12px; }
.topic-content { font-size: 17px; line-height: 1.9; color: var(--paper-text); word-break: break-word; }
.topic-content :deep(img) { max-width: 100%; border-radius: 8px; margin: 8px 0; }
.topic-content :deep(p) { margin: .5em 0; }
.comment-body { font-size: 14px; color: var(--paper-text); line-height: 1.6; word-break: break-word; }
</style>
