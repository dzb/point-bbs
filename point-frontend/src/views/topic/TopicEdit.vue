<template>
  <v-card class="mx-auto" max-width="800">
    <v-card-title>编辑帖子</v-card-title>
    <v-card-text>
      <v-text-field v-model="form.title" label="标题" variant="outlined" density="compact" class="mb-3" />
      <v-textarea v-model="form.content" label="内容" rows="12" variant="outlined" density="compact" class="mb-4" />
      <v-btn block color="primary" :loading="submitting" @click="submit">保存修改</v-btn>
      <v-alert v-if="error" type="error" density="compact" class="mt-3">{{ error }}</v-alert>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import client from '@/api/client'

const route = useRoute()
const router = useRouter()
const form = reactive({ title: '', content: '' })
const submitting = ref(false)
const error = ref('')

onMounted(async () => {
  const id = route.params.id
  const { data } = await client.get(`/topics/${id}`)
  if (data.code === 0 && data.data) {
    form.title = data.data.title
    form.content = data.data.content
  }
})

async function submit() {
  submitting.value = true
  try {
    const id = route.params.id
    const { data } = await client.post(`/topics/${id}/edit`, form)
    if (data.code === 0) {
      router.push(`/topics/${id}`)
    } else {
      error.value = data.message
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || '保存失败'
  }
  submitting.value = false
}
</script>
