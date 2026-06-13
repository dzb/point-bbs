import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import client from '@/api/client'

interface UserInfo {
  id: number
  nickname: string
  avatar: string
  email: string
  roles: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('point_token'))
  const user = ref<UserInfo | null>(null)
  const isLoggedIn = computed(() => !!token.value)

  async function login(loginName: string, password: string) {
    const { data } = await client.post('/auth/signin', { loginName, password })
    if (data.code === 0) {
      token.value = data.data.token
      user.value = data.data
      localStorage.setItem('point_token', data.data.token)
    }
    return data
  }

  async function register(form: {
    nickname: string
    email: string
    username: string
    password: string
  }) {
    const { data } = await client.post('/auth/signup', form)
    if (data.code === 0) {
      token.value = data.data.token
      user.value = data.data
      localStorage.setItem('point_token', data.data.token)
    }
    return data
  }

  async function fetchCurrentUser() {
    if (!token.value) return
    try {
      const { data } = await client.get('/users/current')
      if (data.code === 0 && data.data) {
        user.value = data.data
      }
    } catch {
      // ignore
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('point_token')
    window.location.href = '/'
  }

  return { token, user, isLoggedIn, login, register, fetchCurrentUser, logout }
})
