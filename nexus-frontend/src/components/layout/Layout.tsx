import { Outlet, Link } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

export const Layout = () => {
  const { user, logout } = useAuthStore()

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center gap-8">
              <Link to="/dashboard" className="text-2xl font-bold text-blue-600">
                NexusAI
              </Link>
              <div className="hidden md:flex gap-4">
                <Link to="/dashboard" className="px-3 py-2 text-sm font-medium text-gray-700 hover:text-blue-600">
                  Dashboard
                </Link>
                <Link to="/companions" className="px-3 py-2 text-sm font-medium text-gray-700 hover:text-blue-600">
                  Compagnons
                </Link>
                <Link to="/conversations" className="px-3 py-2 text-sm font-medium text-gray-700 hover:text-blue-600">
                  Conversations
                </Link>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <Link to="/profile" className="text-sm font-medium text-gray-700">
                {user?.username}
              </Link>
              <button
                onClick={() => logout()}
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-red-600"
              >
                Déconnexion
              </button>
            </div>
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}
