import { useState, FormEvent } from 'react'
import { useAuthStore } from '@/store/authStore'
import { Input } from '@/components/common/Input'
import { Button } from '@/components/common/Button'
import { validation } from '@/utils/validation'

interface RegisterFormProps {
  onSuccess?: () => void
}

export const RegisterForm = ({ onSuccess }: RegisterFormProps) => {
  const { register, isLoading, error, clearError } = useAuthStore()
  const [formData, setFormData] = useState({
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
    acceptTerms: false
  })
  const [errors, setErrors] = useState<Record<string, string>>({})

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    clearError()
    
    const newErrors: Record<string, string> = {}
    const emailError = validation.email(formData.email)
    const usernameError = validation.required(formData.username)
    const passwordError = validation.password(formData.password)
    
    if (emailError) newErrors.email = emailError
    if (usernameError) newErrors.username = usernameError
    if (passwordError) newErrors.password = passwordError
    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Les mots de passe ne correspondent pas'
    }
    if (!formData.acceptTerms) {
      newErrors.acceptTerms = 'Vous devez accepter les conditions'
    }
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors)
      return
    }

    await register(formData.email, formData.username, formData.password)
    if (!error) onSuccess?.()
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      <Input
        label="Email"
        type="email"
        value={formData.email}
        onChange={(e) => setFormData({ ...formData, email: e.target.value })}
        error={errors.email}
        required
      />

      <Input
        label="Nom d'utilisateur"
        value={formData.username}
        onChange={(e) => setFormData({ ...formData, username: e.target.value })}
        error={errors.username}
        required
      />

      <Input
        label="Mot de passe"
        type="password"
        value={formData.password}
        onChange={(e) => setFormData({ ...formData, password: e.target.value })}
        error={errors.password}
        required
      />

      <Input
        label="Confirmer le mot de passe"
        type="password"
        value={formData.confirmPassword}
        onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
        error={errors.confirmPassword}
        required
      />

      <div>
        <label className="flex items-start gap-2">
          <input
            type="checkbox"
            checked={formData.acceptTerms}
            onChange={(e) => setFormData({ ...formData, acceptTerms: e.target.checked })}
            className="mt-1"
          />
          <span className="text-sm text-gray-600">
            J'accepte les conditions d'utilisation et la politique de confidentialité
          </span>
        </label>
        {errors.acceptTerms && (
          <p className="text-red-500 text-sm mt-1">{errors.acceptTerms}</p>
        )}
      </div>

      <Button type="submit" loading={isLoading} className="w-full">
        S'inscrire
      </Button>
    </form>
  )
}
