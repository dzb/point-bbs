<template>
  <div class="mention-wrapper" style="overflow:visible">
    <v-textarea
      :model-value="modelValue"
      @update:model-value="onInput"
      @keydown="onKeydown"
      v-bind="$attrs"
      ref="textareaRef"
      autocapitalize="off"
      autocomplete="off"
      autocorrect="off"
      spellcheck="false"
    />
    <div v-if="show" class="mention-dropdown" :style="dropdownStyle">
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
import { ref, computed, nextTick } from 'vue'
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
const dropdownStyle = computed(() => {
  const el = textareaRef.value?.$el?.querySelector('textarea') as HTMLTextAreaElement | null
  const def = { position: 'fixed' as const, top: '0px', left: '0px', width: '0px', zIndex: '99999' }
  if (!el) return def
  const r = el.getBoundingClientRect()
  return { ...def, top: (r.bottom + 4) + 'px', left: r.left + 'px', width: r.width + 'px' }
})
let timer: ReturnType<typeof setTimeout> | null = null

function onInput(value: string) {
  emit('update:model-value', value)
  detect(value)
}

function detect(value: string) {
  const el = textareaRef.value?.$el?.querySelector('textarea') as HTMLTextAreaElement | null
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
    const el = textareaRef.value?.$el?.querySelector('textarea') as HTMLTextAreaElement | null
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
.mention-dropdown {
  max-height: 200px; overflow-y: auto;
  background: #ffffff; border: 1px solid #ccc;
  border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,.2);
}
.dark .mention-dropdown { background: #1e1e1e; border-color: #444; }
.mention-item {
  display: flex; align-items: center;
  padding: 8px 12px; cursor: pointer; transition: background .1s;
  background: inherit;
}
.mention-item:hover, .mention-item.active { background: #f5f5f5; }
.dark .mention-item:hover, .dark .mention-item.active { background: #2a2a2a; }
.mention-item.dimmed { color: #999; cursor: default; }
.mention-username { font-weight: 600; font-size: 13px; color: #c43d3d; }
.mention-nickname { font-size: 12px; color: #666; margin-left: 6px; }
.dark .mention-nickname { color: #aaa; }
</style>
