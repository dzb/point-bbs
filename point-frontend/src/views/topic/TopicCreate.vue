<template>
  <v-card class="mx-auto" max-width="800">
    <v-card-title>发布新帖</v-card-title>
    <v-card-text>
      <v-text-field v-model="form.title" label="标题" variant="outlined" density="compact" class="mb-3" counter="128" />
      <v-select v-model="form.categoryId" label="分类" :items="categories" item-title="name" item-value="id"
        variant="outlined" density="compact" class="mb-3" />
      <v-textarea v-model="form.content" label="内容 (支持 Markdown)" rows="12" variant="outlined"
        density="compact" class="mb-3" />
      <v-combobox v-model="form.tags" label="标签 (回车添加)" multiple chips variant="outlined"
        density="compact" class="mb-4" />
      <v-btn block color="primary" :loading="submitting" @click="submit">发布</v-btn>
      <v-alert v-if="error" type="error" density="compact" class="mt-3">{{ error }}</v-alert>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import client from '@/api/client'

const router = useRouter()
const form = reactive({
  title: '',
  content: '',
  type: 0,
  categoryId: null as number | null,
  tags: [] as string[],
  contentType: 'markdown',
})
const categories = ref<any[]>([])
const submitting = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    const { data } = await client.get('/categories')
    if (data.code === 0) categories.value = data.data || []
  } catch { console.error('api error') }
})

async function submit() {
  if (!form.title || !form.content) {
    error.value = '标题和内容不能为空'
    return
  }
  submitting.value = true
  try {
    const { data } = await client.post('/topics', form)
    if (data.code === 0) {
      router.push(`/topics/${data.data.id}`)
    } else {
      error.value = data.message
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || '发布失败'
  }
  submitting.value = false
}
</script>
