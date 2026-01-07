import { Message } from '@/types/message.types'
import { cn } from '@/utils/helpers'
import { formatDate } from '@/utils/helpers'

interface ChatMessageProps {
  message: Message
}

export const ChatMessage = ({ message }: ChatMessageProps) => {
  const isUser = message.role === 'user'

  return (
    <div className={cn('flex', isUser ? 'justify-end' : 'justify-start')}>
      <div className={cn(
        'max-w-[70%] rounded-lg px-4 py-3',
        isUser 
          ? 'bg-primary-500 text-white' 
          : 'bg-gray-100 text-gray-800'
      )}>
        <p className="whitespace-pre-wrap">{message.content}</p>
        <p className={cn(
          'text-xs mt-2',
          isUser ? 'text-primary-100' : 'text-gray-500'
        )}>
          {formatDate(message.createdAt)}
        </p>
      </div>
    </div>
  )
}
