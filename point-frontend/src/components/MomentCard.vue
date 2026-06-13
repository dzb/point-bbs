<template>
  <router-link :to="`/topics/${moment.id}`" class="moment-card text-decoration-none">
    <div class="d-flex">
      <router-link :to="`/users/${moment.userId}`" class="flex-shrink-0 mr-3" @click.prevent.stop>
        <UserAvatar :src="moment.user?.avatar" :name="moment.user?.nickname" :size="36" />
      </router-link>
      <div class="flex-grow-1" style="min-width:0">
        <div class="d-flex align-center mb-1">
          <router-link :to="`/users/${moment.userId}`" class="text-decoration-none" style="color:var(--paper-text);font-weight:500;font-size:14px" @click.prevent.stop>
            {{ moment.user?.nickname }}
          </router-link>
          <span class="moment-time">{{ fmt(moment.createTime) }}</span>
        </div>
        <div v-html="brief(moment.content)" style="font-size:15px;color:var(--paper-text);line-height:1.7;word-break:break-word" />
        <div class="d-flex mt-2 moment-actions">
          <span @click.prevent.stop>
            <v-icon size="14">mdi-comment-outline</v-icon>{{ moment.commentCount || 0 }}
          </span>
          <span @click.prevent.stop="$emit('toggle-like', moment)">
            <v-icon size="14" :color="moment.liked ? '#c43d3d' : ''">{{ moment.liked ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>{{ moment.likeCount || 0 }}
          </span>
        </div>
      </div>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import UserAvatar from './UserAvatar.vue'

defineProps<{ moment: any }>()
defineEmits<{ 'toggle-like': [moment: any] }>()

function brief(md: string) { return md ? md.replace(/[#*>`_~\[\]()!|-]/g, '').substring(0, 300) : '' }
function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.moment-card { display: block; background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 16px 18px; }
.moment-card:hover { border-color: #c43d3d; }
.moment-time { font-size: 12px; color: var(--paper-text2); margin-left: auto; }
.moment-actions { gap: 20px; font-size: 12px; color: var(--paper-text2); }
.moment-actions span { cursor: pointer; display: flex; align-items: center; gap: 3px; }
</style>
