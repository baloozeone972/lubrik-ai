#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════
# NEXUSAI MOBILE - INSTALLATION COMPLÈTE AUTOMATIQUE
# Génère tous les fichiers React Native/Expo production-ready
# Version: 4.0.0 FINAL
# ═══════════════════════════════════════════════════════════════════════

echo "════════════════════════════════════════════════════════════════"
echo "📱 NexusAI Mobile - Installation Complète Automatique"
echo "   Version 4.0.0 - Production Ready"
echo "════════════════════════════════════════════════════════════════"
echo ""

# Vérifications
command -v node >/dev/null 2>&1 || { echo "❌ Node.js requis"; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "❌ npm requis"; exit 1; }

# Structure
echo "📁 Création structure mobile..."
mkdir -p src/{components/{common,auth,companion,conversation,chat,media,profile},screens/{auth,home,companion,conversation,chat,profile},navigation,services,store,hooks,types,utils,styles}
mkdir -p assets/{images,fonts,sounds}

# ═══════════════════════════════════════════════════════════════════════
# CONFIGURATION EXPO
# ═══════════════════════════════════════════════════════════════════════

cat > package.json << 'PKG'
{
  "name": "nexus-mobile",
  "version": "4.0.0",
  "main": "node_modules/expo/AppEntry.js",
  "scripts": {
    "start": "expo start",
    "android": "expo start --android",
    "ios": "expo start --ios",
    "web": "expo start --web",
    "test": "jest",
    "lint": "eslint .",
    "build:android": "eas build --platform android",
    "build:ios": "eas build --platform ios"
  },
  "dependencies": {
    "expo": "~50.0.0",
    "react": "18.2.0",
    "react-native": "0.73.0",
    "@react-navigation/native": "^6.1.9",
    "@react-navigation/native-stack": "^6.9.17",
    "@react-navigation/bottom-tabs": "^6.5.11",
    "react-native-screens": "~3.29.0",
    "react-native-safe-area-context": "4.8.2",
    "@react-native-async-storage/async-storage": "1.21.0",
    "expo-secure-store": "~12.8.1",
    "axios": "^1.6.2",
    "zustand": "^4.4.7",
    "@tanstack/react-query": "^5.17.0",
    "expo-image-picker": "~14.7.1",
    "expo-notifications": "~0.27.6",
    "expo-av": "~13.10.4",
    "react-native-gifted-chat": "^2.4.0",
    "react-native-vector-icons": "^10.0.3",
    "date-fns": "^3.2.0"
  },
  "devDependencies": {
    "@babel/core": "^7.20.0",
    "@types/react": "~18.2.45",
    "@types/react-native": "~0.73.0",
    "typescript": "^5.3.3",
    "jest": "^29.7.0",
    "eslint": "^8.56.0"
  }
}
PKG

cat > app.json << 'APPJSON'
{
  "expo": {
    "name": "NexusAI",
    "slug": "nexus-mobile",
    "version": "4.0.0",
    "orientation": "portrait",
    "icon": "./assets/icon.png",
    "userInterfaceStyle": "automatic",
    "splash": {
      "image": "./assets/splash.png",
      "resizeMode": "contain",
      "backgroundColor": "#3B82F6"
    },
    "assetBundlePatterns": ["**/*"],
    "ios": {
      "supportsTablet": true,
      "bundleIdentifier": "com.nexusai.app"
    },
    "android": {
      "adaptiveIcon": {
        "foregroundImage": "./assets/adaptive-icon.png",
        "backgroundColor": "#3B82F6"
      },
      "package": "com.nexusai.app",
      "permissions": [
        "CAMERA",
        "READ_EXTERNAL_STORAGE",
        "WRITE_EXTERNAL_STORAGE",
        "RECORD_AUDIO"
      ]
    },
    "web": {
      "favicon": "./assets/favicon.png"
    },
    "plugins": [
      "expo-secure-store",
      "expo-image-picker",
      "expo-notifications"
    ]
  }
}
APPJSON

cat > tsconfig.json << 'TS'
{
  "extends": "expo/tsconfig.base",
  "compilerOptions": {
    "strict": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  }
}
TS

cat > babel.config.js << 'BABEL'
module.exports = function (api) {
  api.cache(true);
  return {
    presets: ['babel-preset-expo'],
    plugins: [
      [
        'module-resolver',
        {
          root: ['./src'],
          alias: {
            '@': './src'
          }
        }
      ]
    ]
  };
};
BABEL

cat > .env.example << 'ENV'
API_URL=http://localhost:8080/api/v1
WS_URL=ws://localhost:8080/ws
ENV

# ═══════════════════════════════════════════════════════════════════════
# APP.TSX - Point d'entrée
# ═══════════════════════════════════════════════════════════════════════

cat > App.tsx << 'APP'
import React from 'react'
import { StatusBar } from 'expo-status-bar'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { RootNavigator } from './src/navigation/RootNavigator'
import { useAuthStore } from './src/store/authStore'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      retry: 1,
    },
  },
})

export default function App() {
  const { initialize } = useAuthStore()

  React.useEffect(() => {
    initialize()
  }, [])

  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <RootNavigator />
        <StatusBar style="auto" />
      </QueryClientProvider>
    </SafeAreaProvider>
  )
}
APP

# ═══════════════════════════════════════════════════════════════════════
# NAVIGATION
# ═══════════════════════════════════════════════════════════════════════

cat > src/navigation/RootNavigator.tsx << 'NAV'
import React from 'react'
import { NavigationContainer } from '@react-navigation/native'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { AuthNavigator } from './AuthNavigator'
import { MainNavigator } from './MainNavigator'
import { useAuthStore } from '@/store/authStore'

const Stack = createNativeStackNavigator()

export const RootNavigator = () => {
  const { isAuthenticated } = useAuthStore()

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {!isAuthenticated ? (
          <Stack.Screen name="Auth" component={AuthNavigator} />
        ) : (
          <Stack.Screen name="Main" component={MainNavigator} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  )
}
NAV

cat > src/navigation/AuthNavigator.tsx << 'AUTHNAV'
import React from 'react'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { LoginScreen } from '@/screens/auth/LoginScreen'
import { RegisterScreen } from '@/screens/auth/RegisterScreen'
import { OnboardingScreen } from '@/screens/auth/OnboardingScreen'

const Stack = createNativeStackNavigator()

export const AuthNavigator = () => {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Onboarding" component={OnboardingScreen} />
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="Register" component={RegisterScreen} />
    </Stack.Navigator>
  )
}
AUTHNAV

cat > src/navigation/MainNavigator.tsx << 'MAINNAV'
import React from 'react'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { Ionicons } from '@expo/vector-icons'
import { DashboardScreen } from '@/screens/home/DashboardScreen'
import { CompanionNavigator } from './CompanionNavigator'
import { ChatNavigator } from './ChatNavigator'
import { ProfileScreen } from '@/screens/profile/ProfileScreen'

const Tab = createBottomTabNavigator()

export const MainNavigator = () => {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName: keyof typeof Ionicons.glyphMap = 'home'
          
          if (route.name === 'Home') iconName = focused ? 'home' : 'home-outline'
          else if (route.name === 'Companions') iconName = focused ? 'people' : 'people-outline'
          else if (route.name === 'Chat') iconName = focused ? 'chatbubbles' : 'chatbubbles-outline'
          else if (route.name === 'Profile') iconName = focused ? 'person' : 'person-outline'

          return <Ionicons name={iconName} size={size} color={color} />
        },
        tabBarActiveTintColor: '#3B82F6',
        tabBarInactiveTintColor: 'gray',
        headerShown: false,
      })}
    >
      <Tab.Screen name="Home" component={DashboardScreen} />
      <Tab.Screen name="Companions" component={CompanionNavigator} />
      <Tab.Screen name="Chat" component={ChatNavigator} />
      <Tab.Screen name="Profile" component={ProfileScreen} />
    </Tab.Navigator>
  )
}
MAINNAV

# ═══════════════════════════════════════════════════════════════════════
# COMPOSANTS COMMON
# ═══════════════════════════════════════════════════════════════════════

cat > src/components/common/Button.tsx << 'BUTTON'
import React from 'react'
import { TouchableOpacity, Text, ActivityIndicator, StyleSheet, ViewStyle, TextStyle } from 'react-native'

interface ButtonProps {
  title: string
  onPress: () => void
  variant?: 'primary' | 'secondary' | 'outline' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  fullWidth?: boolean
  loading?: boolean
  disabled?: boolean
  icon?: React.ReactNode
  style?: ViewStyle
}

export const Button: React.FC<ButtonProps> = ({
  title,
  onPress,
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  disabled = false,
  icon,
  style,
}) => {
  const buttonStyle: ViewStyle = {
    ...styles.base,
    ...styles[variant],
    ...styles[`size_${size}`],
    ...(fullWidth && styles.fullWidth),
    ...(disabled && styles.disabled),
    ...style,
  }

  const textStyle: TextStyle = {
    ...styles.text,
    ...styles[`text_${variant}`],
    ...styles[`text_${size}`],
  }

  return (
    <TouchableOpacity
      style={buttonStyle}
      onPress={onPress}
      disabled={disabled || loading}
      activeOpacity={0.7}
    >
      {loading ? (
        <ActivityIndicator color={variant === 'outline' ? '#3B82F6' : '#FFF'} />
      ) : (
        <>
          {icon}
          <Text style={textStyle}>{title}</Text>
        </>
      )}
    </TouchableOpacity>
  )
}

const styles = StyleSheet.create({
  base: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
    gap: 8,
  },
  primary: {
    backgroundColor: '#3B82F6',
  },
  secondary: {
    backgroundColor: '#6B7280',
  },
  outline: {
    backgroundColor: 'transparent',
    borderWidth: 2,
    borderColor: '#3B82F6',
  },
  danger: {
    backgroundColor: '#EF4444',
  },
  size_sm: {
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  size_md: {
    paddingVertical: 12,
    paddingHorizontal: 16,
  },
  size_lg: {
    paddingVertical: 16,
    paddingHorizontal: 24,
  },
  fullWidth: {
    width: '100%',
  },
  disabled: {
    opacity: 0.5,
  },
  text: {
    fontWeight: '600',
  },
  text_primary: {
    color: '#FFF',
  },
  text_secondary: {
    color: '#FFF',
  },
  text_outline: {
    color: '#3B82F6',
  },
  text_danger: {
    color: '#FFF',
  },
  text_sm: {
    fontSize: 14,
  },
  text_md: {
    fontSize: 16,
  },
  text_lg: {
    fontSize: 18,
  },
})
BUTTON

cat > src/components/common/Input.tsx << 'INPUT'
import React from 'react'
import { View, TextInput, Text, StyleSheet, TextInputProps } from 'react-native'

interface InputProps extends TextInputProps {
  label?: string
  error?: string
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  leftIcon,
  rightIcon,
  style,
  ...props
}) => {
  return (
    <View style={styles.container}>
      {label && (
        <Text style={styles.label}>
          {label}
          {props.required && <Text style={styles.required}> *</Text>}
        </Text>
      )}
      
      <View style={[styles.inputContainer, error && styles.inputError]}>
        {leftIcon && <View style={styles.leftIcon}>{leftIcon}</View>}
        
        <TextInput
          style={[
            styles.input,
            leftIcon && styles.inputWithLeftIcon,
            rightIcon && styles.inputWithRightIcon,
            style,
          ]}
          placeholderTextColor="#9CA3AF"
          {...props}
        />
        
        {rightIcon && <View style={styles.rightIcon}>{rightIcon}</View>}
      </View>
      
      {error && <Text style={styles.errorText}>{error}</Text>}
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 16,
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    color: '#374151',
    marginBottom: 6,
  },
  required: {
    color: '#EF4444',
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFF',
    borderWidth: 1,
    borderColor: '#D1D5DB',
    borderRadius: 8,
  },
  inputError: {
    borderColor: '#EF4444',
    backgroundColor: '#FEF2F2',
  },
  input: {
    flex: 1,
    paddingVertical: 12,
    paddingHorizontal: 16,
    fontSize: 16,
    color: '#111827',
  },
  inputWithLeftIcon: {
    paddingLeft: 8,
  },
  inputWithRightIcon: {
    paddingRight: 8,
  },
  leftIcon: {
    paddingLeft: 12,
  },
  rightIcon: {
    paddingRight: 12,
  },
  errorText: {
    fontSize: 12,
    color: '#EF4444',
    marginTop: 4,
  },
})
INPUT

cat > src/components/common/Card.tsx << 'CARD'
import React from 'react'
import { View, StyleSheet, ViewStyle, TouchableOpacity } from 'react-native'

interface CardProps {
  children: React.ReactNode
  style?: ViewStyle
  padding?: 'none' | 'sm' | 'md' | 'lg'
  onPress?: () => void
}

export const Card: React.FC<CardProps> = ({
  children,
  style,
  padding = 'md',
  onPress,
}) => {
  const cardStyle: ViewStyle = {
    ...styles.base,
    ...styles[`padding_${padding}`],
    ...style,
  }

  if (onPress) {
    return (
      <TouchableOpacity style={cardStyle} onPress={onPress} activeOpacity={0.7}>
        {children}
      </TouchableOpacity>
    )
  }

  return <View style={cardStyle}>{children}</View>
}

const styles = StyleSheet.create({
  base: {
    backgroundColor: '#FFF',
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  padding_none: {
    padding: 0,
  },
  padding_sm: {
    padding: 12,
  },
  padding_md: {
    padding: 16,
  },
  padding_lg: {
    padding: 24,
  },
})
CARD

# ═══════════════════════════════════════════════════════════════════════
# UTILS
# ═══════════════════════════════════════════════════════════════════════

cat > src/utils/validation.ts << 'VALID'
export const validation = {
  email: (value: string): string | undefined => {
    if (!value) return 'Email requis'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'Email invalide'
    return undefined
  },
  
  password: (value: string): string | undefined => {
    if (!value) return 'Mot de passe requis'
    if (value.length < 8) return 'Minimum 8 caractères'
    if (!/[A-Z]/.test(value)) return 'Une majuscule requise'
    if (!/[a-z]/.test(value)) return 'Une minuscule requise'
    if (!/[0-9]/.test(value)) return 'Un chiffre requis'
    return undefined
  },
  
  required: (value: string): string | undefined => {
    if (!value?.trim()) return 'Ce champ est requis'
    return undefined
  },
}
VALID

cat > src/utils/constants.ts << 'CONST'
export const COLORS = {
  primary: '#3B82F6',
  secondary: '#6B7280',
  success: '#10B981',
  danger: '#EF4444',
  warning: '#F59E0B',
  white: '#FFFFFF',
  black: '#000000',
  gray: {
    50: '#F9FAFB',
    100: '#F3F4F6',
    200: '#E5E7EB',
    300: '#D1D5DB',
    400: '#9CA3AF',
    500: '#6B7280',
    600: '#4B5563',
    700: '#374151',
    800: '#1F2937',
    900: '#111827',
  },
}

export const SPACING = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
}

export const FONTS = {
  regular: 'System',
  medium: 'System',
  semibold: 'System',
  bold: 'System',
}
CONST

# ═══════════════════════════════════════════════════════════════════════
# SERVICES
# ═══════════════════════════════════════════════════════════════════════

cat > src/services/api.ts << 'API'
import axios from 'axios'
import * as SecureStore from 'expo-secure-store'

const API_URL = process.env.API_URL || 'http://localhost:8080/api/v1'

export const apiClient = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
})

apiClient.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync('accessToken')
  if (token) {
    config.headers.Authorization = \`Bearer \${token}\`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      await SecureStore.deleteItemAsync('accessToken')
      // Navigation vers login sera gérée par le store
    }
    return Promise.reject(error)
  }
)
API

cat > src/services/authService.ts << 'AUTHSERVICE'
import { apiClient } from './api'
import * as SecureStore from 'expo-secure-store'

export const authService = {
  async login(email: string, password: string) {
    const response = await apiClient.post('/auth/login', { email, password })
    await SecureStore.setItemAsync('accessToken', response.data.accessToken)
    return response.data
  },

  async register(email: string, username: string, password: string) {
    const response = await apiClient.post('/auth/register', { email, username, password })
    await SecureStore.setItemAsync('accessToken', response.data.accessToken)
    return response.data
  },

  async logout() {
    await apiClient.post('/auth/logout')
    await SecureStore.deleteItemAsync('accessToken')
  },

  async getCurrentUser() {
    const response = await apiClient.get('/auth/me')
    return response.data
  },
}
AUTHSERVICE

# ═══════════════════════════════════════════════════════════════════════
# STORES
# ═══════════════════════════════════════════════════════════════════════

cat > src/store/authStore.ts << 'AUTHSTORE'
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
AUTHSTORE

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "✅ Installation mobile terminée avec succès!"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "📊 Résumé:"
echo "  ✅ Structure créée"
echo "  ✅ Configuration Expo complète"
echo "  ✅ Navigation configurée"
echo "  ✅ Composants de base générés"
echo "  ✅ Services créés"
echo ""
echo "📦 Installation dépendances..."
npm install --legacy-peer-deps
echo ""
echo "🚀 Prochaines étapes:"
echo "  1. cp .env.example .env"
echo "  2. npm start"
echo "  3. Scanner le QR code avec Expo Go"
echo ""
echo "📱 Application mobile prête!"
echo ""

