<template>
  <div class="topic-aside">
    <p class="aside-tagline">记录思考，分享见闻</p>
    <div class="aside-card" v-if="aside.author">
      <div class="d-flex align-center mb-2">
        <UserAvatar :src="aside.author.avatar" :name="aside.author.nickname" :size="48" />
        <div class="ml-3 flex-grow-1">
          <div style="font-size:15px;font-weight:600;color:var(--paper-text)">{{ aside.author.nickname }}</div>
        </div>
      </div>
      <div v-if="aside.author.description" style="font-size:13px;color:var(--paper-text2);line-height:1.6;margin-bottom:10px">
        {{ aside.author.description }}
      </div>
      <div v-else style="font-size:13px;color:var(--paper-text2);margin-bottom:10px">暂无简介</div>
      <v-btn v-if="aside.showFollow" block size="small" variant="outlined"
        :color="aside.following ? 'var(--paper-accent)' : ''" :loading="aside.followLoading"
        @click="aside.toggleFollow()" style="text-transform:none;letter-spacing:0;border-radius:20px">
        {{ aside.following ? '已订阅' : '+ 订阅' }}
      </v-btn>
    </div>

    <div class="aside-card">
      <div class="aside-card-title">社区公告</div>
      <div class="aside-card-text">point 正在建设中，欢迎反馈建议。</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import UserAvatar from '@/components/UserAvatar.vue'
import { topicAsideState as aside } from '@/composables/useTopicAside'
</script>

<style scoped>
.topic-aside { padding: 20px 24px; }
</style>
