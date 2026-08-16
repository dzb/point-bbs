import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import client from '@/api/client'
import router from '@/router'
import type { AuthResult, UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  // The session lives in an HttpOnly cookie; the store only mirrors it.
  const token = ref<string | null>(null)
  const user = ref<UserInfo | null>(null)
  const permissions = ref<string[]>([])
  const sessionChecked = ref(false)
  const isLoggedIn = computed(() => sessionChecked.value && user.value != null)
  const isAdmin = computed(() => permissions.value.includes('admin'))

  async function login(loginName: string, password: string) {
    const { data } = await client.post<{ data: AuthResult } & { code: number; message: string }>('/auth/signin', { loginName, password })
    if (data.code === 0) {
      token.value = data.data.token
      await fetchCurrentUser()
    }
    return data
  }

  async function register(form: { nickname: string; email: string; username: string; password: string }) {
    const { data } = await client.post('/auth/signup', form)
    if (data.code === 0) {
      token.value = data.data.token
      await fetchCurrentUser()
    }
    return data
  }

  /** Boot/session probe: /users/current returns 200+user when the cookie
   *  session is valid and 200+null otherwise. */
  async function fetchCurrentUser() {
    try {
      const { data } = await client.get('/users/current')
      if (data.code === 0 && data.data) {
        user.value = data.data
        await fetchPermissions()
        sessionChecked.value = true
        return data.data
      }
    } catch (e) {
      console.error('fetch current user failed', e)
    }
    sessionChecked.value = true
    return null
  }

  async function fetchPermissions() {
    try {
      const { data } = await client.get('/users/current/permissions')
      if (data.code === 0) permissions.value = data.data || []
      else permissions.value = []
    } catch (e) {
      console.error('fetch permissions failed', e)
      permissions.value = []
    }
  }

  async function logout() {
    try {
      await client.post('/auth/signout')
    } catch { /* cookie clearing is best-effort */ }
    token.value = null
    user.value = null
    permissions.value = []
    sessionChecked.value = true
    router.push('/')
  }

  return {
    token,
    user,
    permissions,
    sessionChecked,
    isLoggedIn,
    isAdmin,
    login,
    register,
    fetchCurrentUser,
    fetchPermissions,
    logout,
  }
})
