import { io, Socket } from 'socket.io-client';
import Constants from 'expo-constants';
import type { Message } from '../types';

const SOCKET_URL = Constants.expoConfig?.extra?.apiUrl || 'http://localhost:3001';

/**
 * Socket.IO service for real-time messaging
 */
class SocketService {
  private socket: Socket | null = null;
  private token: string | null = null;

  connect(token: string) {
    if (this.socket?.connected) {
      return;
    }

    this.token = token;
    
    this.socket = io(SOCKET_URL, {
      auth: { token },
      transports: ['websocket', 'polling'],
      reconnection: true,
      reconnectionAttempts: 5,
      reconnectionDelay: 1000,
    });

    this.socket.on('connect', () => {
      console.log('🔌 Socket.IO connected');
    });

    this.socket.on('disconnect', (reason: string) => {
      console.log('🔌 Socket.IO disconnected:', reason);
    });

    this.socket.on('connect_error', (error: Error) => {
      console.error('🔌 Socket.IO connection error:', error.message);
    });
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
    }
    this.token = null;
  }

  on<T>(event: string, callback: (data: T) => void) {
    this.socket?.on(event, callback);

    return () => {
      this.socket?.off(event, callback);
    };
  }

  off(event: string) {
    this.socket?.off(event);
  }

  sendMessage(data: { conversationId: string; content: string; type: 'TEXT' | 'VOICE' }) {
    this.socket?.emit('message:send', data);
  }

  sendTyping(conversationId: string, isTyping: boolean) {
    this.socket?.emit('message:typing', { conversationId, isTyping });
  }

  markAsRead(conversationId: string, messageId: string) {
    this.socket?.emit('message:read', { conversationId, messageId });
  }

  joinConversation(conversationId: string) {
    this.socket?.emit('conversation:join', conversationId);
  }

  leaveConversation(conversationId: string) {
    this.socket?.emit('conversation:leave', conversationId);
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
