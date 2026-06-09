import { createRouter, createWebHistory } from 'vue-router'
import { ensureDeviceSession, getDeviceToken } from '@/utils/deviceAuth'

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
      redirect: '/skill'
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/dashboard/index.vue')
    },
    {
      path: '/dashboard/insights',
      name: 'insights',
      component: () => import('../views/dashboard/insights.vue')
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
      path: '/admin/ai',
      name: 'admin-ai',
      component: () => import('../views/admin/ai.vue')
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('../views/user/index.vue')
    }
  ]
})

// Navigation Guard
router.beforeEach(async (to, from, next) => {
  const token = getDeviceToken()
  if (to.path !== '/login' && !token) {
    try {
      await ensureDeviceSession()
      next()
    } catch (error) {
      next('/login')
    }
    return
  }
  if (to.path === '/login' && token) {
    next('/skill')
    return
  }
  next()
})

export default router
