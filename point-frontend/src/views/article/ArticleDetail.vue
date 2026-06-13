<template>
  <div v-if="article" class="mx-auto" style="max-width:680px">
    <BackButton />
    <article>
      <header class="mb-6">
        <h1 class="text-h4 mb-3" style="font-family:'Noto Serif SC','Source Han Serif SC',Georgia,serif;font-weight:700;line-height:1.4;color:var(--paper-text)">
          {{ article.title }}
        </h1>
        <div class="d-flex align-center text-caption" style="color:var(--paper-text2)">
          <router-link :to="`/users/${article.userId}`" class="d-flex align-center" style="color:var(--paper-text2);text-decoration:none">
            <UserAvatar :src="article.user?.avatar" :name="article.user?.nickname" :size="22" class="mr-2" />
            {{ article.user?.nickname }}
          </router-link>
          <span class="mx-2">·</span>
          {{ formatDate(article.createTime) }}
          <span class="mx-2">·</span>
          <v-icon size="14" class="mr-1">mdi-eye-outline</v-icon>{{ article.viewCount }} 阅读
        </div>
      </header>
      <div v-html="rendered" class="article-body" />
    </article>

    <div style="height:1px;background:var(--paper-border);margin:48px 0" />

    <section>
      <div class="d-flex align-center mb-4">
        <h3 style="font-weight:500;color:var(--paper-text);margin:0;font-size:17px">评论 {{ comments.length }}</h3>
      </div>
      <div v-if="auth.isLoggedIn" class="mb-4">
        <v-textarea v-model="commentText" placeholder="写下你的想法..." rows="2" class="mb-2" />
        <div class="d-flex justify-end">
          <v-btn variant="text" :loading="submitting" @click="postComment" style="color:var(--paper-text2)">发布</v-btn>
        </div>
      </div>
      <div v-for="c in comments" :key="c.id" class="py-3" style="border-top:1px solid var(--paper-border)">
        <div class="d-flex">
          <v-avatar :image="c.user?.avatar" size="32" class="mr-3 flex-shrink-0 mt-1" />
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-1">
              <span style="font-size:14px;color:var(--paper-text);font-weight:500">{{ c.user?.nickname }}</span>
              <span style="font-size:12px;color:var(--paper-text2);margin-left:8px">{{ formatTime(c.createTime) }}</span>
            </div>
            <div v-html="c.content" class="comment-body" />
          </div>
        </div>
      </div>
    </section>
  </div>
  <div v-else class="text-center py-16" style="color:var(--paper-text2)">
    <v-progress-circular v-if="loading" indeterminate color="#ccc" />
    <span v-else>文章不存在</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import BackButton from '@/components/BackButton.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ breaks: true, linkify: true })
const route = useRoute(); const auth = useAuthStore()
const article = ref<any>(null); const comments = ref<any[]>([])
const commentText = ref(''); const submitting = ref(false); const loading = ref(true)
const rendered = computed(() => article.value?.content ? md.render(article.value.content) : '')

onMounted(async () => {
  const id = route.params.id as string
  const [ar, cr] = await Promise.all([
    client.get(`/articles/${id}`),
    client.get(`/articles/${id}/comments`).catch(() => ({ data: { code: -1, data: [] } }))
  ])
  if (ar.data.code === 0) article.value = ar.data.data
  if (cr.data.code === 0) comments.value = cr.data.data || []
  loading.value = false
})

async function postComment() {
  if (!commentText.value.trim()) return
  submitting.value = true
  await client.post(`/articles/${route.params.id}/comments`, { content: commentText.value, contentType: 'markdown' })
  commentText.value = ''
  const { data } = await client.get(`/articles/${route.params.id}/comments`)
  if (data.code === 0) comments.value = data.data || []
  submitting.value = false
}

function formatDate(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
function formatTime(ts: number) { return ts ? new Date(ts).toLocaleString('zh-CN') : '' }
</script>

<style scoped>
.article-body { font-size:17px; line-height:1.9; color:var(--paper-text); word-break:break-word; }
.article-body :deep(h2) { font-family:'Noto Serif SC',Georgia,serif; font-size:1.4em; font-weight:700; margin:1.8em 0 .6em; color:var(--paper-text); }
.article-body :deep(h3) { font-family:'Noto Serif SC',Georgia,serif; font-size:1.15em; font-weight:600; margin:1.5em 0 .4em; color:var(--paper-text); }
.article-body :deep(p) { margin:.6em 0; }
.article-body :deep(blockquote) { border-left:2px solid #c43d3d; padding:0 0 0 1em; margin:1em 0; color:var(--paper-text2); background:rgba(196,61,61,0.04); }
.article-body :deep(pre) { background:#f5f0ea; padding:1.2em; border-radius:4px; overflow-x:auto; font-size:14px; line-height:1.6; }
.article-body :deep(code) { background:#f5f0ea; padding:0 .3em; font-size:.9em; border-radius:2px; color:#c43d3d; }
.article-body :deep(img) { max-width:100%; margin:1em 0; }
.article-body :deep(a) { color:#c43d3d; }
.comment-body { font-size:15px; line-height:1.7; color:var(--paper-text); word-break:break-word; }
.comment-body :deep(p) { margin:.2em 0; }
.comment-body :deep(img) { max-width:100%; }
</style>
