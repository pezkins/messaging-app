import Constants from 'expo-constants';
import type { Message } from '../types';

const WS_URL = Constants.expoConfig?.extra?.wsUrl || 'wss://localhost:3001';

/**
 * WebSocket service for real-time messaging (AWS API Gateway WebSocket)
 */
class SocketService {
  private ws: WebSocket | null = null;
  private token: string | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;
  private listeners: Map<string, Set<(data: any) => void>> = new Map();

  connect(token: string) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      return;
    }

    this.token = token;
    
    // Connect to API Gateway WebSocket with token as query param
    const wsUrl = `${WS_URL}?token=${token}`;
    console.log('🔌 Connecting to WebSocket:', WS_URL);
    
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('🔌 WebSocket connected');
      this.reconnectAttempts = 0;
    };

    this.ws.onclose = (event) => {
      console.log('🔌 WebSocket disconnected:', event.code, event.reason);
      this.handleReconnect();
    };

    this.ws.onerror = (error) => {
      console.error('🔌 WebSocket error:', error);
    };

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const { action, ...payload } = data;
        
        // Notify listeners for this action
        const actionListeners = this.listeners.get(action);
        if (actionListeners) {
          actionListeners.forEach(callback => callback(payload));
        }
        
        // Also notify 'message' listeners for backward compatibility
        if (action === 'message:receive') {
          const messageListeners = this.listeners.get('message:receive');
          if (messageListeners) {
            messageListeners.forEach(callback => callback(payload));
          }
        }
      } catch (error) {
        console.error('🔌 Failed to parse WebSocket message:', error);
      }
    };
  }

  private handleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('🔌 Max reconnect attempts reached');
      return;
    }

    if (this.token) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      console.log(`🔌 Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);
      
      setTimeout(() => {
        if (this.token) {
          this.connect(this.token);
        }
      }, delay);
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.token = null;
    this.reconnectAttempts = 0;
  }

  on<T>(event: string, callback: (data: T) => void) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(callback);

    return () => {
      this.listeners.get(event)?.delete(callback);
    };
  }

  off(event: string) {
    this.listeners.delete(event);
  }

  private send(action: string, data: any) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ action, data }));
    } else {
      console.warn('🔌 WebSocket not connected, cannot send:', action);
    }
  }

  sendMessage(data: { conversationId: string; content: string; type: 'TEXT' | 'VOICE'; tempId?: string }) {
    this.send('message:send', data);
  }

  sendTyping(conversationId: string, isTyping: boolean) {
    this.send('message:typing', { conversationId, isTyping });
  }

  markAsRead(conversationId: string, messageId: string) {
    this.send('message:read', { conversationId, messageId });
  }

  joinConversation(conversationId: string) {
    this.send('conversation:join', conversationId);
  }

  leaveConversation(conversationId: string) {
    this.send('conversation:leave', conversationId);
  }

  sendReaction(conversationId: string, messageId: string, messageTimestamp: string, emoji: string) {
    this.send('message:reaction', { conversationId, messageId, messageTimestamp, emoji });
  }
}

export const socketService = new SocketService();

export interface MessageReceiveEvent {
  message: Message & {
    translatedContent: string;
    targetLanguage: string;
  };
}

export interface TypingEvent {
  conversationId: string;
  userId: string;
  isTyping: boolean;
}

export interface ReactionEvent {
  conversationId: string;
  messageId: string;
  messageTimestamp: string;
  reactions: Record<string, string[]>; // emoji -> [userIds]
  userId: string;
  emoji: string;
}
