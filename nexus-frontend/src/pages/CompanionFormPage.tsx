import { useState, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Input } from '@/components/common/Input'
import { Button } from '@/components/common/Button'
import { PersonalitySlider } from '@/components/companion/PersonalitySlider'
import { useCompanions } from '@/hooks/useCompanions'
import type { Personality } from '@/types/companion.types'

export const CompanionFormPage = () => {
  const navigate = useNavigate()
  const { create } = useCompanions()
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    personality: {
      creativity: 5,
      empathy: 5,
      humor: 5,
      formality: 5,
      verbosity: 5
    } as Personality
  })

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    await create(formData)
    navigate('/companions')
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-8">Créer un Compagnon</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Input
          label="Nom"
          value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          required
        />

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <textarea
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            className="w-full px-3 py-2 border rounded-lg"
            rows={3}
          />
        </div>

        <div className="space-y-4">
          <h3 className="font-semibold">Personnalité</h3>

          <PersonalitySlider
            label="Créativité"
            icon="🎨"
            value={formData.personality.creativity}
            onChange={(v) => setFormData({
              ...formData,
              personality: { ...formData.personality, creativity: v }
            })}
          />

          <PersonalitySlider
            label="Empathie"
            icon="❤️"
            value={formData.personality.empathy}
            onChange={(v) => setFormData({
              ...formData,
              personality: { ...formData.personality, empathy: v }
            })}
          />

          <PersonalitySlider
            label="Humour"
            icon="😄"
            value={formData.personality.humor}
            onChange={(v) => setFormData({
              ...formData,
              personality: { ...formData.personality, humor: v }
            })}
          />

          <PersonalitySlider
            label="Formalité"
            icon="🎩"
            value={formData.personality.formality}
            onChange={(v) => setFormData({
              ...formData,
              personality: { ...formData.personality, formality: v }
            })}
          />

          <PersonalitySlider
            label="Verbosité"
            icon="💬"
            value={formData.personality.verbosity}
            onChange={(v) => setFormData({
              ...formData,
              personality: { ...formData.personality, verbosity: v }
            })}
          />
        </div>

        <div className="flex gap-4">
          <Button type="submit" className="flex-1">
            Créer le Compagnon
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate('/companions')}
          >
            Annuler
          </Button>
        </div>
      </form>
    </div>
  )
}
