<template>
  <div class="detail-layout">
    <div class="detail-main">
    <v-text-field v-model="form.title" placeholder="文章标题..." variant="plain" hide-details class="title-input mb-3" density="compact" />

    <!-- Markdown toolbar -->
    <div class="md-toolbar">
      <v-btn icon="mdi-format-bold" variant="text" size="small" @click="insertMd('**', '**')" />
      <v-btn icon="mdi-format-italic" variant="text" size="small" @click="insertMd('*', '*')" />
      <v-btn icon="mdi-format-header-2" variant="text" size="small" @click="insertLine('## ')" />
      <v-btn icon="mdi-format-header-3" variant="text" size="small" @click="insertLine('### ')" />
      <v-divider vertical class="mx-1" />
      <v-btn icon="mdi-format-quote-open" variant="text" size="small" @click="insertLine('> ')" />
      <v-btn icon="mdi-code-tags" variant="text" size="small" @click="insertMd('`', '`')" />
      <v-btn icon="mdi-link-variant" variant="text" size="small" @click="insertMd('[', '](url)')" />
      <v-btn icon="mdi-image-outline" variant="text" size="small" @click="insertLine('![](')" />
      <v-divider vertical class="mx-1" />
      <v-btn icon="mdi-table" variant="text" size="small" @click="insertTable" />
      <v-btn icon="mdi-minus" variant="text" size="small" @click="insertLine('---\n')" />
      <v-btn icon="mdi-format-list-bulleted" variant="text" size="small" @click="insertLine('- ')" />
      <v-btn icon="mdi-format-list-numbered" variant="text" size="small" @click="insertLine('1. ')" />
    </div>

    <v-textarea ref="contentArea" v-model="form.content" placeholder="开始写作..." rows="18" variant="plain" hide-details class="content-area" />

    <v-alert v-if="error" type="error" density="compact" class="mt-3" variant="tonal">{{ error }}</v-alert>
    <div class="d-flex justify-end mt-4">
      <v-btn class="post-btn" variant="flat" size="large" :loading="submitting" @click="submit">发布文章</v-btn>
    </div>
    </div><!-- .detail-main -->

    <aside class="detail-aside">
      <p class="aside-tagline">记录思考，分享见闻</p>
      <div class="aside-card"><div class="aside-card-title">Markdown 语法参考</div>
        <div class="aside-card-text" style="font-size:12px;line-height:2">
          **粗体** · *斜体* · ## 标题<br />
          &gt; 引用 · `代码` · [链接](url)<br />
          ![](图片) · --- 分割线<br />
          | 表格 | 列 |
        </div>
      </div>
      <div class="aside-card"><div class="aside-card-title">社区公告</div><div class="aside-card-text">point 正在建设中。</div></div>
    </aside>
  </div><!-- .detail-layout -->
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import client from '@/api/client'

const router = useRouter()
const contentArea = ref<any>(null)
const form = reactive({ title: '', content: '', contentType: 'markdown' as string, summary: '', cover: '', sourceUrl: '', tags: [] as string[] })
const submitting = ref(false)
const error = ref('')

function insertMd(before: string, after: string) {
  const el = contentArea.value?.$el?.querySelector('textarea')
  if (!el) return
  const start = el.selectionStart; const end = el.selectionEnd
  const selected = form.content.substring(start, end)
  form.content = form.content.substring(0, start) + before + selected + after + form.content.substring(end)
  setTimeout(() => { el.selectionStart = start + before.length; el.selectionEnd = end + before.length; el.focus() }, 0)
}

function insertLine(prefix: string) {
  const el = contentArea.value?.$el?.querySelector('textarea')
  if (!el) return
  const start = el.selectionStart
  const lineStart = form.content.lastIndexOf('\n', start - 1) + 1
  form.content = form.content.substring(0, lineStart) + prefix + form.content.substring(lineStart)
  setTimeout(() => { el.selectionStart = el.selectionEnd = lineStart + prefix.length; el.focus() }, 0)
}

function insertTable() {
  const table = '\n| 列1 | 列2 | 列3 |\n| --- | --- | --- |\n|     |     |     |\n'
  const el = contentArea.value?.$el?.querySelector('textarea')
  if (!el) return
  const start = el.selectionStart
  form.content = form.content.substring(0, start) + table + form.content.substring(start)
}

async function submit() {
  if (!form.title || !form.content) { error.value = '标题和内容不能为空'; return }
  submitting.value = true
  try {
    const { data } = await client.post('/articles', form)
    if (data.code === 0) router.push(`/articles/${data.data.id}`)
    else error.value = data.message
  } catch (e: any) { error.value = e.response?.data?.message || '发布失败' }
  submitting.value = false
}
</script>

<style scoped>
.title-input :deep(input) { font-family: 'Noto Serif SC', Georgia, serif; font-size: 28px; font-weight: 700; }
.md-toolbar { display: flex; align-items: center; gap: 2px; padding: 4px 0; border-top: 1px solid var(--paper-border); border-bottom: 1px solid var(--paper-border); margin-bottom: 8px; }
.content-area :deep(textarea) { font-size: 17px; line-height: 1.9; font-family: 'Noto Serif SC', Georgia, serif; }
.post-btn { background: #c43d3d !important; color: #fff !important; text-transform: none; letter-spacing: 0; border-radius: 8px; font-weight: 500; padding: 0 32px; }
.post-btn:hover { background: #a83434 !important; }
.detail-layout { display: flex; }
.detail-main { flex: 1; max-width: 680px; min-width: 0; padding-right: 32px; border-right: 1px solid var(--paper-border); }
.detail-aside { width: 340px; flex-shrink: 0; padding-left: 32px; }
.aside-tagline { font-size: 14px; color: var(--paper-text2); line-height: 1.8; margin-bottom: 20px; }
.aside-card { border: 1px solid var(--paper-border); border-radius: 6px; padding: 12px; margin-bottom: 12px; }
.aside-card-title { font-size: 16px; color: var(--paper-text); font-weight: 500; margin-bottom: 2px; }
.aside-card-text { font-size: 14px; color: var(--paper-text2); line-height: 1.6; }
@media (max-width: 1200px) { .detail-main { padding-right: 20px; } .detail-aside { padding-left: 20px; width: 280px; } }
@media (max-width: 1100px) { .detail-aside { display: none; } .detail-main { border-right: none; padding-right: 0; } }
@media (max-width: 900px) { .detail-main { padding-right: 16px; } }
</style>
