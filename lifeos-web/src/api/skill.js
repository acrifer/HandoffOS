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
      timeout: 15000
    })
  },

  distill(skillId) {
    return request({
      url: `/skill/${skillId}/distill`,
      method: 'post',
      timeout: 8000
    })
  },

  ask(skillId, data) {
    return request({
      url: `/skill/${skillId}/ask`,
      method: 'post',
      data,
      timeout: 8000
    })
  },

  getJobs(skillId, params = {}) {
    return request({
      url: `/skill/${skillId}/jobs`,
      method: 'get',
      params
    })
  }
}
