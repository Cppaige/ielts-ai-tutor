import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const email = ref(localStorage.getItem('email') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(data: { token: string; userId: string; email: string }) {
    token.value = data.token
    userId.value = data.userId
    email.value = data.email
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('email', data.email)
  }

  function logout() {
    token.value = ''
    userId.value = ''
    email.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('email')
  }

  return { token, userId, email, isLoggedIn, setAuth, logout }
})
