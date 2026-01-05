import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { companionService } from '@/services/companionService'
import { conversationService } from '@/services/conversationService'
import { Layout } from '@/components/layout'
import { Avatar, Button } from '@/components/common'
import { MessageSquare, Bot, Plus, ArrowRight, Sparkles } from 'lucide-react'
import type { Companion, Conversation } from '@/types'

export function Dashboard() {
  const { user } = useAuthStore()
  const [companions, setCompanions] = useState<Companion[]>([])
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const loadData = async () => {
      try {
        const [companionsRes, conversationsRes] = await Promise.all([
          companionService.getAll(0, 5),
          conversationService.getAll(0, 5),
        ])
        setCompanions(companionsRes.content)
        setConversations(conversationsRes.content)
      } catch (error) {
        console.error('Failed to load dashboard data:', error)
      } finally {
        setIsLoading(false)
      }
    }
    loadData()
  }, [])

  return (
    <Layout>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold">
            Bonjour, {user?.username} !
          </h1>
          <p className="text-gray-600 dark:text-gray-400 mt-1">
            Bienvenue sur NexusAI. Que voulez-vous faire aujourd'hui ?
          </p>
        </div>

        {/* Quick actions */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <Link
            to="/companions/new"
            className="card p-6 hover:shadow-md transition-shadow flex items-center space-x-4"
          >
            <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/20 rounded-lg flex items-center justify-center">
              <Plus className="w-6 h-6 text-primary-600" />
            </div>
            <div>
              <h3 className="font-semibold">Créer un compagnon</h3>
              <p className="text-sm text-gray-500">Personnalisez votre IA</p>
            </div>
          </Link>

          <Link
            to="/conversations"
            className="card p-6 hover:shadow-md transition-shadow flex items-center space-x-4"
          >
            <div className="w-12 h-12 bg-green-100 dark:bg-green-900/20 rounded-lg flex items-center justify-center">
              <MessageSquare className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <h3 className="font-semibold">Nouvelle conversation</h3>
              <p className="text-sm text-gray-500">Démarrez un chat</p>
            </div>
          </Link>

          <Link
            to="/subscription"
            className="card p-6 hover:shadow-md transition-shadow flex items-center space-x-4"
          >
            <div className="w-12 h-12 bg-purple-100 dark:bg-purple-900/20 rounded-lg flex items-center justify-center">
              <Sparkles className="w-6 h-6 text-purple-600" />
            </div>
            <div>
              <h3 className="font-semibold">Passer Premium</h3>
              <p className="text-sm text-gray-500">Messages illimités</p>
            </div>
          </Link>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Companions */}
          <div className="card">
            <div className="p-4 border-b dark:border-gray-700 flex items-center justify-between">
              <h2 className="font-semibold flex items-center">
                <Bot className="w-5 h-5 mr-2" />
                Mes compagnons
              </h2>
              <Link to="/companions" className="text-sm text-primary-600 hover:text-primary-700">
                Voir tout
              </Link>
            </div>
            <div className="p-4">
              {isLoading ? (
                <div className="animate-pulse space-y-3">
                  {[...Array(3)].map((_, i) => (
                    <div key={i} className="h-16 bg-gray-200 dark:bg-gray-700 rounded-lg" />
                  ))}
                </div>
              ) : companions.length === 0 ? (
                <div className="text-center py-8">
                  <Bot className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                  <p className="text-gray-500">Aucun compagnon créé</p>
                  <Link to="/companions/new">
                    <Button size="sm" className="mt-3">
                      <Plus className="w-4 h-4 mr-1" />
                      Créer mon premier compagnon
                    </Button>
                  </Link>
                </div>
              ) : (
                <div className="space-y-3">
                  {companions.map((companion) => (
                    <Link
                      key={companion.id}
                      to={`/companions/${companion.id}`}
                      className="flex items-center p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
                    >
                      <Avatar src={companion.avatarUrl} size="md" />
                      <div className="ml-3 flex-1">
                        <h4 className="font-medium">{companion.name}</h4>
                        <p className="text-sm text-gray-500 truncate">
                          {companion.description}
                        </p>
                      </div>
                      <ArrowRight className="w-5 h-5 text-gray-400" />
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Recent conversations */}
          <div className="card">
            <div className="p-4 border-b dark:border-gray-700 flex items-center justify-between">
              <h2 className="font-semibold flex items-center">
                <MessageSquare className="w-5 h-5 mr-2" />
                Conversations récentes
              </h2>
              <Link to="/conversations" className="text-sm text-primary-600 hover:text-primary-700">
                Voir tout
              </Link>
            </div>
            <div className="p-4">
              {isLoading ? (
                <div className="animate-pulse space-y-3">
                  {[...Array(3)].map((_, i) => (
                    <div key={i} className="h-16 bg-gray-200 dark:bg-gray-700 rounded-lg" />
                  ))}
                </div>
              ) : conversations.length === 0 ? (
                <div className="text-center py-8">
                  <MessageSquare className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                  <p className="text-gray-500">Aucune conversation</p>
                  <Link to="/conversations">
                    <Button size="sm" className="mt-3">
                      <Plus className="w-4 h-4 mr-1" />
                      Démarrer une conversation
                    </Button>
                  </Link>
                </div>
              ) : (
                <div className="space-y-3">
                  {conversations.map((conversation) => (
                    <Link
                      key={conversation.id}
                      to={`/chat/${conversation.id}`}
                      className="flex items-center p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
                    >
                      <Avatar src={conversation.companion?.avatarUrl} size="md" />
                      <div className="ml-3 flex-1">
                        <h4 className="font-medium">
                          {conversation.companion?.name || 'Conversation'}
                        </h4>
                        <p className="text-sm text-gray-500">
                          {conversation.title || 'Sans titre'}
                        </p>
                      </div>
                      <ArrowRight className="w-5 h-5 text-gray-400" />
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}
