import request from './request'

// User operations
export const userApi = {
    // Login
    login(data) {
        return request({
            url: '/auth/login',
            method: 'post',
            data
        })
    },

    // Register
    register(data) {
        return request({
            url: '/auth/register',
            method: 'post',
            data
        })
    },

    // Get Info
    getInfo() {
        return request({
            url: '/user/info',
            method: 'get'
        })
    },

    updateProfile(data) {
        return request({
            url: '/user/profile',
            method: 'put',
            data
        })
    },

    updatePassword(data) {
        return request({
            url: '/user/password',
            method: 'put',
            data
        })
    },

    logout() {
        return request({
            url: '/user/logout',
            method: 'post'
        })
    }
}
