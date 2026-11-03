<template>
  <div class="detail-main">
    <BackButton />
    <h2
      class="text-h6 mb-4"
      style="font-family: 'Noto Serif SC', Georgia, serif; font-weight: 700; color: var(--paper-text)"
    >
      编辑资料
    </h2>

    <v-form class="edit-form" @submit.prevent="save">
      <v-text-field
        v-model="form.nickname"
        label="昵称"
        variant="outlined"
        density="compact"
        hide-details
        class="mb-3"
      />
      <v-text-field
        v-model="form.avatar"
        label="头像 URL"
        variant="outlined"
        density="compact"
        hide-details
        class="mb-3"
      />
      <v-text-field v-model="form.gender" label="性别" variant="outlined" density="compact" hide-details class="mb-3" />
      <v-textarea
        v-model="form.description"
        label="简介"
        variant="outlined"
        density="compact"
        hide-details
        rows="3"
        class="mb-3"
      />
      <v-text-field
        v-model="form.homePage"
        label="个人主页"
        variant="outlined"
        density="compact"
        hide-details
        class="mb-3"
      />
      <v-text-field
        v-model="form.backgroundImage"
        label="背景图 URL"
        variant="outlined"
        density="compact"
        hide-details
        class="mb-3"
      />

      <v-alert v-if="error" type="error" density="compact" class="mb-3" variant="tonal">{{ error }}</v-alert>

      <div class="d-flex justify-end" style="gap: 8px">
        <v-btn variant="text" style="text-transform: none; letter-spacing: 0" @click="router.back()">取消</v-btn>
        <v-btn
          class="save-btn"
          variant="flat"
          :loading="saving"
          type="submit"
          style="text-transform: none; letter-spacing: 0"
        >
          保存
        </v-btn>
      </div>
    </v-form>

    <v-divider class="my-8" />

    <h3
      class="text-subtitle-1 mb-3"
      style="font-family: 'Noto Serif SC', Georgia, serif; font-weight: 700; color: var(--paper-text)"
    >
      修改密码
    </h3>

    <v-form class="edit-form" @submit.prevent="changePassword">
      <v-text-field
        v-model="pwd.oldPassword"
        label="当前密码"
        type="password"
        variant="outlined"
        density="compact"
        hide-details
        class="mb-3"
        autocomplete="current-password"
      />
      <v-text-field
        v-model="pwd.newPassword"
        label="新密码（至少 6 位）"
        type="password"
        variant="outlined"
        density="compact"
        hide-details
        class="mb-3"
        autocomplete="new-password"
      />
      <v-text-field
        v-model="pwd.confirmPassword"
        label="确认新密码"
        type="password"
        variant="outlined"
        density="compact"
        hide-details
        class="mb-3"
        autocomplete="new-password"
      />

      <v-alert v-if="pwdError" type="error" density="compact" class="mb-3" variant="tonal">{{ pwdError }}</v-alert>
      <v-alert v-if="pwdSuccess" type="success" density="compact" class="mb-3" variant="tonal">
        {{ pwdSuccess }}
      </v-alert>

      <div class="d-flex justify-end">
        <v-btn
          class="save-btn"
          variant="flat"
          :loading="pwdSaving"
          type="submit"
          style="text-transform: none; letter-spacing: 0"
        >
          确认修改
        </v-btn>
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
if (!auth.isLoggedIn) {
  router.replace('/login')
}

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
  } catch {
    /* ignore */
  }
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
  } catch (e: any) {
    error.value = e.response?.data?.message || '保存失败'
  }
  saving.value = false
}

// -- change password --

const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdSaving = ref(false)
const pwdError = ref('')
const pwdSuccess = ref('')

async function changePassword() {
  pwdError.value = ''
  pwdSuccess.value = ''
  if (!pwd.oldPassword) {
    pwdError.value = '请输入当前密码'
    return
  }
  if (pwd.newPassword.length < 6) {
    pwdError.value = '新密码长度至少 6 位'
    return
  }
  if (pwd.newPassword !== pwd.confirmPassword) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  pwdSaving.value = true
  try {
    const { data } = await client.post('/users/password', {
      oldPassword: pwd.oldPassword,
      newPassword: pwd.newPassword,
    })
    if (data.code === 0) {
      // The backend clears the session cookie with the change; drop the
      // local session too and sign in again with the new password.
      pwdSuccess.value = '密码已修改，请使用新密码重新登录'
      pwd.oldPassword = ''
      pwd.newPassword = ''
      pwd.confirmPassword = ''
      auth.token = null
      auth.user = null
      auth.permissions = []
      setTimeout(() => router.push('/login'), 1200)
    } else {
      pwdError.value = data.message
    }
  } catch (e: any) {
    pwdError.value = e.response?.data?.message || '修改失败'
  }
  pwdSaving.value = false
}
</script>

<style scoped>
.edit-form {
  max-width: 480px;
}
.save-btn {
  background: var(--paper-accent) !important;
  color: #fff !important;
  border-radius: 8px;
  padding: 0 24px;
}
.save-btn:hover {
  background: var(--paper-accent-hover) !important;
}
</style>
