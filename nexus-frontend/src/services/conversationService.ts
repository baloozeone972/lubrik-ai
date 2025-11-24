import api from './api'
import type {
  Conversation,
  CreateConversationRequest,
  Message,
  SendMessageRequest,
  PaginatedResponse,
} from '@/types'

export const conversationService = {
  async getAll(page = 0, size = 20): Promise<PaginatedResponse<Conversation>> {
    const response = await api.get<PaginatedResponse<Conversation>>('/conversations', {
      params: { page, size },
    })
    return response.data
  },

  async getById(id: string): Promise<Conversation> {
    const response = await api.get<Conversation>(`/conversations/${id}`)
    return response.data
  },

  async create(data: CreateConversationRequest): Promise<Conversation> {
    const response = await api.post<Conversation>('/conversations', data)
    return response.data
  },

  async archive(id: string): Promise<void> {
    await api.put(`/conversations/${id}/archive`)
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/conversations/${id}`)
  },

  async getMessages(conversationId: string, page = 0, size = 50): Promise<PaginatedResponse<Message>> {
    const response = await api.get<PaginatedResponse<Message>>(
      `/conversations/${conversationId}/messages`,
      { params: { page, size } }
    )
    return response.data
  },

  async sendMessage(conversationId: string, data: SendMessageRequest): Promise<Message> {
    const response = await api.post<Message>(`/conversations/${conversationId}/messages`, data)
    return response.data
  },
}
