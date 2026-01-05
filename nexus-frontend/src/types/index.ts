// User types
export interface User {
  id: string
  username: string
  email: string
  role: UserRole
  emailVerified: boolean
  avatarUrl?: string
  subscriptionType: SubscriptionType
  createdAt: string
}

export type UserRole = 'USER' | 'ADMIN' | 'MODERATOR'
export type SubscriptionType = 'FREE' | 'BASIC' | 'PREMIUM' | 'ENTERPRISE'
export type AccountStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'DELETED'

// Auth types
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: string
  email: string
  username: string
  role: string
  emailVerified: boolean
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

// Companion types
export interface Companion {
  id: string
  userId: string
  name: string
  description: string
  personality: CompanionPersonality
  style: CompanionStyle
  status: CompanionStatus
  avatarUrl?: string
  createdAt: string
  updatedAt: string
}

export interface CompanionPersonality {
  traits: string[]
  tone: string
  specialties: string[]
  customPrompt?: string
}

export type CompanionStyle = 'FRIENDLY' | 'PROFESSIONAL' | 'PLAYFUL' | 'WISE' | 'CREATIVE'
export type CompanionStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'

export interface CreateCompanionRequest {
  name: string
  description: string
  personality: CompanionPersonality
  style: CompanionStyle
}

export interface UpdateCompanionRequest extends Partial<CreateCompanionRequest> {
  status?: CompanionStatus
}

// Conversation types
export interface Conversation {
  id: string
  userId: string
  companionId: string
  companion?: Companion
  title: string
  status: ConversationStatus
  lastMessageAt: string
  createdAt: string
}

export type ConversationStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED'

export interface CreateConversationRequest {
  companionId: string
  title?: string
}

// Message types
export interface Message {
  id: string
  conversationId: string
  role: MessageRole
  type: MessageType
  content: string
  tokensUsed?: number
  attachments: Attachment[]
  createdAt: string
}

export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM'
export type MessageType = 'TEXT' | 'IMAGE' | 'AUDIO' | 'VIDEO'

export interface Attachment {
  id: string
  fileName: string
  fileType: string
  fileSize: number
  url: string
}

export interface SendMessageRequest {
  content: string
  attachments?: string[]
}

// Payment types
export interface Subscription {
  id: string
  userId: string
  plan: SubscriptionType
  status: SubscriptionStatus
  currentPeriodStart: string
  currentPeriodEnd: string
  cancelAtPeriodEnd: boolean
}

export type SubscriptionStatus = 'ACTIVE' | 'PAST_DUE' | 'CANCELED' | 'INCOMPLETE'

export interface Plan {
  id: string
  name: string
  price: number
  interval: 'month' | 'year'
  features: string[]
  messagesPerMonth: number | null
  companionsLimit: number | null
}

export interface Invoice {
  id: string
  amount: number
  status: string
  createdAt: string
  pdfUrl: string
}

// API response types
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface ApiError {
  timestamp: string
  status: number
  error: string
  code: string
  message: string
  path: string
}

// WebSocket types
export interface WebSocketMessage {
  type: 'MESSAGE' | 'CHUNK' | 'DONE' | 'ERROR' | 'TYPING'
  content?: string
  messageId?: string
  tokensUsed?: number
  error?: string
}
