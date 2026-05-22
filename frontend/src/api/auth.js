import request from '@/utils/request'

export function register(data) {
  return request.post('/api/auth/register', data)
}

export function login(data) {
  return request.post('/api/auth/login', data)
}
