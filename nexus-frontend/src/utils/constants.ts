export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  DASHBOARD: '/dashboard',
  COMPANIONS: '/companions',
  CHAT: (id: string) => `/chat/${id}`
}

export const SUBSCRIPTION_LIMITS = {
  FREE: { maxCompanions: 1, maxTokens: 10000 },
  PLUS: { maxCompanions: 3, maxTokens: 50000 },
  PREMIUM: { maxCompanions: 10, maxTokens: 200000 }
}
