import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { companionService } from '@/services/companionService'
import { Layout } from '@/components/layout'
import { Button, Input, Avatar } from '@/components/common'
import { ArrowLeft, Upload, X } from 'lucide-react'
import type { CompanionStyle } from '@/types'

const companionSchema = z.object({
  name: z.string().min(1, 'Nom requis').max(50, 'Maximum 50 caractères'),
  description: z.string().max(500, 'Maximum 500 caractères').optional(),
  style: z.enum(['FRIENDLY', 'PROFESSIONAL', 'PLAYFUL', 'WISE', 'CREATIVE']),
  traits: z.string().optional(),
  specialties: z.string().optional(),
  customPrompt: z.string().max(2000, 'Maximum 2000 caractères').optional(),
})

type CompanionForm = z.infer<typeof companionSchema>

const styles: { value: CompanionStyle; label: string; description: string }[] = [
  { value: 'FRIENDLY', label: 'Amical', description: 'Chaleureux et accessible' },
  { value: 'PROFESSIONAL', label: 'Professionnel', description: 'Formel et précis' },
  { value: 'PLAYFUL', label: 'Joueur', description: 'Fun et humoristique' },
  { value: 'WISE', label: 'Sage', description: 'Réfléchi et philosophe' },
  { value: 'CREATIVE', label: 'Créatif', description: 'Imaginatif et original' },
]

export function CompanionForm() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const isEditing = Boolean(id)
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null)
  const [avatarFile, setAvatarFile] = useState<File | null>(null)
  const [error, setError] = useState('')

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CompanionForm>({
    resolver: zodResolver(companionSchema),
    defaultValues: {
      style: 'FRIENDLY',
    },
  })

  const selectedStyle = watch('style')

  useEffect(() => {
    if (id) {
      companionService.getById(id).then((companion) => {
        setValue('name', companion.name)
        setValue('description', companion.description)
        setValue('style', companion.style)
        setValue('traits', companion.personality?.traits?.join(', ') || '')
        setValue('specialties', companion.personality?.specialties?.join(', ') || '')
        setValue('customPrompt', companion.personality?.customPrompt || '')
        if (companion.avatarUrl) {
          setAvatarPreview(companion.avatarUrl)
        }
      })
    }
  }, [id, setValue])

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setAvatarFile(file)
      setAvatarPreview(URL.createObjectURL(file))
    }
  }

  const onSubmit = async (data: CompanionForm) => {
    try {
      setError('')
      const payload = {
        name: data.name,
        description: data.description || '',
        style: data.style,
        personality: {
          traits: data.traits?.split(',').map((t) => t.trim()).filter(Boolean) || [],
          specialties: data.specialties?.split(',').map((s) => s.trim()).filter(Boolean) || [],
          tone: data.style.toLowerCase(),
          customPrompt: data.customPrompt || '',
        },
      }

      let companion
      if (isEditing && id) {
        companion = await companionService.update(id, payload)
      } else {
        companion = await companionService.create(payload)
      }

      if (avatarFile) {
        await companionService.uploadAvatar(companion.id, avatarFile)
      }

      navigate('/companions')
    } catch (err: any) {
      setError(err.response?.data?.message || 'Une erreur est survenue')
    }
  }

  return (
    <Layout>
      <div className="max-w-2xl mx-auto px-4 py-8">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center text-gray-600 hover:text-gray-900 dark:hover:text-white mb-6"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Retour
        </button>

        <h1 className="text-2xl font-bold mb-8">
          {isEditing ? 'Modifier le compagnon' : 'Créer un compagnon'}
        </h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {error && (
            <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 text-red-600 text-sm">
              {error}
            </div>
          )}

          {/* Avatar */}
          <div className="flex items-center space-x-4">
            <Avatar src={avatarPreview} size="xl" />
            <div>
              <label className="btn-secondary cursor-pointer inline-flex items-center">
                <Upload className="w-4 h-4 mr-2" />
                Changer l'avatar
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleAvatarChange}
                  className="hidden"
                />
              </label>
              {avatarPreview && (
                <button
                  type="button"
                  onClick={() => {
                    setAvatarPreview(null)
                    setAvatarFile(null)
                  }}
                  className="ml-2 text-sm text-red-600"
                >
                  Supprimer
                </button>
              )}
            </div>
          </div>

          {/* Name */}
          <Input
            id="name"
            label="Nom du compagnon"
            placeholder="Ex: Assistant Tech"
            error={errors.name?.message}
            {...register('name')}
          />

          {/* Description */}
          <div>
            <label className="label">Description</label>
            <textarea
              {...register('description')}
              placeholder="Décrivez votre compagnon en quelques mots..."
              rows={3}
              className="input"
            />
            {errors.description && (
              <p className="error-text">{errors.description.message}</p>
            )}
          </div>

          {/* Style */}
          <div>
            <label className="label">Style de personnalité</label>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mt-2">
              {styles.map((style) => (
                <label
                  key={style.value}
                  className={`card p-4 cursor-pointer transition-all ${
                    selectedStyle === style.value
                      ? 'ring-2 ring-primary-500 border-primary-500'
                      : 'hover:border-gray-300'
                  }`}
                >
                  <input
                    type="radio"
                    value={style.value}
                    {...register('style')}
                    className="sr-only"
                  />
                  <p className="font-medium">{style.label}</p>
                  <p className="text-sm text-gray-500">{style.description}</p>
                </label>
              ))}
            </div>
          </div>

          {/* Traits */}
          <Input
            id="traits"
            label="Traits de caractère (séparés par des virgules)"
            placeholder="Ex: patient, curieux, empathique"
            {...register('traits')}
          />

          {/* Specialties */}
          <Input
            id="specialties"
            label="Spécialités (séparées par des virgules)"
            placeholder="Ex: technologie, musique, cuisine"
            {...register('specialties')}
          />

          {/* Custom prompt */}
          <div>
            <label className="label">Instructions personnalisées (optionnel)</label>
            <textarea
              {...register('customPrompt')}
              placeholder="Ajoutez des instructions spécifiques pour personnaliser le comportement..."
              rows={4}
              className="input"
            />
            {errors.customPrompt && (
              <p className="error-text">{errors.customPrompt.message}</p>
            )}
          </div>

          {/* Submit */}
          <div className="flex justify-end space-x-3">
            <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
              Annuler
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              {isEditing ? 'Enregistrer' : 'Créer le compagnon'}
            </Button>
          </div>
        </form>
      </div>
    </Layout>
  )
}
