import request from './request'

export const skillApi = {
  create(data) {
    return request({
      url: '/skill',
      method: 'post',
      data
    })
  },

  list() {
    return request({
      url: '/skill',
      method: 'get'
    })
  },

  getDetail(skillId) {
    return request({
      url: `/skill/${skillId}`,
      method: 'get'
    })
  },

  syncSources(skillId, data) {
    return request({
      url: `/skill/${skillId}/sources/sync`,
      method: 'post',
      data,
      timeout: 120000
    })
  },

  distill(skillId) {
    return request({
      url: `/skill/${skillId}/distill`,
      method: 'post',
      timeout: 120000
    })
  },

  ask(skillId, data) {
    return request({
      url: `/skill/${skillId}/ask`,
      method: 'post',
      data,
      timeout: 120000
    })
  },

  getJobs(skillId, params = {}) {
    return request({
      url: `/skill/${skillId}/jobs`,
      method: 'get',
      params
    })
  },

  createDocument(skillId, data) {
    return request({
      url: `/skills/${skillId}/documents`,
      method: 'post',
      data
    })
  },

  listDocuments(skillId) {
    return request({
      url: `/skills/${skillId}/documents`,
      method: 'get'
    })
  },

  parseDocument(skillId, documentId, data = {}) {
    return request({
      url: `/skills/${skillId}/documents/${documentId}/parse`,
      method: 'post',
      data
    })
  },

  vectorizeDocument(skillId, documentId, data = {}) {
    return request({
      url: `/skills/${skillId}/documents/${documentId}/vectorize`,
      method: 'post',
      data,
      timeout: 120000
    })
  },

  summarize(skillId) {
    return request({
      url: `/skills/${skillId}/summary`,
      method: 'post',
      timeout: 120000
    })
  },

  askKnowledge(skillId, data) {
    return request({
      url: `/skills/${skillId}/ask`,
      method: 'post',
      data,
      timeout: 120000
    })
  },

  getQaHistory(skillId, params = {}) {
    return request({
      url: `/skills/${skillId}/qa-history`,
      method: 'get',
      params
    })
  },

  searchKnowledge(skillId, params = {}) {
    return request({
      url: `/skills/${skillId}/search`,
      method: 'get',
      params
    })
  },

  getRecommendedQuestions(skillId) {
    return request({
      url: `/skills/${skillId}/recommended-questions`,
      method: 'get'
    })
  },

  feedback(qaLogId, data) {
    return request({
      url: `/ai/qa/${qaLogId}/feedback`,
      method: 'post',
      data
    })
  },

  adminStats(params = {}) {
    return request({
      url: '/admin/ai/stats',
      method: 'get',
      params
    })
  },

  analyzeLogs(data = {}) {
    return request({
      url: '/admin/ai/log-analysis',
      method: 'post',
      data,
      timeout: 120000
    })
  },

  botStatus() {
    return request({
      url: '/feishu/bot/status',
      method: 'get'
    })
  },

  botBindings(params = {}) {
    return request({
      url: '/feishu/bot/bindings',
      method: 'get',
      params
    })
  },

  createBotBinding(data) {
    return request({
      url: '/feishu/bot/bindings',
      method: 'post',
      data
    })
  },

  deleteBotBinding(bindingId) {
    return request({
      url: `/feishu/bot/bindings/${bindingId}`,
      method: 'delete'
    })
  },

  botEvents(params = {}) {
    return request({
      url: '/feishu/bot/events',
      method: 'get',
      params
    })
  }
}
