import request from '@/utils/request'

export function getTopics(params) {
  return request.get('/api/writing/topics', { params })
}

export function submitEssay(data) {
  return request.post('/api/writing/submit', data)
}

export function getSubmission(submissionId) {
  return request.get(`/api/writing/submissions/${submissionId}`)
}

export function getHistory(params) {
  return request.get('/api/writing/history', { params })
}
