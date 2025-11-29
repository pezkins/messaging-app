import OpenAI from 'openai';
import { translationCache } from '../lib/redis';
import { prisma } from '../lib/prisma';

// DeepSeek API is compatible with OpenAI's SDK
// Free tier: https://platform.deepseek.com/
const deepseek = new OpenAI({
  apiKey: process.env.DEEPSEEK_API_KEY,
  baseURL: 'https://api.deepseek.com',
});

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

    console.log(`🌐 Translating: ${sourceLanguage} -> ${targetLanguage}`);

    try {
      const sourceLangName = LANGUAGE_NAMES[sourceLanguage] || sourceLanguage;
      const targetLangName = LANGUAGE_NAMES[targetLanguage] || targetLanguage;

      const response = await deepseek.chat.completions.create({
        model: 'deepseek-chat',
        messages: [
          {
            role: 'system',
            content: `You are a professional translator. Translate the following text from ${sourceLangName} to ${targetLangName}. 
            
Rules:
- Maintain the original tone and context (casual, formal, friendly, etc.)
- Preserve any emojis, special characters, or formatting
- Do not add explanations or notes
- If the text contains slang or idioms, translate them to equivalent expressions in the target language
- Return ONLY the translated text, nothing else`,
          },
          {
            role: 'user',
            content: text,
          },
        ],
        temperature: 0.3,
        max_tokens: 1000,
      });

      const translation = response.choices[0]?.message?.content?.trim() || text;

      // Store translation in database and cache
      if (messageId) {
        await prisma.translation.create({
          data: {
            messageId,
            targetLanguage,
            translatedContent: translation,
          },
        });
        await translationCache.set(messageId, targetLanguage, translation);
      }

      return translation;
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
      const response = await deepseek.chat.completions.create({
        model: 'deepseek-chat',
        messages: [
          {
            role: 'system',
            content: `Detect the language of the following text and respond with ONLY the ISO 639-1 language code (e.g., 'en', 'es', 'fr', 'de', 'ja', 'zh', etc.). Do not include any other text or explanation.`,
          },
          {
            role: 'user',
            content: text,
          },
        ],
        temperature: 0,
        max_tokens: 10,
      });

      const detected = response.choices[0]?.message?.content?.trim().toLowerCase() || 'en';
      
      // Validate it's a supported language code
      if (LANGUAGE_NAMES[detected]) {
        return detected;
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
}

export const translationService = new TranslationService();

