<template>
  <div v-if="profile">
    <div class="detail-layout">
      <!-- Left: profile + topics/articles -->
      <div class="detail-main">
        <!-- Profile header -->
        <section class="mb-6">
          <div class="d-flex align-start">
            <UserAvatar :src="profile.avatar" :name="profile.nickname" :size="64" class="mr-4 flex-shrink-0" />
            <div class="flex-grow-1">
              <h1 class="text-h5 mb-1" style="font-family:'Noto Serif SC',Georgia,serif;font-weight:700;color:var(--paper-text)">{{ profile.nickname }}</h1>
              <div class="mb-2" style="font-size:13px;color:var(--paper-text2)">
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

        <v-tabs v-model="tab" density="compact" color="#c43d3d" class="mb-4" style="border-bottom:1px solid var(--paper-border)">
          <v-tab value="topics" style="font-size:14px;text-transform:none;letter-spacing:0">帖子</v-tab>
          <v-tab value="articles" style="font-size:14px;text-transform:none;letter-spacing:0">文章</v-tab>
        </v-tabs>

        <div v-if="tab==='topics'">
          <div class="moments-list mb-4">
            <MomentCard v-for="t in topics" :key="t.id" :moment="t" @toggle-like="toggleLike" />
          </div>
          <div v-if="hasMore" class="text-center mb-4">
            <v-btn variant="text" :loading="loadingMore" @click="loadMore" style="text-transform:none;letter-spacing:0;color:var(--paper-text2)">显示更多</v-btn>
          </div>
          <div v-if="topics.length===0" class="text-center py-16" style="color:var(--paper-text2)">暂无帖子</div>
        </div>

        <div v-if="tab==='articles'">
          <div v-for="a in articles" :key="a.id" class="py-3" style="border-bottom:1px solid var(--paper-border);cursor:pointer"
            @click="$router.push(`/articles/${a.id}`)">
            <div style="font-size:15px;color:var(--paper-text);font-weight:500;line-height:1.4">{{ a.title || '无标题' }}</div>
            <div style="font-size:12px;color:var(--paper-text2);margin-top:4px">{{ a.summary?.substring(0,80) }} · {{ fmt(a.createTime) }}</div>
          </div>
          <div v-if="articles.length===0" class="text-center py-16" style="color:var(--paper-text2)">暂无文章</div>
        </div>
      </div>

      <!-- Right aside -->
      <aside class="detail-aside">
        <p class="aside-tagline">记录思考，分享见闻</p>
        <div class="aside-card"><div class="aside-card-title">社区公告</div><div class="aside-card-text">point 正在建设中。</div></div>
        <div class="aside-card"><div class="aside-card-title">推荐阅读</div><div class="aside-card-text">暂无推荐</div></div>
      </aside>
    </div>
  </div>
  <div v-else class="text-center py-16" style="color:var(--paper-text2)">用户不存在</div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import client from '@/api/client'
import UserAvatar from '@/components/UserAvatar.vue'
import MomentCard from '@/components/MomentCard.vue'

const route = useRoute()
const profile = ref<any>(null)
const topics = ref<any[]>([])
const articles = ref<any[]>([])
const tab = ref('topics')
const page = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const pageSize = 20

onMounted(loadAll)
watch(() => route.params.id, resetAndLoad)

function resetAndLoad() { page.value = 1; topics.value = []; articles.value = []; loadAll() }

async function loadAll() {
  const id = route.params.id as string
  const [ur, tr, ar] = await Promise.all([
    client.get(`/users/${id}`),
    client.get(`/users/${id}/topics`, { params: { page: page.value, pageSize } }),
    client.get(`/users/${id}/articles`, { params: { pageSize: 50 } })
  ])
  if (ur.data.code===0) profile.value = ur.data.data
  if (tr.data.code===0) {
    const newItems = (tr.data.data || []).map((m:any)=>({...m,liked:false}))
    topics.value = page.value === 1 ? newItems : [...topics.value, ...newItems]
    hasMore.value = newItems.length === pageSize
  }
  if (ar.data.code===0) articles.value = ar.data.data || []
}

async function loadMore() {
  page.value++
  loadingMore.value = true
  await loadAll()
  loadingMore.value = false
}

async function toggleLike(m: any) {
  if (m.liked) { await client.post(`/topics/${m.id}/unlike`); m.liked=false; m.likeCount-- }
  else { await client.post(`/topics/${m.id}/like`); m.liked=true; m.likeCount++ }
}

function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.detail-layout { display: flex; }
.detail-main { flex: 1; max-width: 680px; min-width: 0; padding-right: 48px; border-right: 1px solid var(--paper-border); }
.detail-aside { width: 300px; flex-shrink: 0; padding-left: 48px; }
.aside-tagline { font-size: 14px; color: var(--paper-text2); line-height: 1.8; margin-bottom: 20px; }
.aside-section { margin-bottom: 24px; }
@media (max-width: 1300px) { .detail-aside { width: 260px; padding-left: 36px; } .detail-main { padding-right: 36px; } }
@media (max-width: 1200px) { .detail-aside { width: 240px; padding-left: 28px; } .detail-main { padding-right: 28px; } }
@media (max-width: 1100px) { .detail-aside { display: none; } .detail-main { border-right: none; padding-right: 0; } }
@media (max-width: 900px)  { .detail-main { padding-right: 16px; } }
</style>
