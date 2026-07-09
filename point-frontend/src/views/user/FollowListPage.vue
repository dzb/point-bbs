<template>
  <div class="detail-main">
    <BackButton />
    <h2 class="text-h6 mb-4" style="font-family:'Noto Serif SC',Georgia,serif;font-weight:700;color:var(--paper-text)">
      {{ kind === 'followers' ? '粉丝' : '关注' }}
    </h2>
    <div v-for="u in users" :key="u.id" class="user-row" @click="goUser(u.id)">
      <UserAvatar :src="u.avatar" :name="u.nickname" :size="40" class="mr-3 flex-shrink-0" />
      <div class="flex-grow-1" style="min-width:0">
        <div style="font-size:15px;font-weight:500;color:var(--paper-text)">{{ u.nickname }}</div>
        <div style="font-size:12px;color:var(--paper-text2);overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ u.description || '@' + (u.username || u.id) }}</div>
      </div>
    </div>
    <div v-if="users.length===0 && !loading" class="text-center py-16" style="color:var(--paper-text2)">还没有</div>
    <v-progress-circular v-if="loading" indeterminate class="d-block mx-auto mt-8" color="var(--paper-accent)" />
    <LoadMore :has-more="hasMore" :loading="loadingMore" @load-more="loadMore" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import client from '@/api/client'
import UserAvatar from '@/components/UserAvatar.vue'
import BackButton from '@/components/BackButton.vue'
import LoadMore from '@/components/LoadMore.vue'

const route = useRoute()
const router = useRouter()
const kind = computed(() => route.path.endsWith('/followers') ? 'followers' : 'following')

const users = ref<any[]>([])
const loading = ref(true)
const page = ref(1); const pageSize = 30
const hasMore = ref(false); const loadingMore = ref(false)

onMounted(() => loadItems())

async function loadItems(reset = false) {
  if (reset) page.value = 1
  const id = route.params.id as string
  try {
    const { data } = await client.get(`/users/${id}/${kind.value}`, { params: { page: page.value, pageSize } })
    if (data.code === 0) {
      const payload = data.data || {}
      const items = payload.items || []
      users.value = page.value === 1 ? items : [...users.value, ...items]
      hasMore.value = (payload.items || []).length < (payload.total ?? 0)
    }
  } catch { /* ignore */ }
  loading.value = false
}

function loadMore() { page.value++; loadingMore.value = true; loadItems(); loadingMore.value = false }
function goUser(id: number) { router.push(`/users/${id}`) }
</script>

<style scoped>
.user-row { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--paper-border); cursor: pointer; }
.user-row:hover { background: rgba(0,0,0,.01); }
</style>
