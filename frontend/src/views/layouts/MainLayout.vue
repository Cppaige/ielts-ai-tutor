<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { name: '仪表盘', path: '/dashboard', icon: '🏠' },
  { name: '写作训练', path: '/writing', icon: '✍️' },
  { name: '写作历史', path: '/writing/history', icon: '📜' },
  { name: '口语训练', path: '/speaking', icon: '🎙️' },
  { name: '口语历史', path: '/speaking/history', icon: '📂' },
  { name: '个人中心', path: '/profile', icon: '👤' },
]

const isActive = (path) => {
  if (path === '/dashboard') return route.path === '/dashboard'
  return route.path.startsWith(path)
}

const userEmail = computed(() => userStore.userInfo?.email || '考鸭同学')

function logout() {
  userStore.logout()
}
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- 顶部导航 -->
    <header class="bg-white border-b border-gray-200 shadow-sm">
      <div class="px-6 h-14 flex items-center justify-between">
        <div class="flex items-center gap-8">
          <router-link to="/dashboard" class="flex items-center gap-2">
            <div class="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold">
              I
            </div>
            <span class="font-semibold text-lg text-gray-800">IELTS AI</span>
          </router-link>
          <nav class="hidden md:flex items-center gap-6 text-sm">
            <router-link
              to="/writing"
              class="text-gray-600 hover:text-blue-600"
              :class="{ 'text-blue-600 font-medium': isActive('/writing') }"
            >
              写作
            </router-link>
            <router-link
              to="/speaking"
              class="text-gray-600 hover:text-blue-600"
              :class="{ 'text-blue-600 font-medium': isActive('/speaking') }"
            >
              口语
            </router-link>
            <router-link
              to="/profile"
              class="text-gray-600 hover:text-blue-600"
              :class="{ 'text-blue-600 font-medium': isActive('/profile') }"
            >
              个人中心
            </router-link>
          </nav>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-sm text-gray-500 hidden sm:inline">{{ userEmail }}</span>
          <button
            class="text-sm text-gray-600 hover:text-red-600"
            @click="logout"
          >
            退出
          </button>
        </div>
      </div>
    </header>

    <div class="flex-1 flex">
      <!-- 侧边栏 -->
      <aside class="hidden md:block w-56 bg-white border-r border-gray-200 py-4">
        <nav class="px-3 space-y-1">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors"
            :class="
              isActive(item.path)
                ? 'bg-blue-50 text-blue-700 font-medium'
                : 'text-gray-700 hover:bg-gray-100'
            "
          >
            <span>{{ item.icon }}</span>
            <span>{{ item.name }}</span>
          </router-link>
        </nav>
      </aside>

      <!-- 主内容区 -->
      <main class="flex-1 p-6 overflow-auto">
        <router-view />
      </main>
    </div>
  </div>
</template>
