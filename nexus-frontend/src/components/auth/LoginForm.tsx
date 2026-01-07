import { useState, FormEvent } from 'react'
import { useAuthStore } from '@/store/authStore'
import { Input } from '@/components/common/Input'
import { Button } from '@/components/common/Button'
import { validation } from '@/utils/validation'

interface LoginFormProps {
  onSuccess?: () => void
}

export const LoginForm = ({ onSuccess }: LoginFormProps) => {
  const { login, isLoading, error, clearError } = useAuthStore()
  const [formData, setFormData] = useState({ email: '', password: '', rememberMe: false })
  const [errors, setErrors] = useState<Record<string, string>>({})

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    clearError()
    
    const newErrors: Record<string, string> = {}
    const emailError = validation.email(formData.email)
    const passwordError = validation.required(formData.password)
    
    if (emailError) newErrors.email = emailError
    if (passwordError) newErrors.password = passwordError
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors)
      return
    }

    await login(formData.email, formData.password)
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
        label="Mot de passe"
        type="password"
        value={formData.password}
        onChange={(e) => setFormData({ ...formData, password: e.target.value })}
        error={errors.password}
        required
      />

      <div className="flex items-center">
        <input
          type="checkbox"
          id="rememberMe"
          checked={formData.rememberMe}
          onChange={(e) => setFormData({ ...formData, rememberMe: e.target.checked })}
          className="mr-2"
        />
        <label htmlFor="rememberMe" className="text-sm text-gray-600">
          Se souvenir de moi
        </label>
      </div>

      <Button type="submit" loading={isLoading} className="w-full">
        Se connecter
      </Button>
    </form>
  )
}
