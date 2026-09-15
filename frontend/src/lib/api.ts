export const API_BASE_URL = (import.meta.env.VITE_API_URL ?? (import.meta.env.DEV ? 'http://localhost:8080' : '')).replace(/\/$/, '')

export type User = {
  id: string
  name: string
  email: string
  avatarUrl: string | null
  createdAt: string
}

export type Domain = { id: string; name: string }
export type InterviewType = {
  id: string
  name: string
  description: string
  sections: { id: string; name: string; orderIndex: number; durationMinutes: number }[]
}
export type Session = {
  id: string
  domain: string
  interviewType: string
  status: string
  role: 'INTERVIEWER' | 'INTERVIEWEE'
  inviteUrl: string
  sections: { name: string; durationMinutes: number }[]
}
export type SessionQuestion = { id: string; sectionName: string; orderIndex: number; text: string; askedAt: string | null }
export type Evaluation = { questionId: string; question: string; aiScore: number; aiFeedback: string; interviewerScore: number | null; interviewerFeedback: string | null }
export type DashboardSession = { id: string; domain: string; interviewType: string; role: Session['role']; status: string; report: string | null; createdAt: string }

type AuthResponse = {
  token: string
  user: User
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('peerview_token')
  const target = `${API_BASE_URL}${path}`
  let response: Response
  try {
    response = await fetch(target, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
    })
  } catch {
    throw new Error(`PeerView could not reach its backend at ${API_BASE_URL || window.location.origin}. Set VITE_API_URL to the Render backend URL.`)
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(body?.message ?? 'Something went wrong. Please try again.')
  }

  return response.json() as Promise<T>
}

export function register(name: string, email: string, password: string) {
  return request<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ name, email, password }),
  })
}

export function login(email: string, password: string) {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export function currentUser() {
  return request<User>('/api/auth/me')
}

export function saveToken(token: string) {
  localStorage.setItem('peerview_token', token)
}

export function clearToken() {
  localStorage.removeItem('peerview_token')
}

export function getCatalog() {
  return Promise.all([request<Domain[]>('/api/domains'), request<InterviewType[]>('/api/interview-types')])
}

export function createSession(domainId: string, interviewTypeId: string, hostRole: Session['role'], peerEmail: string) {
  return request<Session>('/api/sessions', {
    method: 'POST',
    body: JSON.stringify({ domainId, interviewTypeId, hostRole, peerEmail }),
  })
}

export function joinSession(token: string, name: string, email: string) {
  return request<Session>(`/api/sessions/${token}/join`, {
    method: 'POST',
    body: JSON.stringify({ name, email }),
  })
}

export function getInvite(token: string) {
  return request<{ domain: string; interviewType: string; status: string }>(`/api/sessions/invite/${token}`)
}

export function getQuestions(sessionId: string) {
  return request<SessionQuestion[]>(`/api/sessions/${sessionId}/questions`)
}

export function markQuestionAsked(sessionId: string, questionId: string) {
  return request<SessionQuestion>(`/api/sessions/${sessionId}/questions/${questionId}/mark-asked`, { method: 'POST' })
}

export function appendTranscript(sessionId: string, text: string, sectionName: string) {
  return request<{ id: string; text: string; sectionName: string; spokenAt: string; sequenceNo: number }>(`/api/sessions/${sessionId}/transcript`, {
    method: 'POST',
    body: JSON.stringify({ text, sectionName }),
  })
}

export function endSession(sessionId: string) {
  return request<void>(`/api/sessions/${sessionId}/end`, { method: 'POST' })
}

export function getEvaluations(sessionId: string) {
  return request<Evaluation[]>(`/api/sessions/${sessionId}/evaluations`)
}

export function submitReview(sessionId: string, items: { questionId: string; score: number; feedback: string }[], notes: string) {
  return request<{ id: string; finalOverallScore: number; narrative: string; submittedAt: string }>(`/api/sessions/${sessionId}/review`, {
    method: 'POST',
    body: JSON.stringify({ items, notes }),
  })
}

export function getDashboard() {
  return request<DashboardSession[]>('/api/users/me/dashboard')
}