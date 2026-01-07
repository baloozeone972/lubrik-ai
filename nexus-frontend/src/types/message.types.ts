export interface Message {
  id: string
  conversationId: string
  senderId: string
  content: string
  type: 'TEXT' | 'IMAGE' | 'AUDIO' | 'VIDEO'
  metadata?: Record<string, any>
  createdAt: string
}

export interface CreateMessageRequest {
  conversationId: string
  content: string
  type: 'TEXT' | 'IMAGE' | 'AUDIO' | 'VIDEO'
  metadata?: Record<string, any>
}
