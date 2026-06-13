import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/home/HomePage.vue') },
        { path: 'topic/:id', name: 'topic-detail', component: () => import('@/views/topic/TopicDetail.vue') },
        { path: 'topic/create', name: 'topic-create', component: () => import('@/views/topic/TopicCreate.vue') },
        { path: 'topic/:id/edit', name: 'topic-edit', component: () => import('@/views/topic/TopicEdit.vue') },
        { path: 'user/:id', name: 'user-profile', component: () => import('@/views/user/UserProfile.vue') },
        { path: 'search', name: 'search', component: () => import('@/views/home/SearchPage.vue') },
        { path: 'messages', name: 'messages', component: () => import('@/views/user/MessagesPage.vue') },
        { path: 'login', name: 'login', component: () => import('@/views/auth/LoginPage.vue') },
        { path: 'articles', name: 'article-list', component: () => import('@/views/article/ArticleList.vue') },
        { path: 'article/:id', name: 'article-detail', component: () => import('@/views/article/ArticleDetail.vue') },
        { path: 'article/create', name: 'article-create', component: () => import('@/views/article/ArticleCreate.vue') },
        { path: 'register', name: 'register', component: () => import('@/views/auth/RegisterPage.vue') },
      ],
    },
  ],
})

export default router
