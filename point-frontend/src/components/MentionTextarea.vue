<template>
  <div class="mention-wrapper">
    <v-textarea
      :model-value="modelValue"
      @update:model-value="onInput"
      @keydown="onKeydown"
      v-bind="$attrs"
      ref="textareaRef"
    />
    <div v-if="show" class="mention-dropdown">
      <div
        v-for="(user, i) in suggestions"
        :key="user.id"
        :class="['mention-item', { active: i === selectedIndex }]"
        @click="select(user)"
        @mouseenter="selectedIndex = i"
      >
        <UserAvatar :src="user.avatar" :name="user.nickname" :size="24" class="mr-2" />
        <span class="mention-username">@{{ user.username }}</span>
        <span class="mention-nickname">{{ user.nickname }}</span>
      </div>
      <div v-if="suggestions.length === 0 && term.length > 0" class="mention-item dimmed">
        无匹配用户
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import UserAvatar from './UserAvatar.vue'
import { searchUsers } from '@/api/user'
import type { UserInfo } from '@/types'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:model-value': [value: string] }>()

const textareaRef = ref<any>(null)
const show = ref(false)
const suggestions = ref<UserInfo[]>([])
const term = ref('')
const atPos = ref(-1)
const selectedIndex = ref(0)
let timer: ReturnType<typeof setTimeout> | null = null

function getTextarea(): HTMLTextAreaElement | null {
  return textareaRef.value?.$el?.querySelector('textarea') ?? null
}

function onInput(value: string) {
  emit('update:model-value', value)
  detect(value)
}

function detect(value: string) {
  const el = getTextarea()
  if (!el) return
  const pos = el.selectionStart
  const before = value.substring(0, pos)
  const idx = before.lastIndexOf('@')
  if (idx >= 0 && (idx === 0 || /\s/.test(before[idx - 1]))) {
    const partial = before.substring(idx + 1)
    if (/^[\w一-鿿]{0,32}$/.test(partial)) {
      atPos.value = idx
      term.value = partial
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => doSearch(partial), 200)
      return
    }
  }
  show.value = false
  term.value = ''
}

async function doSearch(q: string) {
  if (!q) { suggestions.value = []; show.value = true; return }
  try {
    suggestions.value = await searchUsers(q)
    show.value = true
    selectedIndex.value = 0
  } catch { suggestions.value = [] }
}

function select(user: UserInfo) {
  const val = props.modelValue
  const before = val.substring(0, atPos.value)
  const after = val.substring(atPos.value + 1 + term.value.length)
  const replacement = '@' + user.username + ' '
  emit('update:model-value', before + replacement + after)
  show.value = false
  nextTick(() => {
    const el = getTextarea()
    if (el) {
      const p = atPos.value + replacement.length
      el.focus()
      el.selectionStart = el.selectionEnd = p
    }
  })
}

function onKeydown(e: KeyboardEvent) {
  if (!show.value) return
  if (e.key === 'ArrowDown') { e.preventDefault(); selectedIndex.value = Math.min(selectedIndex.value + 1, suggestions.value.length - 1) }
  else if (e.key === 'ArrowUp') { e.preventDefault(); selectedIndex.value = Math.max(selectedIndex.value - 1, 0) }
  else if (e.key === 'Enter' || e.key === 'Tab') {
    if (suggestions.value[selectedIndex.value]) { e.preventDefault(); select(suggestions.value[selectedIndex.value]) }
  } else if (e.key === 'Escape') { show.value = false }
}
</script>

<style scoped>
.mention-wrapper { position: relative; }
.mention-dropdown {
  position: absolute; bottom: 100%; left: 0; right: 0; z-index: 200;
  max-height: 200px; overflow-y: auto;
  background: var(--paper-bg); border: 1px solid var(--paper-border);
  border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,.12);
  margin-bottom: 4px;
}
.mention-item {
  display: flex; align-items: center;
  padding: 8px 12px; cursor: pointer; transition: background .1s;
}
.mention-item:hover, .mention-item.active { background: var(--paper-nav); }
.mention-item.dimmed { color: var(--paper-text2); cursor: default; }
.mention-username { font-weight: 600; font-size: 13px; color: var(--paper-accent); }
.mention-nickname { font-size: 12px; color: var(--paper-text2); margin-left: 6px; }
</style>
