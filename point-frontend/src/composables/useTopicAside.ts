import { reactive } from 'vue'

/** Shared state bridge between TopicDetail page and TopicAside component */
export const topicAsideState = reactive({
  author: null as { avatar?: string | null; nickname?: string; description?: string } | null,
  following: false,
  followLoading: false,
  showFollow: false,
  toggleFollow: () => {},
})
