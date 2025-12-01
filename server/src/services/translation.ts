import OpenAI from 'openai';
import Anthropic from '@anthropic-ai/sdk';
import { translationCache } from '../lib/redis';
import { prisma } from '../lib/prisma';

// ============================================
// AI Provider Configuration
// ============================================
// Set AI_PROVIDER in .env to: "openai" | "anthropic" | "deepseek"
// Default: "openai"

type AIProvider = 'openai' | 'anthropic' | 'deepseek';

const AI_PROVIDER = (process.env.AI_PROVIDER as AIProvider) || 'openai';

console.log(`🤖 Translation AI Provider: ${AI_PROVIDER.toUpperCase()}`);

// Initialize clients based on provider
let openaiClient: OpenAI | null = null;
let anthropicClient: Anthropic | null = null;

if (AI_PROVIDER === 'openai') {
  openaiClient = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY,
  });
} else if (AI_PROVIDER === 'anthropic') {
  anthropicClient = new Anthropic({
    apiKey: process.env.ANTHROPIC_API_KEY,
  });
} else if (AI_PROVIDER === 'deepseek') {
  openaiClient = new OpenAI({
    apiKey: process.env.DEEPSEEK_API_KEY,
    baseURL: 'https://api.deepseek.com',
  });
}

// Model configuration per provider
const MODEL_CONFIG = {
  openai: {
    model: 'gpt-4o-mini', // Fast and cheap
    maxTokens: 1000,
  },
  anthropic: {
    model: 'claude-3-haiku-20240307', // Fast and cheap
    maxTokens: 1000,
  },
  deepseek: {
    model: 'deepseek-chat',
    maxTokens: 1000,
  },
};

const LANGUAGE_NAMES: Record<string, string> = {
  en: 'English',
  es: 'Spanish',
  fr: 'French',
  de: 'German',
  it: 'Italian',
  pt: 'Portuguese',
  zh: 'Chinese',
  ja: 'Japanese',
  ko: 'Korean',
  ar: 'Arabic',
  ru: 'Russian',
  hi: 'Hindi',
  nl: 'Dutch',
  sv: 'Swedish',
  pl: 'Polish',
  tr: 'Turkish',
};

// ============================================
// AI Provider Abstraction
// ============================================

async function callAI(systemPrompt: string, userPrompt: string, maxTokens?: number): Promise<string> {
  const config = MODEL_CONFIG[AI_PROVIDER];
  const tokens = maxTokens || config.maxTokens;

  if (AI_PROVIDER === 'anthropic' && anthropicClient) {
    const response = await anthropicClient.messages.create({
      model: config.model,
      max_tokens: tokens,
      messages: [
        {
          role: 'user',
          content: `${systemPrompt}\n\n${userPrompt}`,
        },
      ],
    });
    const content = response.content[0];
    return content.type === 'text' ? content.text.trim() : '';
  } 
  
  if ((AI_PROVIDER === 'openai' || AI_PROVIDER === 'deepseek') && openaiClient) {
    const response = await openaiClient.chat.completions.create({
      model: config.model,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userPrompt },
      ],
      temperature: 0.3,
      max_tokens: tokens,
    });
    return response.choices[0]?.message?.content?.trim() || '';
  }

  throw new Error(`AI Provider "${AI_PROVIDER}" not properly configured`);
}

// ============================================
// Translation Service
// ============================================

class TranslationService {
  /**
   * Translate text from source language to target language
   */
  async translate(
    text: string,
    sourceLanguage: string,
    targetLanguage: string,
    messageId?: string
  ): Promise<string> {
    // If same language, return original
    if (sourceLanguage === targetLanguage) {
      return text;
    }

    // Check cache first (if messageId provided)
    if (messageId) {
      const cached = await translationCache.get(messageId, targetLanguage);
      if (cached) {
        console.log(`📦 Cache hit for message ${messageId.slice(0, 8)} -> ${targetLanguage}`);
        return cached;
      }
    }

    // Check database for existing translation
    if (messageId) {
      const existingTranslation = await prisma.translation.findUnique({
        where: {
          messageId_targetLanguage: {
            messageId,
            targetLanguage,
          },
        },
      });

      if (existingTranslation) {
        // Cache it for future requests
        await translationCache.set(messageId, targetLanguage, existingTranslation.translatedContent);
        return existingTranslation.translatedContent;
      }
    }

    const sourceLangName = LANGUAGE_NAMES[sourceLanguage] || sourceLanguage;
    const targetLangName = LANGUAGE_NAMES[targetLanguage] || targetLanguage;

    console.log(`🌐 Translating (${AI_PROVIDER}): ${sourceLangName} -> ${targetLangName}`);

    try {
      const systemPrompt = `You are a professional translator. Translate text from ${sourceLangName} to ${targetLangName}.

Rules:
- Maintain the original tone and context (casual, formal, friendly, etc.)
- Preserve any emojis, special characters, or formatting
- Do not add explanations or notes
- If the text contains slang or idioms, translate them to equivalent expressions in the target language
- Return ONLY the translated text, nothing else`;

      const translation = await callAI(systemPrompt, text);

      // Store translation in database and cache
      if (messageId && translation) {
        await prisma.translation.create({
          data: {
            messageId,
            targetLanguage,
            translatedContent: translation,
          },
        });
        await translationCache.set(messageId, targetLanguage, translation);
      }

      return translation || text;
    } catch (error) {
      console.error('Translation error:', error);
      // Return original text if translation fails
      return text;
    }
  }

  /**
   * Detect the language of a text
   */
  async detectLanguage(text: string): Promise<string> {
    try {
      const systemPrompt = `Detect the language of the text and respond with ONLY the ISO 639-1 language code (e.g., 'en', 'es', 'fr', 'de', 'ja', 'zh', etc.). Do not include any other text or explanation.`;

      const detected = await callAI(systemPrompt, text, 10);
      const code = detected.toLowerCase().replace(/[^a-z]/g, '').slice(0, 2);
      
      // Validate it's a supported language code
      if (LANGUAGE_NAMES[code]) {
        return code;
      }
      
      return 'en'; // Default to English
    } catch (error) {
      console.error('Language detection error:', error);
      return 'en'; // Default to English on error
    }
  }

  /**
   * Preview translation without storing (for pre-send preview)
   */
  async previewTranslation(
    text: string,
    targetLanguage: string
  ): Promise<{ translatedContent: string; detectedLanguage: string }> {
    const detectedLanguage = await this.detectLanguage(text);
    
    if (detectedLanguage === targetLanguage) {
      return {
        translatedContent: text,
        detectedLanguage,
      };
    }

    const translatedContent = await this.translate(text, detectedLanguage, targetLanguage);
    
    return {
      translatedContent,
      detectedLanguage,
    };
  }

  /**
   * Get current AI provider info
   */
  getProviderInfo(): { provider: string; model: string } {
    return {
      provider: AI_PROVIDER,
      model: MODEL_CONFIG[AI_PROVIDER].model,
    };
  }
}

export const translationService = new TranslationService();
