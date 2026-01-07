import { Companion } from '@/types/companion.types'
import { CompanionCard } from './CompanionCard'
import { Loader } from '@/components/common/Loader'

interface CompanionListProps {
  companions: Companion[]
  isLoading?: boolean
  onCompanionClick?: (companion: Companion) => void
}

export const CompanionList = ({ companions, isLoading, onCompanionClick }: CompanionListProps) => {
  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Loader text="Chargement des compagnons..." />
      </div>
    )
  }

  if (companions.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-600 text-lg mb-4">Aucun compagnon pour le moment</p>
        <p className="text-gray-500">Créez votre premier compagnon IA!</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {companions.map((companion) => (
        <CompanionCard
          key={companion.id}
          companion={companion}
          onClick={() => onCompanionClick?.(companion)}
        />
      ))}
    </div>
  )
}
