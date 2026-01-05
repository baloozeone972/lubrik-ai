import api from './api'
import type { Companion, CreateCompanionRequest, UpdateCompanionRequest, PaginatedResponse } from '@/types'

export const companionService = {
  async getAll(page = 0, size = 10): Promise<PaginatedResponse<Companion>> {
    const response = await api.get<PaginatedResponse<Companion>>('/companions', {
      params: { page, size },
    })
    return response.data
  },

  async getById(id: string): Promise<Companion> {
    const response = await api.get<Companion>(`/companions/${id}`)
    return response.data
  },

  async create(data: CreateCompanionRequest): Promise<Companion> {
    const response = await api.post<Companion>('/companions', data)
    return response.data
  },

  async update(id: string, data: UpdateCompanionRequest): Promise<Companion> {
    const response = await api.put<Companion>(`/companions/${id}`, data)
    return response.data
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/companions/${id}`)
  },

  async uploadAvatar(id: string, file: File): Promise<Companion> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post<Companion>(`/companions/${id}/avatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  },
}
