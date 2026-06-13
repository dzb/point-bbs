<template>
  <div class="home-layout">
    <div class="home-feed">
      <!-- Composer -->
      <div v-if="auth.isLoggedIn" class="composer mb-4">
        <div class="d-flex">
          <v-avatar size="36" class="mr-3 mt-1 flex-shrink-0"><v-icon>mdi-pen</v-icon></v-avatar>
          <div class="flex-grow-1" style="min-width:0">
            <v-textarea v-model="newMoment" placeholder="记录思考，分享见闻..." rows="2" variant="plain" hide-details density="compact" @paste="onPaste" class="composer-input" />
            <div v-if="images.length" class="composer-images">
              <div v-for="(img,i) in images" :key="i" class="composer-img">
                <img :src="img.url" />
                <v-btn icon="mdi-close" variant="flat" size="x-small" class="img-remove-btn" @click="images.splice(i,1)" />
              </div>
              <div v-if="uploading" class="composer-img uploading">
                <v-progress-circular indeterminate size="20" color="#c43d3d" />
              </div>
            </div>
            <div class="d-flex align-center mt-2 composer-bar">
              <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
              <v-btn icon="mdi-image-outline" variant="text" size="small" :loading="uploading" @click="triggerUpload" :style="{color:'var(--paper-text2)'}" />
              <v-spacer />
              <v-btn class="post-btn" variant="flat" size="small" :loading="posting" @click="postMoment">发布</v-btn>
            </div>
          </div>
        </div>
      </div>

      <!-- Moments -->
      <div class="moments-list">
        <MomentCard v-for="m in moments" :key="m.id" :moment="m" @toggle-like="toggleLike" />
      </div>
      <div v-if="moments.length===0 && !loading" class="text-center py-16" style="color:var(--paper-text2)">暂无随想</div>
      <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" color="#c43d3d" />
    </div>

    <!-- Right aside -->
    <aside class="home-aside">
      <p class="aside-tagline">记录思考，分享见闻</p>
      <div class="aside-card"><div class="aside-card-title">社区公告</div>
        <div class="aside-card-text">欢迎来到 point — 一个安静的思考角落。记录随想，发表文章，关注有趣的人。</div>
      </div>
      <div class="aside-card"><div class="aside-card-title">推荐阅读</div><div class="aside-card-text">暂无推荐</div></div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import MomentCard from '@/components/MomentCard.vue'

const auth = useAuthStore()
const moments = ref<any[]>([])
const newMoment = ref('')
const posting = ref(false)
const uploading = ref(false)
const loading = ref(true)
const images = ref<{ url: string }[]>([])
const fileInput = ref<HTMLInputElement>()

function triggerUpload() { fileInput.value?.click() }

async function uploadImage(file: Blob): Promise<string> {
  uploading.value = true
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = async () => {
      const base64 = reader.result as string
      const { data } = await client.post('/upload', base64, {
        headers: { 'Content-Type': 'text/plain' }
      })
      resolve(data.code === 0 && data.data?.url ? data.data.url : '')
    }
    reader.readAsDataURL(file)
  }).finally(() => { uploading.value = false })
}

async function onPaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const blob = item.getAsFile()
      if (blob) { const url = await uploadImage(blob); if (url) images.value.push({ url }) }
    }
  }
}

async function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const url = await uploadImage(file)
  if (url) images.value.push({ url })
  if (fileInput.value) fileInput.value.value = ''
}

onMounted(async () => {
  try {
    const { data } = await client.get('/topics/moments', { params: { pageSize: 50 } })
    if (data.code === 0) moments.value = (data.data || []).map((m: any) => ({ ...m, liked: false }))
  } catch { /* */ }
  loading.value = false
})

async function postMoment() {
  if (!newMoment.value.trim() && !images.value.length) return
  posting.value = true
  let content = newMoment.value
  if (images.value.length) {
    const imgs = images.value.map(i => `![](${i.url})`).join('\n')
    content = content ? content + '\n' + imgs : imgs
  }
  try { await client.post('/topics', { title: '', content, type: 1 }) } catch { /* */ }
  posting.value = false
  newMoment.value = ''
  images.value = []
  const { data } = await client.get('/topics/moments', { params: { pageSize: 50 } })
  if (data.code === 0) moments.value = (data.data || []).map((m: any) => ({ ...m, liked: false }))
}

async function toggleLike(m: any) {
  if (m.liked) { await client.post(`/topics/${m.id}/unlike`); m.liked = false; m.likeCount-- }
  else { await client.post(`/topics/${m.id}/like`); m.liked = true; m.likeCount++ }
}
</script>

<style scoped>
.home-layout { display: flex; }
.home-feed { flex: 1; max-width: 680px; min-width: 0; padding-right: 48px; border-right: 1px solid var(--paper-border); transition: padding .2s ease; }
.home-aside { width: 340px; flex-shrink: 0; padding-left: 6px; transition: width .2s ease, padding .2s ease, opacity .2s ease; }
.moments-list { display: flex; flex-direction: column; gap: 10px; }
.aside-tagline { font-size: 14px; color: var(--paper-text2); line-height: 1.8; margin-bottom: 20px; }
.aside-card { border: 1px solid var(--paper-border); border-radius: 6px; padding: 12px; margin-bottom: 12px; }
.aside-card-title { font-size: 16px; color: var(--paper-text); font-weight: 500; margin-bottom: 2px; }
.aside-card-text { font-size: 14px; color: var(--paper-text2); line-height: 1.6; }
.composer { border: 1px solid var(--paper-border); border-radius: 12px; padding: 16px; background: var(--paper-bg); box-shadow: 0 2px 12px rgba(0,0,0,.06); }
.composer-input :deep(.v-field) { border: none !important; }
.composer-input :deep(.v-field__input) { padding-top: 4px !important; padding-bottom: 4px !important; min-height: 40px !important; font-size: 15px; }
.composer-images { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 6px; margin-top: 8px; }
.composer-img { position: relative; border-radius: 8px; overflow: hidden; aspect-ratio: 1; }
.composer-img img { width: 100%; height: 100%; object-fit: cover; }
.composer-img.uploading { display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.03); }
.img-remove-btn { opacity: 0; position: absolute; top: 4px; right: 4px; transition: opacity .15s; background: rgba(0,0,0,.5) !important; color: #fff !important; }
.composer-img:hover .img-remove-btn { opacity: 1; }
.post-btn { background: #c43d3d !important; color: #fff !important; text-transform: none; letter-spacing: 0; border-radius: 20px; padding: 0 20px; font-weight: 500; }
.post-btn:hover { background: #a83434 !important; }
@media (max-width: 1300px) { .home-aside { width: 300px; } .home-feed { padding-right: 36px; } }
@media (max-width: 1200px) { .home-aside { width: 260px; } .home-feed { padding-right: 28px; } }
@media (max-width: 1100px) { .home-aside { width: 0; opacity: 0; overflow: hidden; } .home-feed { border-right: none; padding-right: 0; } }
@media (max-width: 900px)  { .home-feed { padding-right: 16px; } }
</style>
