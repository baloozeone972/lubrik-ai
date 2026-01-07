import { create } from 'zustand'
import * as SecureStore from 'expo-secure-store'
import { authService } from '@/services/authService'

interface AuthState {
  user: any | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
  initialize: () => Promise<void>
  login: (email: string, password: string) => Promise<void>
  register: (email: string, username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  clearError: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,

  initialize: async () => {
    try {
      const token = await SecureStore.getItemAsync('accessToken')
      if (token) {
        const user = await authService.getCurrentUser()
        set({ user, isAuthenticated: true })
      }
    } catch (error) {
      await SecureStore.deleteItemAsync('accessToken')
      set({ isAuthenticated: false })
    }
  },

  login: async (email, password) => {
    set({ isLoading: true, error: null })
    try {
      const data = await authService.login(email, password)
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
      const data = await authService.register(email, username, password)
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

  clearError: () => set({ error: null }),
}))
