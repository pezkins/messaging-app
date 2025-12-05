import Constants from 'expo-constants';
import type { 
  AuthResponse, 
  User, 
  Conversation, 
  Message, 
  TranslationPreview,
  UserPublic,
} from '../types';
import type { LanguageCode, CountryCode } from '../constants/languages';

// Update this to your production server URL
const API_URL = Constants.expoConfig?.extra?.apiUrl || 'http://localhost:3001';

class ApiClient {
  private accessToken: string | null = null;

  setAccessToken(token: string | null) {
    this.accessToken = token;
  }

  getAccessToken() {
    return this.accessToken;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (this.accessToken) {
      (headers as Record<string, string>)['Authorization'] = `Bearer ${this.accessToken}`;
    }

    const response = await fetch(`${API_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Request failed' }));
      throw new Error(error.message || 'Request failed');
    }

    return response.json();
  }

  // Auth
  async register(data: {
    email: string;
    username: string;
    password: string;
    preferredLanguage: LanguageCode;
    preferredCountry: CountryCode;
  }): Promise<AuthResponse> {
    return this.request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async login(email: string, password: string): Promise<AuthResponse> {
    return this.request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  }

  // OAuth login (Google/Apple)
  async oauthLogin(data: {
    provider: 'google' | 'apple';
    providerId: string;
    email: string;
    name: string | null;
    avatarUrl: string | null;
  }): Promise<AuthResponse> {
    return this.request<AuthResponse>('/api/auth/oauth', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async refreshToken(refreshToken: string): Promise<{ accessToken: string; refreshToken: string }> {
    return this.request('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
  }

  async logout(refreshToken: string): Promise<void> {
    return this.request('/api/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
  }

  async getMe(): Promise<{ user: User }> {
    return this.request('/api/auth/me');
  }

  // Users
  async updateProfile(data: { username?: string; avatarUrl?: string }): Promise<{ user: User }> {
    return this.request('/api/users/me', {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  }

  async updateLanguage(preferredLanguage: LanguageCode): Promise<{ user: User }> {
    return this.request('/api/users/me/language', {
      method: 'PATCH',
      body: JSON.stringify({ preferredLanguage }),
    });
  }

  async updateCountry(preferredCountry: CountryCode): Promise<{ user: User }> {
    return this.request('/api/users/me/country', {
      method: 'PATCH',
      body: JSON.stringify({ preferredCountry }),
    });
  }

  async searchUsers(query: string): Promise<{ users: UserPublic[] }> {
    return this.request(`/api/users/search?q=${encodeURIComponent(query)}`);
  }

  // Conversations
  async getConversations(): Promise<{ conversations: Conversation[] }> {
    return this.request('/api/conversations');
  }

  async createConversation(data: {
    participantIds: string[];
    type: 'direct' | 'group';
    name?: string;
  }): Promise<{ conversation: Conversation }> {
    return this.request('/api/conversations', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getMessages(
    conversationId: string,
    params?: { limit?: number; cursor?: string }
  ): Promise<{ messages: Message[]; hasMore: boolean; nextCursor: string | null }> {
    const searchParams = new URLSearchParams();
    if (params?.limit) searchParams.set('limit', params.limit.toString());
    if (params?.cursor) searchParams.set('cursor', params.cursor);
    
    const query = searchParams.toString();
    return this.request(`/api/conversations/${conversationId}/messages${query ? `?${query}` : ''}`);
  }

  // Messages
  async previewTranslation(
    content: string,
    targetLanguage: LanguageCode
  ): Promise<TranslationPreview> {
    return this.request('/api/messages/preview-translation', {
      method: 'POST',
      body: JSON.stringify({ content, targetLanguage }),
    });
  }

  async getOriginalMessage(id: string): Promise<{ originalContent: string; originalLanguage: LanguageCode }> {
    return this.request(`/api/messages/${id}/original`);
  }

  // Attachments
  async getUploadUrl(data: {
    fileName: string;
    contentType: string;
    fileSize: number;
    conversationId: string;
  }): Promise<{
    attachmentId: string;
    uploadUrl: string;
    key: string;
    category: 'image' | 'video' | 'document' | 'audio';
    expiresIn: number;
  }> {
    return this.request('/api/attachments/upload-url', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getDownloadUrl(attachmentId: string, conversationId: string, key: string): Promise<{
    downloadUrl: string;
    expiresIn: number;
  }> {
    const params = new URLSearchParams({ conversationId, key });
    return this.request(`/api/attachments/${attachmentId}?${params}`);
  }

  // Upload file directly to S3 using presigned URL
  async uploadFile(uploadUrl: string, file: Blob, contentType: string, onProgress?: (progress: number) => void): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable && onProgress) {
          const progress = Math.round((event.loaded / event.total) * 100);
          onProgress(progress);
        }
      };

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve();
        } else {
          reject(new Error(`Upload failed with status ${xhr.status}`));
        }
      };

      xhr.onerror = () => reject(new Error('Upload failed'));
      
      xhr.open('PUT', uploadUrl);
      xhr.setRequestHeader('Content-Type', contentType);
      xhr.send(file);
    });
  }
}

export const api = new ApiClient();

