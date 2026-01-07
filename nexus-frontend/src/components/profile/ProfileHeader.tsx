import { Avatar } from '@/components/common/Avatar'
import { Button } from '@/components/common/Button'
import type { User } from '@/types/auth.types'

interface ProfileHeaderProps {
  user: User
  onEditClick?: () => void
}

export const ProfileHeader = ({ user, onEditClick }: ProfileHeaderProps) => {
  return (
    <div className="flex items-center gap-6 mb-8">
      <Avatar src={user.avatarUrl} alt={user.username} size="xl" />
      <div className="flex-1">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">{user.username}</h1>
        <p className="text-gray-600 mb-4">{user.email}</p>
        {onEditClick && (
          <Button onClick={onEditClick} variant="outline" size="sm">
            ✏️ Modifier le profil
          </Button>
        )}
      </div>
    </div>
  )
}
