import { useState } from 'react'
import { SubscriptionPlans } from '@/components/payment/SubscriptionPlans'
import { Modal } from '@/components/common/Modal'
import { PaymentForm } from '@/components/payment/PaymentForm'

export const SubscriptionPage = () => {
  const [selectedPlan, setSelectedPlan] = useState<string | null>(null)
  const [showPayment, setShowPayment] = useState(false)
  
  const currentPlan = 'free'

  const handleSelectPlan = (planId: string) => {
    setSelectedPlan(planId)
    if (planId !== 'free') {
      setShowPayment(true)
    }
  }

  const handlePayment = async (data: any) => {
    console.log('Payment:', data)
    alert('Paiement effectué avec succès!')
    setShowPayment(false)
  }

  return (
    <div>
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold mb-4">Choisissez votre Plan</h1>
        <p className="text-xl text-gray-600">
          Débloquez toutes les fonctionnalités de NexusAI
        </p>
      </div>

      <SubscriptionPlans
        currentPlan={currentPlan}
        onSelectPlan={handleSelectPlan}
      />

      <Modal
        isOpen={showPayment}
        onClose={() => setShowPayment(false)}
        title="Paiement"
        size="md"
      >
        <PaymentForm
          amount={selectedPlan === 'basic' ? 9.99 : selectedPlan === 'premium' ? 19.99 : 49.99}
          onSubmit={handlePayment}
        />
      </Modal>
    </div>
  )
}
