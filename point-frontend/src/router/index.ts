import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/home/HomePage.vue') },
        { path: 'following', name: 'following', component: () => import('@/views/home/FollowingPage.vue') },
        { path: 'explore', name: 'explore', component: () => import('@/views/home/ExplorePage.vue') },
        { path: 'topics/:id', name: 'topic-detail', component: () => import('@/views/topic/TopicDetail.vue') },
        { path: 'topics/create', name: 'topic-create', component: () => import('@/views/topic/TopicCreate.vue') },
        { path: 'topics/:id/edit', name: 'topic-edit', component: () => import('@/views/topic/TopicEdit.vue') },
        { path: 'users/:id', name: 'user-profile', component: () => import('@/views/user/UserProfile.vue') },
        { path: 'search', name: 'search', component: () => import('@/views/home/SearchPage.vue') },
        { path: 'messages', name: 'messages', component: () => import('@/views/user/MessagesPage.vue') },
        { path: 'favorites', name: 'favorites', component: () => import('@/views/user/FavoritesPage.vue') },
        { path: 'login', name: 'login', component: () => import('@/views/auth/LoginPage.vue') },
        { path: 'articles', name: 'article-list', component: () => import('@/views/article/ArticleList.vue') },
        { path: 'articles/:id', name: 'article-detail', component: () => import('@/views/article/ArticleDetail.vue') },
        { path: 'articles/create', name: 'article-create', component: () => import('@/views/article/ArticleCreate.vue') },
        { path: 'register', name: 'register', component: () => import('@/views/auth/RegisterPage.vue') },
      ],
    },
  ],
})

export default router
