<template>
  <div v-if="article">
      <div class="detail-main">
        <BackButton />
        <article>
          <header class="mb-6">
            <h1 class="text-h4 mb-3" style="font-family:'Noto Serif SC','Source Han Serif SC',Georgia,serif;font-weight:700;line-height:1.4;color:var(--paper-text)">
              {{ article.title }}
            </h1>
            <div class="d-flex align-center text-caption" style="color:var(--paper-text2)">
              <router-link :to="`/users/${article.userId}`" class="d-flex align-center" style="color:var(--paper-text2);text-decoration:none">
                <UserAvatar :src="article.user?.avatar" :name="article.user?.nickname" :size="22" class="mr-2" />
                {{ article.user?.nickname }}<span style="font-size:11px;margin-left:4px;color:var(--paper-text2)">@{{ article.user?.username || article.userId }}</span>
              </router-link>
              <span class="mx-2">·</span>
              {{ formatDate(article.createTime) }}
              <span class="mx-2">·</span>
              <v-icon size="14" class="mr-1">mdi-eye-outline</v-icon>{{ article.viewCount }} 阅读
            </div>
          </header>
          <div v-html="rendered" class="article-body" @click="onContentClick" />

          <div class="d-flex mt-4 pt-3" style="border-top:1px solid var(--paper-border);gap:4px">
            <v-btn :color="liked ? 'var(--paper-accent)' : ''" variant="text" size="small"
              :icon="liked ? 'mdi-heart' : 'mdi-heart-outline'" :loading="likeLoading" @click="toggleLike" />
            <v-btn :color="favorited ? 'var(--paper-accent)' : ''" variant="text" size="small"
              :icon="favorited ? 'mdi-bookmark' : 'mdi-bookmark-outline'" :loading="favLoading" @click="toggleFav" />
            <v-spacer />
            <template v-if="isAuthor">
              <v-btn variant="text" size="small" icon="mdi-pencil-outline" @click="editArticle" />
              <v-btn variant="text" size="small" icon="mdi-delete-outline" color="var(--paper-accent)" :loading="deleting" @click="deleteArticle" />
            </template>
          </div>
        </article>

        <div style="height:1px;background:var(--paper-border);margin:48px 0" />

        <section>
          <div class="d-flex align-center mb-4">
            <h3 style="font-weight:500;color:var(--paper-text);margin:0;font-size:17px">评论 {{ comments.length }}</h3>
          </div>
          <div v-if="auth.isLoggedIn" class="mb-4">
            <MentionTextarea v-model="commentText" placeholder="写下你的想法... @用户名 可提及用户" rows="2" class="mb-2" />
            <div class="d-flex justify-end">
              <v-btn variant="text" :loading="submitting" @click="postComment" style="color:var(--paper-text2)">发布</v-btn>
            </div>
          </div>
          <div v-for="c in comments" :key="c.id" class="py-3" style="border-top:1px solid var(--paper-border)">
            <div class="d-flex">
              <UserAvatar :src="c.user?.avatar" :name="c.user?.nickname" :size="32" class="mr-3 flex-shrink-0 mt-1" />
              <div class="flex-grow-1">
                <div class="d-flex align-center mb-1">
                  <span style="font-size:14px;color:var(--paper-text);font-weight:500">{{ c.user?.nickname }}<span style="font-size:11px;color:var(--paper-text2);font-weight:400"> @{{ c.user?.username || c.user?.id }}</span></span>
                  <span style="font-size:12px;color:var(--paper-text2);margin-left:8px">{{ formatTime(c.createTime) }}</span>
                </div>
                <div v-html="renderMarkdown(c.content)" class="comment-body" />
              </div>
            </div>
          </div>
          <LoadMore :has-more="hasMoreComments" :loading="loadingMoreComments" label="更多评论" @load-more="loadMoreComments" />
        </section>

      </div>
      <ImageViewer :images="contentImages" v-model="viewerIndex" />
  </div>
  <div v-else class="text-center py-16" style="color:var(--paper-text2)">
    <v-progress-circular v-if="loading" indeterminate color="#ccc" />
    <span v-else>文章不存在</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import BackButton from '@/components/BackButton.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import ImageViewer from '@/components/ImageViewer.vue'
import MentionTextarea from '@/components/MentionTextarea.vue'
import LoadMore from '@/components/LoadMore.vue'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute(); const router = useRouter(); const auth = useAuthStore()
const article = ref<any>(null); const comments = ref<any[]>([])
const commentText = ref(''); const submitting = ref(false); const loading = ref(true)
const commentPage = ref(1); const commentPageSize = 30
const hasMoreComments = ref(false); const loadingMoreComments = ref(false)
const viewerIndex = ref(0)
const liked = ref(false); const favorited = ref(false)
const likeLoading = ref(false); const favLoading = ref(false)
const deleting = ref(false)
const isAuthor = computed(() => !!article.value && !!auth.user && article.value.userId === auth.user.id)
const rendered = computed(() => article.value?.content ? renderMarkdown(article.value.content) : '')
const contentImages = computed(() => {
  const matches = article.value?.content?.matchAll(/!\[.*?\]\((.+?)\)/g) || []
  return Array.from(matches, (m: any) => m[1])
})

onMounted(async () => {
  const id = route.params.id as string
  const ar = await client.get(`/articles/${id}`)
  if (ar.data.code === 0) article.value = ar.data.data
  await loadComments()
  if (article.value) await loadStatus()
  loading.value = false
})

async function loadStatus() {
  const id = route.params.id as string
  try {
    const [{ data: l }, { data: f }] = await Promise.all([
      client.get(`/articles/${id}/like/status`),
      client.get(`/articles/${id}/favorite/status`),
    ])
    if (l.code === 0) liked.value = !!l.data?.liked
    if (f.code === 0) favorited.value = !!f.data?.favorited
  } catch { /* ignore */ }
}

async function toggleLike() {
  const id = route.params.id as string
  likeLoading.value = true
  try {
    if (liked.value) await client.post(`/articles/${id}/unlike`)
    else await client.post(`/articles/${id}/like`)
    liked.value = !liked.value
  } catch { /* ignore */ }
  likeLoading.value = false
}

async function toggleFav() {
  const id = route.params.id as string
  favLoading.value = true
  try {
    if (favorited.value) await client.post(`/articles/${id}/unfavorite`)
    else await client.post(`/articles/${id}/favorite`)
    favorited.value = !favorited.value
  } catch { /* ignore */ }
  favLoading.value = false
}

function editArticle() {
  router.push(`/articles/create?edit=${route.params.id}`)
}

async function deleteArticle() {
  if (!confirm('确定删除这篇文章吗？')) return
  deleting.value = true
  try {
    await client.post(`/articles/delete/${route.params.id}`)
    router.push('/articles')
  } catch { /* ignore */ }
  deleting.value = false
}

async function loadComments(reset = false) {
  if (reset) commentPage.value = 1
  const id = route.params.id as string
  try {
    const { data } = await client.get(`/articles/${id}/comments`, { params: { page: commentPage.value, pageSize: commentPageSize } })
    if (data.code === 0) {
      const payload = data.data || {}
      const newItems = payload.items || []
      comments.value = commentPage.value === 1 ? newItems : [...comments.value, ...newItems]
      hasMoreComments.value = (payload.items || []).length < (payload.total ?? 0)
    }
  } catch { console.error('api error') }
}

async function loadMoreComments() { commentPage.value++; loadingMoreComments.value = true; await loadComments(); loadingMoreComments.value = false }

async function postComment() {
  if (!commentText.value.trim()) return
  submitting.value = true
  await client.post(`/articles/${route.params.id}/comments`, { content: commentText.value, contentType: 'markdown' })
  commentText.value = ''
  await loadComments(true)
  submitting.value = false
}

function formatDate(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
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
.article-body { font-size:15px; line-height:1.7; color:var(--paper-text); word-break:break-word; }
.article-body :deep(h2) { font-family:'Noto Serif SC',Georgia,serif; font-size:1.4em; font-weight:700; margin:1.8em 0 .6em; color:var(--paper-text); }
.article-body :deep(h3) { font-family:'Noto Serif SC',Georgia,serif; font-size:1.15em; font-weight:600; margin:1.5em 0 .4em; color:var(--paper-text); }
.article-body :deep(p) { margin:.6em 0; }
.article-body :deep(ul), .article-body :deep(ol) { padding-left: 1.8em; margin: .5em 0; }
.article-body :deep(li) { margin: .2em 0; }
.article-body :deep(blockquote) { border-left:2px solid var(--paper-accent); padding:0 0 0 1em; margin:1em 0; color:var(--paper-text2); background:rgba(196,61,61,0.04); }
.article-body :deep(pre) { background:#f5f0ea; padding:1.2em; border-radius:4px; overflow-x:auto; font-size:14px; line-height:1.6; }
.article-body :deep(code) { background:#f5f0ea; padding:0 .3em; font-size:.9em; border-radius:2px; color:var(--paper-accent); }
.article-body :deep(img) { max-width:100%; margin:1em 0; }
.article-body :deep(a) { color:var(--paper-accent); }
.comment-body { font-size:15px; line-height:1.7; color:var(--paper-text); word-break:break-word; }
.comment-body :deep(p) { margin:.2em 0; }
.comment-body :deep(ul), .comment-body :deep(ol) { padding-left: 1.6em; margin: .3em 0; }
.comment-body :deep(li) { margin: .1em 0; }
.comment-body :deep(img) { max-width:100%; }
</style>
