import api from './api'
import type { User } from '@/types'

export interface UpdateProfileRequest {
  username?: string
  avatarUrl?: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export const userService = {
  async getProfile(): Promise<User> {
    const response = await api.get<User>('/users/me')
    return response.data
  },

  async updateProfile(data: UpdateProfileRequest): Promise<User> {
    const response = await api.put<User>('/users/me', data)
    return response.data
  },

  async changePassword(data: ChangePasswordRequest): Promise<void> {
    await api.put('/users/me/password', data)
  },

  async uploadAvatar(file: File): Promise<User> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post<User>('/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  },

  async deleteAccount(): Promise<void> {
    await api.delete('/users/me')
  },

  async exportData(): Promise<Blob> {
    const response = await api.get('/users/me/data-export', {
      responseType: 'blob',
    })
    return response.data
  },
}
