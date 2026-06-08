import request from './request'

export const noteApi = {
    create(data) {
        return request({
            url: '/note',
            method: 'post',
            data
        })
    },

    update(data) {
        return request({
            url: '/note',
            method: 'put',
            data
        })
    },

    delete(noteId) {
        return request({
            url: `/note/${noteId}`,
            method: 'delete'
        })
    },

    getDetail(noteId) {
        return request({
            url: `/note/${noteId}`,
            method: 'get'
        })
    },

    getList() {
        return request({
            url: '/note/list',
            method: 'get'
        })
    },

    search(params = {}) {
        return request({
            url: '/note/search',
            method: 'get',
            params
        })
    },

    getJobs(params = {}) {
        return request({
            url: '/note/jobs',
            method: 'get',
            params
        })
    },

    getJob(jobId) {
        return request({
            url: `/note/jobs/${jobId}`,
            method: 'get'
        })
    },

    generateSummary(noteId) {
        return request({
            url: `/note/${noteId}/summary`,
            method: 'post',
            timeout: 8000
        })
    },

    updatePin(noteId, pinned) {
        return request({
            url: `/note/${noteId}/pin`,
            method: 'post',
            data: { pinned }
        })
    },

    updateReview(noteId, data) {
        return request({
            url: `/note/${noteId}/review`,
            method: 'post',
            data
        })
    },

    organize(noteId) {
        return request({
            url: `/note/${noteId}/organize`,
            method: 'post',
            timeout: 8000
        })
    },

    extractTasks(noteId) {
        return request({
            url: `/note/${noteId}/extract-tasks`,
            method: 'post',
            timeout: 8000
        })
    },

    createWeeklyReview() {
        return request({
            url: '/note/weekly-review',
            method: 'post',
            timeout: 8000
        })
    }
}
