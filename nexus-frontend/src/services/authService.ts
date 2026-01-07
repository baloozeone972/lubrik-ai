import { apiClient } from './api'

export const authService = {
  async login(email: string, password: string) {
    const res = await apiClient.post('/auth/login', { email, password })
    localStorage.setItem('accessToken', res.data.accessToken)
    return res.data
  },
  
  async register(email: string, username: string, password: string) {
    const res = await apiClient.post('/auth/register', { email, username, password })
    localStorage.setItem('accessToken', res.data.accessToken)
    return res.data
  },
  
  async logout() {
    await apiClient.post('/auth/logout')
    localStorage.removeItem('accessToken')
  }
}
