import { Companion } from '@/types/companion.types'
import { Avatar } from '@/components/common/Avatar'
import { Badge } from '@/components/common/Badge'
import { Card } from '@/components/common/Card'

interface CompanionCardProps {
  companion: Companion
  onClick?: () => void
}

export const CompanionCard = ({ companion, onClick }: CompanionCardProps) => {
  return (
    <Card onClick={onClick} className="hover:shadow-lg transition-shadow">
      <div className="flex items-start gap-4">
        <Avatar
          src={companion.avatarUrl}
          alt={companion.name}
          size="lg"
          fallback={companion.name.charAt(0)}
        />
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-800 mb-1">
            {companion.name}
          </h3>
          {companion.description && (
            <p className="text-sm text-gray-600 mb-3 line-clamp-2">
              {companion.description}
            </p>
          )}
          <div className="flex gap-2 flex-wrap">
            <Badge variant="primary" size="sm">
              🎨 Créativité: {companion.personality.creativity}
            </Badge>
            <Badge variant="success" size="sm">
              ❤️ Empathie: {companion.personality.empathy}
            </Badge>
          </div>
        </div>
      </div>
    </Card>
  )
}
