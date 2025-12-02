import AsyncStorage from '@react-native-async-storage/async-storage';
import type { User } from '../types';

const KEYS = {
  ACCESS_TOKEN: 'intok_access_token',
  REFRESH_TOKEN: 'intok_refresh_token',
  USER: 'intok_user',
};

export const storage = {
  async setAccessToken(token: string): Promise<void> {
    await AsyncStorage.setItem(KEYS.ACCESS_TOKEN, token);
  },

  async getAccessToken(): Promise<string | null> {
    return AsyncStorage.getItem(KEYS.ACCESS_TOKEN);
  },

  async setRefreshToken(token: string): Promise<void> {
    await AsyncStorage.setItem(KEYS.REFRESH_TOKEN, token);
  },

  async getRefreshToken(): Promise<string | null> {
    return AsyncStorage.getItem(KEYS.REFRESH_TOKEN);
  },

  async setUser(user: User): Promise<void> {
    await AsyncStorage.setItem(KEYS.USER, JSON.stringify(user));
  },

  async getUser(): Promise<User | null> {
    const data = await AsyncStorage.getItem(KEYS.USER);
    return data ? JSON.parse(data) : null;
  },

  async clearAuth(): Promise<void> {
    await Promise.all([
      AsyncStorage.removeItem(KEYS.ACCESS_TOKEN),
      AsyncStorage.removeItem(KEYS.REFRESH_TOKEN),
      AsyncStorage.removeItem(KEYS.USER),
    ]);
  },
};
