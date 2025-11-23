/**
 * CLIENT FRONTEND REACT - MODULE CONVERSATION
 * 
 * Exemple d'implémentation d'un client pour interagir avec le Module 4
 * 
 * Technologies:
 * - React 18
 * - TypeScript
 * - WebSocket API native
 * - Axios pour REST
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */

// ============================================================================
// TYPES & INTERFACES
// ============================================================================

// types/conversation.types.ts

export enum MessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  AUDIO = 'AUDIO',
  VIDEO = 'VIDEO',
  SYSTEM = 'SYSTEM'
}

export enum MessageSender {
  USER = 'USER',
  COMPANION = 'COMPANION',
  SYSTEM = 'SYSTEM'
}

export enum EmotionType {
  JOY = 'JOY',
  SADNESS = 'SADNESS',
  ANGER = 'ANGER',
  FEAR = 'FEAR',
  SURPRISE = 'SURPRISE',
  LOVE = 'LOVE',
  NEUTRAL = 'NEUTRAL',
  EXCITEMENT = 'EXCITEMENT'
}

export interface Message {
  id: string;
  sender: MessageSender;
  content: string;
  type: MessageType;
  timestamp: string;
  detectedEmotion?: EmotionType;
  emotionConfidence?: number;
  metadata?: Record<string, any>;
}

export interface Conversation {
  id: string;
  userId: string;
  companionId: string;
  title: string;
  messages: Message[];
  context: ConversationContext;
  isEphemeral: boolean;
  tags: string[];
  createdAt: string;
  lastMessageAt: string;
}

export interface ConversationContext {
  topics: string[];
  emotionalTone: string;
  lastSummary: string;
  messageCount: number;
  companionEmotionalState: string;
}

export interface CreateConversationRequest {
  userId: string;
  companionId: string;
  title?: string;
  isEphemeral?: boolean;
  tags?: string[];
}

export interface SendMessageRequest {
  content: string;
  type: MessageType;
  metadata?: Record<string, any>;
}

// ============================================================================
// API CLIENT (REST)
// ============================================================================

// services/conversationApi.ts

import axios, { AxiosInstance } from 'axios';

export class ConversationApiClient {
  private client: AxiosInstance;
  
  constructor(baseURL: string = 'http://localhost:8080/api/v1') {
    this.client = axios.create({
      baseURL,
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 30000,
    });
    
    // Intercepteur pour ajouter le token JWT
    this.client.interceptors.request.use((config) => {
      const token = localStorage.getItem('authToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
  }
  
  /**
   * Crée une nouvelle conversation
   */
  async createConversation(
    request: CreateConversationRequest
  ): Promise<Conversation> {
    const response = await this.client.post<Conversation>(
      '/conversations',
      request
    );
    return response.data;
  }
  
  /**
   * Récupère une conversation par son ID
   */
  async getConversation(conversationId: string): Promise<Conversation> {
    const response = await this.client.get<Conversation>(
      `/conversations/${conversationId}`
    );
    return response.data;
  }
  
  /**
   * Liste les conversations d'un utilisateur
   */
  async getUserConversations(userId: string): Promise<Conversation[]> {
    const response = await this.client.get<Conversation[]>(
      `/conversations/user/${userId}`
    );
    return response.data;
  }
  
  /**
   * Supprime une conversation
   */
  async deleteConversation(conversationId: string): Promise<void> {
    await this.client.delete(`/conversations/${conversationId}`);
  }
  
  /**
   * Envoie un message (REST - non temps réel)
   */
  async sendMessage(
    conversationId: string,
    request: SendMessageRequest
  ): Promise<Message> {
    const response = await this.client.post<Message>(
      `/conversations/${conversationId}/messages`,
      request
    );
    return response.data;
  }
  
  /**
   * Recherche dans l'historique
   */
  async searchConversation(
    conversationId: string,
    query: string,
    limit: number = 20
  ): Promise<Message[]> {
    const response = await this.client.post<Message[]>(
      `/conversations/${conversationId}/search`,
      { query, limit }
    );
    return response.data;
  }
  
  /**
   * Exporte une conversation
   */
  async exportConversation(conversationId: string): Promise<string> {
    const response = await this.client.get<string>(
      `/conversations/${conversationId}/export`
    );
    return response.data;
  }
}

// ============================================================================
// WEBSOCKET CLIENT
// ============================================================================

// services/conversationWebSocket.ts

export type WebSocketMessageHandler = (message: Message) => void;
export type WebSocketTypingHandler = (isTyping: boolean) => void;
export type WebSocketErrorHandler = (error: string) => void;

export class ConversationWebSocketClient {
  private ws: WebSocket | null = null;
  private conversationId: string;
  private onMessageHandlers: WebSocketMessageHandler[] = [];
  private onTypingHandlers: WebSocketTypingHandler[] = [];
  private onErrorHandlers: WebSocketErrorHandler[] = [];
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  
  constructor(
    conversationId: string,
    baseURL: string = 'ws://localhost:8080'
  ) {
    this.conversationId = conversationId;
    this.connect(baseURL);
  }
  
  /**
   * Établit la connexion WebSocket
   */
  private connect(baseURL: string): void {
    const token = localStorage.getItem('authToken');
    const wsUrl = `${baseURL}/ws/conversations/${this.conversationId}?token=${token}`;
    
    this.ws = new WebSocket(wsUrl);
    
    this.ws.onopen = () => {
      console.log('WebSocket connecté');
      this.reconnectAttempts = 0;
    };
    
    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.handleMessage(data);
    };
    
    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      this.onErrorHandlers.forEach(handler => 
        handler('Erreur de connexion')
      );
    };
    
    this.ws.onclose = () => {
      console.log('WebSocket déconnecté');
      this.attemptReconnect(baseURL);
    };
  }
  
  /**
   * Tente de se reconnecter
   */
  private attemptReconnect(baseURL: string): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Tentative de reconnexion ${this.reconnectAttempts}...`);
      
      setTimeout(() => {
        this.connect(baseURL);
      }, 2000 * this.reconnectAttempts);
    } else {
      this.onErrorHandlers.forEach(handler => 
        handler('Impossible de se reconnecter')
      );
    }
  }
  
  /**
   * Gère les messages entrants
   */
  private handleMessage(data: any): void {
    switch (data.type) {
      case 'MESSAGE':
        this.onMessageHandlers.forEach(handler => handler(data.data));
        break;
        
      case 'TYPING':
        this.onTypingHandlers.forEach(handler => 
          handler(data.data.isTyping)
        );
        break;
        
      case 'ERROR':
        this.onErrorHandlers.forEach(handler => 
          handler(data.data.message)
        );
        break;
        
      case 'CONNECTED':
        console.log('Connexion confirmée:', data.data);
        break;
    }
  }
  
  /**
   * Envoie un message
   */
  sendMessage(content: string): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        type: 'MESSAGE',
        content,
        metadata: {}
      }));
    }
  }
  
  /**
   * Envoie un indicateur de saisie
   */
  sendTypingIndicator(isTyping: boolean): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        type: 'TYPING',
        metadata: { isTyping }
      }));
    }
  }
  
  /**
   * Enregistre un handler pour les messages
   */
  onMessage(handler: WebSocketMessageHandler): void {
    this.onMessageHandlers.push(handler);
  }
  
  /**
   * Enregistre un handler pour l'indicateur de saisie
   */
  onTyping(handler: WebSocketTypingHandler): void {
    this.onTypingHandlers.push(handler);
  }
  
  /**
   * Enregistre un handler pour les erreurs
   */
  onError(handler: WebSocketErrorHandler): void {
    this.onErrorHandlers.push(handler);
  }
  
  /**
   * Ferme la connexion
   */
  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

// ============================================================================
// REACT HOOKS
// ============================================================================

// hooks/useConversation.ts

import { useState, useEffect, useCallback } from 'react';

export function useConversation(conversationId: string) {
  const [conversation, setConversation] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const apiClient = new ConversationApiClient();
  
  useEffect(() => {
    loadConversation();
  }, [conversationId]);
  
  const loadConversation = async () => {
    try {
      setLoading(true);
      const conv = await apiClient.getConversation(conversationId);
      setConversation(conv);
      setMessages(conv.messages);
      setError(null);
    } catch (err) {
      setError('Erreur lors du chargement de la conversation');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };
  
  const sendMessage = async (content: string) => {
    try {
      const message = await apiClient.sendMessage(conversationId, {
        content,
        type: MessageType.TEXT
      });
      
      setMessages(prev => [...prev, message]);
      return message;
    } catch (err) {
      setError('Erreur lors de l\'envoi du message');
      throw err;
    }
  };
  
  const deleteConversation = async () => {
    try {
      await apiClient.deleteConversation(conversationId);
    } catch (err) {
      setError('Erreur lors de la suppression');
      throw err;
    }
  };
  
  return {
    conversation,
    messages,
    loading,
    error,
    sendMessage,
    deleteConversation,
    reload: loadConversation
  };
}

// hooks/useWebSocketConversation.ts

import { useState, useEffect, useRef } from 'react';

export function useWebSocketConversation(conversationId: string) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isTyping, setIsTyping] = useState(false);
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<ConversationWebSocketClient | null>(null);
  
  useEffect(() => {
    // Créer la connexion WebSocket
    const ws = new ConversationWebSocketClient(conversationId);
    wsRef.current = ws;
    
    // Handlers
    ws.onMessage((message) => {
      setMessages(prev => [...prev, message]);
    });
    
    ws.onTyping((typing) => {
      setIsTyping(typing);
    });
    
    ws.onError((error) => {
      console.error('WebSocket error:', error);
    });
    
    setConnected(true);
    
    // Cleanup
    return () => {
      ws.disconnect();
      setConnected(false);
    };
  }, [conversationId]);
  
  const sendMessage = useCallback((content: string) => {
    if (wsRef.current) {
      wsRef.current.sendMessage(content);
      
      // Ajouter le message utilisateur immédiatement
      const userMessage: Message = {
        id: `temp-${Date.now()}`,
        sender: MessageSender.USER,
        content,
        type: MessageType.TEXT,
        timestamp: new Date().toISOString()
      };
      setMessages(prev => [...prev, userMessage]);
    }
  }, []);
  
  const sendTypingIndicator = useCallback((typing: boolean) => {
    if (wsRef.current) {
      wsRef.current.sendTypingIndicator(typing);
    }
  }, []);
  
  return {
    messages,
    isTyping,
    connected,
    sendMessage,
    sendTypingIndicator
  };
}

// ============================================================================
// REACT COMPONENTS
// ============================================================================

// components/ConversationView.tsx

import React, { useState } from 'react';

interface ConversationViewProps {
  conversationId: string;
}

export const ConversationView: React.FC<ConversationViewProps> = ({
  conversationId
}) => {
  const { messages, isTyping, sendMessage, sendTypingIndicator } = 
    useWebSocketConversation(conversationId);
  
  const [inputValue, setInputValue] = useState('');
  
  const handleSend = () => {
    if (inputValue.trim()) {
      sendMessage(inputValue);
      setInputValue('');
      sendTypingIndicator(false);
    }
  };
  
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
    sendTypingIndicator(e.target.value.length > 0);
  };
  
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };
  
  return (
    <div className="conversation-view">
      <div className="messages-container">
        {messages.map((message) => (
          <MessageBubble key={message.id} message={message} />
        ))}
        
        {isTyping && (
          <div className="typing-indicator">
            <span>Le compagnon écrit...</span>
          </div>
        )}
      </div>
      
      <div className="input-container">
        <input
          type="text"
          value={inputValue}
          onChange={handleInputChange}
          onKeyPress={handleKeyPress}
          placeholder="Écrivez votre message..."
        />
        <button onClick={handleSend}>Envoyer</button>
      </div>
    </div>
  );
};

// components/MessageBubble.tsx

interface MessageBubbleProps {
  message: Message;
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({ message }) => {
  const isUser = message.sender === MessageSender.USER;
  
  const getEmotionEmoji = (emotion?: EmotionType): string => {
    const emojiMap: Record<EmotionType, string> = {
      [EmotionType.JOY]: '😊',
      [EmotionType.SADNESS]: '😢',
      [EmotionType.ANGER]: '😠',
      [EmotionType.FEAR]: '😰',
      [EmotionType.SURPRISE]: '😲',
      [EmotionType.LOVE]: '❤️',
      [EmotionType.NEUTRAL]: '😐',
      [EmotionType.EXCITEMENT]: '🤩'
    };
    return emotion ? emojiMap[emotion] : '';
  };
  
  return (
    <div className={`message-bubble ${isUser ? 'user' : 'companion'}`}>
      <div className="message-content">
        {message.content}
      </div>
      
      <div className="message-meta">
        <span className="timestamp">
          {new Date(message.timestamp).toLocaleTimeString()}
        </span>
        
        {message.detectedEmotion && (
          <span className="emotion" title={message.detectedEmotion}>
            {getEmotionEmoji(message.detectedEmotion)}
          </span>
        )}
      </div>
    </div>
  );
};

// components/ConversationList.tsx

interface ConversationListProps {
  userId: string;
  onSelectConversation: (conversationId: string) => void;
}

export const ConversationList: React.FC<ConversationListProps> = ({
  userId,
  onSelectConversation
}) => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);
  const apiClient = new ConversationApiClient();
  
  useEffect(() => {
    loadConversations();
  }, [userId]);
  
  const loadConversations = async () => {
    try {
      setLoading(true);
      const convs = await apiClient.getUserConversations(userId);
      setConversations(convs);
    } catch (err) {
      console.error('Erreur chargement conversations', err);
    } finally {
      setLoading(false);
    }
  };
  
  if (loading) return <div>Chargement...</div>;
  
  return (
    <div className="conversation-list">
      {conversations.map((conv) => (
        <div
          key={conv.id}
          className="conversation-item"
          onClick={() => onSelectConversation(conv.id)}
        >
          <h3>{conv.title || 'Sans titre'}</h3>
          <p>{conv.context.messageCount} messages</p>
          <span className="timestamp">
            {new Date(conv.lastMessageAt).toLocaleDateString()}
          </span>
        </div>
      ))}
    </div>
  );
};

// ============================================================================
// APP PRINCIPALE
// ============================================================================

// App.tsx

import React, { useState } from 'react';

export const App: React.FC = () => {
  const [selectedConversationId, setSelectedConversationId] = 
    useState<string | null>(null);
  
  const userId = 'user-123'; // À récupérer de l'auth
  
  return (
    <div className="app">
      <div className="sidebar">
        <ConversationList
          userId={userId}
          onSelectConversation={setSelectedConversationId}
        />
      </div>
      
      <div className="main-content">
        {selectedConversationId ? (
          <ConversationView conversationId={selectedConversationId} />
        ) : (
          <div className="empty-state">
            Sélectionnez une conversation
          </div>
        )}
      </div>
    </div>
  );
};

// ============================================================================
// STYLES CSS
// ============================================================================

/*
styles/conversation.css

.app {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 300px;
  border-right: 1px solid #e0e0e0;
  overflow-y: auto;
}

.conversation-list {
  padding: 16px;
}

.conversation-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  background: #f5f5f5;
  transition: background 0.2s;
}

.conversation-item:hover {
  background: #e0e0e0;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.conversation-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-bubble {
  max-width: 70%;
  margin-bottom: 16px;
  padding: 12px 16px;
  border-radius: 16px;
}

.message-bubble.user {
  margin-left: auto;
  background: #007bff;
  color: white;
}

.message-bubble.companion {
  background: #f0f0f0;
  color: #333;
}

.message-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.7;
}

.typing-indicator {
  padding: 12px;
  font-style: italic;
  color: #666;
}

.input-container {
  display: flex;
  padding: 16px;
  border-top: 1px solid #e0e0e0;
}

.input-container input {
  flex: 1;
  padding: 12px;
  border: 1px solid #ccc;
  border-radius: 24px;
  margin-right: 8px;
}

.input-container button {
  padding: 12px 24px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 24px;
  cursor: pointer;
}

.input-container button:hover {
  background: #0056b3;
}
*/
