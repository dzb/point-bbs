<template>
  <v-app>
    <div class="app-shell">
      <!-- Full-width top header -->
      <header class="top-header">
        <div class="header-logo-area">
          <router-link to="/" class="header-logo">
            <span class="logo-mark">聚</span>
            <span class="logo-text">point</span>
          </router-link>
        </div>
        <nav class="header-tabs">
          <router-link to="/" class="header-tab" :class="{ active: $route.path === '/' }">推荐</router-link>
          <router-link to="/following" class="header-tab" :class="{ active: $route.path === '/following' }">关注</router-link>
          <router-link to="/explore" class="header-tab" :class="{ active: $route.path === '/explore' }">探索</router-link>
          <router-link to="/articles" class="header-tab" :class="{ active: $route.path === '/articles' }">文章</router-link>
        </nav>
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

      <!-- Three-column body -->
      <div class="app-body">
        <AppSidebar :unread="unreadCount" />
        <main class="app-main">
          <router-view />
        </main>
        <aside class="app-aside">
          <router-view name="aside" />
        </aside>
      </div>
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
/* Accent — universal vermilion */
:root {
  --paper-accent: #c43d3d;
  --paper-accent-hover: #a83434;
  --paper-accent-active: #8e2626;
}

/* Dark mode overrides */
html.dark .v-application { background: var(--paper-bg) !important; }
html.dark .v-card { background: #252220 !important; border-color: #3a3530 !important; color: var(--paper-text) !important; }
html.dark .v-list { background: #252220 !important; }
html.dark .v-list-item { color: var(--paper-text) !important; }
html.dark .v-field { color: var(--paper-text) !important; }
html.dark .v-tab { color: var(--paper-text2) !important; }
html.dark .v-tab--selected { color: var(--paper-accent) !important; }
html.dark .v-pagination__item--active { background: var(--paper-accent) !important; }
html.dark .v-divider { border-color: #3a3530 !important; }

/* === Shared component styles === */

/* Aside cards (PageAside, TopicAside, CreateAside) */
.aside-tagline { font-size: 18px; font-weight: 600; color: var(--paper-text); line-height: 1.6; margin-bottom: 20px; font-family: 'Noto Serif SC', Georgia, serif; }
.aside-card { border: 1px solid var(--paper-border); border-radius: 6px; padding: 12px; margin-bottom: 12px; }
.aside-card-title { font-size: 16px; color: var(--paper-text); font-weight: 500; margin-bottom: 2px; }
.aside-card-text { font-size: 14px; color: var(--paper-text2); line-height: 1.6; }

/* Auth pages */
.auth-page { min-height: 80vh; display: flex; align-items: center; justify-content: center; }
.auth-card { width: 380px; background: var(--paper-bg); border-radius: 12px; padding: 40px 36px 32px; border-top: 3px solid var(--paper-accent); box-shadow: 0 1px 3px rgba(0,0,0,.04), 0 4px 16px rgba(0,0,0,.04); }
html.dark .auth-card { background: #252220; }
.auth-logo { display: block; text-align: center; font-family: 'Noto Serif SC', 'Source Han Serif SC', Georgia, serif; font-size: 28px; font-weight: 700; color: var(--paper-text); text-decoration: none; margin-bottom: 6px; }
.auth-subtitle { text-align: center; font-size: 14px; color: var(--paper-text2); margin-bottom: 28px; }
.auth-btn { background: var(--paper-accent) !important; color: #fff !important; font-weight: 500; text-transform: none; letter-spacing: 0; border-radius: 6px; margin-top: 8px; }
.auth-btn:hover { background: var(--paper-accent-hover) !important; }
.auth-error { font-size: 13px; color: var(--paper-accent); background: rgba(196,61,61,.06); padding: 8px 12px; border-radius: 6px; margin-bottom: 4px; border-left: 2px solid var(--paper-accent); }
.auth-link { text-align: center; margin-top: 20px; font-size: 13px; }
.auth-link a { color: var(--paper-accent); text-decoration: none; }

/* Image grid (shared across feed cards and detail pages) */
.img-grid { display: grid; gap: 4px; margin: 8px 0; max-height: 420px; overflow: hidden; border-radius: 8px; }
.img-grid.cols-2 { grid-template-columns: 1fr 1fr; }
.img-grid.cols-3 { grid-template-columns: 1fr 1fr; }
.img-grid.cols-3 img:first-child { grid-row: span 2; }
.img-grid.cols-4 { grid-template-columns: 1fr 1fr; }
.img-grid img { width: 100%; height: 100%; object-fit: cover; aspect-ratio: 1; border: 1px solid var(--paper-border); }

/* Feed / detail content containers */
.home-feed, .detail-main { max-width: 680px; min-width: 0; }
.moments-list { display: flex; flex-direction: column; gap: 10px; }

/* Shell */
.app-shell { display: flex; flex-direction: column; min-height: 100vh; }

/* Top header — full width */
.top-header {
  display: flex; align-items: center;
  height: 46px;
  background: var(--paper-nav); border-bottom: 1px solid var(--paper-nav);
  position: sticky; top: 0; z-index: 10;
}
.header-logo-area { width: 240px; flex-shrink: 0; display: flex; align-items: center; padding-left: 12px; }
.header-right { display: flex; align-items: center; gap: 8px; flex-shrink: 1; min-width: 0; margin-left: auto; padding-right: 32px; }

/* Logo */
.header-logo { display: flex; align-items: center; gap: 8px; text-decoration: none; }
.logo-mark { width: 30px; height: 30px; background: var(--paper-accent); color: #fff; border-radius: 6px;
  display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700;
  font-family: 'Noto Serif SC', Georgia, serif; }
.logo-text { font-family: 'Noto Serif SC', Georgia, serif; font-size: 20px; font-weight: 700; color: var(--paper-text); }

/* Header tabs — align with content area */
.header-tabs { display: flex; padding-left: 32px; }
.header-tab { padding: 14px 16px 6px; font-size: 18px; color: var(--paper-text2); text-decoration: none;
  border-bottom: 2px solid transparent; transition: color .15s, border-color .15s, font-size .15s, padding .15s; white-space: nowrap; }
.header-tab:hover { color: var(--paper-text); }
.header-tab.active { color: var(--paper-accent); border-bottom-color: var(--paper-accent); }

/* Search */
.search-input { width: 200px; transition: width .3s ease; }
.search-input.focused { width: 480px; }
.search-input :deep(.v-field) { min-height: 24px !important; }
.search-input :deep(.v-field__input) { font-size: 13px; min-height: 15px !important; padding-top: 0 !important; padding-bottom: 0 !important; }

/* Buttons */
.btn-login { color: var(--paper-text) !important; text-transform: none; letter-spacing: 0; font-weight: 400; font-size: 14px; border: 1px solid var(--paper-accent) !important; border-radius: 6px; padding: 0 20px; transition: border-width .15s, box-shadow .15s; }
.btn-login:hover { background: rgba(196,61,61,.04) !important; }
.btn-login:focus-visible { border: none !important; box-shadow: inset 0 0 0 2px var(--paper-accent); outline: none; }
.btn-register { background: var(--paper-accent) !important; color: #fff !important; text-transform: none; letter-spacing: 0; font-weight: 500; font-size: 14px; border-radius: 6px; padding: 0 20px; transition: background .15s, transform .1s; }
.btn-register:hover { background: var(--paper-accent-hover) !important; }
.btn-register:active { background: #8e2626 !important; transform: scale(.97); }
.btn-register:focus-visible { box-shadow: inset 0 0 0 2px rgba(255,255,255,.5), 0 0 0 1px var(--paper-accent); outline: none; }

/* Three-column body */
.app-body { display: flex; flex: 1; }

/* Main content area */
.app-main { flex: 1; min-width: 0; padding: 24px 32px; }

/* Right aside */
.app-aside {
  width: 366px; flex-shrink: 0;
  background: var(--paper-nav); border-left: 1px solid var(--paper-border);
  transition: width .2s ease, opacity .2s ease;
}

/* Responsive */
@media (max-width: 1300px) {
  .header-tabs { padding-left: 24px; }
  .app-main { padding: 24px 24px; }
  .app-aside { width: 320px; }
}
@media (max-width: 1200px) {
  .header-logo-area { width: 60px; }
  .header-logo .logo-text { display: none; }
  .header-tabs { padding-left: 20px; }
  .header-tab { font-size: 16px; padding: 12px 12px 6px; }
  .header-right { padding-right: 24px; }
  .app-main { padding: 24px 20px; }
  .app-aside { width: 280px; }
}
@media (max-width: 1100px) {
  .app-aside { width: 0; opacity: 0; overflow: hidden; border-left: none; }
  .app-main { padding: 24px 20px; }
}
@media (max-width: 1000px) {
  .header-tab { font-size: 15px; padding: 10px 10px 6px; }
}
@media (max-width: 900px) {
  .header-tabs { padding-left: 16px; }
  .header-tab { font-size: 14px; padding: 10px 8px 6px; }
  .header-right { padding-right: 16px; }
  .app-main { padding: 20px 16px; }
}
</style>
