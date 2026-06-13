<template>
  <v-app>
    <div class="app-shell">
      <AppSidebar :unread="unreadCount" />
      <main class="app-main">
        <header class="main-header" :style="{ borderBottom: '1px solid ' + theme.border }">
          <div class="header-tabs">
            <router-link to="/" class="header-tab" :class="{ active: $route.path === '/' }">推荐</router-link>
            <router-link to="/explore" class="header-tab">探索</router-link>
            <router-link to="/articles" class="header-tab" :class="{ active: $route.path === '/articles' }">文章</router-link>
          </div>
          <div class="header-right">
            <v-text-field v-model="searchQuery" density="compact" hide-details placeholder="搜索..."
              prepend-inner-icon="mdi-magnify" variant="outlined"
              :class="['search-input', { focused: searchFocused }]"
              @focus="searchFocused = true" @blur="searchFocused = false"
              @keyup.enter="doSearch" />
            <v-menu>
              <template #activator="{ props }">
                <v-btn v-bind="props" icon="mdi-palette-outline" variant="text" size="small" :style="{ color: theme.text2 }" />
              </template>
              <v-list density="compact" min-width="140">
                <v-list-item v-for="t in themes" :key="t.key" :active="paperTheme === t.key"
                  @click="setTheme(t.key)" :title="t.label">
                  <template #prepend>
                    <span :style="{ display:'inline-block',width:16,height:16,borderRadius:3,background:t.bg,border:'1px solid '+t.border }" />
                  </template>
                </v-list-item>
              </v-list>
            </v-menu>
            <template v-if="auth.isLoggedIn">
              <v-menu>
                <template #activator="{ props }"><v-btn v-bind="props" icon variant="text" size="small"><UserAvatar :src="auth.user?.avatar" :name="auth.user?.nickname" :size="28" /></v-btn></template>
                <v-list density="compact">
                  <v-list-item :title="auth.user?.nickname" :to="`/users/${auth.user?.id}`" />
                  <v-divider />
                  <v-list-item title="退出登录" @click="auth.logout()" />
                </v-list>
              </v-menu>
            </template>
            <template v-else>
              <v-btn to="/login" class="btn-login" variant="outlined">登录</v-btn>
              <v-btn to="/register" class="btn-register" variant="flat">注册</v-btn>
            </template>
          </div>
        </header>
        <div class="main-content">
          <router-view />
        </div>
      </main>
    </div>
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import AppSidebar from '@/components/AppSidebar.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { usePaperTheme } from '@/composables/usePaperTheme'

const { current: theme, setTheme, themes, activeKey: paperTheme } = usePaperTheme()
const auth = useAuthStore()
const router = useRouter()
const searchQuery = ref('')
const searchFocused = ref(false)
const unreadCount = ref(0)

onMounted(async () => { if (auth.isLoggedIn) await fetchUnread() })
auth.$subscribe(async () => { if (auth.isLoggedIn) await fetchUnread() })

async function fetchUnread() {
  try { const { data } = await client.get(`/users/${auth.user?.id}/messages/unread`); if (data.code===0) unreadCount.value = data.data?.count||0 } catch { /* */ }
}

function doSearch() { if (searchQuery.value.trim()) router.push({ name: 'search', query: { q: searchQuery.value.trim() } }) }
</script>

<style>
html.dark .v-application { background: var(--paper-bg) !important; }
html.dark .v-card { background: #252220 !important; border-color: #3a3530 !important; color: var(--paper-text) !important; }
html.dark .v-list { background: #252220 !important; }
html.dark .v-list-item { color: var(--paper-text) !important; }
html.dark .v-field { color: var(--paper-text) !important; }
html.dark .v-tab { color: var(--paper-text2) !important; }
html.dark .v-tab--selected { color: #c43d3d !important; }
html.dark .v-pagination__item--active { background: #c43d3d !important; }
html.dark .v-divider { border-color: #3a3530 !important; }

.app-shell { display: flex; min-height: 100vh; }
.app-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.main-header { display: flex; align-items: center; justify-content: space-between; padding: 0 48px; height: 48px; position: sticky; top: 0; background: var(--paper-bg); z-index: 10; }
.header-tabs { display: flex; flex-shrink: 0; }
.header-tab { padding: 12px 16px; font-size: 14px; color: var(--paper-text2); text-decoration: none; border-bottom: 2px solid transparent; transition: color .15s, border-color .15s; }
.header-tab:hover { color: var(--paper-text); }
.header-tab.active { color: #c43d3d; border-bottom-color: #c43d3d; }
.header-right { display: flex; align-items: center; gap: 8px; flex-shrink: 1; min-width: 0; }
.main-content { flex: 1; padding: 24px 48px; }
@media (max-width: 1200px) { .main-content { padding: 24px 32px; } .main-header { padding: 0 32px; } }
@media (max-width: 900px)  { .main-content { padding: 20px 16px; } .main-header { padding: 0 16px; } }

.btn-login { color: var(--paper-text) !important; text-transform: none; letter-spacing: 0; font-weight: 400; font-size: 14px; border: 1px solid #c43d3d !important; border-radius: 6px; padding: 0 20px; transition: border-width .15s, box-shadow .15s; }
.btn-login:hover { background: rgba(196,61,61,.04) !important; }
.btn-login:focus-visible { border: none !important; box-shadow: inset 0 0 0 2px #c43d3d; outline: none; }

.btn-register { background: #c43d3d !important; color: #fff !important; text-transform: none; letter-spacing: 0; font-weight: 500; font-size: 14px; border-radius: 6px; padding: 0 20px; transition: background .15s, transform .1s; }
.btn-register:hover { background: #a83434 !important; }
.btn-register:active { background: #8e2626 !important; transform: scale(.97); }
.btn-register:focus-visible { box-shadow: inset 0 0 0 2px rgba(255,255,255,.5), 0 0 0 1px #c43d3d; outline: none; }

.search-input { width: 200px; transition: width .3s ease; }
.search-input.focused { width: 480px; }
.search-input :deep(.v-field) { min-height: 28px !important; }
.search-input :deep(.v-field__input) { font-size: 13px; min-height: 16px !important; padding-top: 0 !important; padding-bottom: 0 !important; }
</style>
