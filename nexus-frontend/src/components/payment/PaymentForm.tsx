import { useState, FormEvent } from 'react'
import { Input } from '@/components/common/Input'
import { Button } from '@/components/common/Button'

interface PaymentFormProps {
  amount: number
  onSubmit: (data: PaymentData) => Promise<void>
}

interface PaymentData {
  cardNumber: string
  expiryDate: string
  cvv: string
  name: string
}

export const PaymentForm = ({ amount, onSubmit }: PaymentFormProps) => {
  const [formData, setFormData] = useState<PaymentData>({
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    name: ''
  })
  const [isProcessing, setIsProcessing] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setIsProcessing(true)
    try {
      await onSubmit(formData)
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="bg-gray-50 p-4 rounded-lg mb-6">
        <p className="text-sm text-gray-600">Montant à payer</p>
        <p className="text-3xl font-bold text-gray-800">${amount.toFixed(2)}</p>
      </div>

      <Input
        label="Numéro de carte"
        value={formData.cardNumber}
        onChange={(e) => setFormData({ ...formData, cardNumber: e.target.value })}
        placeholder="1234 5678 9012 3456"
        required
      />

      <div className="grid grid-cols-2 gap-4">
        <Input
          label="Date d'expiration"
          value={formData.expiryDate}
          onChange={(e) => setFormData({ ...formData, expiryDate: e.target.value })}
          placeholder="MM/AA"
          required
        />
        <Input
          label="CVV"
          value={formData.cvv}
          onChange={(e) => setFormData({ ...formData, cvv: e.target.value })}
          placeholder="123"
          required
        />
      </div>

      <Input
        label="Nom sur la carte"
        value={formData.name}
        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
        required
      />

      <Button type="submit" loading={isProcessing} className="w-full">
        💳 Payer ${amount.toFixed(2)}
      </Button>

      <p className="text-xs text-gray-500 text-center">
        Paiement sécurisé par Stripe
      </p>
    </form>
  )
}
