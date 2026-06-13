<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link to="/" class="auth-logo">point</router-link>
      <p class="auth-subtitle">登录你的账号</p>

      <v-text-field v-model="loginName" label="邮箱或用户名" variant="outlined" class="mb-3" hide-details="auto" />
      <v-text-field v-model="password" label="密码" type="password" variant="outlined" class="mb-2" hide-details="auto"
        @keyup.enter="doLogin" />

      <div v-if="error" class="auth-error">{{ error }}</div>

      <v-btn block :loading="loading" @click="doLogin" class="auth-btn" variant="flat" size="large" :ripple="false">
        登录
      </v-btn>

      <p class="auth-link">
        <router-link to="/register">还没有账号？立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loginName = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

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

<style scoped>
.auth-page { min-height: 80vh; display: flex; align-items: center; justify-content: center; }
.auth-card { width: 380px; background: var(--paper-bg); border-radius: 12px; padding: 40px 36px 32px; border-top: 3px solid #c43d3d; box-shadow: 0 1px 3px rgba(0,0,0,.04), 0 4px 16px rgba(0,0,0,.04); }
.auth-logo { display: block; text-align: center; font-family: 'Noto Serif SC', 'Source Han Serif SC', Georgia, serif; font-size: 28px; font-weight: 700; color: var(--paper-text); text-decoration: none; margin-bottom: 6px; }
.auth-subtitle { text-align: center; font-size: 14px; color: var(--paper-text2); margin-bottom: 28px; }
html.dark .auth-card { background: #252220; }
.auth-btn { background: #c43d3d !important; color: #fff !important; font-weight: 500; text-transform: none; letter-spacing: 0; border-radius: 6px; margin-top: 8px; }
.auth-btn:hover { background: #a83434 !important; }
.auth-error { font-size: 13px; color: #c43d3d; background: rgba(196,61,61,.06); padding: 8px 12px; border-radius: 6px; margin-bottom: 4px; border-left: 2px solid #c43d3d; }
.auth-link { text-align: center; margin-top: 20px; font-size: 13px; }
.auth-link a { color: #c43d3d; text-decoration: none; }
</style>
