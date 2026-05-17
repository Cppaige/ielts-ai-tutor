import http from './http'

export function login(email: string, password: string) {
  return http.post('/auth/login', { email, password })
}

export function register(email: string, password: string, nickname: string) {
  return http.post('/auth/register', { email, password, nickname })
}

export function submitWriting(topicId: number, taskType: number, essayText: string) {
  return http.post('/writing/submit', { topicId, taskType, essayText })
}

export function getWritingSubmission(submissionId: number) {
  return http.get(`/writing/submissions/${submissionId}`)
}

export function startSpeakingSession(topicId: number, persona: string) {
  return http.post('/speaking/sessions', { topicId, persona })
}

export function sendSpeakingTurn(sessionId: number, transcriptOverride: string) {
  return http.post(`/speaking/sessions/${sessionId}/turns`, { transcriptOverride })
}

export function getWritingTopics() {
  return http.get('/data/writing-topics')
}

export function getSpeakingTopics() {
  return http.get('/data/speaking-topics')
}

export function getPracticeRecords(type?: string) {
  const params = type ? { type } : {}
  return http.get('/data/practice-records', { params })
}
