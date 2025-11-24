import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { conversationService } from '@/services/conversationService'
import { companionService } from '@/services/companionService'
import { Layout } from '@/components/layout'
import { Avatar, Button, Modal } from '@/components/common'
import { Plus, MessageSquare, Archive, Trash2, Search } from 'lucide-react'
import { formatDistanceToNow } from 'date-fns'
import { fr } from 'date-fns/locale'
import type { Conversation, Companion } from '@/types'

export function Conversations() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [companions, setCompanions] = useState<Companion[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [showNewModal, setShowNewModal] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')

  const companionIdFromUrl = searchParams.get('companionId')

  useEffect(() => {
    loadData()
  }, [])

  useEffect(() => {
    if (companionIdFromUrl) {
      setShowNewModal(true)
    }
  }, [companionIdFromUrl])

  const loadData = async () => {
    try {
      const [conversationsRes, companionsRes] = await Promise.all([
        conversationService.getAll(0, 50),
        companionService.getAll(0, 50),
      ])
      setConversations(conversationsRes.content)
      setCompanions(companionsRes.content)
    } catch (error) {
      console.error('Failed to load data:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleCreateConversation = async (companionId: string) => {
    try {
      const conversation = await conversationService.create({ companionId })
      navigate(`/chat/${conversation.id}`)
    } catch (error) {
      console.error('Failed to create conversation:', error)
    }
  }

  const handleArchive = async (id: string) => {
    try {
      await conversationService.archive(id)
      setConversations(conversations.filter((c) => c.id !== id))
    } catch (error) {
      console.error('Failed to archive conversation:', error)
    }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette conversation ?')) return
    try {
      await conversationService.delete(id)
      setConversations(conversations.filter((c) => c.id !== id))
    } catch (error) {
      console.error('Failed to delete conversation:', error)
    }
  }

  const filteredConversations = conversations.filter(
    (c) =>
      c.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.companion?.name?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <Layout>
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold">Conversations</h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">
              Vos discussions avec vos compagnons IA
            </p>
          </div>
          <Button onClick={() => setShowNewModal(true)}>
            <Plus className="w-4 h-4 mr-2" />
            Nouvelle conversation
          </Button>
        </div>

        {/* Search */}
        <div className="relative mb-6">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="Rechercher une conversation..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input pl-10"
          />
        </div>

        {isLoading ? (
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="card p-4 animate-pulse">
                <div className="flex items-center space-x-4">
                  <div className="w-12 h-12 bg-gray-200 dark:bg-gray-700 rounded-full" />
                  <div className="flex-1">
                    <div className="h-5 bg-gray-200 dark:bg-gray-700 rounded w-1/3 mb-2" />
                    <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2" />
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : filteredConversations.length === 0 ? (
          <div className="text-center py-16">
            <MessageSquare className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium mb-2">Aucune conversation</h3>
            <p className="text-gray-500 mb-6">
              Commencez une nouvelle conversation avec un de vos compagnons
            </p>
            <Button onClick={() => setShowNewModal(true)}>
              <Plus className="w-4 h-4 mr-2" />
              Nouvelle conversation
            </Button>
          </div>
        ) : (
          <div className="space-y-2">
            {filteredConversations.map((conversation) => (
              <div
                key={conversation.id}
                className="card p-4 hover:shadow-md transition-shadow group"
              >
                <div className="flex items-center">
                  <Link
                    to={`/chat/${conversation.id}`}
                    className="flex items-center flex-1"
                  >
                    <Avatar src={conversation.companion?.avatarUrl} size="md" />
                    <div className="ml-4 flex-1">
                      <h3 className="font-medium">
                        {conversation.companion?.name || 'Conversation'}
                      </h3>
                      <p className="text-sm text-gray-500">
                        {conversation.title || 'Sans titre'} •{' '}
                        {formatDistanceToNow(new Date(conversation.lastMessageAt), {
                          addSuffix: true,
                          locale: fr,
                        })}
                      </p>
                    </div>
                  </Link>
                  <div className="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={() => handleArchive(conversation.id)}
                      className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
                      title="Archiver"
                    >
                      <Archive className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(conversation.id)}
                      className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg text-red-600"
                      title="Supprimer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* New conversation modal */}
        <Modal
          isOpen={showNewModal}
          onClose={() => setShowNewModal(false)}
          title="Nouvelle conversation"
          size="md"
        >
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            Choisissez un compagnon pour démarrer une conversation
          </p>
          {companions.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-gray-500 mb-4">Vous n'avez pas encore de compagnon</p>
              <Link to="/companions/new">
                <Button>Créer un compagnon</Button>
              </Link>
            </div>
          ) : (
            <div className="space-y-2 max-h-96 overflow-y-auto">
              {companions.map((companion) => (
                <button
                  key={companion.id}
                  onClick={() => handleCreateConversation(companion.id)}
                  className="w-full flex items-center p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                >
                  <Avatar src={companion.avatarUrl} size="md" />
                  <div className="ml-3 text-left">
                    <p className="font-medium">{companion.name}</p>
                    <p className="text-sm text-gray-500 truncate">
                      {companion.description}
                    </p>
                  </div>
                </button>
              ))}
            </div>
          )}
        </Modal>
      </div>
    </Layout>
  )
}
