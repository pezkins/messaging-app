import { create } from 'zustand';
import { api } from '../services/api';
import { socketService } from '../services/socket';
import { storage } from '../services/storage';
import type { User } from '../types';
import type { LanguageCode, CountryCode } from '../constants/languages';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  initializeAuth: () => Promise<void>;
  login: (email: string, password: string) => Promise<void>;
  oauthLogin: (data: {
    provider: 'google' | 'apple';
    providerId: string;
    email: string;
    name: string | null;
    avatarUrl: string | null;
  }) => Promise<void>;
  register: (data: {
    email: string;
    username: string;
    password: string;
    preferredLanguage: LanguageCode;
    preferredCountry: CountryCode;
  }) => Promise<void>;
  logout: () => Promise<void>;
  updateLanguage: (language: LanguageCode) => Promise<void>;
  updateCountry: (country: CountryCode) => Promise<void>;
  clearError: () => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,
  error: null,

  initializeAuth: async () => {
    try {
      const [accessToken, refreshToken, user] = await Promise.all([
        storage.getAccessToken(),
        storage.getRefreshToken(),
        storage.getUser(),
      ]);

      if (!accessToken || !refreshToken || !user) {
        set({ isLoading: false });
        return;
      }

      api.setAccessToken(accessToken);

      try {
        // Verify token is still valid
        const { user: freshUser } = await api.getMe();
        
        set({
          user: freshUser,
          isAuthenticated: true,
          isLoading: false,
        });

        socketService.connect(accessToken);
      } catch {
        // Token expired, try to refresh
        try {
          const tokens = await api.refreshToken(refreshToken);
          api.setAccessToken(tokens.accessToken);
          
          const { user: freshUser } = await api.getMe();
          
          await Promise.all([
            storage.setAccessToken(tokens.accessToken),
            storage.setRefreshToken(tokens.refreshToken),
            storage.setUser(freshUser),
          ]);
          
          set({
            user: freshUser,
            isAuthenticated: true,
            isLoading: false,
          });

          socketService.connect(tokens.accessToken);
        } catch {
          // Refresh failed, clear everything
          await storage.clearAuth();
          api.setAccessToken(null);
          set({ isLoading: false });
        }
      }
    } catch {
      set({ isLoading: false });
    }
  },

  login: async (email, password) => {
    set({ isLoading: true, error: null });

    try {
      const response = await api.login(email, password);
      
      await Promise.all([
        storage.setAccessToken(response.accessToken),
        storage.setRefreshToken(response.refreshToken),
        storage.setUser(response.user),
      ]);
      
      api.setAccessToken(response.accessToken);
      socketService.connect(response.accessToken);
      
      set({
        user: response.user,
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Login failed',
        isLoading: false,
      });
      throw error;
    }
  },

  oauthLogin: async (data) => {
    set({ isLoading: true, error: null });

    try {
      const response = await api.oauthLogin(data);
      
      await Promise.all([
        storage.setAccessToken(response.accessToken),
        storage.setRefreshToken(response.refreshToken),
        storage.setUser(response.user),
      ]);
      
      api.setAccessToken(response.accessToken);
      socketService.connect(response.accessToken);
      
      set({
        user: response.user,
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'OAuth login failed',
        isLoading: false,
      });
      throw error;
    }
  },

  register: async (data) => {
    set({ isLoading: true, error: null });

    try {
      const response = await api.register(data);
      
      await Promise.all([
        storage.setAccessToken(response.accessToken),
        storage.setRefreshToken(response.refreshToken),
        storage.setUser(response.user),
      ]);
      
      api.setAccessToken(response.accessToken);
      socketService.connect(response.accessToken);
      
      set({
        user: response.user,
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Registration failed',
        isLoading: false,
      });
      throw error;
    }
  },

  logout: async () => {
    const refreshToken = await storage.getRefreshToken();
    
    try {
      if (refreshToken) {
        await api.logout(refreshToken);
      }
    } catch {
      // Ignore logout errors
    }
    
    await storage.clearAuth();
    api.setAccessToken(null);
    socketService.disconnect();
    
    set({
      user: null,
      isAuthenticated: false,
      error: null,
    });
  },

  updateLanguage: async (language) => {
    try {
      const { user } = await api.updateLanguage(language);
      await storage.setUser(user);
      set({ user });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to update language',
      });
      throw error;
    }
  },

  updateCountry: async (country) => {
    try {
      const { user } = await api.updateCountry(country);
      await storage.setUser(user);
      set({ user });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to update country',
      });
      throw error;
    }
  },

  clearError: () => set({ error: null }),
}));

