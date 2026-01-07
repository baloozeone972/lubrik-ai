import { apiClient } from './api'

export const mediaService = {
  async uploadImage(file: File): Promise<string> {
    const formData = new FormData()
    formData.append('file', file)
    
    const res = await apiClient.post('/media/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return res.data.url
  },

  async generateImage(prompt: string): Promise<string> {
    const res = await apiClient.post('/media/generate', { prompt })
    return res.data.url
  }
}
