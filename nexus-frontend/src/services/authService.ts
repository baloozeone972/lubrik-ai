import { apiClient } from './api'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types/auth.types'

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post('/auth/login', data)
    localStorage.setItem('accessToken', response.data.accessToken)
    return response.data
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await apiClient.post('/auth/register', data)
    localStorage.setItem('accessToken', response.data.accessToken)
    return response.data
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/logout')
    localStorage.removeItem('accessToken')
  },

  async getCurrentUser() {
    const response = await apiClient.get('/auth/me')
    return response.data
  }
}
