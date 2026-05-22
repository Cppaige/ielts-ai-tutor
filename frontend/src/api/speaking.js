import request from '@/utils/request'

export function getTopics(params) {
  return request.get('/api/speaking/topics', { params })
}

export function createSession(data) {
  return request.post('/api/speaking/sessions', data)
}

export function submitTurn(sessionId, data) {
  return request.post(`/api/speaking/sessions/${sessionId}/turns`, data)
}

export function getReport(sessionId) {
  return request.get(`/api/speaking/sessions/${sessionId}/report`)
}

export function getHistory(params) {
  return request.get('/api/speaking/history', { params })
}
