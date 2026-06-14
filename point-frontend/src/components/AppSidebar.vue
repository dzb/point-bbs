<template>
  <aside class="sidebar" :class="{ collapsed }">
    <nav class="sidebar-nav">
      <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
        <v-icon size="20">mdi-lightbulb-outline</v-icon>
        <span v-if="!collapsed" class="nav-label">随想</span>
      </router-link>
      <router-link to="/articles/create" class="nav-item" :class="{ active: $route.path === '/articles/create' }">
        <v-icon size="20">mdi-pen</v-icon>
        <span v-if="!collapsed" class="nav-label">创作</span>
      </router-link>
      <router-link to="/favorites" class="nav-item" :class="{ active: $route.path === '/favorites' }">
        <v-icon size="20">mdi-bookmark-outline</v-icon>
        <span v-if="!collapsed" class="nav-label">收藏</span>
      </router-link>
      <router-link to="/messages" class="nav-item" :class="{ active: $route.path === '/messages' }">
        <v-icon size="20">mdi-bell-outline</v-icon>
        <span v-if="!collapsed" class="nav-label">消息</span>
        <v-badge v-if="unread" :model-value="unread" color="var(--paper-accent)" dot class="nav-badge" />
      </router-link>
      <a class="nav-item" href="#">
        <v-icon size="20">mdi-rss</v-icon>
        <span v-if="!collapsed" class="nav-label">RSS</span>
      </a>
    </nav>

  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{ unread?: number }>()

const collapsed = ref(window.innerWidth < 1200)

window.addEventListener('resize', () => {
  collapsed.value = window.innerWidth < 1200
})
</script>

<style scoped>
.sidebar {
  width: 240px; min-height: 100vh; background: var(--paper-nav);
  border-right: 1px solid var(--paper-nav);
  display: flex; flex-direction: column; padding: 12px 0;
  transition: width .2s ease; position: sticky; top: 0;
}
.sidebar.collapsed { width: 60px; }

.sidebar-nav { flex: 1; display: flex; flex-direction: column; gap: 2px; padding: 0 8px; }

.nav-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px;
  text-decoration: none; color: var(--paper-text); transition: background .15s; position: relative;
}
.nav-item:hover { background: rgba(0,0,0,.04); }
.nav-item.active { color: var(--paper-accent); background: rgba(196,61,61,.08); }
.nav-label { font-size: 14px; white-space: nowrap; overflow: hidden; }
.nav-badge { position: absolute; right: 8px; }
.collapsed .nav-item { justify-content: center; padding: 10px 0; }
</style>
