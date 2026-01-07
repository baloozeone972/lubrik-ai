export interface User {
  id: string
  email: string
  username: string
  displayName: string
  profileImageUrl?: string
  subscriptionTier: 'FREE' | 'PLUS' | 'PREMIUM' | 'VIP' | 'VIP_PLUS'
  tokenBalance: number
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  username: string
  password: string
}
