/**
 * Multi-provider Translation Service
 * Supports: OpenAI, Anthropic (Claude), DeepSeek
 */

const AI_PROVIDER = process.env.AI_PROVIDER || 'openai';
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const ANTHROPIC_API_KEY = process.env.ANTHROPIC_API_KEY;
const DEEPSEEK_API_KEY = process.env.DEEPSEEK_API_KEY;

console.log(`🤖 Translation AI Provider: ${AI_PROVIDER.toUpperCase()}`);

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

// ============== Provider API Calls ==============

async function callOpenAI(messages: ChatMessage[], maxTokens = 1000): Promise<string> {
  const response = await fetch('https://api.openai.com/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${OPENAI_API_KEY}`,
    },
    body: JSON.stringify({
      model: 'gpt-4o-mini',
      messages,
      temperature: 0.3,
      max_tokens: maxTokens,
    }),
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`OpenAI API error: ${response.status} - ${error}`);
  }

  const data = await response.json();
  return data.choices[0]?.message?.content?.trim() || '';
}

async function callAnthropic(messages: ChatMessage[], maxTokens = 1000): Promise<string> {
  // Convert messages format for Anthropic
  const systemMsg = messages.find(m => m.role === 'system');
  const userMsgs = messages.filter(m => m.role !== 'system');

  const response = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-api-key': ANTHROPIC_API_KEY!,
      'anthropic-version': '2023-06-01',
    },
    body: JSON.stringify({
      model: 'claude-3-haiku-20240307',
      max_tokens: maxTokens,
      system: systemMsg?.content,
      messages: userMsgs.map(m => ({ role: m.role, content: m.content })),
    }),
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Anthropic API error: ${response.status} - ${error}`);
  }

  const data = await response.json();
  return data.content[0]?.text?.trim() || '';
}

async function callDeepSeek(messages: ChatMessage[], maxTokens = 1000): Promise<string> {
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
      max_tokens: maxTokens,
    }),
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`DeepSeek API error: ${response.status} - ${error}`);
  }

  const data = await response.json();
  return data.choices[0]?.message?.content?.trim() || '';
}

// ============== Main Chat Function ==============

async function chat(messages: ChatMessage[], maxTokens = 1000): Promise<string> {
  switch (AI_PROVIDER) {
    case 'anthropic':
      return callAnthropic(messages, maxTokens);
    case 'deepseek':
      return callDeepSeek(messages, maxTokens);
    case 'openai':
    default:
      return callOpenAI(messages, maxTokens);
  }
}

// ============== Translation Functions ==============

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

  console.log(`🌐 Translating (${AI_PROVIDER}): ${sourceName} -> ${targetName}`);

  try {
    const result = await chat([
      {
        role: 'system',
        content: `You are a professional translator. Translate from ${sourceName} to ${targetName}.

Rules:
- Maintain the original tone and context (casual, formal, friendly, etc.)
- Preserve any emojis, special characters, or formatting
- Do not add explanations or notes
- If the text contains slang or idioms, translate them to equivalent expressions
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
        content: 'Detect the language of the text and respond with ONLY the ISO 639-1 code (e.g., en, es, fr, de, ja, zh). Nothing else.',
      },
      {
        role: 'user',
        content: text,
      },
    ], 10);

    const code = result.toLowerCase().replace(/[^a-z]/g, '').slice(0, 2);
    return LANGUAGE_NAMES[code] ? code : 'en';
  } catch (error) {
    console.error('Language detection error:', error);
    return 'en';
  }
}

export function getProviderInfo() {
  return {
    provider: AI_PROVIDER,
    model: AI_PROVIDER === 'openai' ? 'gpt-4o-mini' 
         : AI_PROVIDER === 'anthropic' ? 'claude-3-haiku'
         : 'deepseek-chat',
  };
}
