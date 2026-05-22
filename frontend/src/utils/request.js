import axios from 'axios'
import router from '@/router'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000,
})

// 请求拦截器：自动带上 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：401 清空 token 并跳转登录页
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
