import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { authService } from '@/services/authService'
import type { User } from '@/types/auth.types'

interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  clearError: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      login: async (email, password) => {
        set({ isLoading: true, error: null })
        try {
          const data = await authService.login({ email, password })
          set({ user: data.user, isAuthenticated: true, isLoading: false })
        } catch (error: any) {
          set({ 
            error: error.response?.data?.message || 'Erreur de connexion',
            isLoading: false
          })
        }
      },

      register: async (email, username, password) => {
        set({ isLoading: true, error: null })
        try {
          const data = await authService.register({ email, username, password })
          set({ user: data.user, isAuthenticated: true, isLoading: false })
        } catch (error: any) {
          set({ 
            error: error.response?.data?.message || "Erreur d'inscription",
            isLoading: false
          })
        }
      },

      logout: async () => {
        try {
          await authService.logout()
          set({ user: null, isAuthenticated: false })
        } catch (error) {
          console.error('Logout error:', error)
        }
      },

      clearError: () => set({ error: null })
    }),
    { name: 'auth-storage' }
  )
)
