<template>
  <div class="mention-wrapper">
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
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
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
let dropdownEl: HTMLDivElement | null = null

onUnmounted(() => { removeDropdown() })

function removeDropdown() {
  if (dropdownEl) { dropdownEl.remove(); dropdownEl = null }
}

function getOrCreateDropdown(): HTMLDivElement {
  if (!dropdownEl) {
    dropdownEl = document.createElement('div')
    dropdownEl.className = 'mention-dropdown-global'
    document.body.appendChild(dropdownEl)
  }
  return dropdownEl
}

function updatePosition() {
  const el = textareaRef.value?.$el?.querySelector('textarea') as HTMLTextAreaElement | null
  if (!el || !dropdownEl) return
  const r = el.getBoundingClientRect()
  dropdownEl.style.top = (r.bottom + 4) + 'px'
  dropdownEl.style.left = r.left + 'px'
  dropdownEl.style.width = r.width + 'px'
}

function renderDropdown() {
  const d = getOrCreateDropdown()
  if (!show.value) { d.style.display = 'none'; return }
  d.style.display = ''
  let html = ''
  const dark = document.documentElement.classList.contains('dark')
  const bg = dark ? '#1e1e1e' : '#fff'
  const border = dark ? '#444' : '#ccc'
  const hover = dark ? '#2a2a2a' : '#f5f5f5'
  const text2 = dark ? '#aaa' : '#666'
  d.style.background = bg; d.style.borderColor = border
  for (let i = 0; i < suggestions.value.length; i++) {
    const u = suggestions.value[i]
    const active = i === selectedIndex.value
    const itemBg = active ? hover : bg
    const avatarHtml = u.avatar
      ? `<div style="width:24px;height:24px;border-radius:50%;background:center/cover url(${u.avatar});margin-right:8px;flex-shrink:0"></div>`
      : `<div style="width:24px;height:24px;border-radius:50%;background:#c43d3d;color:#fff;display:flex;align-items:center;justify-content:center;font-size:12px;margin-right:8px;flex-shrink:0">${(u.nickname||'?')[0]}</div>`
    html += `<div class="mention-item-global" style="display:flex;align-items:center;padding:8px 12px;cursor:pointer;background:${itemBg}" data-idx="${i}">${avatarHtml}<span style="font-weight:600;font-size:13px;color:#c43d3d">@${u.username||''}</span><span style="font-size:12px;color:${text2};margin-left:6px">${u.nickname||''}</span></div>`
  }
  if (suggestions.value.length === 0) {
    const hint = term.value.length > 0 ? '无匹配用户' : '输入用户名搜索...'
    html = `<div style="display:flex;align-items:center;padding:8px 12px;color:${text2};cursor:default;min-height:36px">${hint}</div>`
  }
  d.innerHTML = html
  // Attach click handlers
  d.querySelectorAll('.mention-item-global').forEach(item => {
    const idx = parseInt(item.getAttribute('data-idx') || '0')
    item.addEventListener('click', () => select(suggestions.value[idx]))
    item.addEventListener('mouseenter', () => { selectedIndex.value = idx; renderDropdown() })
  })
  updatePosition()
}

watch([show, suggestions, selectedIndex, term], () => renderDropdown())

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

<style>
.mention-dropdown-global {
  position: fixed; z-index: 99999;
  max-height: 200px; overflow-y: auto;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,.15);
  border: 1px solid;
}
</style>
