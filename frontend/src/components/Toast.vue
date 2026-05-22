<script setup>
import { useToast } from '@/utils/toast'

const { state } = useToast()

const typeClass = {
  success: 'bg-green-600',
  error: 'bg-red-600',
  info: 'bg-gray-800',
}
</script>

<template>
  <transition name="toast">
    <div
      v-if="state.visible"
      class="fixed top-6 left-1/2 -translate-x-1/2 z-50 px-5 py-2.5 rounded-lg shadow-lg text-white text-sm"
      :class="typeClass[state.type] || typeClass.info"
    >
      {{ state.message }}
    </div>
  </transition>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px);
}
</style>
