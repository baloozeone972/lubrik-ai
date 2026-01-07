import { Card } from '@/components/common/Card'
import { Button } from '@/components/common/Button'
import { Badge } from '@/components/common/Badge'

const plans = [
  {
    id: 'free',
    name: 'Gratuit',
    price: 0,
    features: ['1 compagnon', '10 messages/jour', 'Fonctions de base']
  },
  {
    id: 'basic',
    name: 'Basic',
    price: 9.99,
    features: ['3 compagnons', '100 messages/jour', 'Images IA', 'Support email']
  },
  {
    id: 'premium',
    name: 'Premium',
    price: 19.99,
    features: ['10 compagnons', 'Messages illimités', 'Images & Vidéos IA', 'Support prioritaire', 'VR Beta'],
    popular: true
  },
  {
    id: 'vip',
    name: 'VIP+',
    price: 49.99,
    features: ['Compagnons illimités', 'Tout illimité', 'Accès anticipé', 'Support dédié', 'VR Complet']
  }
]

interface SubscriptionPlansProps {
  currentPlan?: string
  onSelectPlan: (planId: string) => void
}

export const SubscriptionPlans = ({ currentPlan, onSelectPlan }: SubscriptionPlansProps) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      {plans.map((plan) => (
        <Card key={plan.id} className={plan.popular ? 'border-2 border-primary-500' : ''}>
          {plan.popular && (
            <Badge variant="primary" className="mb-2">Populaire</Badge>
          )}
          <h3 className="text-2xl font-bold mb-2">{plan.name}</h3>
          <div className="mb-4">
            <span className="text-4xl font-bold">${plan.price}</span>
            <span className="text-gray-600">/mois</span>
          </div>
          <ul className="space-y-2 mb-6">
            {plan.features.map((feature, i) => (
              <li key={i} className="flex items-center gap-2">
                <span className="text-green-500">✓</span>
                <span className="text-sm">{feature}</span>
              </li>
            ))}
          </ul>
          <Button
            onClick={() => onSelectPlan(plan.id)}
            disabled={currentPlan === plan.id}
            variant={plan.popular ? 'primary' : 'outline'}
            className="w-full"
          >
            {currentPlan === plan.id ? 'Plan actuel' : 'Choisir'}
          </Button>
        </Card>
      ))}
    </div>
  )
}
