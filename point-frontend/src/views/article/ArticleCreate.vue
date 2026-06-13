<template>
  <v-card class="mx-auto" max-width="900">
    <v-card-title>写文章</v-card-title>
    <v-card-text>
      <v-text-field v-model="form.title" label="标题" variant="outlined" density="compact" class="mb-3" />
      <v-text-field v-model="form.summary" label="摘要（选填）" variant="outlined" density="compact" class="mb-3" />
      <v-text-field v-model="form.cover" label="封面图 URL（选填）" variant="outlined" density="compact" class="mb-3" />
      <v-textarea v-model="form.content" label="内容（Markdown）" rows="15" variant="outlined" class="mb-3" />
      <v-text-field v-model="form.sourceUrl" label="原文链接（选填）" variant="outlined" density="compact" class="mb-3" />
      <v-combobox v-model="form.tags" label="标签" multiple chips variant="outlined" density="compact" class="mb-4" />
      <v-btn block color="primary" :loading="submitting" @click="submit">发布</v-btn>
      <v-alert v-if="error" type="error" density="compact" class="mt-3">{{ error }}</v-alert>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import client from '@/api/client'

const router = useRouter()
const form = reactive({ title: '', summary: '', content: '', cover: '', sourceUrl: '', tags: [] as string[], contentType: 'markdown' })
const submitting = ref(false)
const error = ref('')

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
