<template>
  <div v-if="topic">
    <BackButton />
    <div class="detail-layout">
      <!-- Left: post + comments -->
      <div class="detail-main">
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
          <div v-html="renderedContent" class="topic-content" />

          <div class="d-flex mt-3" style="gap:20px;font-size:13px;color:var(--paper-text2)">
            <span class="d-flex align-center" style="gap:4px"><v-icon size="16">mdi-eye-outline</v-icon>{{ topic.viewCount }}</span>
            <span class="d-flex align-center" style="gap:4px"><v-icon size="16">mdi-comment-outline</v-icon>{{ topic.commentCount }}</span>
          </div>

          <div class="d-flex mt-2 pt-3" style="border-top:1px solid var(--paper-border);gap:4px">
            <v-btn :color="liked ? '#c43d3d' : ''" variant="text" size="small"
              :icon="liked ? 'mdi-heart' : 'mdi-heart-outline'" :loading="likeLoading" @click="toggleLike" />
            <v-btn :color="favorited ? '#c43d3d' : ''" variant="text" size="small"
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
                  style="background:#c43d3d;color:#fff;text-transform:none;letter-spacing:0;border-radius:20px;padding:0 16px">回复</v-btn>
              </div>
            </div>
          </div>

          <div v-for="c in comments" :key="c.id" class="py-3" style="border-top:1px solid var(--paper-border)">
            <div class="d-flex">
              <router-link :to="`/users/${c.userId}`" class="flex-shrink-0 mr-3">
                <UserAvatar :src="c.user?.avatar" :name="c.user?.nickname" :size="32" />
              </router-link>
              <div>
                <div class="mb-1">
                  <router-link :to="`/users/${c.userId}`" class="text-decoration-none" style="font-size:13px;font-weight:500;color:var(--paper-text)">
                    {{ c.user?.nickname }}
                  </router-link>
                  <span style="font-size:12px;color:var(--paper-text2);margin-left:8px">{{ formatTime(c.createTime) }}</span>
                </div>
                <div v-html="c.content" class="comment-body" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: user info + announcements -->
      <aside class="detail-aside">
        <div class="aside-card">
          <div class="d-flex align-center mb-2">
            <UserAvatar :src="topic.user?.avatar" :name="topic.user?.nickname" :size="48" />
            <div class="ml-3 flex-grow-1">
              <div style="font-size:15px;font-weight:600;color:var(--paper-text)">{{ topic.user?.nickname }}</div>
            </div>
          </div>
          <div v-if="topic.user?.description" style="font-size:13px;color:var(--paper-text2);line-height:1.6;margin-bottom:10px">
            {{ topic.user?.description }}
          </div>
          <div v-else style="font-size:13px;color:var(--paper-text2);margin-bottom:10px">暂无简介</div>
          <v-btn v-if="topic.userId !== auth.user?.id" block size="small" variant="outlined"
            :color="following ? '#c43d3d' : ''" :loading="followLoading"
            @click="toggleFollow" style="text-transform:none;letter-spacing:0;border-radius:20px">
            {{ following ? '已关注' : '+ 关注' }}
          </v-btn>
        </div>

        <div class="aside-card">
          <div class="aside-card-title">社区公告</div>
          <div class="aside-card-text">point 正在建设中，欢迎反馈建议。</div>
        </div>
      </aside>
    </div>
  </div>
  <div v-else class="text-center py-16" style="color:var(--paper-text2)">
    <v-progress-circular v-if="loading" indeterminate color="#c43d3d" />
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
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ breaks: true, linkify: true })
const route = useRoute(); const auth = useAuthStore()
const topic = ref<any>(null)
const comments = ref<any[]>([])
const commentText = ref('')
const submitting = ref(false); const loading = ref(true)
const liked = ref(false); const favorited = ref(false); const following = ref(false)
const likeLoading = ref(false); const favLoading = ref(false); const followLoading = ref(false)
const renderedContent = computed(() => topic.value?.content ? md.render(topic.value.content) : '')

onMounted(loadAll)
watch(() => route.params.id, loadAll)

async function loadAll() {
  loading.value = true
  const id = route.params.id as string
  const [tr, cr] = await Promise.all([
    client.get(`/topics/${id}`),
    client.get(`/topics/${id}/comments`, { params: { pageSize: 50 } })
  ])
  if (tr.data.code===0) {
    topic.value = tr.data.data
    if (auth.isLoggedIn) await Promise.all([checkLike(), checkFav(), checkFollow()])
  }
  if (cr.data.code===0) comments.value = cr.data.data || []
  loading.value = false
}

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
async function toggleFollow() {
  followLoading.value = true
  try {
    if (following.value) { await client.post(`/users/${topic.value.userId}/unfollow`); following.value=false }
    else { await client.post(`/users/${topic.value.userId}/follow`); following.value=true }
  } catch(e) { console.error('follow error', e) }
  finally { followLoading.value = false }
}
async function postComment() {
  if (!commentText.value.trim()) return
  submitting.value = true
  await client.post(`/topics/${route.params.id}/comments`, { content: commentText.value, contentType: 'markdown' })
  commentText.value = ''
  const { data } = await client.get(`/topics/${route.params.id}/comments`, { params: { pageSize: 50 } })
  if (data.code===0) comments.value = data.data || []
  submitting.value = false
}

function formatTime(ts: number) { return ts ? new Date(ts).toLocaleString('zh-CN') : '' }
</script>

<style scoped>
.detail-layout { display: flex; gap: 0; }
.detail-main { flex: 1; max-width: 680px; min-width: 0; padding-right: 48px; border-right: 1px solid var(--paper-border); }
.detail-aside { width: 300px; flex-shrink: 0; padding-left: 48px; }
.post-card { background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 20px; margin-bottom: 12px; }
.topic-content { font-size: 17px; line-height: 1.9; color: var(--paper-text); word-break: break-word; }
.topic-content :deep(img) { max-width: 100%; border-radius: 8px; margin: 8px 0; }
.topic-content :deep(p) { margin: .5em 0; }
.comment-body { font-size: 14px; color: var(--paper-text); line-height: 1.6; word-break: break-word; }
.aside-card { border: 1px solid var(--paper-border); border-radius: 8px; padding: 14px 16px; margin-bottom: 10px; }
.aside-card-title { font-size: 13px; color: var(--paper-text); font-weight: 500; margin-bottom: 4px; }
.aside-card-text { font-size: 13px; color: var(--paper-text2); line-height: 1.7; }
@media (max-width: 1100px) { .detail-aside { display: none; } .detail-main { border-right: none; padding-right: 0; } }
@media (max-width: 1200px) { .detail-main { padding-right: 32px; } .detail-aside { padding-left: 32px; } }
@media (max-width: 900px) { .detail-main { padding-right: 16px; } }
</style>
