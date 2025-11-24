// User types
export interface User {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  emailVerified: boolean;
  avatarUrl?: string;
  subscriptionType: SubscriptionType;
  createdAt: string;
}

export type UserRole = 'USER' | 'ADMIN' | 'MODERATOR';
export type SubscriptionType = 'FREE' | 'BASIC' | 'PREMIUM' | 'ENTERPRISE';

// Auth types
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
  username: string;
  role: string;
  emailVerified: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

// Companion types
export interface Companion {
  id: string;
  userId: string;
  name: string;
  description: string;
  personality: CompanionPersonality;
  style: CompanionStyle;
  status: CompanionStatus;
  avatarUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CompanionPersonality {
  traits: string[];
  tone: string;
  specialties: string[];
  customPrompt?: string;
}

export type CompanionStyle = 'FRIENDLY' | 'PROFESSIONAL' | 'PLAYFUL' | 'WISE' | 'CREATIVE';
export type CompanionStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

// Conversation types
export interface Conversation {
  id: string;
  userId: string;
  companionId: string;
  companion?: Companion;
  title: string;
  status: ConversationStatus;
  lastMessageAt: string;
  createdAt: string;
}

export type ConversationStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

// Message types
export interface Message {
  id: string;
  conversationId: string;
  role: MessageRole;
  type: MessageType;
  content: string;
  tokensUsed?: number;
  attachments: Attachment[];
  createdAt: string;
}

export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM';
export type MessageType = 'TEXT' | 'IMAGE' | 'AUDIO' | 'VIDEO';

export interface Attachment {
  id: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  url: string;
}

// API types
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
}

// Navigation types
export type RootStackParamList = {
  Auth: undefined;
  Main: undefined;
};

export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
  ForgotPassword: undefined;
};

export type MainTabParamList = {
  Home: undefined;
  Companions: undefined;
  Conversations: undefined;
  Profile: undefined;
};

export type HomeStackParamList = {
  Dashboard: undefined;
};

export type CompanionStackParamList = {
  CompanionList: undefined;
  CompanionDetail: { id: string };
  CompanionForm: { id?: string };
};

export type ConversationStackParamList = {
  ConversationList: undefined;
  Chat: { conversationId: string };
};
