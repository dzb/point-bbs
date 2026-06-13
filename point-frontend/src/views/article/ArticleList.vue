<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h1 class="text-h5">文章</h1>
      <v-spacer />
      <v-btn color="primary" to="/articles/create" v-if="auth.isLoggedIn">写文章</v-btn>
    </div>

    <v-row>
      <v-col v-for="a in articles" :key="a.id" cols="12" md="4">
        <v-card class="h-100" @click="$router.push(`/articles/${a.id}`)">
          <v-img v-if="a.cover" :src="a.cover" height="160" cover />
          <v-card-item>
            <v-card-title class="text-body-1">{{ a.title }}</v-card-title>
            <v-card-subtitle class="text-caption">
              {{ a.summary || a.content?.substring(0, 100) }}
            </v-card-subtitle>
            <template #append>
              <div class="text-caption text-grey mt-1">
                {{ a.viewCount }} 阅读 · {{ a.commentCount }} 评论 · {{ formatTime(a.createTime) }}
              </div>
            </template>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <div v-if="articles.length === 0 && !loading" class="text-center py-8 text-grey">暂无文章</div>
    <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'

const auth = useAuthStore()
const articles = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  const { data } = await client.get('/articles', { params: { pageSize: 50 } })
  if (data.code === 0) articles.value = data.data || []
  loading.value = false
})

function formatTime(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>
