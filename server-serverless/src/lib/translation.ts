/**
 * Multi-provider Translation Service
 * Supports: OpenAI, Anthropic (Claude), DeepSeek
 * 
 * Features region-aware translation for maximum accuracy
 */

const AI_PROVIDER = process.env.AI_PROVIDER || 'openai';
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const ANTHROPIC_API_KEY = process.env.ANTHROPIC_API_KEY;
const DEEPSEEK_API_KEY = process.env.DEEPSEEK_API_KEY;

console.log(`🤖 Translation AI Provider: ${AI_PROVIDER.toUpperCase()}`);

// Comprehensive language code to name mapping (120+ languages)
const LANGUAGE_NAMES: Record<string, string> = {
  // Major World Languages
  en: 'English', es: 'Spanish', fr: 'French', de: 'German',
  it: 'Italian', pt: 'Portuguese', ru: 'Russian', zh: 'Chinese (Mandarin)',
  ja: 'Japanese', ko: 'Korean', ar: 'Arabic', nl: 'Dutch',
  sv: 'Swedish', pl: 'Polish', tr: 'Turkish', uk: 'Ukrainian',
  cs: 'Czech', el: 'Greek', he: 'Hebrew', ro: 'Romanian',
  hu: 'Hungarian', da: 'Danish', fi: 'Finnish', no: 'Norwegian',
  sk: 'Slovak', bg: 'Bulgarian', hr: 'Croatian', sr: 'Serbian',
  sl: 'Slovenian', et: 'Estonian', lv: 'Latvian', lt: 'Lithuanian',
  
  // European Regional Languages
  ca: 'Catalan', gl: 'Galician', eu: 'Basque', oc: 'Occitan',
  ast: 'Asturian', sc: 'Sardinian', scn: 'Sicilian', nap: 'Neapolitan',
  fur: 'Friulian', br: 'Breton', co: 'Corsican', gsw: 'Alsatian',
  cy: 'Welsh', gd: 'Scottish Gaelic', ga: 'Irish', kw: 'Cornish',
  fy: 'Frisian', wa: 'Walloon', li: 'Limburgish', rm: 'Romansh',
  lb: 'Luxembourgish', nds: 'Low German', bar: 'Bavarian', hsb: 'Upper Sorbian',
  is: 'Icelandic', fo: 'Faroese', be: 'Belarusian', mk: 'Macedonian',
  sq: 'Albanian', bs: 'Bosnian', mt: 'Maltese',
  
  // Asian Languages
  yue: 'Cantonese', bo: 'Tibetan', ug: 'Uyghur', mn: 'Mongolian',
  vi: 'Vietnamese', th: 'Thai', id: 'Indonesian', ms: 'Malay',
  tl: 'Filipino', km: 'Khmer', lo: 'Lao', my: 'Burmese',
  jv: 'Javanese', su: 'Sundanese', ceb: 'Cebuano', ilo: 'Ilocano',
  
  // Indian Subcontinent Languages
  hi: 'Hindi', bn: 'Bengali', pa: 'Punjabi', ta: 'Tamil',
  te: 'Telugu', mr: 'Marathi', gu: 'Gujarati', kn: 'Kannada',
  ml: 'Malayalam', or: 'Odia', as: 'Assamese', ne: 'Nepali',
  si: 'Sinhala', ur: 'Urdu', sd: 'Sindhi', ks: 'Kashmiri',
  doi: 'Dogri', mai: 'Maithili', sat: 'Santali', kok: 'Konkani',
  mni: 'Manipuri', dv: 'Dhivehi',
  
  // Middle Eastern Languages
  fa: 'Persian', ku: 'Kurdish', ps: 'Pashto', az: 'Azerbaijani',
  hy: 'Armenian', ka: 'Georgian', uz: 'Uzbek', kk: 'Kazakh',
  tg: 'Tajik', tk: 'Turkmen', ky: 'Kyrgyz',
  
  // African Languages
  sw: 'Swahili', af: 'Afrikaans', am: 'Amharic', ha: 'Hausa',
  yo: 'Yoruba', ig: 'Igbo', zu: 'Zulu', xh: 'Xhosa',
  so: 'Somali', rw: 'Kinyarwanda', rn: 'Kirundi', sn: 'Shona',
  ny: 'Chichewa', mg: 'Malagasy', ti: 'Tigrinya', om: 'Oromo',
  wo: 'Wolof', ff: 'Fulah', ln: 'Lingala', kg: 'Kongo',
  st: 'Sesotho', tn: 'Setswana',
  
  // Americas Indigenous Languages
  qu: 'Quechua', gn: 'Guaraní', ay: 'Aymara', nah: 'Nahuatl',
  yua: 'Yucatec Maya', oj: 'Ojibwe', cr: 'Cree', iu: 'Inuktitut',
  nv: 'Navajo', chr: 'Cherokee', ht: 'Haitian Creole', srn: 'Sranan Tongo',
  
  // Classical & Historical Languages
  la: 'Latin', sa: 'Sanskrit', grc: 'Ancient Greek', cu: 'Church Slavonic',
  pi: 'Pali', cop: 'Coptic', syr: 'Syriac',
};

// Comprehensive country code to name mapping
const COUNTRY_NAMES: Record<string, string> = {
  // Americas
  US: 'United States', CA: 'Canada', MX: 'Mexico', BR: 'Brazil',
  AR: 'Argentina', CO: 'Colombia', PE: 'Peru', CL: 'Chile',
  VE: 'Venezuela', EC: 'Ecuador', BO: 'Bolivia', PY: 'Paraguay',
  UY: 'Uruguay', CR: 'Costa Rica', PA: 'Panama', GT: 'Guatemala',
  HN: 'Honduras', SV: 'El Salvador', NI: 'Nicaragua', CU: 'Cuba',
  DO: 'Dominican Republic', PR: 'Puerto Rico', JM: 'Jamaica', HT: 'Haiti',
  TT: 'Trinidad and Tobago', SR: 'Suriname',
  
  // Europe
  GB: 'United Kingdom', FR: 'France', DE: 'Germany', IT: 'Italy',
  ES: 'Spain', PT: 'Portugal', NL: 'Netherlands', BE: 'Belgium',
  CH: 'Switzerland', AT: 'Austria', SE: 'Sweden', NO: 'Norway',
  DK: 'Denmark', FI: 'Finland', IE: 'Ireland', PL: 'Poland',
  CZ: 'Czech Republic', SK: 'Slovakia', HU: 'Hungary', RO: 'Romania',
  BG: 'Bulgaria', GR: 'Greece', UA: 'Ukraine', RU: 'Russia',
  HR: 'Croatia', RS: 'Serbia', SI: 'Slovenia', EE: 'Estonia',
  LV: 'Latvia', LT: 'Lithuania', IS: 'Iceland', LU: 'Luxembourg',
  MT: 'Malta', AL: 'Albania', MK: 'North Macedonia', BA: 'Bosnia and Herzegovina',
  ME: 'Montenegro', XK: 'Kosovo', BY: 'Belarus', MD: 'Moldova',
  
  // Asia
  CN: 'China', JP: 'Japan', KR: 'South Korea', IN: 'India',
  ID: 'Indonesia', TH: 'Thailand', VN: 'Vietnam', MY: 'Malaysia',
  SG: 'Singapore', PH: 'Philippines', TW: 'Taiwan', HK: 'Hong Kong',
  PK: 'Pakistan', BD: 'Bangladesh', NP: 'Nepal', LK: 'Sri Lanka',
  MM: 'Myanmar', KH: 'Cambodia', LA: 'Laos', MN: 'Mongolia',
  MV: 'Maldives', BT: 'Bhutan',
  
  // Middle East & Central Asia
  TR: 'Turkey', SA: 'Saudi Arabia', AE: 'United Arab Emirates',
  IL: 'Israel', IR: 'Iran', IQ: 'Iraq', EG: 'Egypt', JO: 'Jordan',
  LB: 'Lebanon', SY: 'Syria', KW: 'Kuwait', QA: 'Qatar',
  BH: 'Bahrain', OM: 'Oman', YE: 'Yemen', AF: 'Afghanistan',
  AZ: 'Azerbaijan', AM: 'Armenia', GE: 'Georgia', KZ: 'Kazakhstan',
  UZ: 'Uzbekistan', TM: 'Turkmenistan', TJ: 'Tajikistan', KG: 'Kyrgyzstan',
  
  // Africa
  ZA: 'South Africa', NG: 'Nigeria', KE: 'Kenya', ET: 'Ethiopia',
  GH: 'Ghana', TZ: 'Tanzania', UG: 'Uganda', MA: 'Morocco',
  DZ: 'Algeria', TN: 'Tunisia', SN: 'Senegal', CI: 'Ivory Coast',
  CM: 'Cameroon', AO: 'Angola', MZ: 'Mozambique', ZW: 'Zimbabwe',
  RW: 'Rwanda', BI: 'Burundi', MW: 'Malawi', MG: 'Madagascar',
  SO: 'Somalia', ER: 'Eritrea', BW: 'Botswana', NA: 'Namibia',
  LS: 'Lesotho', SZ: 'Eswatini', CD: 'DR Congo', CG: 'Congo',
  
  // Oceania
  AU: 'Australia', NZ: 'New Zealand', FJ: 'Fiji', PG: 'Papua New Guinea',
  WS: 'Samoa', TO: 'Tonga',
};

interface ChatMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

// ============== Provider API Calls ==============

interface OpenAIResponse {
  choices: Array<{ message?: { content?: string } }>;
}

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

  const data = await response.json() as OpenAIResponse;
  return data.choices[0]?.message?.content?.trim() || '';
}

interface AnthropicResponse {
  content: Array<{ text?: string }>;
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

  const data = await response.json() as AnthropicResponse;
  return data.content[0]?.text?.trim() || '';
}

interface DeepSeekResponse {
  choices: Array<{ message?: { content?: string } }>;
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

  const data = await response.json() as DeepSeekResponse;
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

// ============== Translation Prompt Builder ==============

/**
 * Build a region-aware translation prompt for maximum accuracy
 */
function buildTranslationPrompt(
  targetLanguage: string,
  targetCountry?: string,
  targetRegion?: string
): string {
  const languageName = LANGUAGE_NAMES[targetLanguage] || targetLanguage;
  const countryName = targetCountry ? COUNTRY_NAMES[targetCountry] : null;

  // Build the prompt based on available context
  if (targetRegion && countryName) {
    // Full regional context
    return `Translate the following text with maximum accuracy into **${languageName}**,

using the vocabulary, grammar, idioms, and tone that are natural for speakers
from **${countryName}**, specifically the **${targetRegion}** region.

If the source sentence is ambiguous, choose the meaning that would be most natural
for native speakers of that region. Maintain the original tone (formal, informal,
slang, emotional, etc.) and preserve the intent of the message.

Do NOT explain the translation — only output the translated text.`;
  } else if (countryName) {
    // Country context only
    return `Translate the following text with maximum accuracy into **${languageName}**,

using the vocabulary, grammar, idioms, and tone that are natural for speakers
from **${countryName}**.

If the source sentence is ambiguous, choose the meaning that would be most natural
for native speakers of that country. Maintain the original tone (formal, informal,
slang, emotional, etc.) and preserve the intent of the message.

Do NOT explain the translation — only output the translated text.`;
  } else {
    // Language only
    return `Translate the following text with maximum accuracy into **${languageName}**.

Maintain the original tone (formal, informal, slang, emotional, etc.) and preserve 
the intent of the message.

Do NOT explain the translation — only output the translated text.`;
  }
}

/**
 * Build a region-aware document translation prompt
 */
function buildDocumentTranslationPrompt(
  targetLanguage: string,
  targetCountry?: string,
  targetRegion?: string
): string {
  const languageName = LANGUAGE_NAMES[targetLanguage] || targetLanguage;
  const countryName = targetCountry ? COUNTRY_NAMES[targetCountry] : null;

  // Build the prompt based on available context
  if (targetRegion && countryName) {
    return `Translate the following document content with maximum accuracy into **${languageName}**,

using the vocabulary, grammar, idioms, and tone that are natural for speakers
from **${countryName}**, specifically the **${targetRegion}** region.

Preserve any formatting, file paths, technical terms, or special notation.
Maintain the original tone and intent of the document.

Do NOT explain the translation — only output the translated text.`;
  } else if (countryName) {
    return `Translate the following document content with maximum accuracy into **${languageName}**,

using the vocabulary and style natural for speakers from **${countryName}**.

Preserve any formatting, file paths, technical terms, or special notation.
Maintain the original tone and intent of the document.

Do NOT explain the translation — only output the translated text.`;
  } else {
    return `Translate the following document content with maximum accuracy into **${languageName}**.

Preserve any formatting, file paths, technical terms, or special notation.
Maintain the original tone and intent of the document.

Do NOT explain the translation — only output the translated text.`;
  }
}

// ============== Translation Functions ==============

export async function translate(
  text: string,
  sourceLanguage: string,
  targetLanguage: string,
  targetCountry?: string,
  targetRegion?: string
): Promise<string> {
  // Check if same language (ignore country/region for this check)
  const sourceBase = sourceLanguage.split('-')[0];
  const targetBase = targetLanguage.split('-')[0];
  if (sourceBase === targetBase && !targetCountry && !targetRegion) {
    return text;
  }

  const sourceName = LANGUAGE_NAMES[sourceLanguage] || LANGUAGE_NAMES[sourceBase] || sourceLanguage;
  const targetName = LANGUAGE_NAMES[targetLanguage] || LANGUAGE_NAMES[targetBase] || targetLanguage;
  const countryName = targetCountry ? COUNTRY_NAMES[targetCountry] : null;

  // Build log description
  let targetDescription = targetName;
  if (countryName) targetDescription += ` (${countryName})`;
  if (targetRegion) targetDescription += ` [${targetRegion}]`;

  console.log(`🌐 Translating (${AI_PROVIDER}): ${sourceName} -> ${targetDescription}`);

  try {
    const systemPrompt = buildTranslationPrompt(targetLanguage, targetCountry, targetRegion);
    
    const result = await chat([
      {
        role: 'system',
        content: systemPrompt,
      },
      {
        role: 'user',
        content: `Text to translate:\n"${text}"`,
      },
    ]);

    return result || text;
  } catch (error) {
    console.error('Translation error:', error);
    return text; // Return original on error
  }
}

/**
 * Translate document content with appropriate context
 * Documents may contain different formatting than chat messages
 */
export async function translateDocumentContent(
  text: string,
  sourceLanguage: string,
  targetLanguage: string,
  targetCountry?: string,
  targetRegion?: string
): Promise<string> {
  // Check if same language
  const sourceBase = sourceLanguage.split('-')[0];
  const targetBase = targetLanguage.split('-')[0];
  if (sourceBase === targetBase && !targetCountry && !targetRegion) {
    return text;
  }

  const sourceName = LANGUAGE_NAMES[sourceLanguage] || LANGUAGE_NAMES[sourceBase] || sourceLanguage;
  const targetName = LANGUAGE_NAMES[targetLanguage] || LANGUAGE_NAMES[targetBase] || targetLanguage;
  const countryName = targetCountry ? COUNTRY_NAMES[targetCountry] : null;

  // Build log description
  let targetDescription = targetName;
  if (countryName) targetDescription += ` (${countryName})`;
  if (targetRegion) targetDescription += ` [${targetRegion}]`;

  console.log(`📄 Translating document (${AI_PROVIDER}): ${sourceName} -> ${targetDescription}`);

  try {
    const systemPrompt = buildDocumentTranslationPrompt(targetLanguage, targetCountry, targetRegion);
    
    const result = await chat([
      {
        role: 'system',
        content: systemPrompt,
      },
      {
        role: 'user',
        content: `Document content to translate:\n"${text}"`,
      },
    ]);

    return result || text;
  } catch (error) {
    console.error('Document translation error:', error);
    return text;
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

// Export language/country mappings for use elsewhere
export { LANGUAGE_NAMES, COUNTRY_NAMES };
