import { defineStore } from 'pinia'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
  }),
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(info) {
      this.userInfo = info
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      router.push('/login')
    },
  },
})
