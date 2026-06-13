<template>
  <div v-if="profile" class="mx-auto" style="max-width:680px">
    <section class="mb-6">
      <div class="d-flex align-start">
        <v-avatar :image="profile.avatar" size="64" class="mr-4 flex-shrink-0" />
        <div class="flex-grow-1">
          <h1 class="text-h5 mb-1" style="font-family:'Noto Serif SC',Georgia,serif;font-weight:700;color:#1a1a1a">{{ profile.nickname }}</h1>
          <div class="mb-2" style="font-size:13px;color:#999">
            <span v-if="profile.email" class="mr-3">{{ profile.email }}</span>
            <span v-if="profile.score>0" class="mr-3">{{ profile.score }} 积分</span>
          </div>
          <div v-if="profile.description" style="font-size:14px;color:#666;line-height:1.7">{{ profile.description }}</div>
          <div class="d-flex mt-3" style="gap:32px">
            <div><span style="font-size:18px;font-weight:600;color:#1a1a1a">{{ profile.topicCount||0 }}</span><span style="font-size:12px;color:#bbb;margin-left:4px">帖子</span></div>
            <div><span style="font-size:18px;font-weight:600;color:#1a1a1a">{{ profile.fansCount||0 }}</span><span style="font-size:12px;color:#bbb;margin-left:4px">粉丝</span></div>
            <div><span style="font-size:18px;font-weight:600;color:#1a1a1a">{{ profile.followCount||0 }}</span><span style="font-size:12px;color:#bbb;margin-left:4px">关注</span></div>
          </div>
        </div>
      </div>
    </section>

    <v-tabs v-model="tab" density="compact" color="#c43d3d" class="mb-4" style="border-bottom:1px solid #e8e0d5">
      <v-tab value="topics" style="font-size:14px;text-transform:none;letter-spacing:0">帖子</v-tab>
      <v-tab value="articles" style="font-size:14px;text-transform:none;letter-spacing:0">文章</v-tab>
    </v-tabs>

    <div v-if="tab==='topics'">
      <div v-for="t in topics" :key="t.id" class="py-3" style="border-bottom:1px solid #f0f0f0;cursor:pointer"
        @click="$router.push(`/topics/${t.id}`)">
        <div style="font-size:15px;color:#1a1a1a;font-weight:500;line-height:1.4">{{ t.title || '无标题' }}</div>
        <div style="font-size:12px;color:#bbb;margin-top:4px">{{ t.commentCount||0 }} 评论 · {{ fmt(t.createTime) }}</div>
      </div>
      <div v-if="topics.length===0" class="text-center py-16" style="color:#bbb">暂无帖子</div>
    </div>

    <div v-if="tab==='articles'">
      <div v-for="a in articles" :key="a.id" class="py-3" style="border-bottom:1px solid #f0f0f0;cursor:pointer"
        @click="$router.push(`/articles/${a.id}`)">
        <div style="font-size:15px;color:#1a1a1a;font-weight:500;line-height:1.4">{{ a.title || '无标题' }}</div>
        <div style="font-size:12px;color:#bbb;margin-top:4px">{{ a.summary?.substring(0,80) }} · {{ fmt(a.createTime) }}</div>
      </div>
      <div v-if="articles.length===0" class="text-center py-16" style="color:#bbb">暂无文章</div>
    </div>
  </div>
  <div v-else class="text-center py-16" style="color:#bbb">用户不存在</div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import client from '@/api/client'

const route = useRoute()
const profile = ref<any>(null)
const topics = ref<any[]>([])
const articles = ref<any[]>([])
const tab = ref('topics')

onMounted(loadAll)
watch(() => route.params.id, loadAll)

async function loadAll() {
  const id = route.params.id as string
  const [ur, tr, ar] = await Promise.all([
    client.get(`/users/${id}`),
    client.get(`/users/${id}/topics`, { params: { pageSize: 20 } }),
    client.get(`/users/${id}/articles`, { params: { pageSize: 20 } })
  ])
  if (ur.data.code===0) profile.value = ur.data.data
  if (tr.data.code===0) topics.value = tr.data.data || []
  if (ar.data.code===0) articles.value = ar.data.data || []
}

function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>
