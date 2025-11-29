// User types
export interface User {
  id: string;
  email: string;
  username: string;
  preferredLanguage: LanguageCode;
  avatarUrl?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface UserPublic {
  id: string;
  username: string;
  preferredLanguage: LanguageCode;
  avatarUrl?: string;
}

// Language types
export type LanguageCode = 
  | 'en' // English
  | 'es' // Spanish
  | 'fr' // French
  | 'de' // German
  | 'it' // Italian
  | 'pt' // Portuguese
  | 'zh' // Chinese
  | 'ja' // Japanese
  | 'ko' // Korean
  | 'ar' // Arabic
  | 'ru' // Russian
  | 'hi' // Hindi
  | 'nl' // Dutch
  | 'sv' // Swedish
  | 'pl' // Polish
  | 'tr'; // Turkish

export const SUPPORTED_LANGUAGES: Record<LanguageCode, string> = {
  en: 'English',
  es: 'Español',
  fr: 'Français',
  de: 'Deutsch',
  it: 'Italiano',
  pt: 'Português',
  zh: '中文',
  ja: '日本語',
  ko: '한국어',
  ar: 'العربية',
  ru: 'Русский',
  hi: 'हिन्दी',
  nl: 'Nederlands',
  sv: 'Svenska',
  pl: 'Polski',
  tr: 'Türkçe',
};

// Message types
export type MessageType = 'text' | 'voice';
export type MessageStatus = 'sent' | 'delivered' | 'seen';

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  type: MessageType;
  originalContent: string;
  originalLanguage: LanguageCode;
  timestamp: Date;
  status: MessageStatus;
}

export interface TranslatedMessage extends Message {
  translatedContent: string;
  targetLanguage: LanguageCode;
}

export interface MessageWithTranslations extends Message {
  translations: Translation[];
  sender: UserPublic;
}

// Translation types
export interface Translation {
  id: string;
  messageId: string;
  targetLanguage: LanguageCode;
  translatedContent: string;
  createdAt: Date;
}

// Conversation types
export type ConversationType = 'direct' | 'group';

export interface Conversation {
  id: string;
  type: ConversationType;
  name?: string; // For group chats
  participants: UserPublic[];
  lastMessage?: Message;
  createdAt: Date;
  updatedAt: Date;
}

export interface ConversationWithMessages extends Conversation {
  messages: MessageWithTranslations[];
}

// WebSocket event types
export interface WsMessageSend {
  conversationId: string;
  content: string;
  type: MessageType;
}

export interface WsMessageReceive {
  message: TranslatedMessage;
  conversation: Conversation;
}

export interface WsTypingIndicator {
  conversationId: string;
  userId: string;
  isTyping: boolean;
}

export interface WsMessageRead {
  conversationId: string;
  messageId: string;
  userId: string;
}

// API Request/Response types
export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  preferredLanguage: LanguageCode;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
}

export interface TranslationPreviewRequest {
  content: string;
  targetLanguage: LanguageCode;
}

export interface TranslationPreviewResponse {
  originalContent: string;
  translatedContent: string;
  detectedLanguage: LanguageCode;
  targetLanguage: LanguageCode;
}

export interface CreateConversationRequest {
  participantIds: string[];
  type: ConversationType;
  name?: string;
}

export interface SendMessageRequest {
  conversationId: string;
  content: string;
  type: MessageType;
}

// API Error response
export interface ApiError {
  message: string;
  code: string;
  details?: Record<string, unknown>;
}

