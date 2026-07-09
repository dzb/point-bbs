<template>
  <div class="detail-main">
    <BackButton />
    <h2 class="text-h6 mb-4" style="font-family:'Noto Serif SC',Georgia,serif;font-weight:700;color:var(--paper-text)">编辑资料</h2>

    <v-form @submit.prevent="save" class="edit-form">
      <v-text-field v-model="form.nickname" label="昵称" variant="outlined" density="compact" hide-details class="mb-3" />
      <v-text-field v-model="form.avatar" label="头像 URL" variant="outlined" density="compact" hide-details class="mb-3" />
      <v-text-field v-model="form.gender" label="性别" variant="outlined" density="compact" hide-details class="mb-3" />
      <v-textarea v-model="form.description" label="简介" variant="outlined" density="compact" hide-details rows="3" class="mb-3" />
      <v-text-field v-model="form.homePage" label="个人主页" variant="outlined" density="compact" hide-details class="mb-3" />
      <v-text-field v-model="form.backgroundImage" label="背景图 URL" variant="outlined" density="compact" hide-details class="mb-3" />

      <v-alert v-if="error" type="error" density="compact" class="mb-3" variant="tonal">{{ error }}</v-alert>

      <div class="d-flex justify-end" style="gap:8px">
        <v-btn variant="text" @click="router.back()" style="text-transform:none;letter-spacing:0">取消</v-btn>
        <v-btn class="save-btn" variant="flat" :loading="saving" type="submit" style="text-transform:none;letter-spacing:0">保存</v-btn>
      </div>
    </v-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import BackButton from '@/components/BackButton.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
if (!auth.isLoggedIn) { router.replace('/login') }

const form = reactive({ nickname: '', avatar: '', gender: '', description: '', homePage: '', backgroundImage: '' })
const saving = ref(false)
const error = ref('')

onMounted(async () => {
  const id = route.params.id as string
  try {
    const { data } = await client.get(`/users/${id}`)
    if (data.code === 0 && data.data) {
      const u = data.data
      form.nickname = u.nickname || ''
      form.avatar = u.avatar || ''
      form.gender = u.gender || ''
      form.description = u.description || ''
      form.homePage = u.homePage || ''
      form.backgroundImage = u.backgroundImage || ''
    }
  } catch { /* ignore */ }
})

async function save() {
  saving.value = true
  const id = route.params.id as string
  try {
    const { data } = await client.post(`/users/edit/${id}`, { ...form })
    if (data.code === 0) {
      // Refresh cached user if editing self
      if (auth.user && String(auth.user.id) === id) auth.fetchCurrentUser()
      router.push(`/users/${id}`)
    } else error.value = data.message
  } catch (e: any) { error.value = e.response?.data?.message || '保存失败' }
  saving.value = false
}
</script>

<style scoped>
.edit-form { max-width: 480px; }
.save-btn { background: var(--paper-accent) !important; color: #fff !important; border-radius: 8px; padding: 0 24px; }
.save-btn:hover { background: var(--paper-accent-hover) !important; }
</style>
