<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import { useToast } from '@/utils/toast'

const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const form = ref({ email: '', password: '' })
const errorMsg = ref('')
const loading = ref(false)

async function handleLogin() {
  errorMsg.value = ''
  if (!form.value.email || !form.value.password) {
    errorMsg.value = '请填写邮箱和密码'
    return
  }
  loading.value = true
  try {
    const res = await login(form.value)
    userStore.setToken(res.token)
    userStore.setUserInfo(res.user)
    toast.success('登录成功')
    router.push('/dashboard')
  } catch (err) {
    if (err.response?.status === 401) {
      errorMsg.value = '邮箱或密码错误'
    } else {
      errorMsg.value = err.response?.data?.message || '登录失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex items-center justify-center px-4">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <router-link to="/" class="inline-flex items-center gap-2">
          <div class="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold">
            I
          </div>
          <span class="font-semibold text-lg text-gray-800">IELTS AI</span>
        </router-link>
        <h1 class="mt-4 text-2xl font-bold text-gray-900">登录</h1>
      </div>

      <form class="bg-white rounded-xl shadow-sm p-6 space-y-4" @submit.prevent="handleLogin">
        <div v-if="errorMsg" class="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
          {{ errorMsg }}
        </div>

        <div>
          <label class="block text-sm text-gray-700 mb-1">邮箱</label>
          <input
            v-model="form.email"
            type="email"
            placeholder="your@email.com"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label class="block text-sm text-gray-700 mb-1">密码</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-4 py-2 font-medium disabled:opacity-50"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <p class="mt-4 text-center text-sm text-gray-500">
        还没有账号？
        <router-link to="/register" class="text-blue-600 hover:underline">注册</router-link>
      </p>
    </div>
  </div>
</template>
