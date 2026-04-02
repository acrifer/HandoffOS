import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue') },
  {
    path: '/',
    component: () => import('../views/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { permission: 'dashboard:view' } },
      { path: 'users', component: () => import('../views/UsersView.vue'), meta: { permission: 'users:view' } },
      { path: 'notes', component: () => import('../views/NotesView.vue'), meta: { permission: 'notes:view' } },
      { path: 'tasks', component: () => import('../views/TasksView.vue'), meta: { permission: 'tasks:view' } },
      { path: 'ai-jobs', component: () => import('../views/AiJobsView.vue'), meta: { permission: 'ai-jobs:view' } },
      { path: 'behaviors', component: () => import('../views/BehaviorsView.vue'), meta: { permission: 'behaviors:view' } },
      { path: 'ops/services', component: () => import('../views/ServicesView.vue'), meta: { permission: 'ops:services:view' } },
      { path: 'ops/config', component: () => import('../views/ConfigView.vue'), meta: { permission: 'ops:config:view' } },
      { path: 'ops/tools', component: () => import('../views/ToolsView.vue'), meta: { permission: 'ops:tools:view' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const token = localStorage.getItem('lifeos_admin_token')
  if (to.path === '/login') {
    if (token) {
      return '/dashboard'
    }
    return true
  }
  if (!token) {
    return '/login'
  }
  const authStore = useAuthStore()
  if (!authStore.currentUser) {
    try {
      await authStore.fetchCurrentUser()
    } catch (error) {
      localStorage.removeItem('lifeos_admin_token')
      authStore.clear()
      return '/login'
    }
  }
  const requiredPermission = to.meta?.permission
  if (requiredPermission && !authStore.hasPermission(requiredPermission)) {
    const fallback = authStore.menuItems[0]?.path || '/login'
    return fallback
  }
  return true
})

export default router
