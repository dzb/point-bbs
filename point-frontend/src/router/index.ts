import { createRouter, createWebHashHistory } from 'vue-router'

const PageAside = () => import('@/components/PageAside.vue')
const TopicAside = () => import('@/components/TopicAside.vue')
const CreateAside = () => import('@/components/CreateAside.vue')

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          components: {
            default: () => import('@/views/home/HomePage.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'following',
          name: 'following',
          components: {
            default: () => import('@/views/home/FollowingPage.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'explore',
          name: 'explore',
          components: {
            default: () => import('@/views/home/ExplorePage.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'articles',
          name: 'article-list',
          components: {
            default: () => import('@/views/article/ArticleList.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'articles/create',
          name: 'article-create',
          components: {
            default: () => import('@/views/article/ArticleCreate.vue'),
            aside: CreateAside,
          },
        },
        {
          path: 'articles/:id',
          name: 'article-detail',
          components: {
            default: () => import('@/views/article/ArticleDetail.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'topics/create',
          name: 'topic-create',
          components: {
            default: () => import('@/views/topic/TopicCreate.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'topics/:id',
          name: 'topic-detail',
          components: {
            default: () => import('@/views/topic/TopicDetail.vue'),
            aside: TopicAside,
          },
        },
        {
          path: 'topics/:id/edit',
          name: 'topic-edit',
          components: {
            default: () => import('@/views/topic/TopicEdit.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'users/:id',
          name: 'user-profile',
          components: {
            default: () => import('@/views/user/UserProfile.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'search',
          name: 'search',
          components: {
            default: () => import('@/views/home/SearchPage.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'messages',
          name: 'messages',
          components: {
            default: () => import('@/views/user/MessagesPage.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'favorites',
          name: 'favorites',
          components: {
            default: () => import('@/views/user/FavoritesPage.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'login',
          name: 'login',
          components: {
            default: () => import('@/views/auth/LoginPage.vue'),
            aside: PageAside,
          },
        },
        {
          path: 'register',
          name: 'register',
          components: {
            default: () => import('@/views/auth/RegisterPage.vue'),
            aside: PageAside,
          },
        },
      ],
    },
  ],
})

export default router
