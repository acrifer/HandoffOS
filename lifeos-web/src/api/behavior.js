import request from './request'

export const behaviorApi = {
    getDashboard() {
        return request({
            url: '/behavior/dashboard',
            method: 'get'
        })
    }
}
