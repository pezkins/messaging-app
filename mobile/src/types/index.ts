import type { LanguageCode, CountryCode } from '../constants/languages';

export interface User {
  id: string;
  email: string;
  username: string;
  preferredLanguage: LanguageCode;
  preferredCountry?: CountryCode;
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

export type MessageType = 'text' | 'voice' | 'image' | 'file' | 'gif';

export interface Attachment {
  id: string;
  key: string;
  fileName: string;
  contentType: string;
  fileSize: number;
  category: 'image' | 'video' | 'document' | 'audio';
  url?: string; // Presigned download URL (populated on demand)
}
export type MessageStatus = 'sending' | 'sent' | 'delivered' | 'seen' | 'failed';
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
  reactions?: Record<string, string[]>; // emoji -> [userIds]
  attachments?: Attachment[];
  gifUrl?: string; // For GIF messages
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

