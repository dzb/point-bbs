<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link to="/" class="auth-logo">point</router-link>
      <p class="auth-subtitle">登录你的账号</p>

      <v-text-field v-model="loginName" placeholder="邮箱或用户名" variant="outlined" class="mb-3" hide-details="auto" />
      <v-text-field v-model="password" placeholder="密码" type="password" variant="outlined" class="mb-2" hide-details="auto"
        @keyup.enter="doLogin" />

      <div v-if="error" class="auth-error">{{ error }}</div>

      <v-btn block :loading="loading" @click="doLogin" class="auth-btn" variant="flat" size="large" :ripple="false">
        登录
      </v-btn>

      <v-divider class="my-4" />
      <v-btn block variant="outlined" class="github-btn" :loading="ghLoading" @click="githubLogin">
        <v-icon size="18" class="mr-2">mdi-github</v-icon> 使用 GitHub 登录
      </v-btn>

      <p class="auth-link">
        <router-link to="/register">还没有账号？立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loginName = ref('')
const password = ref('')
const loading = ref(false)
const ghLoading = ref(false)
const error = ref('')

// Handle OAuth callback — check for token in URL
onMounted(() => {
  const token = route.query.token as string | undefined
  if (token) {
    localStorage.setItem('point_token', token)
    auth.token = token
    auth.fetchCurrentUser()
    router.replace('/')
  }
})

async function githubLogin() {
  ghLoading.value = true
  try {
    const { data } = await client.get('/auth/github/authorize')
    if (data.code === 0 && data.data?.url) {
      window.location.href = data.data.url
    } else error.value = '获取 GitHub 授权地址失败'
  } catch { error.value = 'GitHub 登录请求失败' }
  ghLoading.value = false
}

async function doLogin() {
  if (!loginName.value || !password.value) { error.value = '请填写完整信息'; return }
  loading.value = true; error.value = ''
  try {
    const res = await auth.login(loginName.value, password.value)
    if (res.code === 0) router.push('/')
    else error.value = res.message || '登录失败'
  } catch (e: any) { error.value = e.response?.data?.message || '登录失败' }
  loading.value = false
}
</script>

