import type { Message } from '../types';

const SOCKET_URL = process.env.EXPO_PUBLIC_WS_URL || 'ws://localhost:3001';

/**
 * WebSocket service that works with both:
 * - Socket.IO (original server)
 * - AWS API Gateway WebSocket (serverless)
 */
class SocketService {
  private socket: WebSocket | null = null;
  private listeners: Map<string, Set<(...args: unknown[]) => void>> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private token: string | null = null;

  connect(token: string) {
    if (this.socket?.readyState === WebSocket.OPEN) {
      return;
    }

    this.token = token;
    
    // Add token as query param for AWS API Gateway WebSocket
    const url = `${SOCKET_URL}?token=${encodeURIComponent(token)}`;
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      console.log('🔌 WebSocket connected');
      this.reconnectAttempts = 0;
    };

    this.socket.onclose = (event) => {
      console.log('🔌 WebSocket disconnected:', event.code);
      this.handleReconnect();
    };

    this.socket.onerror = (error) => {
      console.error('🔌 WebSocket error:', error);
    };

    this.socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const action = data.action;
        
        // Notify listeners
        const eventListeners = this.listeners.get(action);
        if (eventListeners) {
          eventListeners.forEach((listener) => listener(data));
        }
      } catch (error) {
        console.error('Failed to parse message:', error);
      }
    };
  }

  private handleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts || !this.token) {
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    
    console.log(`🔌 Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);
    
    setTimeout(() => {
      if (this.token) {
        this.connect(this.token);
      }
    }, delay);
  }

  disconnect() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.token = null;
  }

  on<T>(event: string, callback: (data: T) => void) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(callback as (...args: unknown[]) => void);

    return () => {
      this.listeners.get(event)?.delete(callback as (...args: unknown[]) => void);
    };
  }

  off(event: string) {
    this.listeners.delete(event);
  }

  private send(action: string, data: unknown) {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify({ action, data }));
    }
  }

  sendMessage(data: { conversationId: string; content: string; type: 'TEXT' | 'VOICE' }) {
    this.send('message', data);
  }

  sendTyping(conversationId: string, isTyping: boolean) {
    this.send('message', { action: 'message:typing', conversationId, isTyping });
  }

  markAsRead(conversationId: string, messageId: string) {
    this.send('message', { action: 'message:read', conversationId, messageId });
  }

  joinConversation(_conversationId: string) {
    // No-op for serverless - connections are managed per-user
  }

  leaveConversation(_conversationId: string) {
    // No-op for serverless
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

