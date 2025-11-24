import api from './api'
import type { Subscription, Plan, Invoice } from '@/types'

export interface CreateSubscriptionRequest {
  planId: string
  paymentMethodId: string
}

export interface SetupIntentResponse {
  clientSecret: string
}

export const paymentService = {
  async getPlans(): Promise<Plan[]> {
    const response = await api.get<Plan[]>('/payments/plans')
    return response.data
  },

  async getSubscription(): Promise<Subscription | null> {
    try {
      const response = await api.get<Subscription>('/payments/subscription')
      return response.data
    } catch {
      return null
    }
  },

  async createSetupIntent(): Promise<SetupIntentResponse> {
    const response = await api.post<SetupIntentResponse>('/payments/setup-intent')
    return response.data
  },

  async subscribe(data: CreateSubscriptionRequest): Promise<Subscription> {
    const response = await api.post<Subscription>('/payments/subscribe', data)
    return response.data
  },

  async changePlan(planId: string): Promise<Subscription> {
    const response = await api.put<Subscription>('/payments/subscription', { planId })
    return response.data
  },

  async cancelSubscription(): Promise<void> {
    await api.post('/payments/cancel')
  },

  async reactivateSubscription(): Promise<Subscription> {
    const response = await api.post<Subscription>('/payments/reactivate')
    return response.data
  },

  async getInvoices(): Promise<Invoice[]> {
    const response = await api.get<Invoice[]>('/payments/invoices')
    return response.data
  },
}
