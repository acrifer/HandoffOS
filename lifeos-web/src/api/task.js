import request from './request'

export const taskApi = {
    // Create task
    create(data) {
        return request({
            url: '/task',
            method: 'post',
            data
        })
    },

    // Update task
    update(data) {
        return request({
            url: '/task',
            method: 'put',
            data
        })
    },

    // Delete task
    delete(taskId) {
        return request({
            url: `/task/${taskId}`,
            method: 'delete'
        })
    },

    // Get all tasks
    getList() {
        return request({
            url: '/task/list',
            method: 'get'
        })
    },

    // Mark task as complete
    complete(taskId) {
        return request({
            url: `/task/${taskId}/complete`,
            method: 'post'
        })
    }
}
