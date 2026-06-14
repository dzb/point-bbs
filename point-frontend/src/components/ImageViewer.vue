<template>
  <Teleport to="body">
    <div v-if="visible" class="viewer-overlay" @click.self="close" @keydown.esc="close">
      <v-btn icon="mdi-close" variant="text" class="viewer-close-btn" @click="close" size="36" />
      <div class="viewer-layout">
        <div class="viewer-image-side">
          <v-btn v-if="images.length>1" icon="mdi-chevron-left" variant="text" class="viewer-arrow left" @click="prev" />
          <img :src="images[index]" class="viewer-main-img" />
          <v-btn v-if="images.length>1" icon="mdi-chevron-right" variant="text" class="viewer-arrow right" @click="next" />
          <div v-if="images.length>1" class="viewer-counter">{{ index + 1 }} / {{ images.length }}</div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps<{ images: string[]; modelValue: number }>()
const emit = defineEmits<{ 'update:modelValue': [number] }>()

const visible = ref(false)
const index = ref(0)

watch(() => props.modelValue, (v) => {
  if (v > 0) { index.value = v - 1; visible.value = true }
  else visible.value = false
})

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
  if (e.key === 'ArrowLeft') prev()
  if (e.key === 'ArrowRight') next()
}

onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))

function close() { emit('update:modelValue', 0) }
function prev() { if (index.value > 0) index.value-- }
function next() { if (index.value < props.images.length - 1) index.value++ }
</script>

<style scoped>
.viewer-overlay { position: fixed; inset: 0; z-index: 9999; background: rgba(0,0,0,.94); display: flex; align-items: center; justify-content: center; }
.viewer-close-btn { position: fixed; top: 12px; left: 12px; z-index: 10; color: #fff !important; background: rgba(255,255,255,.12) !important; border-radius: 50%; }
.viewer-layout { display: flex; width: 100%; height: 100%; align-items: center; justify-content: center; }
.viewer-image-side { display: flex; align-items: center; justify-content: center; position: relative; width: 100%; height: 100%; }
.viewer-main-img { max-width: 90%; max-height: 90vh; object-fit: contain; border-radius: 4px; }
.viewer-arrow { position: absolute; top: 50%; transform: translateY(-50%); color: rgba(255,255,255,.7) !important; }
.viewer-arrow.left { left: 16px; }
.viewer-arrow.right { right: 16px; }
.viewer-counter { position: absolute; bottom: 16px; left: 50%; transform: translateX(-50%); color: rgba(255,255,255,.5); font-size: 13px; }
</style>
