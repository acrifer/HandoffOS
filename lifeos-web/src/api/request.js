import axios from 'axios'
import router from '../router'
import { clearDeviceToken, getDeviceToken } from '@/utils/deviceAuth'

// Create an axios instance
const service = axios.create({
    baseURL: '/api', // Gateway routes all /api requests
    timeout: 5000 // Request timeout
})

// Request interceptor
service.interceptors.request.use(
    config => {
        // Add token to headers if it exists
        const token = getDeviceToken()
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        const adminKey = localStorage.getItem('handoff_admin_key')
        if (adminKey && config.url?.startsWith('/admin/quota')) {
            config.headers['X-Admin-Key'] = adminKey
        }
        return config
    },
    error => {
        console.log(error)
        return Promise.reject(error)
    }
)

// Response interceptor
service.interceptors.response.use(
    response => {
        const res = response.data

        // If the custom code is not 200, it is judged as an error.
        if (res.code !== 200) {
            console.error(res.message || 'Error')

            // 401: Unauthorized
            if (res.code === 401) {
                // to re-login
                clearDeviceToken()
                router.push('/login')
            }
            return Promise.reject(new Error(res.message || 'Error'))
        } else {
            return res.data
        }
    },
    error => {
        console.error('err' + error) // for debug
        if (error.response && error.response.status === 401) {
            clearDeviceToken()
            router.push('/login')
        }
        return Promise.reject(error)
    }
)

export default service
