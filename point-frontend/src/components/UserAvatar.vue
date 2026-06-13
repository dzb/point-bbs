<template>
  <v-avatar v-if="src" :image="src" :size="size" />
  <v-avatar v-else :color="bgColor" :size="size" class="avatar-initial">
    {{ initial }}
  </v-avatar>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  src?: string | null
  name?: string
  size?: number
}>(), { size: 36 })

const avatarPalette = ['#c43d3d','#5b8c5a','#d4893a','#6b5e4f','#8b5a2b','#4a6fa5','#b5838d','#7b6c5d','#9c6b4a','#5a7a8a']

const initial = computed(() => {
  const n = props.name || '?'
  return n.charAt(0)
})

const bgColor = computed(() => {
  const n = props.name || '?'
  let hash = 0
  for (let i = 0; i < n.length; i++) hash = n.charCodeAt(i) + ((hash << 5) - hash)
  return avatarPalette[Math.abs(hash) % avatarPalette.length]
})
</script>

<style scoped>
.avatar-initial {
  font-weight: 600; color: #fff;
  font-family: 'Noto Serif SC', Georgia, serif;
}
</style>
