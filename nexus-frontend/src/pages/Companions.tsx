import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { companionService } from '@/services/companionService'
import { Layout } from '@/components/layout'
import { Avatar, Button } from '@/components/common'
import { Plus, Bot, MessageSquare, Edit, Trash2, MoreVertical } from 'lucide-react'
import type { Companion } from '@/types'

export function Companions() {
  const [companions, setCompanions] = useState<Companion[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    loadCompanions()
  }, [])

  const loadCompanions = async () => {
    try {
      const response = await companionService.getAll(0, 50)
      setCompanions(response.content)
    } catch (error) {
      console.error('Failed to load companions:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce compagnon ?')) return
    try {
      await companionService.delete(id)
      setCompanions(companions.filter((c) => c.id !== id))
    } catch (error) {
      console.error('Failed to delete companion:', error)
    }
  }

  const getStyleBadge = (style: string) => {
    const styles: Record<string, string> = {
      FRIENDLY: 'bg-green-100 text-green-700',
      PROFESSIONAL: 'bg-blue-100 text-blue-700',
      PLAYFUL: 'bg-yellow-100 text-yellow-700',
      WISE: 'bg-purple-100 text-purple-700',
      CREATIVE: 'bg-pink-100 text-pink-700',
    }
    const labels: Record<string, string> = {
      FRIENDLY: 'Amical',
      PROFESSIONAL: 'Professionnel',
      PLAYFUL: 'Joueur',
      WISE: 'Sage',
      CREATIVE: 'Créatif',
    }
    return (
      <span className={`px-2 py-1 rounded-full text-xs font-medium ${styles[style] || ''}`}>
        {labels[style] || style}
      </span>
    )
  }

  return (
    <Layout>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold">Mes compagnons</h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">
              Gérez vos compagnons IA personnalisés
            </p>
          </div>
          <Link to="/companions/new">
            <Button>
              <Plus className="w-4 h-4 mr-2" />
              Nouveau compagnon
            </Button>
          </Link>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="card p-6 animate-pulse">
                <div className="flex items-center space-x-4 mb-4">
                  <div className="w-14 h-14 bg-gray-200 dark:bg-gray-700 rounded-full" />
                  <div className="flex-1">
                    <div className="h-5 bg-gray-200 dark:bg-gray-700 rounded w-3/4 mb-2" />
                    <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2" />
                  </div>
                </div>
                <div className="h-16 bg-gray-200 dark:bg-gray-700 rounded" />
              </div>
            ))}
          </div>
        ) : companions.length === 0 ? (
          <div className="text-center py-16">
            <Bot className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium mb-2">Aucun compagnon</h3>
            <p className="text-gray-500 mb-6">
              Créez votre premier compagnon IA pour commencer à discuter
            </p>
            <Link to="/companions/new">
              <Button>
                <Plus className="w-4 h-4 mr-2" />
                Créer mon premier compagnon
              </Button>
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {companions.map((companion) => (
              <div key={companion.id} className="card p-6 hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center space-x-3">
                    <Avatar src={companion.avatarUrl} size="lg" />
                    <div>
                      <h3 className="font-semibold">{companion.name}</h3>
                      {getStyleBadge(companion.style)}
                    </div>
                  </div>
                  <div className="relative group">
                    <button className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg">
                      <MoreVertical className="w-4 h-4" />
                    </button>
                    <div className="absolute right-0 mt-1 w-40 bg-white dark:bg-gray-800 rounded-lg shadow-lg border dark:border-gray-700 py-1 hidden group-hover:block z-10">
                      <Link
                        to={`/companions/${companion.id}/edit`}
                        className="flex items-center px-4 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-700"
                      >
                        <Edit className="w-4 h-4 mr-2" />
                        Modifier
                      </Link>
                      <button
                        onClick={() => handleDelete(companion.id)}
                        className="flex items-center w-full px-4 py-2 text-sm text-red-600 hover:bg-gray-100 dark:hover:bg-gray-700"
                      >
                        <Trash2 className="w-4 h-4 mr-2" />
                        Supprimer
                      </button>
                    </div>
                  </div>
                </div>

                <p className="text-gray-600 dark:text-gray-400 text-sm mb-4 line-clamp-2">
                  {companion.description}
                </p>

                {companion.personality?.traits && (
                  <div className="flex flex-wrap gap-1 mb-4">
                    {companion.personality.traits.slice(0, 3).map((trait, i) => (
                      <span
                        key={i}
                        className="px-2 py-0.5 bg-gray-100 dark:bg-gray-700 rounded text-xs"
                      >
                        {trait}
                      </span>
                    ))}
                  </div>
                )}

                <Link
                  to={`/conversations/new?companionId=${companion.id}`}
                  className="flex items-center justify-center w-full py-2 mt-2 text-primary-600 hover:bg-primary-50 dark:hover:bg-primary-900/20 rounded-lg font-medium"
                >
                  <MessageSquare className="w-4 h-4 mr-2" />
                  Démarrer une conversation
                </Link>
              </div>
            ))}
          </div>
        )}
      </div>
    </Layout>
  )
}
