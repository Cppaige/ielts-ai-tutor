import { reactive } from 'vue'

// 全局 Toast 状态，可在任意组件调用 useToast() 触发提示
const state = reactive({
  visible: false,
  message: '',
  type: 'info', // success | error | info
  timer: null,
})

export function useToast() {
  function show(message, type = 'info', duration = 2500) {
    state.message = message
    state.type = type
    state.visible = true
    if (state.timer) clearTimeout(state.timer)
    state.timer = setTimeout(() => {
      state.visible = false
    }, duration)
  }

  return {
    state,
    success: (msg, d) => show(msg, 'success', d),
    error: (msg, d) => show(msg, 'error', d),
    info: (msg, d) => show(msg, 'info', d),
  }
}
