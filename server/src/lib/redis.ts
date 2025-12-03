import Redis from 'ioredis';

const redisUrl = process.env.REDIS_URL || 'redis://localhost:6379';

export const redis = new Redis(redisUrl, {
  maxRetriesPerRequest: 3,
  retryStrategy(times) {
    const delay = Math.min(times * 50, 2000);
    return delay;
  },
});

redis.on('connect', () => {
  console.log('✅ Redis connected');
});

redis.on('error', (err) => {
  console.error('❌ Redis error:', err.message);
});

// Translation cache helpers
const TRANSLATION_CACHE_PREFIX = 'translation:';
const TRANSLATION_CACHE_TTL = 60 * 60 * 24 * 7; // 7 days

export const translationCache = {
  /**
   * Get cached translation
   * @param messageId - The message ID
   * @param targetLanguage - Target language code
   */
  async get(messageId: string, targetLanguage: string): Promise<string | null> {
    const key = `${TRANSLATION_CACHE_PREFIX}${messageId}:${targetLanguage}`;
    return redis.get(key);
  },

  /**
   * Set translation in cache
   * @param messageId - The message ID
   * @param targetLanguage - Target language code
   * @param translation - Translated content
   */
  async set(messageId: string, targetLanguage: string, translation: string): Promise<void> {
    const key = `${TRANSLATION_CACHE_PREFIX}${messageId}:${targetLanguage}`;
    await redis.setex(key, TRANSLATION_CACHE_TTL, translation);
  },

  /**
   * Get all cached translations for a message
   * @param messageId - The message ID
   */
  async getAllForMessage(messageId: string): Promise<Record<string, string>> {
    const pattern = `${TRANSLATION_CACHE_PREFIX}${messageId}:*`;
    const keys = await redis.keys(pattern);
    
    if (keys.length === 0) return {};
    
    const translations: Record<string, string> = {};
    const values = await redis.mget(keys);
    
    keys.forEach((key, index) => {
      const lang = key.split(':').pop();
      if (lang && values[index]) {
        translations[lang] = values[index];
      }
    });
    
    return translations;
  },
};

// User session helpers (for WebSocket connections)
const USER_SOCKET_PREFIX = 'user:socket:';

export const userSessions = {
  async set(userId: string, socketId: string): Promise<void> {
    await redis.set(`${USER_SOCKET_PREFIX}${userId}`, socketId);
  },

  async get(userId: string): Promise<string | null> {
    return redis.get(`${USER_SOCKET_PREFIX}${userId}`);
  },

  async remove(userId: string): Promise<void> {
    await redis.del(`${USER_SOCKET_PREFIX}${userId}`);
  },

  async getMultiple(userIds: string[]): Promise<(string | null)[]> {
    if (userIds.length === 0) return [];
    const keys = userIds.map(id => `${USER_SOCKET_PREFIX}${id}`);
    return redis.mget(keys);
  },
};



