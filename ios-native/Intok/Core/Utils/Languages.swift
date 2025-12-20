import Foundation

// MARK: - Language Type
struct Language: Identifiable, Hashable {
    let id: String
    let code: String
    let name: String
    let native: String
    let category: LanguageCategory
    let region: String?
    
    init(code: String, name: String, native: String, category: LanguageCategory, region: String? = nil) {
        self.id = code
        self.code = code
        self.name = name
        self.native = native
        self.category = category
        self.region = region
    }
}

// MARK: - Language Category
enum LanguageCategory: String, CaseIterable, Identifiable {
    case majorWorld = "Major World Languages"
    case europeanRegional = "European Regional"
    case asian = "Asian Languages"
    case indianSubcontinent = "Indian Subcontinent"
    case middleEastern = "Middle Eastern"
    case african = "African Languages"
    case americasIndigenous = "Americas Indigenous"
    case classical = "Classical & Historical"
    
    var id: String { rawValue }
    
    var icon: String {
        switch self {
        case .majorWorld: return "🌍"
        case .europeanRegional: return "🇪🇺"
        case .asian: return "🌏"
        case .indianSubcontinent: return "🇮🇳"
        case .middleEastern: return "🕌"
        case .african: return "🌍"
        case .americasIndigenous: return "🌎"
        case .classical: return "📜"
        }
    }
}

// MARK: - Country Type
struct Country: Identifiable, Hashable {
    let id: String
    let code: String
    let name: String
    let flag: String
    
    init(code: String, name: String, flag: String) {
        self.id = code
        self.code = code
        self.name = name
        self.flag = flag
    }
}

// MARK: - Languages List (Organized by Category)
let LANGUAGES: [Language] = [
    // ========================================
    // MAJOR WORLD LANGUAGES
    // ========================================
    Language(code: "en", name: "English", native: "English", category: .majorWorld),
    Language(code: "es", name: "Spanish", native: "Español", category: .majorWorld),
    Language(code: "fr", name: "French", native: "Français", category: .majorWorld),
    Language(code: "de", name: "German", native: "Deutsch", category: .majorWorld),
    Language(code: "it", name: "Italian", native: "Italiano", category: .majorWorld),
    Language(code: "pt", name: "Portuguese", native: "Português", category: .majorWorld),
    Language(code: "ru", name: "Russian", native: "Русский", category: .majorWorld),
    Language(code: "zh", name: "Chinese (Mandarin)", native: "普通话", category: .majorWorld),
    Language(code: "ja", name: "Japanese", native: "日本語", category: .majorWorld),
    Language(code: "ko", name: "Korean", native: "한국어", category: .majorWorld),
    Language(code: "ar", name: "Arabic", native: "العربية", category: .majorWorld),
    Language(code: "nl", name: "Dutch", native: "Nederlands", category: .majorWorld),
    Language(code: "sv", name: "Swedish", native: "Svenska", category: .majorWorld),
    Language(code: "pl", name: "Polish", native: "Polski", category: .majorWorld),
    Language(code: "tr", name: "Turkish", native: "Türkçe", category: .majorWorld),
    Language(code: "uk", name: "Ukrainian", native: "Українська", category: .majorWorld),
    Language(code: "cs", name: "Czech", native: "Čeština", category: .majorWorld),
    Language(code: "el", name: "Greek", native: "Ελληνικά", category: .majorWorld),
    Language(code: "he", name: "Hebrew", native: "עברית", category: .majorWorld),
    Language(code: "ro", name: "Romanian", native: "Română", category: .majorWorld),
    Language(code: "hu", name: "Hungarian", native: "Magyar", category: .majorWorld),
    Language(code: "da", name: "Danish", native: "Dansk", category: .majorWorld),
    Language(code: "fi", name: "Finnish", native: "Suomi", category: .majorWorld),
    Language(code: "no", name: "Norwegian", native: "Norsk", category: .majorWorld),
    Language(code: "sk", name: "Slovak", native: "Slovenčina", category: .majorWorld),
    Language(code: "bg", name: "Bulgarian", native: "Български", category: .majorWorld),
    Language(code: "hr", name: "Croatian", native: "Hrvatski", category: .majorWorld),
    Language(code: "sr", name: "Serbian", native: "Српски", category: .majorWorld),
    Language(code: "sl", name: "Slovenian", native: "Slovenščina", category: .majorWorld),
    Language(code: "et", name: "Estonian", native: "Eesti", category: .majorWorld),
    Language(code: "lv", name: "Latvian", native: "Latviešu", category: .majorWorld),
    Language(code: "lt", name: "Lithuanian", native: "Lietuvių", category: .majorWorld),
    
    // ========================================
    // EUROPEAN REGIONAL LANGUAGES
    // ========================================
    // Spain
    Language(code: "ca", name: "Catalan", native: "Català", category: .europeanRegional, region: "Spain - Catalonia, Valencia, Balearic Islands"),
    Language(code: "gl", name: "Galician", native: "Galego", category: .europeanRegional, region: "Spain - Galicia"),
    Language(code: "eu", name: "Basque", native: "Euskara", category: .europeanRegional, region: "Spain/France - Basque Country"),
    Language(code: "oc", name: "Occitan", native: "Occitan", category: .europeanRegional, region: "France/Spain - Southern France, Val d'Aran"),
    Language(code: "ast", name: "Asturian", native: "Asturianu", category: .europeanRegional, region: "Spain - Asturias"),
    
    // Italy
    Language(code: "sc", name: "Sardinian", native: "Sardu", category: .europeanRegional, region: "Italy - Sardinia"),
    Language(code: "scn", name: "Sicilian", native: "Sicilianu", category: .europeanRegional, region: "Italy - Sicily"),
    Language(code: "nap", name: "Neapolitan", native: "Napulitano", category: .europeanRegional, region: "Italy - Naples, Campania"),
    Language(code: "fur", name: "Friulian", native: "Furlan", category: .europeanRegional, region: "Italy - Friuli"),
    
    // France
    Language(code: "br", name: "Breton", native: "Brezhoneg", category: .europeanRegional, region: "France - Brittany"),
    Language(code: "co", name: "Corsican", native: "Corsu", category: .europeanRegional, region: "France - Corsica"),
    Language(code: "gsw", name: "Alsatian", native: "Elsässisch", category: .europeanRegional, region: "France - Alsace"),
    
    // UK & Ireland
    Language(code: "cy", name: "Welsh", native: "Cymraeg", category: .europeanRegional, region: "United Kingdom - Wales"),
    Language(code: "gd", name: "Scottish Gaelic", native: "Gàidhlig", category: .europeanRegional, region: "United Kingdom - Scotland"),
    Language(code: "ga", name: "Irish", native: "Gaeilge", category: .europeanRegional, region: "Ireland"),
    Language(code: "kw", name: "Cornish", native: "Kernewek", category: .europeanRegional, region: "United Kingdom - Cornwall"),
    
    // Low Countries & Switzerland
    Language(code: "fy", name: "Frisian", native: "Frysk", category: .europeanRegional, region: "Netherlands - Friesland"),
    Language(code: "wa", name: "Walloon", native: "Walon", category: .europeanRegional, region: "Belgium - Wallonia"),
    Language(code: "li", name: "Limburgish", native: "Limburgs", category: .europeanRegional, region: "Belgium/Netherlands - Limburg"),
    Language(code: "rm", name: "Romansh", native: "Rumantsch", category: .europeanRegional, region: "Switzerland - Graubünden"),
    Language(code: "lb", name: "Luxembourgish", native: "Lëtzebuergesch", category: .europeanRegional, region: "Luxembourg"),
    
    // Germany & Austria
    Language(code: "nds", name: "Low German", native: "Plattdüütsch", category: .europeanRegional, region: "Germany - Northern Germany"),
    Language(code: "bar", name: "Bavarian", native: "Boarisch", category: .europeanRegional, region: "Germany/Austria - Bavaria, Austria"),
    Language(code: "hsb", name: "Upper Sorbian", native: "Hornjoserbšćina", category: .europeanRegional, region: "Germany - Saxony"),
    
    // Nordic
    Language(code: "is", name: "Icelandic", native: "Íslenska", category: .europeanRegional, region: "Iceland"),
    Language(code: "fo", name: "Faroese", native: "Føroyskt", category: .europeanRegional, region: "Faroe Islands"),
    
    // Eastern Europe
    Language(code: "be", name: "Belarusian", native: "Беларуская", category: .europeanRegional, region: "Belarus"),
    Language(code: "mk", name: "Macedonian", native: "Македонски", category: .europeanRegional, region: "North Macedonia"),
    Language(code: "sq", name: "Albanian", native: "Shqip", category: .europeanRegional, region: "Albania, Kosovo"),
    Language(code: "bs", name: "Bosnian", native: "Bosanski", category: .europeanRegional, region: "Bosnia and Herzegovina"),
    Language(code: "mt", name: "Maltese", native: "Malti", category: .europeanRegional, region: "Malta"),
    
    // ========================================
    // ASIAN LANGUAGES
    // ========================================
    Language(code: "yue", name: "Cantonese", native: "粵語", category: .asian, region: "China - Hong Kong, Guangdong"),
    Language(code: "bo", name: "Tibetan", native: "བོད་སྐད", category: .asian, region: "China - Tibet"),
    Language(code: "ug", name: "Uyghur", native: "ئۇيغۇرچە", category: .asian, region: "China - Xinjiang"),
    Language(code: "mn", name: "Mongolian", native: "Монгол", category: .asian, region: "Mongolia"),
    Language(code: "vi", name: "Vietnamese", native: "Tiếng Việt", category: .asian),
    Language(code: "th", name: "Thai", native: "ไทย", category: .asian),
    Language(code: "id", name: "Indonesian", native: "Bahasa Indonesia", category: .asian),
    Language(code: "ms", name: "Malay", native: "Bahasa Melayu", category: .asian),
    Language(code: "tl", name: "Filipino", native: "Filipino", category: .asian),
    Language(code: "km", name: "Khmer", native: "ភាសាខ្មែរ", category: .asian, region: "Cambodia"),
    Language(code: "lo", name: "Lao", native: "ລາວ", category: .asian, region: "Laos"),
    Language(code: "my", name: "Burmese", native: "မြန်မာဘာသာ", category: .asian, region: "Myanmar"),
    Language(code: "jv", name: "Javanese", native: "Basa Jawa", category: .asian, region: "Indonesia - Java"),
    Language(code: "su", name: "Sundanese", native: "Basa Sunda", category: .asian, region: "Indonesia - West Java"),
    Language(code: "ceb", name: "Cebuano", native: "Cebuano", category: .asian, region: "Philippines - Visayas"),
    Language(code: "ilo", name: "Ilocano", native: "Ilokano", category: .asian, region: "Philippines - Northern Luzon"),
    
    // ========================================
    // INDIAN SUBCONTINENT LANGUAGES
    // ========================================
    Language(code: "hi", name: "Hindi", native: "हिन्दी", category: .indianSubcontinent),
    Language(code: "bn", name: "Bengali", native: "বাংলা", category: .indianSubcontinent),
    Language(code: "pa", name: "Punjabi", native: "ਪੰਜਾਬੀ", category: .indianSubcontinent),
    Language(code: "ta", name: "Tamil", native: "தமிழ்", category: .indianSubcontinent),
    Language(code: "te", name: "Telugu", native: "తెలుగు", category: .indianSubcontinent),
    Language(code: "mr", name: "Marathi", native: "मराठी", category: .indianSubcontinent),
    Language(code: "gu", name: "Gujarati", native: "ગુજરાતી", category: .indianSubcontinent),
    Language(code: "kn", name: "Kannada", native: "ಕನ್ನಡ", category: .indianSubcontinent),
    Language(code: "ml", name: "Malayalam", native: "മലയാളം", category: .indianSubcontinent),
    Language(code: "or", name: "Odia", native: "ଓଡ଼ିଆ", category: .indianSubcontinent, region: "India - Odisha"),
    Language(code: "as", name: "Assamese", native: "অসমীয়া", category: .indianSubcontinent, region: "India - Assam"),
    Language(code: "ne", name: "Nepali", native: "नेपाली", category: .indianSubcontinent, region: "Nepal"),
    Language(code: "si", name: "Sinhala", native: "සිංහල", category: .indianSubcontinent, region: "Sri Lanka"),
    Language(code: "ur", name: "Urdu", native: "اردو", category: .indianSubcontinent),
    Language(code: "sd", name: "Sindhi", native: "سنڌي", category: .indianSubcontinent, region: "Pakistan - Sindh"),
    Language(code: "ks", name: "Kashmiri", native: "कॉशुर", category: .indianSubcontinent, region: "India/Pakistan - Kashmir"),
    Language(code: "doi", name: "Dogri", native: "डोगरी", category: .indianSubcontinent, region: "India - Jammu"),
    Language(code: "mai", name: "Maithili", native: "मैथिली", category: .indianSubcontinent, region: "India - Bihar"),
    Language(code: "sat", name: "Santali", native: "ᱥᱟᱱᱛᱟᱲᱤ", category: .indianSubcontinent, region: "India - Jharkhand"),
    Language(code: "kok", name: "Konkani", native: "कोंकणी", category: .indianSubcontinent, region: "India - Goa"),
    Language(code: "mni", name: "Manipuri", native: "মৈতৈলোন্", category: .indianSubcontinent, region: "India - Manipur"),
    Language(code: "dv", name: "Dhivehi", native: "ދިވެހި", category: .indianSubcontinent, region: "Maldives"),
    
    // ========================================
    // MIDDLE EASTERN LANGUAGES
    // ========================================
    Language(code: "fa", name: "Persian", native: "فارسی", category: .middleEastern),
    Language(code: "ku", name: "Kurdish", native: "Kurdî", category: .middleEastern, region: "Kurdistan Region"),
    Language(code: "ps", name: "Pashto", native: "پښتو", category: .middleEastern, region: "Afghanistan, Pakistan"),
    Language(code: "az", name: "Azerbaijani", native: "Azərbaycan", category: .middleEastern),
    Language(code: "hy", name: "Armenian", native: "Հayskylerен", category: .middleEastern),
    Language(code: "ka", name: "Georgian", native: "ქართული", category: .middleEastern),
    Language(code: "uz", name: "Uzbek", native: "Oʻzbek", category: .middleEastern),
    Language(code: "kk", name: "Kazakh", native: "Қазақ", category: .middleEastern),
    Language(code: "tg", name: "Tajik", native: "Тоҷикӣ", category: .middleEastern),
    Language(code: "tk", name: "Turkmen", native: "Türkmen", category: .middleEastern),
    Language(code: "ky", name: "Kyrgyz", native: "Кыргыз", category: .middleEastern),
    
    // ========================================
    // AFRICAN LANGUAGES
    // ========================================
    Language(code: "sw", name: "Swahili", native: "Kiswahili", category: .african),
    Language(code: "af", name: "Afrikaans", native: "Afrikaans", category: .african),
    Language(code: "am", name: "Amharic", native: "አማርኛ", category: .african, region: "Ethiopia"),
    Language(code: "ha", name: "Hausa", native: "Hausa", category: .african, region: "Nigeria, Niger"),
    Language(code: "yo", name: "Yoruba", native: "Yorùbá", category: .african, region: "Nigeria"),
    Language(code: "ig", name: "Igbo", native: "Igbo", category: .african, region: "Nigeria"),
    Language(code: "zu", name: "Zulu", native: "isiZulu", category: .african, region: "South Africa"),
    Language(code: "xh", name: "Xhosa", native: "isiXhosa", category: .african, region: "South Africa"),
    Language(code: "so", name: "Somali", native: "Soomaali", category: .african, region: "Somalia"),
    Language(code: "rw", name: "Kinyarwanda", native: "Ikinyarwanda", category: .african, region: "Rwanda"),
    Language(code: "rn", name: "Kirundi", native: "Ikirundi", category: .african, region: "Burundi"),
    Language(code: "sn", name: "Shona", native: "chiShona", category: .african, region: "Zimbabwe"),
    Language(code: "ny", name: "Chichewa", native: "Chichewa", category: .african, region: "Malawi"),
    Language(code: "mg", name: "Malagasy", native: "Malagasy", category: .african, region: "Madagascar"),
    Language(code: "ti", name: "Tigrinya", native: "ትግርኛ", category: .african, region: "Eritrea, Ethiopia"),
    Language(code: "om", name: "Oromo", native: "Afaan Oromoo", category: .african, region: "Ethiopia"),
    Language(code: "wo", name: "Wolof", native: "Wolof", category: .african, region: "Senegal"),
    Language(code: "ff", name: "Fulah", native: "Fulfulde", category: .african, region: "West Africa"),
    Language(code: "ln", name: "Lingala", native: "Lingála", category: .african, region: "Congo"),
    Language(code: "kg", name: "Kongo", native: "Kikongo", category: .african, region: "Congo"),
    Language(code: "st", name: "Sesotho", native: "Sesotho", category: .african, region: "Lesotho, South Africa"),
    Language(code: "tn", name: "Setswana", native: "Setswana", category: .african, region: "Botswana, South Africa"),
    
    // ========================================
    // AMERICAS INDIGENOUS LANGUAGES
    // ========================================
    Language(code: "qu", name: "Quechua", native: "Runasimi", category: .americasIndigenous, region: "Peru, Bolivia, Ecuador"),
    Language(code: "gn", name: "Guaraní", native: "Avañe'ẽ", category: .americasIndigenous, region: "Paraguay"),
    Language(code: "ay", name: "Aymara", native: "Aymar aru", category: .americasIndigenous, region: "Bolivia, Peru"),
    Language(code: "nah", name: "Nahuatl", native: "Nāhuatl", category: .americasIndigenous, region: "Mexico"),
    Language(code: "yua", name: "Yucatec Maya", native: "Màaya t'àan", category: .americasIndigenous, region: "Mexico - Yucatán"),
    Language(code: "oj", name: "Ojibwe", native: "Anishinaabemowin", category: .americasIndigenous, region: "USA/Canada - Great Lakes"),
    Language(code: "cr", name: "Cree", native: "ᓀᐦᐃᔭᐍᐏᐣ", category: .americasIndigenous, region: "Canada"),
    Language(code: "iu", name: "Inuktitut", native: "ᐃᓄᒃᑎᑐᑦ", category: .americasIndigenous, region: "Canada - Nunavut"),
    Language(code: "nv", name: "Navajo", native: "Diné bizaad", category: .americasIndigenous, region: "USA - Southwest"),
    Language(code: "chr", name: "Cherokee", native: "ᏣᎳᎩ", category: .americasIndigenous, region: "USA - Oklahoma"),
    Language(code: "ht", name: "Haitian Creole", native: "Kreyòl ayisyen", category: .americasIndigenous, region: "Haiti"),
    Language(code: "srn", name: "Sranan Tongo", native: "Sranan", category: .americasIndigenous, region: "Suriname"),
    
    // ========================================
    // CLASSICAL & HISTORICAL LANGUAGES
    // ========================================
    Language(code: "la", name: "Latin", native: "Latina", category: .classical, region: "Vatican, Scholarly"),
    Language(code: "sa", name: "Sanskrit", native: "संस्कृतम्", category: .classical, region: "Hindu Rituals, Scholarly"),
    Language(code: "grc", name: "Ancient Greek", native: "Ἑλληνική", category: .classical, region: "Scholarly, Theological"),
    Language(code: "cu", name: "Church Slavonic", native: "Словѣ́ньскъ", category: .classical, region: "Orthodox Liturgy"),
    Language(code: "pi", name: "Pali", native: "पालि", category: .classical, region: "Buddhist Texts"),
    Language(code: "cop", name: "Coptic", native: "Ⲙⲉⲧⲣⲉⲙⲛ̀ⲭⲏⲙⲓ", category: .classical, region: "Coptic Christian Liturgy"),
    Language(code: "syr", name: "Syriac", native: "ܠܫܢܐ ܣܘܪܝܝܐ", category: .classical, region: "Syriac Christian Liturgy"),
]

// MARK: - Countries List
let COUNTRIES: [Country] = [
    // Americas
    Country(code: "US", name: "United States", flag: "🇺🇸"),
    Country(code: "CA", name: "Canada", flag: "🇨🇦"),
    Country(code: "MX", name: "Mexico", flag: "🇲🇽"),
    Country(code: "BR", name: "Brazil", flag: "🇧🇷"),
    Country(code: "AR", name: "Argentina", flag: "🇦🇷"),
    Country(code: "CO", name: "Colombia", flag: "🇨🇴"),
    Country(code: "PE", name: "Peru", flag: "🇵🇪"),
    Country(code: "CL", name: "Chile", flag: "🇨🇱"),
    Country(code: "VE", name: "Venezuela", flag: "🇻🇪"),
    Country(code: "EC", name: "Ecuador", flag: "🇪🇨"),
    Country(code: "BO", name: "Bolivia", flag: "🇧🇴"),
    Country(code: "PY", name: "Paraguay", flag: "🇵🇾"),
    Country(code: "UY", name: "Uruguay", flag: "🇺🇾"),
    Country(code: "CR", name: "Costa Rica", flag: "🇨🇷"),
    Country(code: "PA", name: "Panama", flag: "🇵🇦"),
    Country(code: "GT", name: "Guatemala", flag: "🇬🇹"),
    Country(code: "HN", name: "Honduras", flag: "🇭🇳"),
    Country(code: "SV", name: "El Salvador", flag: "🇸🇻"),
    Country(code: "NI", name: "Nicaragua", flag: "🇳🇮"),
    Country(code: "CU", name: "Cuba", flag: "🇨🇺"),
    Country(code: "DO", name: "Dominican Republic", flag: "🇩🇴"),
    Country(code: "PR", name: "Puerto Rico", flag: "🇵🇷"),
    Country(code: "JM", name: "Jamaica", flag: "🇯🇲"),
    Country(code: "HT", name: "Haiti", flag: "🇭🇹"),
    Country(code: "TT", name: "Trinidad and Tobago", flag: "🇹🇹"),
    Country(code: "SR", name: "Suriname", flag: "🇸🇷"),
    
    // Europe
    Country(code: "GB", name: "United Kingdom", flag: "🇬🇧"),
    Country(code: "FR", name: "France", flag: "🇫🇷"),
    Country(code: "DE", name: "Germany", flag: "🇩🇪"),
    Country(code: "IT", name: "Italy", flag: "🇮🇹"),
    Country(code: "ES", name: "Spain", flag: "🇪🇸"),
    Country(code: "PT", name: "Portugal", flag: "🇵🇹"),
    Country(code: "NL", name: "Netherlands", flag: "🇳🇱"),
    Country(code: "BE", name: "Belgium", flag: "🇧🇪"),
    Country(code: "CH", name: "Switzerland", flag: "🇨🇭"),
    Country(code: "AT", name: "Austria", flag: "🇦🇹"),
    Country(code: "SE", name: "Sweden", flag: "🇸🇪"),
    Country(code: "NO", name: "Norway", flag: "🇳🇴"),
    Country(code: "DK", name: "Denmark", flag: "🇩🇰"),
    Country(code: "FI", name: "Finland", flag: "🇫🇮"),
    Country(code: "IE", name: "Ireland", flag: "🇮🇪"),
    Country(code: "PL", name: "Poland", flag: "🇵🇱"),
    Country(code: "CZ", name: "Czech Republic", flag: "🇨🇿"),
    Country(code: "SK", name: "Slovakia", flag: "🇸🇰"),
    Country(code: "HU", name: "Hungary", flag: "🇭🇺"),
    Country(code: "RO", name: "Romania", flag: "🇷🇴"),
    Country(code: "BG", name: "Bulgaria", flag: "🇧🇬"),
    Country(code: "GR", name: "Greece", flag: "🇬🇷"),
    Country(code: "UA", name: "Ukraine", flag: "🇺🇦"),
    Country(code: "RU", name: "Russia", flag: "🇷🇺"),
    Country(code: "HR", name: "Croatia", flag: "🇭🇷"),
    Country(code: "RS", name: "Serbia", flag: "🇷🇸"),
    Country(code: "SI", name: "Slovenia", flag: "🇸🇮"),
    Country(code: "EE", name: "Estonia", flag: "🇪🇪"),
    Country(code: "LV", name: "Latvia", flag: "🇱🇻"),
    Country(code: "LT", name: "Lithuania", flag: "🇱🇹"),
    Country(code: "IS", name: "Iceland", flag: "🇮🇸"),
    Country(code: "LU", name: "Luxembourg", flag: "🇱🇺"),
    Country(code: "MT", name: "Malta", flag: "🇲🇹"),
    Country(code: "AL", name: "Albania", flag: "🇦🇱"),
    Country(code: "MK", name: "North Macedonia", flag: "🇲🇰"),
    Country(code: "BA", name: "Bosnia and Herzegovina", flag: "🇧🇦"),
    Country(code: "ME", name: "Montenegro", flag: "🇲🇪"),
    Country(code: "XK", name: "Kosovo", flag: "🇽🇰"),
    Country(code: "BY", name: "Belarus", flag: "🇧🇾"),
    Country(code: "MD", name: "Moldova", flag: "🇲🇩"),
    
    // Asia
    Country(code: "CN", name: "China", flag: "🇨🇳"),
    Country(code: "JP", name: "Japan", flag: "🇯🇵"),
    Country(code: "KR", name: "South Korea", flag: "🇰🇷"),
    Country(code: "IN", name: "India", flag: "🇮🇳"),
    Country(code: "ID", name: "Indonesia", flag: "🇮🇩"),
    Country(code: "TH", name: "Thailand", flag: "🇹🇭"),
    Country(code: "VN", name: "Vietnam", flag: "🇻🇳"),
    Country(code: "MY", name: "Malaysia", flag: "🇲🇾"),
    Country(code: "SG", name: "Singapore", flag: "🇸🇬"),
    Country(code: "PH", name: "Philippines", flag: "🇵🇭"),
    Country(code: "TW", name: "Taiwan", flag: "🇹🇼"),
    Country(code: "HK", name: "Hong Kong", flag: "🇭🇰"),
    Country(code: "PK", name: "Pakistan", flag: "🇵🇰"),
    Country(code: "BD", name: "Bangladesh", flag: "🇧🇩"),
    Country(code: "NP", name: "Nepal", flag: "🇳🇵"),
    Country(code: "LK", name: "Sri Lanka", flag: "🇱🇰"),
    Country(code: "MM", name: "Myanmar", flag: "🇲🇲"),
    Country(code: "KH", name: "Cambodia", flag: "🇰🇭"),
    Country(code: "LA", name: "Laos", flag: "🇱🇦"),
    Country(code: "MN", name: "Mongolia", flag: "🇲🇳"),
    Country(code: "MV", name: "Maldives", flag: "🇲🇻"),
    Country(code: "BT", name: "Bhutan", flag: "🇧🇹"),
    
    // Middle East & Central Asia
    Country(code: "TR", name: "Turkey", flag: "🇹🇷"),
    Country(code: "SA", name: "Saudi Arabia", flag: "🇸🇦"),
    Country(code: "AE", name: "United Arab Emirates", flag: "🇦🇪"),
    Country(code: "IL", name: "Israel", flag: "🇮🇱"),
    Country(code: "IR", name: "Iran", flag: "🇮🇷"),
    Country(code: "IQ", name: "Iraq", flag: "🇮🇶"),
    Country(code: "EG", name: "Egypt", flag: "🇪🇬"),
    Country(code: "JO", name: "Jordan", flag: "🇯🇴"),
    Country(code: "LB", name: "Lebanon", flag: "🇱🇧"),
    Country(code: "SY", name: "Syria", flag: "🇸🇾"),
    Country(code: "KW", name: "Kuwait", flag: "🇰🇼"),
    Country(code: "QA", name: "Qatar", flag: "🇶🇦"),
    Country(code: "BH", name: "Bahrain", flag: "🇧🇭"),
    Country(code: "OM", name: "Oman", flag: "🇴🇲"),
    Country(code: "YE", name: "Yemen", flag: "🇾🇪"),
    Country(code: "AF", name: "Afghanistan", flag: "🇦🇫"),
    Country(code: "AZ", name: "Azerbaijan", flag: "🇦🇿"),
    Country(code: "AM", name: "Armenia", flag: "🇦🇲"),
    Country(code: "GE", name: "Georgia", flag: "🇬🇪"),
    Country(code: "KZ", name: "Kazakhstan", flag: "🇰🇿"),
    Country(code: "UZ", name: "Uzbekistan", flag: "🇺🇿"),
    Country(code: "TM", name: "Turkmenistan", flag: "🇹🇲"),
    Country(code: "TJ", name: "Tajikistan", flag: "🇹🇯"),
    Country(code: "KG", name: "Kyrgyzstan", flag: "🇰🇬"),
    
    // Africa
    Country(code: "ZA", name: "South Africa", flag: "🇿🇦"),
    Country(code: "NG", name: "Nigeria", flag: "🇳🇬"),
    Country(code: "KE", name: "Kenya", flag: "🇰🇪"),
    Country(code: "ET", name: "Ethiopia", flag: "🇪🇹"),
    Country(code: "GH", name: "Ghana", flag: "🇬🇭"),
    Country(code: "TZ", name: "Tanzania", flag: "🇹🇿"),
    Country(code: "UG", name: "Uganda", flag: "🇺🇬"),
    Country(code: "MA", name: "Morocco", flag: "🇲🇦"),
    Country(code: "DZ", name: "Algeria", flag: "🇩🇿"),
    Country(code: "TN", name: "Tunisia", flag: "🇹🇳"),
    Country(code: "SN", name: "Senegal", flag: "🇸🇳"),
    Country(code: "CI", name: "Ivory Coast", flag: "🇨🇮"),
    Country(code: "CM", name: "Cameroon", flag: "🇨🇲"),
    Country(code: "AO", name: "Angola", flag: "🇦🇴"),
    Country(code: "MZ", name: "Mozambique", flag: "🇲🇿"),
    Country(code: "ZW", name: "Zimbabwe", flag: "🇿🇼"),
    Country(code: "RW", name: "Rwanda", flag: "🇷🇼"),
    Country(code: "BI", name: "Burundi", flag: "🇧🇮"),
    Country(code: "MW", name: "Malawi", flag: "🇲🇼"),
    Country(code: "MG", name: "Madagascar", flag: "🇲🇬"),
    Country(code: "SO", name: "Somalia", flag: "🇸🇴"),
    Country(code: "ER", name: "Eritrea", flag: "🇪🇷"),
    Country(code: "BW", name: "Botswana", flag: "🇧🇼"),
    Country(code: "NA", name: "Namibia", flag: "🇳🇦"),
    Country(code: "LS", name: "Lesotho", flag: "🇱🇸"),
    Country(code: "SZ", name: "Eswatini", flag: "🇸🇿"),
    Country(code: "CD", name: "DR Congo", flag: "🇨🇩"),
    Country(code: "CG", name: "Congo", flag: "🇨🇬"),
    
    // Oceania
    Country(code: "AU", name: "Australia", flag: "🇦🇺"),
    Country(code: "NZ", name: "New Zealand", flag: "🇳🇿"),
    Country(code: "FJ", name: "Fiji", flag: "🇫🇯"),
    Country(code: "PG", name: "Papua New Guinea", flag: "🇵🇬"),
    Country(code: "WS", name: "Samoa", flag: "🇼🇸"),
    Country(code: "TO", name: "Tonga", flag: "🇹🇴"),
]

// MARK: - Region Type
struct Region: Identifiable, Hashable {
    let id: String
    let code: String
    let name: String
    
    init(code: String, name: String) {
        self.id = code
        self.code = code
        self.name = name
    }
}

// MARK: - Country Regions (Only for countries with significant regional variations)
let COUNTRY_REGIONS: [String: [Region]] = [
    // United States
    "US": [
        Region(code: "northeast", name: "Northeast (New York, Boston, etc.)"),
        Region(code: "southeast", name: "Southeast (Atlanta, Miami, etc.)"),
        Region(code: "midwest", name: "Midwest (Chicago, Detroit, etc.)"),
        Region(code: "southwest", name: "Southwest (Texas, Arizona, etc.)"),
        Region(code: "west_coast", name: "West Coast (California, Oregon, etc.)"),
        Region(code: "pacific_northwest", name: "Pacific Northwest (Seattle, Portland)"),
        Region(code: "mountain", name: "Mountain (Colorado, Utah, etc.)"),
        Region(code: "alaska", name: "Alaska"),
        Region(code: "hawaii", name: "Hawaii"),
    ],
    
    // United Kingdom
    "GB": [
        Region(code: "england", name: "England"),
        Region(code: "scotland", name: "Scotland"),
        Region(code: "wales", name: "Wales"),
        Region(code: "northern_ireland", name: "Northern Ireland"),
    ],
    
    // Spain
    "ES": [
        Region(code: "catalonia", name: "Catalonia (Barcelona)"),
        Region(code: "basque_country", name: "Basque Country (Bilbao)"),
        Region(code: "galicia", name: "Galicia (Santiago)"),
        Region(code: "andalusia", name: "Andalusia (Seville, Málaga)"),
        Region(code: "castile", name: "Castile (Madrid, Toledo)"),
        Region(code: "valencia", name: "Valencia"),
        Region(code: "aragon", name: "Aragon (Zaragoza)"),
        Region(code: "asturias", name: "Asturias"),
        Region(code: "canary_islands", name: "Canary Islands"),
        Region(code: "balearic_islands", name: "Balearic Islands"),
    ],
    
    // Italy
    "IT": [
        Region(code: "northern_italy", name: "Northern Italy (Milan, Turin)"),
        Region(code: "central_italy", name: "Central Italy (Rome, Florence)"),
        Region(code: "southern_italy", name: "Southern Italy (Naples)"),
        Region(code: "sicily", name: "Sicily"),
        Region(code: "sardinia", name: "Sardinia"),
    ],
    
    // Germany
    "DE": [
        Region(code: "bavaria", name: "Bavaria (Munich)"),
        Region(code: "northern_germany", name: "Northern Germany (Hamburg, Bremen)"),
        Region(code: "berlin", name: "Berlin"),
        Region(code: "saxony", name: "Saxony (Dresden, Leipzig)"),
        Region(code: "rhineland", name: "Rhineland (Cologne, Düsseldorf)"),
        Region(code: "baden_wurttemberg", name: "Baden-Württemberg (Stuttgart)"),
    ],
    
    // France
    "FR": [
        Region(code: "paris_region", name: "Paris Region (Île-de-France)"),
        Region(code: "northern_france", name: "Northern France"),
        Region(code: "southern_france", name: "Southern France (Occitanie)"),
        Region(code: "brittany", name: "Brittany"),
        Region(code: "alsace", name: "Alsace"),
        Region(code: "provence", name: "Provence (Marseille, Nice)"),
        Region(code: "normandy", name: "Normandy"),
        Region(code: "corsica", name: "Corsica"),
    ],
    
    // China
    "CN": [
        Region(code: "northern_china", name: "Northern China (Beijing)"),
        Region(code: "southern_china", name: "Southern China (Guangdong)"),
        Region(code: "eastern_china", name: "Eastern China (Shanghai)"),
        Region(code: "western_china", name: "Western China (Sichuan)"),
        Region(code: "northeastern_china", name: "Northeastern China"),
    ],
    
    // India
    "IN": [
        Region(code: "north_india", name: "North India (Delhi, UP)"),
        Region(code: "south_india", name: "South India (Chennai, Bangalore)"),
        Region(code: "west_india", name: "West India (Mumbai, Gujarat)"),
        Region(code: "east_india", name: "East India (Kolkata, Bengal)"),
        Region(code: "central_india", name: "Central India"),
        Region(code: "northeast_india", name: "Northeast India"),
    ],
    
    // Brazil
    "BR": [
        Region(code: "southeast_brazil", name: "Southeast (São Paulo, Rio)"),
        Region(code: "south_brazil", name: "South (Porto Alegre)"),
        Region(code: "northeast_brazil", name: "Northeast (Salvador, Recife)"),
        Region(code: "north_brazil", name: "North (Amazon)"),
        Region(code: "central_west_brazil", name: "Central-West (Brasília)"),
    ],
    
    // Canada
    "CA": [
        Region(code: "quebec", name: "Quebec (French-speaking)"),
        Region(code: "ontario", name: "Ontario (Toronto)"),
        Region(code: "british_columbia", name: "British Columbia (Vancouver)"),
        Region(code: "alberta", name: "Alberta (Calgary, Edmonton)"),
        Region(code: "atlantic_canada", name: "Atlantic Canada"),
        Region(code: "prairies", name: "Prairies (Manitoba, Saskatchewan)"),
    ],
    
    // Mexico
    "MX": [
        Region(code: "northern_mexico", name: "Northern Mexico (Monterrey)"),
        Region(code: "central_mexico", name: "Central Mexico (Mexico City)"),
        Region(code: "southern_mexico", name: "Southern Mexico (Oaxaca)"),
        Region(code: "yucatan", name: "Yucatán Peninsula"),
        Region(code: "baja_california", name: "Baja California"),
    ],
    
    // Australia
    "AU": [
        Region(code: "new_south_wales", name: "New South Wales (Sydney)"),
        Region(code: "victoria", name: "Victoria (Melbourne)"),
        Region(code: "queensland", name: "Queensland (Brisbane)"),
        Region(code: "western_australia", name: "Western Australia (Perth)"),
        Region(code: "south_australia", name: "South Australia (Adelaide)"),
    ],
    
    // Russia
    "RU": [
        Region(code: "european_russia", name: "European Russia (Moscow, St. Petersburg)"),
        Region(code: "siberia", name: "Siberia"),
        Region(code: "far_east", name: "Far East (Vladivostok)"),
        Region(code: "ural", name: "Ural Region"),
        Region(code: "south_russia", name: "Southern Russia"),
    ],
    
    // Switzerland
    "CH": [
        Region(code: "german_switzerland", name: "German-speaking Switzerland"),
        Region(code: "french_switzerland", name: "French-speaking Switzerland (Romandie)"),
        Region(code: "italian_switzerland", name: "Italian-speaking Switzerland (Ticino)"),
    ],
    
    // Belgium
    "BE": [
        Region(code: "flanders", name: "Flanders (Dutch-speaking)"),
        Region(code: "wallonia", name: "Wallonia (French-speaking)"),
        Region(code: "brussels", name: "Brussels"),
    ],
    
    // Argentina
    "AR": [
        Region(code: "buenos_aires", name: "Buenos Aires"),
        Region(code: "patagonia", name: "Patagonia"),
        Region(code: "north_argentina", name: "Northern Argentina"),
        Region(code: "cuyo", name: "Cuyo (Mendoza)"),
    ],
    
    // Colombia
    "CO": [
        Region(code: "bogota_region", name: "Bogotá Region"),
        Region(code: "caribbean_coast", name: "Caribbean Coast"),
        Region(code: "pacific_coast", name: "Pacific Coast"),
        Region(code: "coffee_region", name: "Coffee Region (Eje Cafetero)"),
    ],
    
    // Japan
    "JP": [
        Region(code: "kanto", name: "Kanto (Tokyo)"),
        Region(code: "kansai", name: "Kansai (Osaka, Kyoto)"),
        Region(code: "hokkaido", name: "Hokkaido"),
        Region(code: "kyushu", name: "Kyushu"),
        Region(code: "tohoku", name: "Tohoku"),
    ],
    
    // Indonesia
    "ID": [
        Region(code: "java", name: "Java (Jakarta)"),
        Region(code: "bali", name: "Bali"),
        Region(code: "sumatra", name: "Sumatra"),
        Region(code: "kalimantan", name: "Kalimantan (Borneo)"),
        Region(code: "sulawesi", name: "Sulawesi"),
    ],
    
    // Philippines
    "PH": [
        Region(code: "luzon", name: "Luzon (Manila)"),
        Region(code: "visayas", name: "Visayas (Cebu)"),
        Region(code: "mindanao", name: "Mindanao (Davao)"),
    ],
    
    // South Africa
    "ZA": [
        Region(code: "gauteng", name: "Gauteng (Johannesburg, Pretoria)"),
        Region(code: "western_cape", name: "Western Cape (Cape Town)"),
        Region(code: "kwazulu_natal", name: "KwaZulu-Natal (Durban)"),
        Region(code: "eastern_cape", name: "Eastern Cape"),
    ],
    
    // Nigeria
    "NG": [
        Region(code: "southwest_nigeria", name: "Southwest (Lagos, Yorubaland)"),
        Region(code: "southeast_nigeria", name: "Southeast (Igboland)"),
        Region(code: "north_nigeria", name: "Northern Nigeria"),
        Region(code: "south_south_nigeria", name: "South-South (Niger Delta)"),
    ],
]

// MARK: - Helper Functions
func getLanguageByCode(_ code: String) -> Language? {
    return LANGUAGES.first { $0.code == code }
}

func getCountryByCode(_ code: String) -> Country? {
    return COUNTRIES.first { $0.code == code }
}

func getLanguagesByCategory(_ category: LanguageCategory) -> [Language] {
    return LANGUAGES.filter { $0.category == category }
}

func getAllLanguageCategories() -> [LanguageCategory] {
    return LanguageCategory.allCases
}

// MARK: - Grouped Languages for UI
func getGroupedLanguages() -> [(category: LanguageCategory, languages: [Language])] {
    return LanguageCategory.allCases.map { category in
        (category: category, languages: getLanguagesByCategory(category))
    }
}

// MARK: - Region Helper Functions
func getRegionsForCountry(_ countryCode: String) -> [Region] {
    return COUNTRY_REGIONS[countryCode] ?? []
}

func hasRegions(_ countryCode: String) -> Bool {
    return COUNTRY_REGIONS[countryCode] != nil
}

func getRegionByCode(_ countryCode: String, regionCode: String) -> Region? {
    return COUNTRY_REGIONS[countryCode]?.first { $0.code == regionCode }
}
