import { ref, computed, onMounted } from 'vue'
import { useTheme } from 'vuetify'

export interface PaperTheme {
  key: string; label: string
  bg: string; nav: string; text: string; text2: string; border: string
}

export const PAPER_THEMES: PaperTheme[] = [
  { key: 'cicada', label: '蝉翼 · 冷灰', bg: '#f5f4f1', nav: '#ecebe7', text: '#1e1d1a', text2: '#7d7a73', border: '#e6e3dd' },
  { key: 'night',  label: '夜读 · 墨池', bg: '#1e1b18', nav: '#252220', text: '#e8e0d5', text2: '#9b8e7e', border: '#3a3530' },
]

export function usePaperTheme() {
  const vuetifyTheme = useTheme()
  const activeKey = ref(localStorage.getItem('paperTheme') || 'cicada')
  const isDark = computed(() => activeKey.value === 'night')
  const current = computed(() => PAPER_THEMES.find(t => t.key === activeKey.value) || PAPER_THEMES[0])

  function apply(t: PaperTheme) {
    const root = document.documentElement; const dark = t.key === 'night'
    root.style.setProperty('--paper-bg', t.bg)
    root.style.setProperty('--paper-nav', t.nav)
    root.style.setProperty('--paper-text', t.text)
    root.style.setProperty('--paper-text2', t.text2)
    root.style.setProperty('--paper-border', t.border)
    document.body.style.backgroundColor = t.bg
    if (dark) root.classList.add('dark'); else root.classList.remove('dark')
    vuetifyTheme.themes.value.light.colors.background = t.bg
    vuetifyTheme.themes.value.light.colors.surface = dark ? '#252220' : '#ffffff'
    vuetifyTheme.themes.value.light.colors.primary = t.text
    vuetifyTheme.themes.value.light.colors.secondary = t.text2
  }

  function setTheme(key: string) {
    activeKey.value = key
    localStorage.setItem('paperTheme', key)
    apply(current.value)
  }

  function toggleTheme() {
    setTheme(isDark.value ? 'cicada' : 'night')
  }

  onMounted(() => apply(current.value))

  return { activeKey, current, isDark, setTheme, toggleTheme, themes: PAPER_THEMES }
}
