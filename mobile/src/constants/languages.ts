export type LanguageCode = 
  | 'en' | 'es' | 'fr' | 'de' | 'it' | 'pt' 
  | 'zh' | 'ja' | 'ko' | 'ar' | 'ru' | 'hi' 
  | 'nl' | 'sv' | 'pl' | 'tr';

export const SUPPORTED_LANGUAGES: Record<LanguageCode, string> = {
  en: 'English',
  es: 'Español',
  fr: 'Français',
  de: 'Deutsch',
  it: 'Italiano',
  pt: 'Português',
  zh: '中文',
  ja: '日本語',
  ko: '한국어',
  ar: 'العربية',
  ru: 'Русский',
  hi: 'हिन्दी',
  nl: 'Nederlands',
  sv: 'Svenska',
  pl: 'Polski',
  tr: 'Türkçe',
};

export const LANGUAGE_FLAGS: Record<LanguageCode, string> = {
  en: '🇺🇸',
  es: '🇪🇸',
  fr: '🇫🇷',
  de: '🇩🇪',
  it: '🇮🇹',
  pt: '🇧🇷',
  zh: '🇨🇳',
  ja: '🇯🇵',
  ko: '🇰🇷',
  ar: '🇸🇦',
  ru: '🇷🇺',
  hi: '🇮🇳',
  nl: '🇳🇱',
  sv: '🇸🇪',
  pl: '🇵🇱',
  tr: '🇹🇷',
};

