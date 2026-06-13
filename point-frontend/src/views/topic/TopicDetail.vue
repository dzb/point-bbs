<template>
  <div v-if="topic">
    <BackButton />
    <v-card class="mb-4">
      <v-card-item>
        <v-card-title class="text-h5">{{ topic.title }}</v-card-title>
        <v-card-subtitle>
          <UserAvatar :src="topic.user?.avatar" :name="topic.user?.nickname" :size="24" class="mr-1" />
          {{ topic.user?.nickname }} · {{ formatTime(topic.createTime) }}
          <span class="ml-3"><v-icon size="small">mdi-eye-outline</v-icon> {{ topic.viewCount }}</span>
          <span class="ml-2"><v-icon size="small">mdi-comment-outline</v-icon> {{ topic.commentCount }}</span>
        </v-card-subtitle>
      </v-card-item>
      <v-card-text><div v-html="renderedContent" class="topic-content" /></v-card-text>
      <v-card-actions>
        <v-btn :color="liked ? '#c43d3d' : ''" variant="text" :icon="liked ? 'mdi-heart' : 'mdi-heart-outline'"
          :loading="likeLoading" @click="toggleLike" />
        <span class="text-caption mr-3">{{ topic.likeCount }}</span>
        <v-btn :color="favorited ? '#c43d3d' : ''" variant="text"
          :icon="favorited ? 'mdi-bookmark' : 'mdi-bookmark-outline'" :loading="favLoading" @click="toggleFav" />
        <v-btn v-if="topic.userId !== auth.user?.id" variant="text" :color="following ? '#c43d3d' : ''"
          :icon="following ? 'mdi-account-check-outline' : 'mdi-account-plus-outline'"
          :loading="followLoading" @click="toggleFollow" />
        <v-btn v-if="topic.userId === auth.user?.id" variant="text" icon="mdi-pencil" :to="`/topic/${topic.id}/edit`" />
      </v-card-actions>
    </v-card>

    <v-card class="mb-4">
      <v-card-title class="text-body-1" style="font-weight:500">评论 <span style="font-size:13px;color:var(--paper-text2);font-weight:400">{{ comments.length }}</span></v-card-title>
      <v-card-text>
        <div v-if="auth.isLoggedIn" class="mb-4">
          <v-textarea v-model="commentText" label="写下你的评论..." rows="3" variant="outlined" density="compact" hide-details class="mb-2" />
          <v-btn color="primary" :loading="submitting" @click="postComment">发表评论</v-btn>
        </div>
        <v-divider class="mb-3" />
        <div v-for="c in comments" :key="c.id" class="mb-3">
          <div class="d-flex align-start">
            <UserAvatar :src="c.user?.avatar" :name="c.user?.nickname" :size="32" class="mr-2" />
            <div class="flex-grow-1">
              <div class="text-caption text-grey">{{ c.user?.nickname }} · {{ formatTime(c.createTime) }}</div>
              <div v-html="c.content" class="comment-content" />
            </div>
          </div>
          <v-divider v-if="c !== comments[comments.length - 1]" class="mt-2" />
        </div>
      </v-card-text>
    </v-card>
  </div>
  <div v-else class="text-center py-8">
    <v-progress-circular v-if="loading" indeterminate />
    <span v-else class="text-grey">帖子不存在</span>
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
const route = useRoute()
const auth = useAuthStore()
const topic = ref<any>(null)
const comments = ref<any[]>([])
const commentText = ref('')
const submitting = ref(false)
const loading = ref(true)
const liked = ref(false); const favorited = ref(false); const following = ref(false)
const likeLoading = ref(false); const favLoading = ref(false); const followLoading = ref(false)
const renderedContent = computed(() => topic.value?.content ? md.render(topic.value.content) : '')

onMounted(async () => { await loadTopic(); if (auth.isLoggedIn && topic.value) await Promise.all([checkLike(), checkFav(), checkFollow()]) })
watch(() => route.params.id, () => loadTopic())

async function loadTopic() {
  loading.value = true
  const id = route.params.id
  const { data } = await client.get(`/topics/${id}`)
  if (data.code === 0) topic.value = data.data
  const { data: cdata } = await client.get(`/topics/${id}/comments`, { params: { pageSize: 50 } })
  if (cdata.code === 0) comments.value = cdata.data || []
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
  try {
    if (liked.value) { await client.post(`/topics/${route.params.id}/unlike`); liked.value = false; topic.value.likeCount-- }
    else { await client.post(`/topics/${route.params.id}/like`); liked.value = true; topic.value.likeCount++ }
  } catch { /* ignore */ }
  likeLoading.value = false
}
async function toggleFav() {
  favLoading.value = true
  try {
    if (favorited.value) { await client.post(`/topics/${route.params.id}/unfavorite`); favorited.value = false }
    else { await client.post(`/topics/${route.params.id}/favorite`); favorited.value = true }
  } catch { /* ignore */ }
  favLoading.value = false
}
async function toggleFollow() {
  followLoading.value = true
  try {
    if (following.value) { await client.post(`/users/${topic.value.userId}/unfollow`); following.value = false }
    else { await client.post(`/users/${topic.value.userId}/follow`); following.value = true }
  } catch { /* ignore */ }
  followLoading.value = false
}
async function postComment() {
  if (!commentText.value.trim()) return
  submitting.value = true
  const { data } = await client.post(`/topics/${route.params.id}/comments`, { content: commentText.value, contentType: 'markdown' })
  if (data.code === 0) { commentText.value = ''; await loadTopic() }
  submitting.value = false
}

function formatTime(ts: number) { return ts ? new Date(ts).toLocaleString('zh-CN') : '' }
</script>

<style scoped>
.topic-content :deep(img), .comment-content :deep(img) { max-width: 100%; }
</style>
