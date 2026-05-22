<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/utils/toast'

const userStore = useUserStore()
const toast = useToast()

const form = ref({ nickname: '', targetScore: '' })
const saving = ref(false)

onMounted(() => {
  if (userStore.userInfo) {
    form.value.nickname = userStore.userInfo.nickname || ''
    form.value.targetScore = userStore.userInfo.targetScore || ''
  }
})

async function saveProfile() {
  saving.value = true
  try {
    // TODO: 接入后端 PUT /api/user/profile
    await new Promise((r) => setTimeout(r, 600))
    userStore.setUserInfo({ ...userStore.userInfo, ...form.value })
    toast.success('保存成功')
  } catch {
    toast.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const userEmail = () => userStore.userInfo?.email || '—'
</script>

<template>
  <div class="space-y-5 max-w-lg">
    <h1 class="text-xl font-semibold text-gray-800">个人中心</h1>

    <!-- 账号信息 -->
    <div class="bg-white rounded-xl shadow-sm p-6 space-y-4">
      <h2 class="text-sm font-semibold text-gray-700">账号信息</h2>
      <div>
        <label class="block text-sm text-gray-500 mb-1">邮箱</label>
        <div class="text-sm text-gray-800 bg-gray-50 rounded-lg px-3 py-2 border border-gray-200">
          {{ userEmail() }}
        </div>
      </div>
    </div>

    <!-- 个人设置 -->
    <div class="bg-white rounded-xl shadow-sm p-6 space-y-4">
      <h2 class="text-sm font-semibold text-gray-700">个人设置</h2>

      <div>
        <label class="block text-sm text-gray-700 mb-1">昵称</label>
        <input
          v-model="form.nickname"
          type="text"
          placeholder="请输入昵称"
          class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div>
        <label class="block text-sm text-gray-700 mb-1">目标分数</label>
        <select
          v-model="form.targetScore"
          class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
        >
          <option value="">请选择目标分数</option>
          <option v-for="s in ['5.0','5.5','6.0','6.5','7.0','7.5','8.0','8.5','9.0']" :key="s" :value="s">
            Band {{ s }}
          </option>
        </select>
      </div>

      <button
        :disabled="saving"
        class="bg-blue-600 text-white hover:bg-blue-700 rounded-lg px-5 py-2 text-sm font-medium disabled:opacity-50"
        @click="saveProfile"
      >
        {{ saving ? '保存中...' : '保存设置' }}
      </button>
    </div>

    <!-- 退出登录 -->
    <div class="bg-white rounded-xl shadow-sm p-6">
      <h2 class="text-sm font-semibold text-gray-700 mb-3">账号操作</h2>
      <button
        class="border border-red-300 text-red-600 hover:bg-red-50 rounded-lg px-5 py-2 text-sm font-medium"
        @click="userStore.logout()"
      >
        退出登录
      </button>
    </div>
  </div>
</template>
