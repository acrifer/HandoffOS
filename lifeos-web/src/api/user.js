import request from './request'

// User operations
export const userApi = {
    deviceLogin(data) {
        return request({
            url: '/auth/device-login',
            method: 'post',
            data
        })
    },

    currentQuota() {
        return request({
            url: '/quota/me',
            method: 'get'
        })
    },

    adminDevices() {
        return request({
            url: '/admin/quota/devices',
            method: 'get'
        })
    },

    adminDeviceDetail(deviceId) {
        return request({
            url: `/admin/quota/devices/${deviceId}`,
            method: 'get'
        })
    },

    updateDeviceQuota(deviceId, data) {
        return request({
            url: `/admin/quota/devices/${deviceId}`,
            method: 'put',
            data
        })
    },

    resetDeviceQuota(deviceId) {
        return request({
            url: `/admin/quota/devices/${deviceId}/reset`,
            method: 'post'
        })
    },

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
