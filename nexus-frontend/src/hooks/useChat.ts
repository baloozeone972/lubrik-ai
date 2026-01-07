import { useState, useEffect } from 'react'
import { messageService } from '@/services/messageService'
import { websocketService } from '@/services/websocketService'
import type { Message } from '@/types/message.types'

export const useChat = (conversationId: string) => {
  const [messages, setMessages] = useState<Message[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isTyping, setIsTyping] = useState(false)

  useEffect(() => {
    loadMessages()
    
    const unsubscribe = websocketService.subscribe('message', (data) => {
      setMessages(prev => [...prev, data.message])
      setIsTyping(false)
    })

    return () => unsubscribe()
  }, [conversationId])

  const loadMessages = async () => {
    try {
      const data = await messageService.getMessages(conversationId)
      setMessages(data)
    } catch (error) {
      console.error('Failed to load messages:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const sendMessage = async (content: string) => {
    const tempMessage: Message = {
      id: Date.now().toString(),
      conversationId,
      role: 'user',
      content,
      createdAt: new Date().toISOString()
    }
    
    setMessages(prev => [...prev, tempMessage])
    setIsTyping(true)

    try {
      await messageService.sendMessage(conversationId, content)
    } catch (error) {
      console.error('Failed to send message:', error)
      setIsTyping(false)
    }
  }

  return {
    messages,
    isLoading,
    isTyping,
    sendMessage
  }
}
