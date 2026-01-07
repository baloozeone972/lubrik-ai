import { ReactNode } from 'react'
import { ChatHeader } from './ChatHeader'

interface ChatContainerProps {
  companionName: string
  companionAvatar?: string
  children: ReactNode
  onSettingsClick?: () => void
}

export const ChatContainer = ({ 
  companionName, 
  companionAvatar, 
  children, 
  onSettingsClick 
}: ChatContainerProps) => {
  return (
    <div className="flex flex-col h-full bg-white rounded-lg shadow">
      <ChatHeader
        companionName={companionName}
        companionAvatar={companionAvatar}
        onSettingsClick={onSettingsClick}
      />
      <div className="flex-1 overflow-hidden">
        {children}
      </div>
    </div>
  )
}
