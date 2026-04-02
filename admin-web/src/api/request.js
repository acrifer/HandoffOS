import axios from 'axios'
import router from '../router'

const service = axios.create({
  baseURL: '/admin-api',
  timeout: 8000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('lifeos_admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        localStorage.removeItem('lifeos_admin_token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('lifeos_admin_token')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default service
