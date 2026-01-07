export interface Companion {
  id: string
  userId: string
  name: string
  avatarUrl?: string
  personality: {
    openness: number
    conscientiousness: number
    extraversion: number
    agreeableness: number
    neuroticism: number
  }
  voice?: string
  language: string
  systemPrompt?: string
  generation: number
  totalMessages: number
  averageResponseTime: number
  createdAt: string
  updatedAt: string
}

export interface CreateCompanionRequest {
  name: string
  avatarUrl?: string
  personality: {
    openness: number
    conscientiousness: number
    extraversion: number
    agreeableness: number
    neuroticism: number
  }
  voice?: string
  language: string
  systemPrompt?: string
}
