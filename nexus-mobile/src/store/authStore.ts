import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';
import type { User, AuthResponse } from '@/types';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  setAuth: (response: AuthResponse) => Promise<void>;
  setUser: (user: User) => void;
  setLoading: (loading: boolean) => void;
  logout: () => Promise<void>;
  loadStoredAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,

  setAuth: async (response: AuthResponse) => {
    await SecureStore.setItemAsync('accessToken', response.accessToken);
    await SecureStore.setItemAsync('refreshToken', response.refreshToken);

    const user: User = {
      id: response.userId,
      username: response.username,
      email: response.email,
      role: response.role as User['role'],
      emailVerified: response.emailVerified,
      subscriptionType: 'FREE',
      createdAt: new Date().toISOString(),
    };

    await SecureStore.setItemAsync('user', JSON.stringify(user));

    set({
      user,
      isAuthenticated: true,
      isLoading: false,
    });
  },

  setUser: (user: User) => set({ user }),

  setLoading: (isLoading: boolean) => set({ isLoading }),

  logout: async () => {
    await SecureStore.deleteItemAsync('accessToken');
    await SecureStore.deleteItemAsync('refreshToken');
    await SecureStore.deleteItemAsync('user');

    set({
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
  },

  loadStoredAuth: async () => {
    try {
      const accessToken = await SecureStore.getItemAsync('accessToken');
      const userStr = await SecureStore.getItemAsync('user');

      if (accessToken && userStr) {
        const user = JSON.parse(userStr) as User;
        set({
          user,
          isAuthenticated: true,
          isLoading: false,
        });
      } else {
        set({ isLoading: false });
      }
    } catch {
      set({ isLoading: false });
    }
  },
}));
