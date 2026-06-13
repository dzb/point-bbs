<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h1 class="text-h5">通知中心</h1>
      <v-spacer />
      <v-btn v-if="messages.length > 0" variant="text" size="small" @click="markAllRead">全部已读</v-btn>
    </div>

    <v-card v-for="m in messages" :key="m.id" class="mb-2" :class="{ 'bg-grey-lighten-4': m.status === 0 }">
      <v-card-item>
        <template #prepend>
          <v-icon :color="m.status === 0 ? 'primary' : ''">
            {{ iconForType(m.type) }}
          </v-icon>
        </template>
        <v-card-title class="text-body-2">{{ m.title }}</v-card-title>
        <v-card-subtitle class="text-caption">
          {{ m.content }}
          <span v-if="m.quoteContent" class="text-grey"> — {{ m.quoteContent }}</span>
          <div class="text-grey">{{ formatTime(m.createTime) }}</div>
        </v-card-subtitle>
      </v-card-item>
    </v-card>

    <div v-if="messages.length === 0 && !loading" class="text-center py-8 text-grey">暂无通知</div>
    <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'

const auth = useAuthStore()
const messages = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await client.get(`/users/${auth.user?.id}/messages`, { params: { page: 1, pageSize: 50 } })
    if (data.code === 0) messages.value = data.data || []
  } catch { /* ignore */ }
  loading.value = false
})

async function markAllRead() {
  await client.post(`/users/${auth.user?.id}/messages/read`, { ids: [] })
  messages.value.forEach(m => (m.status = 1))
}

function iconForType(type: number): string {
  return ['mdi-comment-outline', 'mdi-heart-outline', 'mdi-account-plus-outline'][type] || 'mdi-bell-outline'
}

function formatTime(ts: number) {
  return ts ? new Date(ts).toLocaleString('zh-CN') : ''
}
</script>
