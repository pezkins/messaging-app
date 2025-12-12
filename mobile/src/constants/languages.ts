// Language Category Types
export type LanguageCategory = 
  | 'majorWorld'
  | 'europeanRegional'
  | 'asian'
  | 'indianSubcontinent'
  | 'middleEastern'
  | 'african'
  | 'americasIndigenous'
  | 'classical';

export const LANGUAGE_CATEGORIES: Record<LanguageCategory, { name: string; icon: string }> = {
  majorWorld: { name: 'Major World Languages', icon: '🌍' },
  europeanRegional: { name: 'European Regional', icon: '🇪🇺' },
  asian: { name: 'Asian Languages', icon: '🌏' },
  indianSubcontinent: { name: 'Indian Subcontinent', icon: '🇮🇳' },
  middleEastern: { name: 'Middle Eastern', icon: '🕌' },
  african: { name: 'African Languages', icon: '🌍' },
  americasIndigenous: { name: 'Americas Indigenous', icon: '🌎' },
  classical: { name: 'Classical & Historical', icon: '📜' },
};

// All languages with categories and optional regions
export const LANGUAGES = [
  // ========================================
  // MAJOR WORLD LANGUAGES
  // ========================================
  { code: 'en', name: 'English', native: 'English', category: 'majorWorld' as LanguageCategory },
  { code: 'es', name: 'Spanish', native: 'Español', category: 'majorWorld' as LanguageCategory },
  { code: 'fr', name: 'French', native: 'Français', category: 'majorWorld' as LanguageCategory },
  { code: 'de', name: 'German', native: 'Deutsch', category: 'majorWorld' as LanguageCategory },
  { code: 'it', name: 'Italian', native: 'Italiano', category: 'majorWorld' as LanguageCategory },
  { code: 'pt', name: 'Portuguese', native: 'Português', category: 'majorWorld' as LanguageCategory },
  { code: 'ru', name: 'Russian', native: 'Русский', category: 'majorWorld' as LanguageCategory },
  { code: 'zh', name: 'Chinese (Mandarin)', native: '普通话', category: 'majorWorld' as LanguageCategory },
  { code: 'ja', name: 'Japanese', native: '日本語', category: 'majorWorld' as LanguageCategory },
  { code: 'ko', name: 'Korean', native: '한국어', category: 'majorWorld' as LanguageCategory },
  { code: 'ar', name: 'Arabic', native: 'العربية', category: 'majorWorld' as LanguageCategory },
  { code: 'nl', name: 'Dutch', native: 'Nederlands', category: 'majorWorld' as LanguageCategory },
  { code: 'sv', name: 'Swedish', native: 'Svenska', category: 'majorWorld' as LanguageCategory },
  { code: 'pl', name: 'Polish', native: 'Polski', category: 'majorWorld' as LanguageCategory },
  { code: 'tr', name: 'Turkish', native: 'Türkçe', category: 'majorWorld' as LanguageCategory },
  { code: 'uk', name: 'Ukrainian', native: 'Українська', category: 'majorWorld' as LanguageCategory },
  { code: 'cs', name: 'Czech', native: 'Čeština', category: 'majorWorld' as LanguageCategory },
  { code: 'el', name: 'Greek', native: 'Ελληνικά', category: 'majorWorld' as LanguageCategory },
  { code: 'he', name: 'Hebrew', native: 'עברית', category: 'majorWorld' as LanguageCategory },
  { code: 'ro', name: 'Romanian', native: 'Română', category: 'majorWorld' as LanguageCategory },
  { code: 'hu', name: 'Hungarian', native: 'Magyar', category: 'majorWorld' as LanguageCategory },
  { code: 'da', name: 'Danish', native: 'Dansk', category: 'majorWorld' as LanguageCategory },
  { code: 'fi', name: 'Finnish', native: 'Suomi', category: 'majorWorld' as LanguageCategory },
  { code: 'no', name: 'Norwegian', native: 'Norsk', category: 'majorWorld' as LanguageCategory },
  { code: 'sk', name: 'Slovak', native: 'Slovenčina', category: 'majorWorld' as LanguageCategory },
  { code: 'bg', name: 'Bulgarian', native: 'Български', category: 'majorWorld' as LanguageCategory },
  { code: 'hr', name: 'Croatian', native: 'Hrvatski', category: 'majorWorld' as LanguageCategory },
  { code: 'sr', name: 'Serbian', native: 'Српски', category: 'majorWorld' as LanguageCategory },
  { code: 'sl', name: 'Slovenian', native: 'Slovenščina', category: 'majorWorld' as LanguageCategory },
  { code: 'et', name: 'Estonian', native: 'Eesti', category: 'majorWorld' as LanguageCategory },
  { code: 'lv', name: 'Latvian', native: 'Latviešu', category: 'majorWorld' as LanguageCategory },
  { code: 'lt', name: 'Lithuanian', native: 'Lietuvių', category: 'majorWorld' as LanguageCategory },

  // ========================================
  // EUROPEAN REGIONAL LANGUAGES
  // ========================================
  // Spain
  { code: 'ca', name: 'Catalan', native: 'Català', category: 'europeanRegional' as LanguageCategory, region: 'Spain - Catalonia, Valencia, Balearic Islands' },
  { code: 'gl', name: 'Galician', native: 'Galego', category: 'europeanRegional' as LanguageCategory, region: 'Spain - Galicia' },
  { code: 'eu', name: 'Basque', native: 'Euskara', category: 'europeanRegional' as LanguageCategory, region: 'Spain/France - Basque Country' },
  { code: 'oc', name: 'Occitan', native: 'Occitan', category: 'europeanRegional' as LanguageCategory, region: 'France/Spain - Southern France, Val d\'Aran' },
  { code: 'ast', name: 'Asturian', native: 'Asturianu', category: 'europeanRegional' as LanguageCategory, region: 'Spain - Asturias' },

  // Italy
  { code: 'sc', name: 'Sardinian', native: 'Sardu', category: 'europeanRegional' as LanguageCategory, region: 'Italy - Sardinia' },
  { code: 'scn', name: 'Sicilian', native: 'Sicilianu', category: 'europeanRegional' as LanguageCategory, region: 'Italy - Sicily' },
  { code: 'nap', name: 'Neapolitan', native: 'Napulitano', category: 'europeanRegional' as LanguageCategory, region: 'Italy - Naples, Campania' },
  { code: 'fur', name: 'Friulian', native: 'Furlan', category: 'europeanRegional' as LanguageCategory, region: 'Italy - Friuli' },

  // France
  { code: 'br', name: 'Breton', native: 'Brezhoneg', category: 'europeanRegional' as LanguageCategory, region: 'France - Brittany' },
  { code: 'co', name: 'Corsican', native: 'Corsu', category: 'europeanRegional' as LanguageCategory, region: 'France - Corsica' },
  { code: 'gsw', name: 'Alsatian', native: 'Elsässisch', category: 'europeanRegional' as LanguageCategory, region: 'France - Alsace' },

  // UK & Ireland
  { code: 'cy', name: 'Welsh', native: 'Cymraeg', category: 'europeanRegional' as LanguageCategory, region: 'United Kingdom - Wales' },
  { code: 'gd', name: 'Scottish Gaelic', native: 'Gàidhlig', category: 'europeanRegional' as LanguageCategory, region: 'United Kingdom - Scotland' },
  { code: 'ga', name: 'Irish', native: 'Gaeilge', category: 'europeanRegional' as LanguageCategory, region: 'Ireland' },
  { code: 'kw', name: 'Cornish', native: 'Kernewek', category: 'europeanRegional' as LanguageCategory, region: 'United Kingdom - Cornwall' },

  // Low Countries & Switzerland
  { code: 'fy', name: 'Frisian', native: 'Frysk', category: 'europeanRegional' as LanguageCategory, region: 'Netherlands - Friesland' },
  { code: 'wa', name: 'Walloon', native: 'Walon', category: 'europeanRegional' as LanguageCategory, region: 'Belgium - Wallonia' },
  { code: 'li', name: 'Limburgish', native: 'Limburgs', category: 'europeanRegional' as LanguageCategory, region: 'Belgium/Netherlands - Limburg' },
  { code: 'rm', name: 'Romansh', native: 'Rumantsch', category: 'europeanRegional' as LanguageCategory, region: 'Switzerland - Graubünden' },
  { code: 'lb', name: 'Luxembourgish', native: 'Lëtzebuergesch', category: 'europeanRegional' as LanguageCategory, region: 'Luxembourg' },

  // Germany & Austria
  { code: 'nds', name: 'Low German', native: 'Plattdüütsch', category: 'europeanRegional' as LanguageCategory, region: 'Germany - Northern Germany' },
  { code: 'bar', name: 'Bavarian', native: 'Boarisch', category: 'europeanRegional' as LanguageCategory, region: 'Germany/Austria - Bavaria, Austria' },
  { code: 'hsb', name: 'Upper Sorbian', native: 'Hornjoserbšćina', category: 'europeanRegional' as LanguageCategory, region: 'Germany - Saxony' },

  // Nordic
  { code: 'is', name: 'Icelandic', native: 'Íslenska', category: 'europeanRegional' as LanguageCategory, region: 'Iceland' },
  { code: 'fo', name: 'Faroese', native: 'Føroyskt', category: 'europeanRegional' as LanguageCategory, region: 'Faroe Islands' },

  // Eastern Europe
  { code: 'be', name: 'Belarusian', native: 'Беларуская', category: 'europeanRegional' as LanguageCategory, region: 'Belarus' },
  { code: 'mk', name: 'Macedonian', native: 'Македонски', category: 'europeanRegional' as LanguageCategory, region: 'North Macedonia' },
  { code: 'sq', name: 'Albanian', native: 'Shqip', category: 'europeanRegional' as LanguageCategory, region: 'Albania, Kosovo' },
  { code: 'bs', name: 'Bosnian', native: 'Bosanski', category: 'europeanRegional' as LanguageCategory, region: 'Bosnia and Herzegovina' },
  { code: 'mt', name: 'Maltese', native: 'Malti', category: 'europeanRegional' as LanguageCategory, region: 'Malta' },

  // ========================================
  // ASIAN LANGUAGES
  // ========================================
  { code: 'yue', name: 'Cantonese', native: '粵語', category: 'asian' as LanguageCategory, region: 'China - Hong Kong, Guangdong' },
  { code: 'bo', name: 'Tibetan', native: 'བོད་སྐད', category: 'asian' as LanguageCategory, region: 'China - Tibet' },
  { code: 'ug', name: 'Uyghur', native: 'ئۇيغۇرچە', category: 'asian' as LanguageCategory, region: 'China - Xinjiang' },
  { code: 'mn', name: 'Mongolian', native: 'Монгол', category: 'asian' as LanguageCategory, region: 'Mongolia' },
  { code: 'vi', name: 'Vietnamese', native: 'Tiếng Việt', category: 'asian' as LanguageCategory },
  { code: 'th', name: 'Thai', native: 'ไทย', category: 'asian' as LanguageCategory },
  { code: 'id', name: 'Indonesian', native: 'Bahasa Indonesia', category: 'asian' as LanguageCategory },
  { code: 'ms', name: 'Malay', native: 'Bahasa Melayu', category: 'asian' as LanguageCategory },
  { code: 'tl', name: 'Tagalog', native: 'Tagalog', category: 'asian' as LanguageCategory },
  { code: 'km', name: 'Khmer', native: 'ភាសាខ្មែរ', category: 'asian' as LanguageCategory, region: 'Cambodia' },
  { code: 'lo', name: 'Lao', native: 'ລາວ', category: 'asian' as LanguageCategory, region: 'Laos' },
  { code: 'my', name: 'Burmese', native: 'မြန်မာဘာသာ', category: 'asian' as LanguageCategory, region: 'Myanmar' },
  { code: 'jv', name: 'Javanese', native: 'Basa Jawa', category: 'asian' as LanguageCategory, region: 'Indonesia - Java' },
  { code: 'su', name: 'Sundanese', native: 'Basa Sunda', category: 'asian' as LanguageCategory, region: 'Indonesia - West Java' },
  { code: 'ceb', name: 'Cebuano', native: 'Cebuano', category: 'asian' as LanguageCategory, region: 'Philippines - Visayas' },
  { code: 'ilo', name: 'Ilocano', native: 'Ilokano', category: 'asian' as LanguageCategory, region: 'Philippines - Northern Luzon' },

  // ========================================
  // INDIAN SUBCONTINENT LANGUAGES
  // ========================================
  { code: 'hi', name: 'Hindi', native: 'हिन्दी', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'bn', name: 'Bengali', native: 'বাংলা', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'pa', name: 'Punjabi', native: 'ਪੰਜਾਬੀ', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'ta', name: 'Tamil', native: 'தமிழ்', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'te', name: 'Telugu', native: 'తెలుగు', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'mr', name: 'Marathi', native: 'मराठी', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'gu', name: 'Gujarati', native: 'ગુજરાતી', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'kn', name: 'Kannada', native: 'ಕನ್ನಡ', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'ml', name: 'Malayalam', native: 'മലയാളം', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'or', name: 'Odia', native: 'ଓଡ଼ିଆ', category: 'indianSubcontinent' as LanguageCategory, region: 'India - Odisha' },
  { code: 'as', name: 'Assamese', native: 'অসমীয়া', category: 'indianSubcontinent' as LanguageCategory, region: 'India - Assam' },
  { code: 'ne', name: 'Nepali', native: 'नेपाली', category: 'indianSubcontinent' as LanguageCategory, region: 'Nepal' },
  { code: 'si', name: 'Sinhala', native: 'සිංහල', category: 'indianSubcontinent' as LanguageCategory, region: 'Sri Lanka' },
  { code: 'ur', name: 'Urdu', native: 'اردو', category: 'indianSubcontinent' as LanguageCategory },
  { code: 'sd', name: 'Sindhi', native: 'سنڌي', category: 'indianSubcontinent' as LanguageCategory, region: 'Pakistan - Sindh' },
  { code: 'ks', name: 'Kashmiri', native: 'कॉशुر', category: 'indianSubcontinent' as LanguageCategory, region: 'India/Pakistan - Kashmir' },
  { code: 'doi', name: 'Dogri', native: 'डोगरी', category: 'indianSubcontinent' as LanguageCategory, region: 'India - Jammu' },
  { code: 'mai', name: 'Maithili', native: 'मैथिली', category: 'indianSubcontinent' as LanguageCategory, region: 'India - Bihar' },
  { code: 'sat', name: 'Santali', native: 'ᱥᱟᱱᱛᱟᱲᱤ', category: 'indianSubcontinent' as LanguageCategory, region: 'India - Jharkhand' },
  { code: 'kok', name: 'Konkani', native: 'कोंकणी', category: 'indianSubcontinent' as LanguageCategory, region: 'India - Goa' },
  { code: 'mni', name: 'Manipuri', native: 'মৈতৈলোন্', category: 'indianSubcontinent' as LanguageCategory, region: 'India - Manipur' },
  { code: 'dv', name: 'Dhivehi', native: 'ދިވެހި', category: 'indianSubcontinent' as LanguageCategory, region: 'Maldives' },

  // ========================================
  // MIDDLE EASTERN LANGUAGES
  // ========================================
  { code: 'fa', name: 'Persian', native: 'فارسی', category: 'middleEastern' as LanguageCategory },
  { code: 'ku', name: 'Kurdish', native: 'Kurdî', category: 'middleEastern' as LanguageCategory, region: 'Kurdistan Region' },
  { code: 'ps', name: 'Pashto', native: 'پښتو', category: 'middleEastern' as LanguageCategory, region: 'Afghanistan, Pakistan' },
  { code: 'az', name: 'Azerbaijani', native: 'Azərbaycan', category: 'middleEastern' as LanguageCategory },
  { code: 'hy', name: 'Armenian', native: 'Հայdelays', category: 'middleEastern' as LanguageCategory },
  { code: 'ka', name: 'Georgian', native: 'ქართული', category: 'middleEastern' as LanguageCategory },
  { code: 'uz', name: 'Uzbek', native: 'Oʻzbek', category: 'middleEastern' as LanguageCategory },
  { code: 'kk', name: 'Kazakh', native: 'Қазақ', category: 'middleEastern' as LanguageCategory },
  { code: 'tg', name: 'Tajik', native: 'Тоҷикӣ', category: 'middleEastern' as LanguageCategory },
  { code: 'tk', name: 'Turkmen', native: 'Türkmen', category: 'middleEastern' as LanguageCategory },
  { code: 'ky', name: 'Kyrgyz', native: 'Кыргыз', category: 'middleEastern' as LanguageCategory },

  // ========================================
  // AFRICAN LANGUAGES
  // ========================================
  { code: 'sw', name: 'Swahili', native: 'Kiswahili', category: 'african' as LanguageCategory },
  { code: 'af', name: 'Afrikaans', native: 'Afrikaans', category: 'african' as LanguageCategory },
  { code: 'am', name: 'Amharic', native: 'አማርኛ', category: 'african' as LanguageCategory, region: 'Ethiopia' },
  { code: 'ha', name: 'Hausa', native: 'Hausa', category: 'african' as LanguageCategory, region: 'Nigeria, Niger' },
  { code: 'yo', name: 'Yoruba', native: 'Yorùbá', category: 'african' as LanguageCategory, region: 'Nigeria' },
  { code: 'ig', name: 'Igbo', native: 'Igbo', category: 'african' as LanguageCategory, region: 'Nigeria' },
  { code: 'zu', name: 'Zulu', native: 'isiZulu', category: 'african' as LanguageCategory, region: 'South Africa' },
  { code: 'xh', name: 'Xhosa', native: 'isiXhosa', category: 'african' as LanguageCategory, region: 'South Africa' },
  { code: 'so', name: 'Somali', native: 'Soomaali', category: 'african' as LanguageCategory, region: 'Somalia' },
  { code: 'rw', name: 'Kinyarwanda', native: 'Ikinyarwanda', category: 'african' as LanguageCategory, region: 'Rwanda' },
  { code: 'rn', name: 'Kirundi', native: 'Ikirundi', category: 'african' as LanguageCategory, region: 'Burundi' },
  { code: 'sn', name: 'Shona', native: 'chiShona', category: 'african' as LanguageCategory, region: 'Zimbabwe' },
  { code: 'ny', name: 'Chichewa', native: 'Chichewa', category: 'african' as LanguageCategory, region: 'Malawi' },
  { code: 'mg', name: 'Malagasy', native: 'Malagasy', category: 'african' as LanguageCategory, region: 'Madagascar' },
  { code: 'ti', name: 'Tigrinya', native: 'ትግርኛ', category: 'african' as LanguageCategory, region: 'Eritrea, Ethiopia' },
  { code: 'om', name: 'Oromo', native: 'Afaan Oromoo', category: 'african' as LanguageCategory, region: 'Ethiopia' },
  { code: 'wo', name: 'Wolof', native: 'Wolof', category: 'african' as LanguageCategory, region: 'Senegal' },
  { code: 'ff', name: 'Fulah', native: 'Fulfulde', category: 'african' as LanguageCategory, region: 'West Africa' },
  { code: 'ln', name: 'Lingala', native: 'Lingála', category: 'african' as LanguageCategory, region: 'Congo' },
  { code: 'kg', name: 'Kongo', native: 'Kikongo', category: 'african' as LanguageCategory, region: 'Congo' },
  { code: 'st', name: 'Sesotho', native: 'Sesotho', category: 'african' as LanguageCategory, region: 'Lesotho, South Africa' },
  { code: 'tn', name: 'Setswana', native: 'Setswana', category: 'african' as LanguageCategory, region: 'Botswana, South Africa' },

  // ========================================
  // AMERICAS INDIGENOUS LANGUAGES
  // ========================================
  { code: 'qu', name: 'Quechua', native: 'Runasimi', category: 'americasIndigenous' as LanguageCategory, region: 'Peru, Bolivia, Ecuador' },
  { code: 'gn', name: 'Guaraní', native: 'Avañe\'ẽ', category: 'americasIndigenous' as LanguageCategory, region: 'Paraguay' },
  { code: 'ay', name: 'Aymara', native: 'Aymar aru', category: 'americasIndigenous' as LanguageCategory, region: 'Bolivia, Peru' },
  { code: 'nah', name: 'Nahuatl', native: 'Nāhuatl', category: 'americasIndigenous' as LanguageCategory, region: 'Mexico' },
  { code: 'yua', name: 'Yucatec Maya', native: 'Màaya t\'àan', category: 'americasIndigenous' as LanguageCategory, region: 'Mexico - Yucatán' },
  { code: 'oj', name: 'Ojibwe', native: 'Anishinaabemowin', category: 'americasIndigenous' as LanguageCategory, region: 'USA/Canada - Great Lakes' },
  { code: 'cr', name: 'Cree', native: 'ᓀᐦᐃᔭᐍᐏᐣ', category: 'americasIndigenous' as LanguageCategory, region: 'Canada' },
  { code: 'iu', name: 'Inuktitut', native: 'ᐃᓄᒃᑎᑐᑦ', category: 'americasIndigenous' as LanguageCategory, region: 'Canada - Nunavut' },
  { code: 'nv', name: 'Navajo', native: 'Diné bizaad', category: 'americasIndigenous' as LanguageCategory, region: 'USA - Southwest' },
  { code: 'chr', name: 'Cherokee', native: 'ᏣᎳᎩ', category: 'americasIndigenous' as LanguageCategory, region: 'USA - Oklahoma' },
  { code: 'ht', name: 'Haitian Creole', native: 'Kreyòl ayisyen', category: 'americasIndigenous' as LanguageCategory, region: 'Haiti' },
  { code: 'srn', name: 'Sranan Tongo', native: 'Sranan', category: 'americasIndigenous' as LanguageCategory, region: 'Suriname' },

  // ========================================
  // CLASSICAL & HISTORICAL LANGUAGES
  // ========================================
  { code: 'la', name: 'Latin', native: 'Latina', category: 'classical' as LanguageCategory, region: 'Vatican, Scholarly' },
  { code: 'sa', name: 'Sanskrit', native: 'संस्कृतम्', category: 'classical' as LanguageCategory, region: 'Hindu Rituals, Scholarly' },
  { code: 'grc', name: 'Ancient Greek', native: 'Ἑλληνική', category: 'classical' as LanguageCategory, region: 'Scholarly, Theological' },
  { code: 'cu', name: 'Church Slavonic', native: 'Словѣ́ньскъ', category: 'classical' as LanguageCategory, region: 'Orthodox Liturgy' },
  { code: 'pi', name: 'Pali', native: 'पालि', category: 'classical' as LanguageCategory, region: 'Buddhist Texts' },
  { code: 'cop', name: 'Coptic', native: 'Ⲙⲉⲧⲣⲉⲙⲛ̀ⲭⲏⲙⲓ', category: 'classical' as LanguageCategory, region: 'Coptic Christian Liturgy' },
  { code: 'syr', name: 'Syriac', native: 'ܠܫܢܐ ܣܘܪܝܝܐ', category: 'classical' as LanguageCategory, region: 'Syriac Christian Liturgy' },
] as const;

// All countries with flag emojis
export const COUNTRIES = [
  // Americas
  { code: 'US', name: 'United States', flag: '🇺🇸' },
  { code: 'CA', name: 'Canada', flag: '🇨🇦' },
  { code: 'MX', name: 'Mexico', flag: '🇲🇽' },
  { code: 'BR', name: 'Brazil', flag: '🇧🇷' },
  { code: 'AR', name: 'Argentina', flag: '🇦🇷' },
  { code: 'CO', name: 'Colombia', flag: '🇨🇴' },
  { code: 'PE', name: 'Peru', flag: '🇵🇪' },
  { code: 'CL', name: 'Chile', flag: '🇨🇱' },
  { code: 'VE', name: 'Venezuela', flag: '🇻🇪' },
  { code: 'EC', name: 'Ecuador', flag: '🇪🇨' },
  { code: 'BO', name: 'Bolivia', flag: '🇧🇴' },
  { code: 'PY', name: 'Paraguay', flag: '🇵🇾' },
  { code: 'UY', name: 'Uruguay', flag: '🇺🇾' },
  { code: 'CR', name: 'Costa Rica', flag: '🇨🇷' },
  { code: 'PA', name: 'Panama', flag: '🇵🇦' },
  { code: 'GT', name: 'Guatemala', flag: '🇬🇹' },
  { code: 'HN', name: 'Honduras', flag: '🇭🇳' },
  { code: 'SV', name: 'El Salvador', flag: '🇸🇻' },
  { code: 'NI', name: 'Nicaragua', flag: '🇳🇮' },
  { code: 'CU', name: 'Cuba', flag: '🇨🇺' },
  { code: 'DO', name: 'Dominican Republic', flag: '🇩🇴' },
  { code: 'PR', name: 'Puerto Rico', flag: '🇵🇷' },
  { code: 'JM', name: 'Jamaica', flag: '🇯🇲' },
  { code: 'HT', name: 'Haiti', flag: '🇭🇹' },
  { code: 'TT', name: 'Trinidad and Tobago', flag: '🇹🇹' },
  { code: 'SR', name: 'Suriname', flag: '🇸🇷' },
  
  // Europe
  { code: 'GB', name: 'United Kingdom', flag: '🇬🇧' },
  { code: 'FR', name: 'France', flag: '🇫🇷' },
  { code: 'DE', name: 'Germany', flag: '🇩🇪' },
  { code: 'IT', name: 'Italy', flag: '🇮🇹' },
  { code: 'ES', name: 'Spain', flag: '🇪🇸' },
  { code: 'PT', name: 'Portugal', flag: '🇵🇹' },
  { code: 'NL', name: 'Netherlands', flag: '🇳🇱' },
  { code: 'BE', name: 'Belgium', flag: '🇧🇪' },
  { code: 'CH', name: 'Switzerland', flag: '🇨🇭' },
  { code: 'AT', name: 'Austria', flag: '🇦🇹' },
  { code: 'SE', name: 'Sweden', flag: '🇸🇪' },
  { code: 'NO', name: 'Norway', flag: '🇳🇴' },
  { code: 'DK', name: 'Denmark', flag: '🇩🇰' },
  { code: 'FI', name: 'Finland', flag: '🇫🇮' },
  { code: 'IE', name: 'Ireland', flag: '🇮🇪' },
  { code: 'PL', name: 'Poland', flag: '🇵🇱' },
  { code: 'CZ', name: 'Czech Republic', flag: '🇨🇿' },
  { code: 'SK', name: 'Slovakia', flag: '🇸🇰' },
  { code: 'HU', name: 'Hungary', flag: '🇭🇺' },
  { code: 'RO', name: 'Romania', flag: '🇷🇴' },
  { code: 'BG', name: 'Bulgaria', flag: '🇧🇬' },
  { code: 'GR', name: 'Greece', flag: '🇬🇷' },
  { code: 'UA', name: 'Ukraine', flag: '🇺🇦' },
  { code: 'RU', name: 'Russia', flag: '🇷🇺' },
  { code: 'HR', name: 'Croatia', flag: '🇭🇷' },
  { code: 'RS', name: 'Serbia', flag: '🇷🇸' },
  { code: 'SI', name: 'Slovenia', flag: '🇸🇮' },
  { code: 'EE', name: 'Estonia', flag: '🇪🇪' },
  { code: 'LV', name: 'Latvia', flag: '🇱🇻' },
  { code: 'LT', name: 'Lithuania', flag: '🇱🇹' },
  { code: 'IS', name: 'Iceland', flag: '🇮🇸' },
  { code: 'LU', name: 'Luxembourg', flag: '🇱🇺' },
  { code: 'MT', name: 'Malta', flag: '🇲🇹' },
  { code: 'AL', name: 'Albania', flag: '🇦🇱' },
  { code: 'MK', name: 'North Macedonia', flag: '🇲🇰' },
  { code: 'BA', name: 'Bosnia and Herzegovina', flag: '🇧🇦' },
  { code: 'ME', name: 'Montenegro', flag: '🇲🇪' },
  { code: 'XK', name: 'Kosovo', flag: '🇽🇰' },
  { code: 'BY', name: 'Belarus', flag: '🇧🇾' },
  { code: 'MD', name: 'Moldova', flag: '🇲🇩' },
  
  // Asia
  { code: 'CN', name: 'China', flag: '🇨🇳' },
  { code: 'JP', name: 'Japan', flag: '🇯🇵' },
  { code: 'KR', name: 'South Korea', flag: '🇰🇷' },
  { code: 'IN', name: 'India', flag: '🇮🇳' },
  { code: 'ID', name: 'Indonesia', flag: '🇮🇩' },
  { code: 'TH', name: 'Thailand', flag: '🇹🇭' },
  { code: 'VN', name: 'Vietnam', flag: '🇻🇳' },
  { code: 'MY', name: 'Malaysia', flag: '🇲🇾' },
  { code: 'SG', name: 'Singapore', flag: '🇸🇬' },
  { code: 'PH', name: 'Philippines', flag: '🇵🇭' },
  { code: 'TW', name: 'Taiwan', flag: '🇹🇼' },
  { code: 'HK', name: 'Hong Kong', flag: '🇭🇰' },
  { code: 'PK', name: 'Pakistan', flag: '🇵🇰' },
  { code: 'BD', name: 'Bangladesh', flag: '🇧🇩' },
  { code: 'NP', name: 'Nepal', flag: '🇳🇵' },
  { code: 'LK', name: 'Sri Lanka', flag: '🇱🇰' },
  { code: 'MM', name: 'Myanmar', flag: '🇲🇲' },
  { code: 'KH', name: 'Cambodia', flag: '🇰🇭' },
  { code: 'LA', name: 'Laos', flag: '🇱🇦' },
  { code: 'MN', name: 'Mongolia', flag: '🇲🇳' },
  { code: 'MV', name: 'Maldives', flag: '🇲🇻' },
  { code: 'BT', name: 'Bhutan', flag: '🇧🇹' },
  
  // Middle East & Central Asia
  { code: 'TR', name: 'Turkey', flag: '🇹🇷' },
  { code: 'SA', name: 'Saudi Arabia', flag: '🇸🇦' },
  { code: 'AE', name: 'United Arab Emirates', flag: '🇦🇪' },
  { code: 'IL', name: 'Israel', flag: '🇮🇱' },
  { code: 'IR', name: 'Iran', flag: '🇮🇷' },
  { code: 'IQ', name: 'Iraq', flag: '🇮🇶' },
  { code: 'EG', name: 'Egypt', flag: '🇪🇬' },
  { code: 'JO', name: 'Jordan', flag: '🇯🇴' },
  { code: 'LB', name: 'Lebanon', flag: '🇱🇧' },
  { code: 'SY', name: 'Syria', flag: '🇸🇾' },
  { code: 'KW', name: 'Kuwait', flag: '🇰🇼' },
  { code: 'QA', name: 'Qatar', flag: '🇶🇦' },
  { code: 'BH', name: 'Bahrain', flag: '🇧🇭' },
  { code: 'OM', name: 'Oman', flag: '🇴🇲' },
  { code: 'YE', name: 'Yemen', flag: '🇾🇪' },
  { code: 'AF', name: 'Afghanistan', flag: '🇦🇫' },
  { code: 'AZ', name: 'Azerbaijan', flag: '🇦🇿' },
  { code: 'AM', name: 'Armenia', flag: '🇦🇲' },
  { code: 'GE', name: 'Georgia', flag: '🇬🇪' },
  { code: 'KZ', name: 'Kazakhstan', flag: '🇰🇿' },
  { code: 'UZ', name: 'Uzbekistan', flag: '🇺🇿' },
  { code: 'TM', name: 'Turkmenistan', flag: '🇹🇲' },
  { code: 'TJ', name: 'Tajikistan', flag: '🇹🇯' },
  { code: 'KG', name: 'Kyrgyzstan', flag: '🇰🇬' },
  
  // Africa
  { code: 'ZA', name: 'South Africa', flag: '🇿🇦' },
  { code: 'NG', name: 'Nigeria', flag: '🇳🇬' },
  { code: 'KE', name: 'Kenya', flag: '🇰🇪' },
  { code: 'ET', name: 'Ethiopia', flag: '🇪🇹' },
  { code: 'GH', name: 'Ghana', flag: '🇬🇭' },
  { code: 'TZ', name: 'Tanzania', flag: '🇹🇿' },
  { code: 'UG', name: 'Uganda', flag: '🇺🇬' },
  { code: 'MA', name: 'Morocco', flag: '🇲🇦' },
  { code: 'DZ', name: 'Algeria', flag: '🇩🇿' },
  { code: 'TN', name: 'Tunisia', flag: '🇹🇳' },
  { code: 'SN', name: 'Senegal', flag: '🇸🇳' },
  { code: 'CI', name: 'Ivory Coast', flag: '🇨🇮' },
  { code: 'CM', name: 'Cameroon', flag: '🇨🇲' },
  { code: 'AO', name: 'Angola', flag: '🇦🇴' },
  { code: 'MZ', name: 'Mozambique', flag: '🇲🇿' },
  { code: 'ZW', name: 'Zimbabwe', flag: '🇿🇼' },
  { code: 'RW', name: 'Rwanda', flag: '🇷🇼' },
  { code: 'BI', name: 'Burundi', flag: '🇧🇮' },
  { code: 'MW', name: 'Malawi', flag: '🇲🇼' },
  { code: 'MG', name: 'Madagascar', flag: '🇲🇬' },
  { code: 'SO', name: 'Somalia', flag: '🇸🇴' },
  { code: 'ER', name: 'Eritrea', flag: '🇪🇷' },
  { code: 'BW', name: 'Botswana', flag: '🇧🇼' },
  { code: 'NA', name: 'Namibia', flag: '🇳🇦' },
  { code: 'LS', name: 'Lesotho', flag: '🇱🇸' },
  { code: 'SZ', name: 'Eswatini', flag: '🇸🇿' },
  { code: 'CD', name: 'DR Congo', flag: '🇨🇩' },
  { code: 'CG', name: 'Congo', flag: '🇨🇬' },
  
  // Oceania
  { code: 'AU', name: 'Australia', flag: '🇦🇺' },
  { code: 'NZ', name: 'New Zealand', flag: '🇳🇿' },
  { code: 'FJ', name: 'Fiji', flag: '🇫🇯' },
  { code: 'PG', name: 'Papua New Guinea', flag: '🇵🇬' },
  { code: 'WS', name: 'Samoa', flag: '🇼🇸' },
  { code: 'TO', name: 'Tonga', flag: '🇹🇴' },
] as const;

export type LanguageCode = typeof LANGUAGES[number]['code'];
export type CountryCode = typeof COUNTRIES[number]['code'];

// Helper functions
export function getLanguageByCode(code: string) {
  return LANGUAGES.find(l => l.code === code);
}

export function getCountryByCode(code: string) {
  return COUNTRIES.find(c => c.code === code);
}

export function getLanguagesByCategory(category: LanguageCategory) {
  return LANGUAGES.filter(l => l.category === category);
}

export function getGroupedLanguages() {
  return Object.keys(LANGUAGE_CATEGORIES).map(category => ({
    category: category as LanguageCategory,
    categoryInfo: LANGUAGE_CATEGORIES[category as LanguageCategory],
    languages: getLanguagesByCategory(category as LanguageCategory),
  }));
}
