/**
 * Translation service with two modes:
 * 1. DeepSeek API - Cheap cloud API ($0.14/million tokens)
 * 2. Ollama - Self-hosted, completely free after setup
 */

const TRANSLATION_MODE = process.env.TRANSLATION_MODE || 'api';
const DEEPSEEK_API_KEY = process.env.DEEPSEEK_API_KEY;
const OLLAMA_URL = process.env.OLLAMA_URL || 'http://localhost:11434';

const LANGUAGE_NAMES: Record<string, string> = {
  en: 'English', es: 'Spanish', fr: 'French', de: 'German',
  it: 'Italian', pt: 'Portuguese', zh: 'Chinese', ja: 'Japanese',
  ko: 'Korean', ar: 'Arabic', ru: 'Russian', hi: 'Hindi',
  nl: 'Dutch', sv: 'Swedish', pl: 'Polish', tr: 'Turkish',
};

interface ChatMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

async function callDeepSeekAPI(messages: ChatMessage[]): Promise<string> {
  const response = await fetch('https://api.deepseek.com/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${DEEPSEEK_API_KEY}`,
    },
    body: JSON.stringify({
      model: 'deepseek-chat',
      messages,
      temperature: 0.3,
      max_tokens: 1000,
    }),
  });

  if (!response.ok) {
    throw new Error(`DeepSeek API error: ${response.status}`);
  }

  const data = await response.json();
  return data.choices[0]?.message?.content?.trim() || '';
}

async function callOllama(messages: ChatMessage[]): Promise<string> {
  // Convert to Ollama format
  const prompt = messages.map(m => {
    if (m.role === 'system') return `System: ${m.content}`;
    if (m.role === 'user') return `User: ${m.content}`;
    return `Assistant: ${m.content}`;
  }).join('\n\n');

  const response = await fetch(`${OLLAMA_URL}/api/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: 'deepseek-r1:8b',  // Or deepseek-coder, qwen2.5, etc.
      prompt,
      stream: false,
      options: {
        temperature: 0.3,
        num_predict: 1000,
      },
    }),
  });

  if (!response.ok) {
    throw new Error(`Ollama error: ${response.status}`);
  }

  const data = await response.json();
  return data.response?.trim() || '';
}

async function chat(messages: ChatMessage[]): Promise<string> {
  if (TRANSLATION_MODE === 'ollama') {
    return callOllama(messages);
  }
  return callDeepSeekAPI(messages);
}

export async function translate(
  text: string,
  sourceLanguage: string,
  targetLanguage: string
): Promise<string> {
  if (sourceLanguage === targetLanguage) {
    return text;
  }

  const sourceName = LANGUAGE_NAMES[sourceLanguage] || sourceLanguage;
  const targetName = LANGUAGE_NAMES[targetLanguage] || targetLanguage;

  try {
    const result = await chat([
      {
        role: 'system',
        content: `You are a professional translator. Translate from ${sourceName} to ${targetName}.
Rules:
- Maintain the original tone and context
- Preserve emojis and formatting
- Return ONLY the translated text, nothing else`,
      },
      {
        role: 'user',
        content: text,
      },
    ]);

    return result || text;
  } catch (error) {
    console.error('Translation error:', error);
    return text; // Return original on error
  }
}

export async function detectLanguage(text: string): Promise<string> {
  try {
    const result = await chat([
      {
        role: 'system',
        content: 'Detect the language of the text and respond with ONLY the ISO 639-1 code (e.g., en, es, fr). Nothing else.',
      },
      {
        role: 'user',
        content: text,
      },
    ]);

    const code = result.toLowerCase().trim().slice(0, 2);
    return LANGUAGE_NAMES[code] ? code : 'en';
  } catch {
    return 'en';
  }
}

