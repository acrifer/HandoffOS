import { defineStore } from 'pinia'
import { adminApi } from '../api/admin'

export const useAuthStore = defineStore('adminAuth', {
  state: () => ({
    currentUser: null
  }),
  getters: {
    permissions(state) {
      return state.currentUser?.permissions || []
    },
    hasPermission(state) {
      return (permission) => (state.currentUser?.permissions || []).includes(permission)
    },
    menuItems(state) {
      const permissions = new Set(state.currentUser?.permissions || [])
      const items = [
        { path: '/dashboard', label: 'Dashboard', perm: 'dashboard:view' },
        { path: '/users', label: 'Users', perm: 'users:view' },
        { path: '/notes', label: 'Notes', perm: 'notes:view' },
        { path: '/tasks', label: 'Tasks', perm: 'tasks:view' },
        { path: '/ai-jobs', label: 'AI Jobs', perm: 'ai-jobs:view' },
        { path: '/behaviors', label: 'Behaviors', perm: 'behaviors:view' },
        { path: '/ops/services', label: 'Ops Services', perm: 'ops:services:view' },
        { path: '/ops/config', label: 'Ops Config', perm: 'ops:config:view' },
        { path: '/ops/tools', label: 'Ops Tools', perm: 'ops:tools:view' }
      ]
      return items.filter((item) => permissions.has(item.perm))
    }
  },
  actions: {
    async fetchCurrentUser() {
      this.currentUser = await adminApi.me()
      return this.currentUser
    },
    clear() {
      this.currentUser = null
    }
  }
})
