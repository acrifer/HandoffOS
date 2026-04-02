import request from './request'

export const adminApi = {
  login(data) {
    return request.post('/admin/auth/login', data)
  },
  me() {
    return request.get('/admin/auth/me')
  },
  logout() {
    return request.post('/admin/auth/logout')
  },
  dashboard() {
    return request.get('/admin/ops/dashboard')
  },
  listUsers(params) {
    return request.get('/admin/users', { params })
  },
  enableUser(userId) {
    return request.post(`/admin/users/${userId}/enable`)
  },
  disableUser(userId) {
    return request.post(`/admin/users/${userId}/disable`)
  },
  resetPassword(userId, newPassword) {
    return request.post(`/admin/users/${userId}/reset-password`, { newPassword })
  },
  listNotes(params) {
    return request.get('/admin/notes', { params })
  },
  deleteNote(noteId) {
    return request.delete(`/admin/notes/${noteId}`)
  },
  updateNoteReview(noteId, payload) {
    return request.post(`/admin/notes/${noteId}/review`, payload)
  },
  listTasks(params) {
    return request.get('/admin/tasks', { params })
  },
  updateTaskStatus(taskId, status) {
    return request.post(`/admin/tasks/${taskId}/status`, { status })
  },
  deleteTask(taskId) {
    return request.delete(`/admin/tasks/${taskId}`)
  },
  listAiJobs(params) {
    return request.get('/admin/ai-jobs', { params })
  },
  retryAiJob(jobId) {
    return request.post(`/admin/ai-jobs/${jobId}/retry`)
  },
  cancelAiJob(jobId) {
    return request.post(`/admin/ai-jobs/${jobId}/cancel`)
  },
  listBehaviors(params) {
    return request.get('/admin/behaviors', { params })
  },
  listServices() {
    return request.get('/admin/ops/services')
  },
  listConfigs() {
    return request.get('/admin/ops/config')
  },
  getTools() {
    return request.get('/admin/ops/tools')
  },
  resetTestData() {
    return request.post('/admin/ops/reset-test-data')
  }
}
