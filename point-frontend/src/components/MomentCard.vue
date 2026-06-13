<template>
  <router-link :to="`/topics/${moment.id}`" class="mc-link">
    <div class="d-flex">
      <router-link :to="`/users/${moment.userId}`" class="flex-shrink-0 mr-3" @click.prevent.stop>
        <UserAvatar :src="moment.user?.avatar" :name="moment.user?.nickname" :size="36" />
      </router-link>
      <div class="flex-grow-1" style="min-width:0">
        <div class="d-flex align-center mb-1">
          <router-link :to="`/users/${moment.userId}`" class="mc-name" @click.prevent.stop>{{ moment.user?.nickname }}</router-link>
          <span class="mc-time">{{ fmt(moment.createTime) }}</span>
        </div>
        <div v-html="rendered" class="mc-body" />
        <div class="d-flex mt-2 mc-actions">
          <span @click.prevent.stop><v-icon size="14">mdi-comment-outline</v-icon>{{ moment.commentCount || 0 }}</span>
          <span @click.prevent.stop="$emit('toggle-like', moment)">
            <v-icon size="14" :color="moment.liked ? '#c43d3d' : ''">{{ moment.liked ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>{{ moment.likeCount || 0 }}
          </span>
        </div>
      </div>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import UserAvatar from './UserAvatar.vue'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ breaks: true, linkify: true })
const props = defineProps<{ moment: any }>()
defineEmits<{ 'toggle-like': [moment: any] }>()

const rendered = computed(() => {
  const c = props.moment?.content || ''
  const html = md.render(c)
  return html.length > 600 ? html.substring(0, 600) + '…' : html
})

function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.mc-link { display: block; background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 16px 18px; text-decoration: none; }
.mc-link:hover { border-color: #c43d3d; }
.mc-name { text-decoration: none; color: var(--paper-text); font-weight: 500; font-size: 14px; }
.mc-time { font-size: 12px; color: var(--paper-text2); margin-left: auto; }
.mc-body { font-size: 15px; color: var(--paper-text); line-height: 1.7; word-break: break-word; }
.mc-body :deep(img) { max-width: 100%; max-height: 300px; border-radius: 6px; margin: 6px 0; }
.mc-body :deep(p) { margin: .3em 0; }
.mc-actions { gap: 20px; font-size: 12px; color: var(--paper-text2); }
.mc-actions span { cursor: pointer; display: flex; align-items: center; gap: 3px; }
</style>
