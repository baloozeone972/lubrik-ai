import { apiClient } from './api'
import type { Companion, CreateCompanionRequest } from '@/types/companion.types'

export const companionService = {
  async getAll(): Promise<Companion[]> {
    const response = await apiClient.get('/companions')
    return response.data
  },

  async getById(id: string): Promise<Companion> {
    const response = await apiClient.get(`/companions/${id}`)
    return response.data
  },

  async create(data: CreateCompanionRequest): Promise<Companion> {
    const response = await apiClient.post('/companions', data)
    return response.data
  },

  async update(id: string, data: Partial<CreateCompanionRequest>): Promise<Companion> {
    const response = await apiClient.put(`/companions/${id}`, data)
    return response.data
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/companions/${id}`)
  }
}
