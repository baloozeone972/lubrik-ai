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
