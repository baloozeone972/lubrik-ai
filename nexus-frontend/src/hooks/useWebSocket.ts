import { useEffect, useRef, useCallback, useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import { useChatStore } from '@/store/chatStore'
import type { WebSocketMessage, Message } from '@/types'

const WS_BASE_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'

interface UseWebSocketOptions {
  conversationId: string
  onMessage?: (message: Message) => void
  onError?: (error: string) => void
}

export function useWebSocket({ conversationId, onMessage, onError }: UseWebSocketOptions) {
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectAttemptsRef = useRef(0)
  const maxReconnectAttempts = 5
  const [isConnected, setIsConnected] = useState(false)

  const { accessToken } = useAuthStore()
  const { setTyping, appendStreamingContent, clearStreamingContent, addMessage } = useChatStore()

  const connect = useCallback(() => {
    if (!accessToken || !conversationId) return

    const wsUrl = `${WS_BASE_URL}/chat?token=${accessToken}&conversationId=${conversationId}`
    const ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      console.log('WebSocket connected')
      setIsConnected(true)
      reconnectAttemptsRef.current = 0
    }

    ws.onmessage = (event) => {
      try {
        const data: WebSocketMessage = JSON.parse(event.data)

        switch (data.type) {
          case 'TYPING':
            setTyping(true)
            break

          case 'CHUNK':
            if (data.content) {
              appendStreamingContent(data.content)
            }
            break

          case 'DONE':
            setTyping(false)
            if (data.messageId) {
              const message: Message = {
                id: data.messageId,
                conversationId,
                role: 'ASSISTANT',
                type: 'TEXT',
                content: useChatStore.getState().streamingContent,
                tokensUsed: data.tokensUsed,
                attachments: [],
                createdAt: new Date().toISOString(),
              }
              addMessage(message)
              clearStreamingContent()
              onMessage?.(message)
            }
            break

          case 'ERROR':
            setTyping(false)
            clearStreamingContent()
            onError?.(data.error || 'Unknown error')
            break
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error)
      }
    }

    ws.onclose = () => {
      console.log('WebSocket disconnected')
      setIsConnected(false)

      // Reconnect with exponential backoff
      if (reconnectAttemptsRef.current < maxReconnectAttempts) {
        const delay = Math.min(1000 * Math.pow(2, reconnectAttemptsRef.current), 30000)
        reconnectAttemptsRef.current++
        setTimeout(connect, delay)
      }
    }

    ws.onerror = (error) => {
      console.error('WebSocket error:', error)
      onError?.('Connection error')
    }

    wsRef.current = ws
  }, [accessToken, conversationId, setTyping, appendStreamingContent, clearStreamingContent, addMessage, onMessage, onError])

  const disconnect = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }
  }, [])

  const sendMessage = useCallback((content: string, attachments?: string[]) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'MESSAGE',
        content,
        attachments: attachments || [],
      }))

      // Add user message locally
      const userMessage: Message = {
        id: crypto.randomUUID(),
        conversationId,
        role: 'USER',
        type: 'TEXT',
        content,
        attachments: [],
        createdAt: new Date().toISOString(),
      }
      addMessage(userMessage)
    }
  }, [conversationId, addMessage])

  useEffect(() => {
    connect()
    return () => disconnect()
  }, [connect, disconnect])

  return {
    isConnected,
    sendMessage,
    disconnect,
    reconnect: connect,
  }
}
