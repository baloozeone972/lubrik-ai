import { Link } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { Card } from '@/components/common/Card'

export const DashboardScreen = () => {
  const { user } = useAuthStore()

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">
          Bienvenue, {user?.displayName || user?.username} 👋
        </h1>
        <p className="mt-2 text-gray-600">
          Voici un aperçu de votre activité
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card padding="lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Compagnons</p>
              <p className="mt-2 text-3xl font-bold text-gray-900">3</p>
            </div>
            <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
              <span className="text-2xl">🤖</span>
            </div>
          </div>
        </Card>

        <Card padding="lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Conversations</p>
              <p className="mt-2 text-3xl font-bold text-gray-900">12</p>
            </div>
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
              <span className="text-2xl">💬</span>
            </div>
          </div>
        </Card>

        <Card padding="lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Messages</p>
              <p className="mt-2 text-3xl font-bold text-gray-900">248</p>
            </div>
            <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center">
              <span className="text-2xl">📨</span>
            </div>
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card padding="lg">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Actions rapides</h2>
          <div className="space-y-3">
            <Link
              to="/companions/new"
              className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                <span className="text-xl">➕</span>
              </div>
              <div>
                <p className="font-medium text-gray-900">Nouveau compagnon</p>
                <p className="text-sm text-gray-600">Créer un compagnon IA</p>
              </div>
            </Link>
            <Link
              to="/companions"
              className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                <span className="text-xl">💬</span>
              </div>
              <div>
                <p className="font-medium text-gray-900">Démarrer un chat</p>
                <p className="text-sm text-gray-600">Parler avec un compagnon</p>
              </div>
            </Link>
          </div>
        </Card>

        <Card padding="lg">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Abonnement</h2>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Plan actuel</span>
              <span className="font-semibold text-blue-600">{user?.subscriptionTier}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Tokens restants</span>
              <span className="font-semibold">{user?.tokenBalance?.toLocaleString()}</span>
            </div>
            <Link
              to="/subscription"
              className="block w-full text-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Améliorer mon plan
            </Link>
          </div>
        </Card>
      </div>
    </div>
  )
}
