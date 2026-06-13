<template>
  <div class="home-layout">
    <div class="home-feed">
      <div class="moments-list">
      <router-link v-for="m in moments" :key="m.id" :to="`/topics/${m.id}`" class="moment-card text-decoration-none">
        <div class="d-flex">
          <router-link :to="`/users/${m.userId}`" class="flex-shrink-0 mr-3" @click.prevent.stop>
            <UserAvatar :src="m.user?.avatar" :name="m.user?.nickname" :size="36" />
          </router-link>
          <div class="flex-grow-1" style="min-width:0">
            <div class="d-flex align-center mb-1">
              <router-link :to="`/users/${m.userId}`" class="text-decoration-none" style="color:var(--paper-text);font-weight:500;font-size:14px" @click.prevent.stop>
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
      </div>
      <div v-if="moments.length===0" class="text-center py-16" style="color:var(--paper-text2)">暂无随想</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import client from '@/api/client'
import UserAvatar from '@/components/UserAvatar.vue'

const moments = ref<any[]>([])

onMounted(async () => {
  const { data } = await client.get('/topics/moments', { params: { pageSize: 100 } })
  if (data.code===0) moments.value = (data.data||[]).map((m:any)=>({...m,liked:false}))
})

async function toggleLike(m: any) {
  if (m.liked) {
    await client.post(`/topics/${m.id}/unlike`); m.liked=false; m.likeCount--
  } else {
    await client.post(`/topics/${m.id}/like`); m.liked=true; m.likeCount++
  }
}

function mdBrief(md: string) { return md ? md.replace(/[#*>`_~\[\]()!|-]/g,'').substring(0,300) : '' }
function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.home-layout { display: flex; gap: 0; }
.home-feed { flex: 1; max-width: 680px; min-width: 0; padding-right: 12px; }
.moments-list { display: flex; flex-direction: column; gap: 10px; }
.moment-card { background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 16px 18px; }
.moment-card:hover { border-color: #c43d3d; }
.moment-time { font-size: 12px; color: var(--paper-text2); margin-left: auto; }
.moment-actions { gap: 20px; font-size: 12px; color: var(--paper-text2); }
.moment-actions span { cursor: pointer; display: flex; align-items: center; gap: 3px; }
</style>
