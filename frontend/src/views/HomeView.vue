<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex flex-col">
    <!-- 顶部导航 -->
    <header class="bg-white border-b border-gray-200">
      <div class="max-w-6xl mx-auto px-6 h-14 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold">
            I
          </div>
          <span class="font-semibold text-lg text-gray-800">IELTS AI</span>
        </div>
        <div class="flex items-center gap-3">
          <button
            class="text-sm text-gray-600 hover:text-blue-600"
            @click="router.push('/login')"
          >
            登录
          </button>
          <button
            class="bg-blue-600 text-white text-sm hover:bg-blue-700 rounded-lg px-4 py-2"
            @click="router.push('/register')"
          >
            注册
          </button>
        </div>
      </div>
    </header>

    <!-- Hero 区域 -->
    <main class="flex-1 flex flex-col items-center justify-center px-6 py-20">
      <h1 class="text-4xl md:text-5xl font-bold text-gray-900 text-center max-w-3xl leading-tight">
        AI 驱动的 IELTS 备考平台
      </h1>
      <p class="mt-4 text-lg text-gray-500 text-center max-w-xl">
        智能写作批改 + 模拟口语考官，精准提升你的雅思成绩
      </p>
      <div class="mt-8 flex gap-4">
        <button
          class="bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-6 py-3 text-base font-medium"
          @click="router.push('/register')"
        >
          免费开始
        </button>
        <button
          class="border border-gray-300 text-gray-700 hover:bg-gray-100 rounded-lg px-6 py-3 text-base font-medium"
          @click="router.push('/login')"
        >
          已有账号
        </button>
      </div>

      <!-- 功能亮点 -->
      <div class="mt-20 grid grid-cols-1 md:grid-cols-3 gap-6 max-w-4xl w-full">
        <div class="bg-white rounded-xl shadow-sm p-6">
          <div class="text-2xl mb-3">✍️</div>
          <h3 class="font-semibold text-gray-800 mb-2">智能写作批改</h3>
          <p class="text-sm text-gray-500">
            AI 从 TR、CC、LR、GRA 四个维度精准评分，提供详细反馈和改进建议
          </p>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-6">
          <div class="text-2xl mb-3">🎙️</div>
          <h3 class="font-semibold text-gray-800 mb-2">模拟口语考官</h3>
          <p class="text-sm text-gray-500">
            真实模拟 Part 1/2/3 考试流程，支持多种考官风格，实时语音对话
          </p>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-6">
          <div class="text-2xl mb-3">📊</div>
          <h3 class="font-semibold text-gray-800 mb-2">进步追踪</h3>
          <p class="text-sm text-gray-500">
            可视化分数趋势，记录每次练习，帮你看到自己的成长轨迹
          </p>
        </div>
      </div>
    </main>

    <!-- 底部 -->
    <footer class="text-center text-sm text-gray-400 py-6 border-t border-gray-200">
      IELTS AI 备考平台 &copy; 2025
    </footer>
  </div>
</template>
