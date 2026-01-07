import { apiClient } from './api'

export const subscriptionService = {
  async getCurrentPlan() {
    const res = await apiClient.get('/subscription/current')
    return res.data
  },

  async changePlan(planId: string) {
    const res = await apiClient.post('/subscription/change', { planId })
    return res.data
  },

  async cancelPlan() {
    await apiClient.post('/subscription/cancel')
  }
}
