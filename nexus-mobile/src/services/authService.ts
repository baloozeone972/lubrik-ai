import { apiClient } from './api'
import * as SecureStore from 'expo-secure-store'

export const authService = {
  async login(email: string, password: string) {
    const response = await apiClient.post('/auth/login', { email, password })
    await SecureStore.setItemAsync('accessToken', response.data.accessToken)
    return response.data
  },

  async register(email: string, username: string, password: string) {
    const response = await apiClient.post('/auth/register', { email, username, password })
    await SecureStore.setItemAsync('accessToken', response.data.accessToken)
    return response.data
  },

  async logout() {
    await apiClient.post('/auth/logout')
    await SecureStore.deleteItemAsync('accessToken')
  },

  async getCurrentUser() {
    const response = await apiClient.get('/auth/me')
    return response.data
  },
}
