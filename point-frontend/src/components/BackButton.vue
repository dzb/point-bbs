<template>
  <v-btn icon="mdi-arrow-left" variant="text" size="34" class="mb-2 back-btn" @click="$router.back()" aria-label="返回" />
</template>

<style scoped>
.back-btn { margin-left: -10px; color: var(--paper-text2); }
</style>
