import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login/index.vue')
    },
    {
      path: '/',
      name: 'home',
      redirect: '/dashboard'
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/dashboard/index.vue')
    },
    {
      path: '/task',
      name: 'task',
      component: () => import('../views/task/index.vue')
    },
    {
      path: '/note',
      name: 'note',
      component: () => import('../views/note/index.vue')
    },
    {
      path: '/ai',
      name: 'ai',
      component: () => import('../views/ai/index.vue')
    },
    {
      path: '/ai/chat',
      name: 'ai-chat',
      component: () => import('../views/ai/chat.vue')
    },
    {
      path: '/skill',
      name: 'skill',
      component: () => import('../views/skill/index.vue')
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('../views/user/index.vue')
    }
  ]
})

// Navigation Guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('lifeos_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
