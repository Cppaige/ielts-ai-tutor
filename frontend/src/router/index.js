import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
  },
  {
    path: '/',
    component: () => import('@/views/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
      },
      {
        path: 'writing',
        name: 'WritingTopics',
        component: () => import('@/views/writing/TopicListView.vue'),
      },
      {
        path: 'writing/practice/:id',
        name: 'WritingPractice',
        component: () => import('@/views/writing/PracticeView.vue'),
      },
      {
        path: 'writing/waiting/:submissionId',
        name: 'WritingWaiting',
        component: () => import('@/views/writing/WaitingView.vue'),
      },
      {
        path: 'writing/result/:submissionId',
        name: 'WritingResult',
        component: () => import('@/views/writing/ResultView.vue'),
      },
      {
        path: 'writing/history',
        name: 'WritingHistory',
        component: () => import('@/views/writing/HistoryView.vue'),
      },
      {
        path: 'speaking',
        name: 'SpeakingTopics',
        component: () => import('@/views/speaking/TopicListView.vue'),
      },
      {
        path: 'speaking/session',
        name: 'SpeakingSession',
        component: () => import('@/views/speaking/SessionView.vue'),
      },
      {
        path: 'speaking/report/:sessionId',
        name: 'SpeakingReport',
        component: () => import('@/views/speaking/ReportView.vue'),
      },
      {
        path: 'speaking/history',
        name: 'SpeakingHistory',
        component: () => import('@/views/speaking/HistoryView.vue'),
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfileView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：检查 token
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.matched.some((record) => record.meta.requiresAuth) && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
