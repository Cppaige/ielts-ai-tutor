<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { useToast } from '@/utils/toast'

const router = useRouter()
const toast = useToast()

const form = ref({ email: '', password: '', confirmPassword: '' })
const errorMsg = ref('')
const loading = ref(false)

async function handleRegister() {
  errorMsg.value = ''
  if (!form.value.email || !form.value.password || !form.value.confirmPassword) {
    errorMsg.value = '请填写所有字段'
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    errorMsg.value = '两次密码输入不一致'
    return
  }
  loading.value = true
  try {
    await register({ email: form.value.email, password: form.value.password })
    toast.success('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    if (err.response?.status === 409) {
      errorMsg.value = '该邮箱已被注册'
    } else {
      errorMsg.value = err.response?.data?.message || '注册失败，请稍后重试'
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
        <h1 class="mt-4 text-2xl font-bold text-gray-900">注册</h1>
      </div>

      <form class="bg-white rounded-xl shadow-sm p-6 space-y-4" @submit.prevent="handleRegister">
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
            placeholder="至少 6 位"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label class="block text-sm text-gray-700 mb-1">确认密码</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-4 py-2 font-medium disabled:opacity-50"
        >
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <p class="mt-4 text-center text-sm text-gray-500">
        已有账号？
        <router-link to="/login" class="text-blue-600 hover:underline">登录</router-link>
      </p>
    </div>
  </div>
</template>
