import { Link } from 'react-router-dom'
import { Card } from '@/components/common/Card'
import { Button } from '@/components/common/Button'

export const CompanionsScreen = () => {
  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Mes Compagnons</h1>
        <Link to="/companions/new">
          <Button>Nouveau compagnon</Button>
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {[1, 2, 3].map((i) => (
          <Card key={i} padding="lg" hover>
            <div className="text-center">
              <div className="w-24 h-24 bg-gradient-to-br from-blue-400 to-purple-600 rounded-full mx-auto mb-4 flex items-center justify-center">
                <span className="text-4xl">🤖</span>
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                Compagnon {i}
              </h3>
              <p className="text-sm text-gray-600 mb-4">
                Assistant IA personnalisé
              </p>
              <div className="flex gap-2">
                <Link to={`/chat/${i}`} className="flex-1">
                  <Button fullWidth size="sm">Chat</Button>
                </Link>
                <Link to={`/companions/${i}/edit`} className="flex-1">
                  <Button fullWidth size="sm" variant="outline">Éditer</Button>
                </Link>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
