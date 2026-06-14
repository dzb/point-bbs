<template>
  <div v-if="profile">
      <div class="detail-main">
        <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" color="var(--paper-accent)" />
        <div v-else>
        <!-- Profile header -->
        <section class="mb-6">
          <div class="d-flex align-start">
            <UserAvatar :src="profile.avatar" :name="profile.nickname" :size="64" class="mr-4 flex-shrink-0" />
            <div class="flex-grow-1">
              <div class="d-flex align-center">
                <h1 class="text-h5 mb-0" style="font-family:'Noto Serif SC',Georgia,serif;font-weight:700;color:var(--paper-text)">{{ profile.nickname }}</h1>
                <v-btn v-if="auth.isLoggedIn && !isSelf" size="small" variant="outlined"
                  :color="following ? 'var(--paper-accent)' : ''" :loading="followLoading"
                  @click="toggleFollow" class="follow-btn ml-auto">
                  {{ following ? '已关注' : '+ 关注' }}
                </v-btn>
              </div>
              <div class="mt-1 mb-2" style="font-size:13px;color:var(--paper-text2)">
                <span v-if="profile.email" class="mr-3">{{ profile.email }}</span>
                <span v-if="profile.score>0" class="mr-3">{{ profile.score }} 积分</span>
              </div>
              <div v-if="profile.description" style="font-size:14px;color:var(--paper-text2);line-height:1.7">{{ profile.description }}</div>
              <div class="d-flex mt-3" style="gap:32px">
                <div><span style="font-size:18px;font-weight:600;color:var(--paper-text)">{{ profile.topicCount||0 }}</span><span style="font-size:12px;color:var(--paper-text2);margin-left:4px">帖子</span></div>
                <div><span style="font-size:18px;font-weight:600;color:var(--paper-text)">{{ profile.fansCount||0 }}</span><span style="font-size:12px;color:var(--paper-text2);margin-left:4px">粉丝</span></div>
                <div><span style="font-size:18px;font-weight:600;color:var(--paper-text)">{{ profile.followCount||0 }}</span><span style="font-size:12px;color:var(--paper-text2);margin-left:4px">关注</span></div>
              </div>
            </div>
          </div>
        </section>

        <v-tabs v-model="tab" density="compact" color="var(--paper-accent)" class="mb-4" style="border-bottom:1px solid var(--paper-border)">
          <v-tab value="topics" style="font-size:14px;text-transform:none;letter-spacing:0">帖子</v-tab>
          <v-tab value="articles" style="font-size:14px;text-transform:none;letter-spacing:0">文章</v-tab>
        </v-tabs>

        <div v-if="tab==='topics'">
          <div class="moments-list mb-4">
            <MomentCard v-for="t in topics" :key="t.id" :moment="t" @toggle-like="toggleLike" @delete-moment="removeTopic" />
          </div>
          <div v-if="hasMoreTopics" class="text-center mb-4">
            <v-btn variant="text" :loading="loadingMore" @click="loadMoreTopics" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)">显示更多</v-btn>
          </div>
          <div v-if="topics.length===0" class="text-center py-16" style="color:var(--paper-text2)">暂无帖子</div>
        </div>

        <div v-if="tab==='articles'">
          <div v-for="a in articles" :key="a.id" class="py-3" style="border-bottom:1px solid var(--paper-border);cursor:pointer"
            @click="$router.push(`/articles/${a.id}`)">
            <div style="font-size:15px;color:var(--paper-text);font-weight:500;line-height:1.4">{{ a.title || '无标题' }}</div>
            <div style="font-size:12px;color:var(--paper-text2);margin-top:4px">{{ a.summary?.substring(0,80) }} · {{ fmt(a.createTime) }}</div>
          </div>
          <div v-if="hasMoreArticles" class="text-center mb-4">
            <v-btn variant="text" :loading="loadingMore" @click="loadMoreArticles" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)">显示更多</v-btn>
          </div>
          <div v-if="articles.length===0" class="text-center py-16" style="color:var(--paper-text2)">暂无文章</div>
        </div>
      </div>
        </div>

  </div>
  <div v-else class="text-center py-16" style="color:var(--paper-text2)">用户不存在</div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import UserAvatar from '@/components/UserAvatar.vue'
import MomentCard from '@/components/MomentCard.vue'

const route = useRoute()
const auth = useAuthStore()
const profile = ref<any>(null)
const topics = ref<any[]>([])
const articles = ref<any[]>([])
const loading = ref(true)
const tab = ref('topics')
const pageTopics = ref(1)
const pageArticles = ref(1)
const pageSize = 30
const hasMoreTopics = ref(false)
const hasMoreArticles = ref(false)
const loadingMore = ref(false)
const following = ref(false)
const followLoading = ref(false)
const isSelf = computed(() => auth.user?.id === Number(route.params.id))

onMounted(loadProfile)
watch(() => route.params.id, resetAndLoad)

function resetAndLoad() {
  pageTopics.value = 1; pageArticles.value = 1
  topics.value = []; articles.value = []; loading.value = true
  loadProfile()
}

async function loadProfile() {
  const id = route.params.id as string
  try { const { data } = await client.get(`/users/${id}`); if (data.code===0) profile.value = data.data } catch { console.error('api error') }
  await Promise.all([loadTopics(), loadArticles()])
  loading.value = false
  if (auth.isLoggedIn && !isSelf.value) checkFollow()
}

async function loadTopics(reset = false) {
  if (reset) pageTopics.value = 1
  const id = route.params.id as string
  try {
    const { data } = await client.get(`/users/${id}/topics`, { params: { page: pageTopics.value, pageSize } })
    if (data.code===0) {
      const newItems = (data.data || []).map((m:any)=>({...m,liked:false}))
      topics.value = pageTopics.value === 1 ? newItems : [...topics.value, ...newItems]
      hasMoreTopics.value = newItems.length === pageSize
    }
  } catch { console.error('api error') }
}

async function loadArticles(reset = false) {
  if (reset) pageArticles.value = 1
  const id = route.params.id as string
  try {
    const { data } = await client.get(`/users/${id}/articles`, { params: { page: pageArticles.value, pageSize } })
    if (data.code===0) {
      const newItems = data.data || []
      articles.value = pageArticles.value === 1 ? newItems : [...articles.value, ...newItems]
      hasMoreArticles.value = newItems.length === pageSize
    }
  } catch { console.error('api error') }
}

async function loadMoreTopics() { pageTopics.value++; loadingMore.value = true; await loadTopics(); loadingMore.value = false }
async function loadMoreArticles() { pageArticles.value++; loadingMore.value = true; await loadArticles(); loadingMore.value = false }

async function checkFollow() {
  try {
    const { data } = await client.get(`/users/${route.params.id}/follow/status`)
    following.value = data?.data?.following || false
  } catch { console.error('api error') }
}

async function toggleFollow() {
  followLoading.value = true
  try {
    if (following.value) {
      await client.post(`/users/${route.params.id}/unfollow`)
      following.value = false
      if (profile.value) profile.value.fansCount = Math.max(0, (profile.value.fansCount || 1) - 1)
    } else {
      await client.post(`/users/${route.params.id}/follow`)
      following.value = true
      if (profile.value) profile.value.fansCount = (profile.value.fansCount || 0) + 1
    }
  } catch { console.error('api error') }
  finally { followLoading.value = false }
}

async function toggleLike(m: any) {
  if (m.liked) { await client.post(`/topics/${m.id}/unlike`); m.liked=false; m.likeCount-- }
  else { await client.post(`/topics/${m.id}/like`); m.liked=true; m.likeCount++ }
}

function removeTopic(id: number) { topics.value = topics.value.filter(t => t.id !== id) }

function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.follow-btn { text-transform: none; letter-spacing: 0; border-radius: 20px; flex-shrink: 0; }
</style>
