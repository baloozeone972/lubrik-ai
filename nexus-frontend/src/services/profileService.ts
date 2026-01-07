import { apiClient } from './api'
import type { User } from '@/types/auth.types'

export const profileService = {
  async updateProfile(data: Partial<User>) {
    const res = await apiClient.put('/profile', data)
    return res.data
  },

  async uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('avatar', file)
    const res = await apiClient.post('/profile/avatar', formData)
    return res.data.avatarUrl
  },

  async getUsageStats() {
    const res = await apiClient.get('/profile/stats')
    return res.data
  }
}
