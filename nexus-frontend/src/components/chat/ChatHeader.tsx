import { Avatar } from '@/components/common/Avatar'
import { Button } from '@/components/common/Button'

interface ChatHeaderProps {
  companionName: string
  companionAvatar?: string
  onSettingsClick?: () => void
}

export const ChatHeader = ({ companionName, companionAvatar, onSettingsClick }: ChatHeaderProps) => {
  return (
    <div className="border-b px-6 py-4 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <Avatar src={companionAvatar} alt={companionName} size="md" />
        <div>
          <h2 className="font-semibold text-gray-800">{companionName}</h2>
          <p className="text-sm text-gray-500">En ligne</p>
        </div>
      </div>
      {onSettingsClick && (
        <Button variant="outline" size="sm" onClick={onSettingsClick}>
          ⚙️
        </Button>
      )}
    </div>
  )
}
