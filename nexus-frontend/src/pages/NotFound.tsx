import { Link } from 'react-router-dom'
import { Button } from '@/components/common/Button'

export const NotFoundScreen = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <h1 className="text-9xl font-bold text-gray-300">404</h1>
        <h2 className="text-2xl font-semibold text-gray-900 mt-4 mb-2">
          Page non trouvée
        </h2>
        <p className="text-gray-600 mb-8">
          La page que vous recherchez n'existe pas.
        </p>
        <Link to="/dashboard">
          <Button>Retour à l'accueil</Button>
        </Link>
      </div>
    </div>
  )
}
