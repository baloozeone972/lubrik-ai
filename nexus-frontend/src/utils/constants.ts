export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'
export const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'

export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  DASHBOARD: '/dashboard',
  COMPANIONS: '/companions',
  CHAT: (id: string) => `/chat/${id}`,
  PROFILE: '/profile',
  SUBSCRIPTION: '/subscription'
}

export const SUBSCRIPTION_TIERS = {
  FREE: { maxCompanions: 1, maxTokens: 10000, price: 0 },
  PLUS: { maxCompanions: 3, maxTokens: 50000, price: 9.99 },
  PREMIUM: { maxCompanions: 10, maxTokens: 200000, price: 29.99 },
  VIP: { maxCompanions: 50, maxTokens: 1000000, price: 99.99 },
  VIP_PLUS: { maxCompanions: -1, maxTokens: -1, price: 299.99 }
}
