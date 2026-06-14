<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link to="/" class="auth-logo">point</router-link>
      <p class="auth-subtitle">创建你的账号</p>

      <v-text-field v-model="form.nickname" label="昵称" variant="outlined" class="mb-3" hide-details="auto" />
      <v-text-field v-model="form.email" label="邮箱" variant="outlined" class="mb-3" hide-details="auto" />
      <v-text-field v-model="form.username" label="用户名" variant="outlined" class="mb-3" hide-details="auto" />
      <v-text-field v-model="form.password" label="密码" type="password" variant="outlined" class="mb-2" hide-details="auto" />

      <div v-if="error" class="auth-error">{{ error }}</div>

      <v-btn block :loading="loading" @click="doRegister" class="auth-btn" variant="flat" size="large" :ripple="false">
        注册
      </v-btn>

      <p class="auth-link">
        <router-link to="/login">已有账号？立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ nickname: '', email: '', username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function doRegister() {
  if (!form.nickname || !form.email || !form.password) { error.value = '请填写完整信息'; return }
  loading.value = true; error.value = ''
  try {
    const res = await auth.register({ ...form })
    if (res.code === 0) router.push('/')
    else error.value = res.message || '注册失败'
  } catch (e: any) { error.value = e.response?.data?.message || '注册失败' }
  loading.value = false
}
</script>

