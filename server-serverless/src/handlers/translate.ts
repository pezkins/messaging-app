import type { APIGatewayProxyHandler } from 'aws-lambda';
import { dynamodb, Tables, GetCommand, PutCommand } from '../lib/dynamo';
import { response } from '../lib/auth';
import { chat, LANGUAGE_NAMES, COUNTRY_NAMES } from '../lib/translation';

// Import the master English strings
import uiStringsData from '../data/ui-strings.json';

const UI_TRANSLATIONS_TABLE = process.env.UI_TRANSLATIONS_TABLE || 'lingualink-ui-translations';
const CACHE_TTL_DAYS = 30;

interface TranslateUIStringsRequest {
  targetLanguage: string;
  targetCountry?: string;
  strings?: Record<string, string>;
  forceRefresh?: boolean;
}

interface CachedTranslation {
  language: string;
  country?: string;
  translations: Record<string, string>;
  version: string;
  generatedAt: string;
  ttl: number;
}

/**
 * Flatten nested object into dot-notation keys
 * e.g., { auth: { login: "hi" } } => { "auth.login": "hi" }
 */
function flattenStrings(obj: Record<string, any>, prefix = ''): Record<string, string> {
  const result: Record<string, string> = {};
  
  for (const [key, value] of Object.entries(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    
    if (typeof value === 'string') {
      result[fullKey] = value;
    } else if (typeof value === 'object' && value !== null) {
      Object.assign(result, flattenStrings(value, fullKey));
    }
  }
  
  return result;
}

/**
 * Build UI translation prompt optimized for batch translation
 */
function buildUITranslationPrompt(
  targetLanguage: string,
  targetCountry?: string
): string {
  const languageName = LANGUAGE_NAMES[targetLanguage] || targetLanguage;
  const countryName = targetCountry ? COUNTRY_NAMES[targetCountry] : null;

  if (countryName) {
    return `You are translating UI strings for a mobile messaging app.
Translate each string to ${languageName} as spoken in ${countryName}.

Rules:
- Keep placeholders like {count}, {username}, {language} exactly as they are
- Keep emoji exactly as they are (do not translate emoji)
- Keep the translation concise and natural for mobile UI
- Maintain the same tone (formal/informal) as the original
- Output ONLY the JSON object with translated strings, nothing else`;
  } else {
    return `You are translating UI strings for a mobile messaging app.
Translate each string to ${languageName}.

Rules:
- Keep placeholders like {count}, {username}, {language} exactly as they are
- Keep emoji exactly as they are (do not translate emoji)
- Keep the translation concise and natural for mobile UI
- Maintain the same tone (formal/informal) as the original
- Output ONLY the JSON object with translated strings, nothing else`;
  }
}

/**
 * Translate a batch of strings using AI
 */
async function translateBatch(
  strings: Record<string, string>,
  targetLanguage: string,
  targetCountry?: string
): Promise<Record<string, string>> {
  const systemPrompt = buildUITranslationPrompt(targetLanguage, targetCountry);
  
  // Format strings as JSON for the AI
  const inputJson = JSON.stringify(strings, null, 2);
  
  const result = await chat([
    {
      role: 'system',
      content: systemPrompt,
    },
    {
      role: 'user',
      content: `Translate these UI strings:\n${inputJson}`,
    },
  ]);

  if (!result) {
    throw new Error('AI returned empty result');
  }

  // Parse the JSON response
  try {
    // Try to extract JSON from the response (AI might add markdown code blocks)
    let jsonStr = result;
    
    // Remove markdown code blocks if present
    const jsonMatch = result.match(/```(?:json)?\s*([\s\S]*?)```/);
    if (jsonMatch) {
      jsonStr = jsonMatch[1];
    }
    
    // Clean up any leading/trailing whitespace or quotes
    jsonStr = jsonStr.trim();
    
    const translated = JSON.parse(jsonStr);
    return translated;
  } catch (parseError) {
    console.error('Failed to parse AI response:', result);
    throw new Error('Failed to parse AI translation response');
  }
}

/**
 * GET /api/translate/ui-strings
 * Get the master English UI strings
 */
export const getUIStrings: APIGatewayProxyHandler = async () => {
  try {
    const flatStrings = flattenStrings(uiStringsData.strings);
    
    return response(200, {
      language: 'en',
      version: uiStringsData.version,
      stringCount: Object.keys(flatStrings).length,
      strings: flatStrings,
    });
  } catch (error) {
    console.error('Get UI strings error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * POST /api/translate/ui-strings
 * Translate UI strings to a target language
 */
export const translateUIStrings: APIGatewayProxyHandler = async (event) => {
  try {
    const body: TranslateUIStringsRequest = JSON.parse(event.body || '{}');
    
    if (!body.targetLanguage) {
      return response(400, { message: 'targetLanguage is required' });
    }

    const { targetLanguage, targetCountry, forceRefresh } = body;
    const version = uiStringsData.version;
    
    // If target language is English, return the original strings
    if (targetLanguage === 'en') {
      const flatStrings = flattenStrings(uiStringsData.strings);
      return response(200, {
        language: 'en',
        translations: flatStrings,
        version,
        generatedAt: new Date().toISOString(),
        cached: false,
      });
    }

    // Check cache first (unless forceRefresh is true)
    const cacheKey = `ui-translations#${targetLanguage}#${version}`;
    
    if (!forceRefresh) {
      try {
        const cached = await dynamodb.send(new GetCommand({
          TableName: UI_TRANSLATIONS_TABLE,
          Key: { id: cacheKey },
        }));

        if (cached.Item) {
          console.log(`✅ [UI-TRANSLATE] Cache hit for ${targetLanguage} v${version}`);
          return response(200, {
            language: cached.Item.language,
            country: cached.Item.country,
            translations: cached.Item.translations,
            version: cached.Item.version,
            generatedAt: cached.Item.generatedAt,
            cached: true,
          });
        }
      } catch (cacheError) {
        console.warn('Cache lookup failed:', cacheError);
        // Continue to generate translations
      }
    }

    console.log(`🌐 [UI-TRANSLATE] Generating translations for ${targetLanguage} v${version}`);
    const startTime = Date.now();

    // Get the source strings (use provided or default to master)
    const sourceStrings = body.strings || flattenStrings(uiStringsData.strings);
    const stringKeys = Object.keys(sourceStrings);
    const totalStrings = stringKeys.length;

    console.log(`🌐 [UI-TRANSLATE] Translating ${totalStrings} strings to ${targetLanguage}`);

    // Batch strings into groups of 25 for parallel processing
    const BATCH_SIZE = 25;
    const batches: Record<string, string>[] = [];
    
    for (let i = 0; i < stringKeys.length; i += BATCH_SIZE) {
      const batchKeys = stringKeys.slice(i, i + BATCH_SIZE);
      const batch: Record<string, string> = {};
      for (const key of batchKeys) {
        batch[key] = sourceStrings[key];
      }
      batches.push(batch);
    }

    console.log(`🌐 [UI-TRANSLATE] Processing ${batches.length} batches of ${BATCH_SIZE} strings each`);

    // Process batches in parallel (max 5 concurrent)
    const MAX_CONCURRENT = 5;
    const translations: Record<string, string> = {};
    
    for (let i = 0; i < batches.length; i += MAX_CONCURRENT) {
      const currentBatches = batches.slice(i, i + MAX_CONCURRENT);
      const batchPromises = currentBatches.map((batch, idx) => {
        const batchNum = i + idx + 1;
        console.log(`🌐 [UI-TRANSLATE] Starting batch ${batchNum}/${batches.length}`);
        return translateBatch(batch, targetLanguage, targetCountry)
          .then(result => {
            console.log(`✅ [UI-TRANSLATE] Completed batch ${batchNum}/${batches.length}`);
            return result;
          })
          .catch(error => {
            console.error(`❌ [UI-TRANSLATE] Batch ${batchNum} failed:`, error.message);
            // Return original strings on failure
            return batch;
          });
      });

      const results = await Promise.all(batchPromises);
      for (const result of results) {
        Object.assign(translations, result);
      }
    }

    const duration = Date.now() - startTime;
    console.log(`✅ [UI-TRANSLATE] Completed ${totalStrings} translations in ${duration}ms`);

    // Cache the translations
    const ttl = Math.floor(Date.now() / 1000) + (CACHE_TTL_DAYS * 24 * 60 * 60);
    const generatedAt = new Date().toISOString();

    try {
      await dynamodb.send(new PutCommand({
        TableName: UI_TRANSLATIONS_TABLE,
        Item: {
          id: cacheKey,
          language: targetLanguage,
          country: targetCountry,
          translations,
          version,
          generatedAt,
          ttl,
        },
      }));
      console.log(`💾 [UI-TRANSLATE] Cached translations for ${targetLanguage} v${version}`);
    } catch (cacheError) {
      console.warn('Failed to cache translations:', cacheError);
      // Continue anyway - translations were generated successfully
    }

    return response(200, {
      language: targetLanguage,
      country: targetCountry,
      translations,
      version,
      generatedAt,
      cached: false,
      generationTimeMs: duration,
    });
  } catch (error) {
    console.error('Translate UI strings error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * GET /api/translate/ui-strings/languages
 * Get list of supported languages
 */
export const getSupportedLanguages: APIGatewayProxyHandler = async () => {
  try {
    const languages = Object.entries(LANGUAGE_NAMES).map(([code, name]) => ({
      code,
      name,
    }));

    return response(200, {
      count: languages.length,
      languages,
    });
  } catch (error) {
    console.error('Get supported languages error:', error);
    return response(500, { message: 'Internal server error' });
  }
};
