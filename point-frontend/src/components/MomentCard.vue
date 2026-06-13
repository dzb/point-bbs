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
        <div v-html="rendered" class="mc-body" @click.prevent.stop="onBodyClick" />
        <div class="d-flex mt-2 mc-actions">
          <span @click.prevent.stop><v-icon size="14">mdi-comment-outline</v-icon>{{ moment.commentCount || 0 }}</span>
          <span @click.prevent.stop="$emit('toggle-like', moment)">
            <v-icon size="14" :color="moment.liked ? '#c43d3d' : ''">{{ moment.liked ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>{{ moment.likeCount || 0 }}
          </span>
        </div>
      </div>
    </div>
  </router-link>

  <!-- Image Viewer — Twitter-style -->
  <Teleport to="body">
    <div v-if="viewer" class="viewer-overlay" @click.self="closeViewer" @keydown.esc="closeViewer">
      <v-btn icon="mdi-close" variant="text" class="viewer-close-btn" @click="closeViewer" size="36" />

      <div class="viewer-layout">
        <!-- Left: Image -->
        <div class="viewer-image-side">
          <v-btn v-if="images.length>1" icon="mdi-chevron-left" variant="text" class="viewer-arrow left" @click="prevImage" />
          <img :src="images[viewer-1]" class="viewer-main-img" />
          <v-btn v-if="images.length>1" icon="mdi-chevron-right" variant="text" class="viewer-arrow right" @click="nextImage" />
          <div v-if="images.length>1" class="viewer-counter">{{ viewer }} / {{ images.length }}</div>
        </div>

        <!-- Right: Post context -->
        <div class="viewer-context">
          <div class="d-flex align-center mb-3">
            <UserAvatar :src="moment.user?.avatar" :name="moment.user?.nickname" :size="36" />
            <div class="ml-3">
              <div style="font-size:14px;font-weight:600;color:var(--paper-text)">{{ moment.user?.nickname }}</div>
            </div>
          </div>
          <div v-html="textOnly" style="font-size:15px;color:var(--paper-text);line-height:1.7;word-break:break-word;flex:1;overflow-y:auto" />
          <div class="viewer-divider" />
          <div class="d-flex viewer-actions-row">
            <span class="d-flex align-center" style="gap:4px"><v-icon size="16">mdi-comment-outline</v-icon>{{ moment.commentCount||0 }}</span>
            <span class="d-flex align-center" style="gap:4px;cursor:pointer" @click="$emit('toggle-like',moment)">
              <v-icon size="16" :color="moment.liked?'#c43d3d':''">{{ moment.liked?'mdi-heart':'mdi-heart-outline' }}</v-icon>{{ moment.likeCount||0 }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import UserAvatar from './UserAvatar.vue'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ breaks: true, linkify: true })
const props = defineProps<{ moment: any }>()
defineEmits<{ 'toggle-like': [moment: any] }>()

const viewer = ref(0)

function onKey(e: KeyboardEvent) { if (e.key === 'Escape') viewer.value = 0 }
onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))

const rendered = computed(() => {
  const c = props.moment?.content || ''
  let html = md.render(c)
  // Wrap consecutive <img> tags in a grid container
  html = html.replace(/((?:<p><img[^>]*><\/p>\s*)+)/g, (match) => {
    const count = (match.match(/<img/g) || []).length
    const imgs = match.replace(/<\/?p>/g, '')
    const cols = count === 1 ? 'cols-1' : count === 2 ? 'cols-2' : count === 3 ? 'cols-3' : 'cols-4'
    return `<div class="img-grid ${cols}">${imgs}</div>`
  })
  if (html.length > 600) html = html.substring(0, 600) + '…'
  return html
})

const textOnly = computed(() => {
  const c = props.moment?.content || ''
  return md.render(c.replace(/!\[.*?\]\(.+?\)/g, ''))
})

const images = computed(() => {
  const matches = props.moment?.content?.matchAll(/!\[.*?\]\((.+?)\)/g) || []
  return Array.from(matches, (m: any) => m[1])
})

function onBodyClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    e.preventDefault()
    const src = target.getAttribute('src')
    if (src) {
      const idx = images.value.indexOf(src)
      viewer.value = idx >= 0 ? idx + 1 : 1
    }
  }
}

function closeViewer() { viewer.value = 0 }
function prevImage() { if (viewer.value > 1) viewer.value-- }
function nextImage() { if (viewer.value < images.value.length) viewer.value++ }
function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.mc-link { display: block; background: var(--paper-bg); border: 1px solid var(--paper-border); border-radius: 8px; padding: 16px 18px; text-decoration: none; }
.mc-link:hover { border-color: #c43d3d; }
.mc-name { text-decoration: none; color: var(--paper-text); font-weight: 500; font-size: 14px; }
.mc-time { font-size: 12px; color: var(--paper-text2); margin-left: auto; }
.mc-body { font-size: 15px; color: var(--paper-text); line-height: 1.7; word-break: break-word; }
.mc-body :deep(p) { margin: .3em 0; }
.mc-body :deep(img) { max-width: 100%; max-height: 400px; border-radius: 8px; cursor: pointer; vertical-align: top; }
/* Multi-image grid via container */
.mc-body :deep(.img-grid) { display: grid; gap: 4px; margin: 8px 0; }
.mc-body :deep(.img-grid.cols-1) { grid-template-columns: 1fr; }
.mc-body :deep(.img-grid.cols-2) { grid-template-columns: 1fr 1fr; }
.mc-body :deep(.img-grid.cols-3) { grid-template-columns: 1fr 1fr; }
.mc-body :deep(.img-grid.cols-3 img:first-child) { grid-row: span 2; }
.mc-body :deep(.img-grid.cols-4) { grid-template-columns: 1fr 1fr; }
.mc-body :deep(.img-grid img) { width: 100%; height: 100%; object-fit: cover; border: 1px solid var(--paper-border); }
.mc-actions { gap: 20px; font-size: 12px; color: var(--paper-text2); }
.mc-actions span { cursor: pointer; display: flex; align-items: center; gap: 3px; }
</style>

<style>
.viewer-overlay { position: fixed; inset: 0; z-index: 9999; background: rgba(0,0,0,.75); display: flex; align-items: stretch; }
.viewer-close-btn { position: fixed; top: 12px; left: 12px; z-index: 10; color: #fff !important; }
.viewer-layout { display: flex; width: 100%; height: 100%; }
.viewer-image-side { flex: 1; display: flex; align-items: center; justify-content: center; position: relative; min-width: 0; }
.viewer-main-img { max-width: 95%; max-height: 95vh; object-fit: contain; border-radius: 4px; }
.viewer-arrow { position: absolute; top: 50%; transform: translateY(-50%); color: rgba(255,255,255,.7) !important; }
.viewer-arrow.left { left: 8px; }
.viewer-arrow.right { right: 8px; }
.viewer-counter { position: absolute; bottom: 16px; left: 50%; transform: translateX(-50%); color: rgba(255,255,255,.5); font-size: 13px; }
.viewer-context { width: 360px; flex-shrink: 0; background: #fff; display: flex; flex-direction: column; padding: 48px 24px 24px; overflow-y: auto; }
.viewer-divider { height: 1px; background: var(--paper-border); margin: 20px 0; }
.viewer-actions-row { gap: 24px; font-size: 13px; color: var(--paper-text2); }
@media (max-width: 800px) { .viewer-context { display: none; } }
</style>
