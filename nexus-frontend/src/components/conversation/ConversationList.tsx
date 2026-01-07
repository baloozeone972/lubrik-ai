import type { Conversation } from '@/types/message.types'
import { ConversationCard } from './ConversationCard'

interface ConversationListProps {
  conversations: Conversation[]
  onSelect: (conversation: Conversation) => void
}

export const ConversationList = ({ conversations, onSelect }: ConversationListProps) => {
  if (conversations.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-600">Aucune conversation</p>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      {conversations.map((conversation) => (
        <ConversationCard
          key={conversation.id}
          conversation={conversation}
          onClick={() => onSelect(conversation)}
        />
      ))}
    </div>
  )
}
