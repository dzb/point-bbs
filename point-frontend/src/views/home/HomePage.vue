<template>
  <div class="home-layout">
    <div class="home-feed">
    <!-- Input box -->
    <div v-if="auth.isLoggedIn" class="mb-4">
      <div class="d-flex">
        <v-avatar size="36" class="mr-3 flex-shrink-0 mt-1">
          <v-icon>mdi-pen</v-icon>
        </v-avatar>
        <div class="flex-grow-1">
          <v-textarea v-model="newMoment" placeholder="记录思考，分享见闻..." rows="2"
            variant="outlined" hide-details class="mb-2" density="compact" />
          <div class="d-flex align-center">
            <v-btn icon="mdi-image-outline" variant="text" size="small" :style="{color:theme.text2}" />
            <v-spacer />
            <v-btn :style="{background:'#c43d3d',color:'#fff',textTransform:'none',letterSpacing:0,borderRadius:'20px',padding:'0 20px'}"
              variant="flat" size="small" :loading="posting" @click="postMoment">发布</v-btn>
          </div>
        </div>
      </div>
    </div>

    <!-- Moments feed -->
    <div class="moments-list">
    <router-link v-for="m in moments" :key="m.id" :to="`/topics/${m.id}`" class="moment-card text-decoration-none">
      <div class="d-flex">
        <router-link :to="`/users/${m.userId}`" class="flex-shrink-0 mr-3">
          <UserAvatar :src="m.user?.avatar" :name="m.user?.nickname" :size="36" />
        </router-link>
        <div class="flex-grow-1" style="min-width:0">
          <div class="d-flex align-center mb-1">
            <router-link :to="`/users/${m.userId}`" class="text-decoration-none" style="color:var(--paper-text);font-weight:500;font-size:14px">
              {{ m.user?.nickname }}
            </router-link>
            <span class="moment-time">{{ fmt(m.createTime) }}</span>
          </div>
          <div v-html="mdBrief(m.content)" style="font-size:15px;color:var(--paper-text);line-height:1.7;word-break:break-word" />
          <div class="d-flex mt-2 moment-actions">
            <span @click.prevent.stop>
              <v-icon size="14">mdi-comment-outline</v-icon>{{ m.commentCount||0 }}
            </span>
            <span @click.prevent.stop="toggleLike(m)">
              <v-icon size="14" :color="m.liked ? '#c43d3d' : ''">{{ m.liked ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>{{ m.likeCount||0 }}
            </span>
          </div>
        </div>
      </div>
    </router-link>
    </div><!-- .moments-list -->

    <div v-if="moments.length===0" class="text-center py-16" style="color:var(--paper-text2)">暂无随想</div>
    </div><!-- .home-feed -->

    <!-- Right: announcements -->
    <aside class="home-aside">
      <p class="aside-tagline">记录思考，分享见闻</p>

      <div class="aside-card">
        <div class="aside-card-title">社区公告</div>
        <div class="aside-card-text">point 正在建设中，欢迎反馈建议。</div>
      </div>

      <div class="aside-card">
        <div class="aside-card-title">推荐阅读</div>
        <div class="aside-card-text">暂无推荐</div>
      </div>
    </aside>
  </div><!-- .home-layout -->
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useTheme } from 'vuetify'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import UserAvatar from '@/components/UserAvatar.vue'

const auth = useAuthStore()
const moments = ref<any[]>([])
const newMoment = ref('')
const posting = ref(false)

const vuetifyTheme = useTheme()
const theme = ref({
  text2: '#8b7e6a', border: '#e8e0d5'
})

onMounted(async () => {
  loadMoments()
})

async function loadMoments() {
  const { data } = await client.get('/topics/moments', { params: { pageSize: 50 } })
  if (data.code === 0) moments.value = (data.data || []).map((m: any) => ({ ...m, liked: false }))
}

async function postMoment() {
  if (!newMoment.value.trim()) return
  posting.value = true
  const { data } = await client.post('/topics', { title: '', content: newMoment.value, type: 1 })
  if (data.code === 0) { newMoment.value = ''; loadMoments() }
  posting.value = false
}

async function toggleLike(m: any) {
  const id = m.id
  if (m.liked) {
    await client.post(`/topics/${id}/unlike`)
    m.liked = false; m.likeCount--
  } else {
    await client.post(`/topics/${id}/like`)
    m.liked = true; m.likeCount++
  }
}

function mdBrief(md: string) {
  if (!md) return ''
  return md.replace(/[#*>`_~\[\]()!|-]/g, '').substring(0, 300)
}

function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.home-layout { display: flex; gap: 0; }
.home-feed { flex: 1; max-width: 680px; min-width: 0; padding-right: 48px; border-right: 1px solid var(--paper-border); }
.home-aside { width: 340px; flex-shrink: 0; padding-left: 36px; }

@media (max-width: 1200px) { .home-feed { padding-right: 32px; } .home-aside { padding-left: 32px; } }
@media (max-width: 1100px) { .home-aside { display: none; } .home-feed { border-right: none; padding-right: 0; } }
@media (max-width: 900px)  { .home-feed { padding-right: 16px; } }

.moments-list { display: flex; flex-direction: column; gap: 10px; }
.moment-card { background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 16px 18px; }
.moment-card:hover { border-color: #c43d3d; }
.moment-time { font-size: 12px; color: var(--paper-text2); margin-left: auto; }
.moment-actions { gap: 20px; font-size: 12px; color: var(--paper-text2); }
.moment-actions span { cursor: pointer; display: flex; align-items: center; gap: 3px; }

.aside-tagline { font-size: 14px; color: var(--paper-text2); line-height: 1.8; margin-bottom: 20px; }
.aside-card { background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 10px 14px; margin-bottom: 10px; }
.aside-card-title { font-size: 14px; color: var(--paper-text); font-weight: 500; margin-bottom: 4px; }
.aside-card-text { font-size: 14px; color: var(--paper-text2); line-height: 1.7; }
</style>
