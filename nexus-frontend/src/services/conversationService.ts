import { apiClient } from './api'
import type { Conversation } from '@/types/message.types'

export const conversationService = {
  async getAll(companionId?: string): Promise<Conversation[]> {
    const url = companionId ? `/conversations?companionId=${companionId}` : '/conversations'
    const res = await apiClient.get(url)
    return res.data
  },

  async getById(id: string): Promise<Conversation> {
    const res = await apiClient.get(`/conversations/${id}`)
    return res.data
  },

  async create(companionId: string, title?: string): Promise<Conversation> {
    const res = await apiClient.post('/conversations', { companionId, title })
    return res.data
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/conversations/${id}`)
  }
}
