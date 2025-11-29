import type { LanguageCode } from '../constants/languages';

export interface User {
  id: string;
  email: string;
  username: string;
  preferredLanguage: LanguageCode;
  avatarUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserPublic {
  id: string;
  username: string;
  preferredLanguage: LanguageCode;
  avatarUrl?: string;
}

export type MessageType = 'text' | 'voice';
export type MessageStatus = 'sent' | 'delivered' | 'seen';
export type ConversationType = 'direct' | 'group';

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  sender: UserPublic;
  type: MessageType;
  originalContent: string;
  originalLanguage: LanguageCode;
  translatedContent?: string;
  targetLanguage?: LanguageCode;
  status: MessageStatus;
  createdAt: string;
}

export interface Conversation {
  id: string;
  type: ConversationType;
  name?: string;
  participants: UserPublic[];
  lastMessage?: Message;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
}

export interface TranslationPreview {
  originalContent: string;
  translatedContent: string;
  detectedLanguage: LanguageCode;
  targetLanguage: LanguageCode;
}

