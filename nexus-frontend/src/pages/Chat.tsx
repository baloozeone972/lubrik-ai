import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useChatStore } from '@/store/chatStore'
import { conversationService } from '@/services/conversationService'
import { useWebSocket } from '@/hooks/useWebSocket'
import { Layout } from '@/components/layout'
import { Avatar, Button } from '@/components/common'
import { Send, ArrowLeft, Paperclip, MoreVertical } from 'lucide-react'
import { cn } from '@/utils/cn'
import ReactMarkdown from 'react-markdown'
import type { Message } from '@/types'

export function Chat() {
  const { conversationId } = useParams<{ conversationId: string }>()
  const navigate = useNavigate()
  const [inputValue, setInputValue] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  const {
    currentConversation,
    messages,
    isTyping,
    streamingContent,
    setCurrentConversation,
    setMessages,
  } = useChatStore()

  const { isConnected, sendMessage } = useWebSocket({
    conversationId: conversationId!,
    onError: (error) => console.error('WebSocket error:', error),
  })

  useEffect(() => {
    const loadConversation = async () => {
      if (!conversationId) return
      try {
        const [conversation, messagesRes] = await Promise.all([
          conversationService.getById(conversationId),
          conversationService.getMessages(conversationId),
        ])
        setCurrentConversation(conversation)
        setMessages(messagesRes.content.reverse())
      } catch (error) {
        console.error('Failed to load conversation:', error)
        navigate('/conversations')
      }
    }
    loadConversation()
  }, [conversationId, setCurrentConversation, setMessages, navigate])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamingContent])

  const handleSend = () => {
    if (!inputValue.trim() || !isConnected) return
    sendMessage(inputValue.trim())
    setInputValue('')
    inputRef.current?.focus()
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <Layout showNavbar={false}>
      <div className="h-screen flex flex-col bg-gray-50 dark:bg-gray-900">
        {/* Header */}
        <header className="bg-white dark:bg-gray-800 border-b dark:border-gray-700 px-4 py-3 flex items-center">
          <button
            onClick={() => navigate('/conversations')}
            className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg mr-2"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <Avatar src={currentConversation?.companion?.avatarUrl} size="md" />
          <div className="ml-3 flex-1">
            <h2 className="font-semibold">
              {currentConversation?.companion?.name || 'Chat'}
            </h2>
            <p className="text-sm text-gray-500">
              {isConnected ? (
                <span className="flex items-center">
                  <span className="w-2 h-2 bg-green-500 rounded-full mr-1" />
                  Connecté
                </span>
              ) : (
                'Connexion...'
              )}
            </p>
          </div>
          <button className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg">
            <MoreVertical className="w-5 h-5" />
          </button>
        </header>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {messages.map((message) => (
            <MessageBubble key={message.id} message={message} />
          ))}

          {/* Streaming message */}
          {(isTyping || streamingContent) && (
            <div className="flex justify-start">
              <div className="flex items-end space-x-2 max-w-[80%]">
                <Avatar src={currentConversation?.companion?.avatarUrl} size="sm" />
                <div className="message-assistant px-4 py-2">
                  {streamingContent ? (
                    <ReactMarkdown className="prose dark:prose-invert prose-sm">
                      {streamingContent}
                    </ReactMarkdown>
                  ) : (
                    <div className="typing-indicator">
                      <span />
                      <span />
                      <span />
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input */}
        <div className="bg-white dark:bg-gray-800 border-t dark:border-gray-700 p-4">
          <div className="max-w-4xl mx-auto flex items-end space-x-2">
            <button className="p-2 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300">
              <Paperclip className="w-5 h-5" />
            </button>
            <div className="flex-1 relative">
              <textarea
                ref={inputRef}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Écrivez votre message..."
                rows={1}
                className="w-full px-4 py-3 bg-gray-100 dark:bg-gray-700 rounded-2xl resize-none focus:outline-none focus:ring-2 focus:ring-primary-500"
                style={{ maxHeight: '120px' }}
              />
            </div>
            <Button
              onClick={handleSend}
              disabled={!inputValue.trim() || !isConnected}
              className="rounded-full p-3"
            >
              <Send className="w-5 h-5" />
            </Button>
          </div>
        </div>
      </div>
    </Layout>
  )
}

function MessageBubble({ message }: { message: Message }) {
  const isUser = message.role === 'USER'

  return (
    <div className={cn('flex', isUser ? 'justify-end' : 'justify-start')}>
      <div
        className={cn(
          'max-w-[80%] px-4 py-2',
          isUser ? 'message-user' : 'message-assistant'
        )}
      >
        <ReactMarkdown className="prose dark:prose-invert prose-sm">
          {message.content}
        </ReactMarkdown>
        <p className="text-xs opacity-70 mt-1">
          {new Date(message.createdAt).toLocaleTimeString('fr-FR', {
            hour: '2-digit',
            minute: '2-digit',
          })}
        </p>
      </div>
    </div>
  )
}
