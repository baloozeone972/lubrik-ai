import { formatDate } from '@/utils/helpers'
import type { Conversation } from '@/types/message.types'

interface ConversationCardProps {
  conversation: Conversation
  onClick: () => void
}

export const ConversationCard = ({ conversation, onClick }: ConversationCardProps) => {
  return (
    <div
      onClick={onClick}
      className="p-4 bg-white rounded-lg shadow hover:shadow-md transition-shadow cursor-pointer"
    >
      <div className="flex justify-between items-start mb-2">
        <h3 className="font-semibold text-gray-800">
          {conversation.title || 'Conversation sans titre'}
        </h3>
        <span className="text-xs text-gray-500">
          {formatDate(conversation.lastMessageAt)}
        </span>
      </div>
      <p className="text-sm text-gray-600">
        Dernière activité : {formatDate(conversation.lastMessageAt)}
      </p>
    </div>
  )
}
