<script setup lang="ts">
defineProps<{
  hasMore?: boolean
  loading?: boolean
  label?: string
}>()

const emit = defineEmits<{ (e: 'load-more'): void }>()
</script>

<template>
  <div v-if="hasMore" class="text-center mt-4 mb-2">
    <v-btn
      variant="text"
      :loading="loading"
      @click="emit('load-more')"
      style="text-transform:none;letter-spacing:0;color:var(--paper-text2)"
    >{{ label || '显示更多' }}</v-btn>
  </div>
</template>
