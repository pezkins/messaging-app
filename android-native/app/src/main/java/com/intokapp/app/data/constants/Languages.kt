package com.intokapp.app.data.constants

// MARK: - Language Category Enum
enum class LanguageCategory(val displayName: String, val icon: String) {
    MAJOR_WORLD("Major World Languages", "🌍"),
    EUROPEAN_REGIONAL("European Regional", "🇪🇺"),
    ASIAN("Asian Languages", "🌏"),
    INDIAN_SUBCONTINENT("Indian Subcontinent", "🇮🇳"),
    MIDDLE_EASTERN("Middle Eastern", "🕌"),
    AFRICAN("African Languages", "🌍"),
    AMERICAS_INDIGENOUS("Americas Indigenous", "🌎"),
    CLASSICAL("Classical & Historical", "📜")
}

// MARK: - Language Data Class
data class Language(
    val code: String,
    val name: String,
    val native: String,
    val category: LanguageCategory,
    val region: String? = null
)

// MARK: - Country Data Class
data class Country(
    val code: String,
    val name: String,
    val flag: String
)

// MARK: - Languages List (Organized by Category)
val LANGUAGES = listOf(
    // ========================================
    // MAJOR WORLD LANGUAGES
    // ========================================
    Language("en", "English", "English", LanguageCategory.MAJOR_WORLD),
    Language("es", "Spanish", "Español", LanguageCategory.MAJOR_WORLD),
    Language("fr", "French", "Français", LanguageCategory.MAJOR_WORLD),
    Language("de", "German", "Deutsch", LanguageCategory.MAJOR_WORLD),
    Language("it", "Italian", "Italiano", LanguageCategory.MAJOR_WORLD),
    Language("pt", "Portuguese", "Português", LanguageCategory.MAJOR_WORLD),
    Language("ru", "Russian", "Русский", LanguageCategory.MAJOR_WORLD),
    Language("zh", "Chinese (Mandarin)", "普通话", LanguageCategory.MAJOR_WORLD),
    Language("ja", "Japanese", "日本語", LanguageCategory.MAJOR_WORLD),
    Language("ko", "Korean", "한국어", LanguageCategory.MAJOR_WORLD),
    Language("ar", "Arabic", "العربية", LanguageCategory.MAJOR_WORLD),
    Language("nl", "Dutch", "Nederlands", LanguageCategory.MAJOR_WORLD),
    Language("sv", "Swedish", "Svenska", LanguageCategory.MAJOR_WORLD),
    Language("pl", "Polish", "Polski", LanguageCategory.MAJOR_WORLD),
    Language("tr", "Turkish", "Türkçe", LanguageCategory.MAJOR_WORLD),
    Language("uk", "Ukrainian", "Українська", LanguageCategory.MAJOR_WORLD),
    Language("cs", "Czech", "Čeština", LanguageCategory.MAJOR_WORLD),
    Language("el", "Greek", "Ελληνικά", LanguageCategory.MAJOR_WORLD),
    Language("he", "Hebrew", "עברית", LanguageCategory.MAJOR_WORLD),
    Language("ro", "Romanian", "Română", LanguageCategory.MAJOR_WORLD),
    Language("hu", "Hungarian", "Magyar", LanguageCategory.MAJOR_WORLD),
    Language("da", "Danish", "Dansk", LanguageCategory.MAJOR_WORLD),
    Language("fi", "Finnish", "Suomi", LanguageCategory.MAJOR_WORLD),
    Language("no", "Norwegian", "Norsk", LanguageCategory.MAJOR_WORLD),
    Language("sk", "Slovak", "Slovenčina", LanguageCategory.MAJOR_WORLD),
    Language("bg", "Bulgarian", "Български", LanguageCategory.MAJOR_WORLD),
    Language("hr", "Croatian", "Hrvatski", LanguageCategory.MAJOR_WORLD),
    Language("sr", "Serbian", "Српски", LanguageCategory.MAJOR_WORLD),
    Language("sl", "Slovenian", "Slovenščina", LanguageCategory.MAJOR_WORLD),
    Language("et", "Estonian", "Eesti", LanguageCategory.MAJOR_WORLD),
    Language("lv", "Latvian", "Latviešu", LanguageCategory.MAJOR_WORLD),
    Language("lt", "Lithuanian", "Lietuvių", LanguageCategory.MAJOR_WORLD),

    // ========================================
    // EUROPEAN REGIONAL LANGUAGES
    // ========================================
    // Spain
    Language("ca", "Catalan", "Català", LanguageCategory.EUROPEAN_REGIONAL, "Spain - Catalonia, Valencia, Balearic Islands"),
    Language("gl", "Galician", "Galego", LanguageCategory.EUROPEAN_REGIONAL, "Spain - Galicia"),
    Language("eu", "Basque", "Euskara", LanguageCategory.EUROPEAN_REGIONAL, "Spain/France - Basque Country"),
    Language("oc", "Occitan", "Occitan", LanguageCategory.EUROPEAN_REGIONAL, "France/Spain - Southern France, Val d'Aran"),
    Language("ast", "Asturian", "Asturianu", LanguageCategory.EUROPEAN_REGIONAL, "Spain - Asturias"),

    // Italy
    Language("sc", "Sardinian", "Sardu", LanguageCategory.EUROPEAN_REGIONAL, "Italy - Sardinia"),
    Language("scn", "Sicilian", "Sicilianu", LanguageCategory.EUROPEAN_REGIONAL, "Italy - Sicily"),
    Language("nap", "Neapolitan", "Napulitano", LanguageCategory.EUROPEAN_REGIONAL, "Italy - Naples, Campania"),
    Language("fur", "Friulian", "Furlan", LanguageCategory.EUROPEAN_REGIONAL, "Italy - Friuli"),

    // France
    Language("br", "Breton", "Brezhoneg", LanguageCategory.EUROPEAN_REGIONAL, "France - Brittany"),
    Language("co", "Corsican", "Corsu", LanguageCategory.EUROPEAN_REGIONAL, "France - Corsica"),
    Language("gsw", "Alsatian", "Elsässisch", LanguageCategory.EUROPEAN_REGIONAL, "France - Alsace"),

    // UK & Ireland
    Language("cy", "Welsh", "Cymraeg", LanguageCategory.EUROPEAN_REGIONAL, "United Kingdom - Wales"),
    Language("gd", "Scottish Gaelic", "Gàidhlig", LanguageCategory.EUROPEAN_REGIONAL, "United Kingdom - Scotland"),
    Language("ga", "Irish", "Gaeilge", LanguageCategory.EUROPEAN_REGIONAL, "Ireland"),
    Language("kw", "Cornish", "Kernewek", LanguageCategory.EUROPEAN_REGIONAL, "United Kingdom - Cornwall"),

    // Low Countries & Switzerland
    Language("fy", "Frisian", "Frysk", LanguageCategory.EUROPEAN_REGIONAL, "Netherlands - Friesland"),
    Language("wa", "Walloon", "Walon", LanguageCategory.EUROPEAN_REGIONAL, "Belgium - Wallonia"),
    Language("li", "Limburgish", "Limburgs", LanguageCategory.EUROPEAN_REGIONAL, "Belgium/Netherlands - Limburg"),
    Language("rm", "Romansh", "Rumantsch", LanguageCategory.EUROPEAN_REGIONAL, "Switzerland - Graubünden"),
    Language("lb", "Luxembourgish", "Lëtzebuergesch", LanguageCategory.EUROPEAN_REGIONAL, "Luxembourg"),

    // Germany & Austria
    Language("nds", "Low German", "Plattdüütsch", LanguageCategory.EUROPEAN_REGIONAL, "Germany - Northern Germany"),
    Language("bar", "Bavarian", "Boarisch", LanguageCategory.EUROPEAN_REGIONAL, "Germany/Austria - Bavaria, Austria"),
    Language("hsb", "Upper Sorbian", "Hornjoserbšćina", LanguageCategory.EUROPEAN_REGIONAL, "Germany - Saxony"),

    // Nordic
    Language("is", "Icelandic", "Íslenska", LanguageCategory.EUROPEAN_REGIONAL, "Iceland"),
    Language("fo", "Faroese", "Føroyskt", LanguageCategory.EUROPEAN_REGIONAL, "Faroe Islands"),

    // Eastern Europe
    Language("be", "Belarusian", "Беларуская", LanguageCategory.EUROPEAN_REGIONAL, "Belarus"),
    Language("mk", "Macedonian", "Македонски", LanguageCategory.EUROPEAN_REGIONAL, "North Macedonia"),
    Language("sq", "Albanian", "Shqip", LanguageCategory.EUROPEAN_REGIONAL, "Albania, Kosovo"),
    Language("bs", "Bosnian", "Bosanski", LanguageCategory.EUROPEAN_REGIONAL, "Bosnia and Herzegovina"),
    Language("mt", "Maltese", "Malti", LanguageCategory.EUROPEAN_REGIONAL, "Malta"),

    // ========================================
    // ASIAN LANGUAGES
    // ========================================
    Language("yue", "Cantonese", "粵語", LanguageCategory.ASIAN, "China - Hong Kong, Guangdong"),
    Language("bo", "Tibetan", "བོད་སྐད", LanguageCategory.ASIAN, "China - Tibet"),
    Language("ug", "Uyghur", "ئۇيغۇرچە", LanguageCategory.ASIAN, "China - Xinjiang"),
    Language("mn", "Mongolian", "Монгол", LanguageCategory.ASIAN, "Mongolia"),
    Language("vi", "Vietnamese", "Tiếng Việt", LanguageCategory.ASIAN),
    Language("th", "Thai", "ไทย", LanguageCategory.ASIAN),
    Language("id", "Indonesian", "Bahasa Indonesia", LanguageCategory.ASIAN),
    Language("ms", "Malay", "Bahasa Melayu", LanguageCategory.ASIAN),
    Language("tl", "Filipino", "Filipino", LanguageCategory.ASIAN),
    Language("km", "Khmer", "ភាសាខ្មែរ", LanguageCategory.ASIAN, "Cambodia"),
    Language("lo", "Lao", "ລາວ", LanguageCategory.ASIAN, "Laos"),
    Language("my", "Burmese", "မြန်မာဘာသာ", LanguageCategory.ASIAN, "Myanmar"),
    Language("jv", "Javanese", "Basa Jawa", LanguageCategory.ASIAN, "Indonesia - Java"),
    Language("su", "Sundanese", "Basa Sunda", LanguageCategory.ASIAN, "Indonesia - West Java"),
    Language("ceb", "Cebuano", "Cebuano", LanguageCategory.ASIAN, "Philippines - Visayas"),
    Language("ilo", "Ilocano", "Ilokano", LanguageCategory.ASIAN, "Philippines - Northern Luzon"),

    // ========================================
    // INDIAN SUBCONTINENT LANGUAGES
    // ========================================
    Language("hi", "Hindi", "हिन्दी", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("bn", "Bengali", "বাংলা", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("pa", "Punjabi", "ਪੰਜਾਬੀ", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("ta", "Tamil", "தமிழ்", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("te", "Telugu", "తెలుగు", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("mr", "Marathi", "मराठी", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("gu", "Gujarati", "ગુજરાતી", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("kn", "Kannada", "ಕನ್ನಡ", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("ml", "Malayalam", "മലയാളം", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("or", "Odia", "ଓଡ଼ିଆ", LanguageCategory.INDIAN_SUBCONTINENT, "India - Odisha"),
    Language("as", "Assamese", "অসমীয়া", LanguageCategory.INDIAN_SUBCONTINENT, "India - Assam"),
    Language("ne", "Nepali", "नेपाली", LanguageCategory.INDIAN_SUBCONTINENT, "Nepal"),
    Language("si", "Sinhala", "සිංහල", LanguageCategory.INDIAN_SUBCONTINENT, "Sri Lanka"),
    Language("ur", "Urdu", "اردو", LanguageCategory.INDIAN_SUBCONTINENT),
    Language("sd", "Sindhi", "سنڌي", LanguageCategory.INDIAN_SUBCONTINENT, "Pakistan - Sindh"),
    Language("ks", "Kashmiri", "कॉशुर", LanguageCategory.INDIAN_SUBCONTINENT, "India/Pakistan - Kashmir"),
    Language("doi", "Dogri", "डोगरी", LanguageCategory.INDIAN_SUBCONTINENT, "India - Jammu"),
    Language("mai", "Maithili", "मैथिली", LanguageCategory.INDIAN_SUBCONTINENT, "India - Bihar"),
    Language("sat", "Santali", "ᱥᱟᱱᱛᱟᱲᱤ", LanguageCategory.INDIAN_SUBCONTINENT, "India - Jharkhand"),
    Language("kok", "Konkani", "कोंकणी", LanguageCategory.INDIAN_SUBCONTINENT, "India - Goa"),
    Language("mni", "Manipuri", "মৈতৈলোন্", LanguageCategory.INDIAN_SUBCONTINENT, "India - Manipur"),
    Language("dv", "Dhivehi", "ދިވެހި", LanguageCategory.INDIAN_SUBCONTINENT, "Maldives"),

    // ========================================
    // MIDDLE EASTERN LANGUAGES
    // ========================================
    Language("fa", "Persian", "فارسی", LanguageCategory.MIDDLE_EASTERN),
    Language("ku", "Kurdish", "Kurdî", LanguageCategory.MIDDLE_EASTERN, "Kurdistan Region"),
    Language("ps", "Pashto", "پښتو", LanguageCategory.MIDDLE_EASTERN, "Afghanistan, Pakistan"),
    Language("az", "Azerbaijani", "Azərbaycan", LanguageCategory.MIDDLE_EASTERN),
    Language("hy", "Armenian", "Հayskylerен", LanguageCategory.MIDDLE_EASTERN),
    Language("ka", "Georgian", "ქართული", LanguageCategory.MIDDLE_EASTERN),
    Language("uz", "Uzbek", "Oʻzbek", LanguageCategory.MIDDLE_EASTERN),
    Language("kk", "Kazakh", "Қазақ", LanguageCategory.MIDDLE_EASTERN),
    Language("tg", "Tajik", "Тоҷикӣ", LanguageCategory.MIDDLE_EASTERN),
    Language("tk", "Turkmen", "Türkmen", LanguageCategory.MIDDLE_EASTERN),
    Language("ky", "Kyrgyz", "Кыргыз", LanguageCategory.MIDDLE_EASTERN),

    // ========================================
    // AFRICAN LANGUAGES
    // ========================================
    Language("sw", "Swahili", "Kiswahili", LanguageCategory.AFRICAN),
    Language("af", "Afrikaans", "Afrikaans", LanguageCategory.AFRICAN),
    Language("am", "Amharic", "አማርኛ", LanguageCategory.AFRICAN, "Ethiopia"),
    Language("ha", "Hausa", "Hausa", LanguageCategory.AFRICAN, "Nigeria, Niger"),
    Language("yo", "Yoruba", "Yorùbá", LanguageCategory.AFRICAN, "Nigeria"),
    Language("ig", "Igbo", "Igbo", LanguageCategory.AFRICAN, "Nigeria"),
    Language("zu", "Zulu", "isiZulu", LanguageCategory.AFRICAN, "South Africa"),
    Language("xh", "Xhosa", "isiXhosa", LanguageCategory.AFRICAN, "South Africa"),
    Language("so", "Somali", "Soomaali", LanguageCategory.AFRICAN, "Somalia"),
    Language("rw", "Kinyarwanda", "Ikinyarwanda", LanguageCategory.AFRICAN, "Rwanda"),
    Language("rn", "Kirundi", "Ikirundi", LanguageCategory.AFRICAN, "Burundi"),
    Language("sn", "Shona", "chiShona", LanguageCategory.AFRICAN, "Zimbabwe"),
    Language("ny", "Chichewa", "Chichewa", LanguageCategory.AFRICAN, "Malawi"),
    Language("mg", "Malagasy", "Malagasy", LanguageCategory.AFRICAN, "Madagascar"),
    Language("ti", "Tigrinya", "ትግርኛ", LanguageCategory.AFRICAN, "Eritrea, Ethiopia"),
    Language("om", "Oromo", "Afaan Oromoo", LanguageCategory.AFRICAN, "Ethiopia"),
    Language("wo", "Wolof", "Wolof", LanguageCategory.AFRICAN, "Senegal"),
    Language("ff", "Fulah", "Fulfulde", LanguageCategory.AFRICAN, "West Africa"),
    Language("ln", "Lingala", "Lingála", LanguageCategory.AFRICAN, "Congo"),
    Language("kg", "Kongo", "Kikongo", LanguageCategory.AFRICAN, "Congo"),
    Language("st", "Sesotho", "Sesotho", LanguageCategory.AFRICAN, "Lesotho, South Africa"),
    Language("tn", "Setswana", "Setswana", LanguageCategory.AFRICAN, "Botswana, South Africa"),

    // ========================================
    // AMERICAS INDIGENOUS LANGUAGES
    // ========================================
    Language("qu", "Quechua", "Runasimi", LanguageCategory.AMERICAS_INDIGENOUS, "Peru, Bolivia, Ecuador"),
    Language("gn", "Guaraní", "Avañe'ẽ", LanguageCategory.AMERICAS_INDIGENOUS, "Paraguay"),
    Language("ay", "Aymara", "Aymar aru", LanguageCategory.AMERICAS_INDIGENOUS, "Bolivia, Peru"),
    Language("nah", "Nahuatl", "Nāhuatl", LanguageCategory.AMERICAS_INDIGENOUS, "Mexico"),
    Language("yua", "Yucatec Maya", "Màaya t'àan", LanguageCategory.AMERICAS_INDIGENOUS, "Mexico - Yucatán"),
    Language("oj", "Ojibwe", "Anishinaabemowin", LanguageCategory.AMERICAS_INDIGENOUS, "USA/Canada - Great Lakes"),
    Language("cr", "Cree", "ᓀᐦᐃᔭᐍᐏᐣ", LanguageCategory.AMERICAS_INDIGENOUS, "Canada"),
    Language("iu", "Inuktitut", "ᐃᓄᒃᑎᑐᑦ", LanguageCategory.AMERICAS_INDIGENOUS, "Canada - Nunavut"),
    Language("nv", "Navajo", "Diné bizaad", LanguageCategory.AMERICAS_INDIGENOUS, "USA - Southwest"),
    Language("chr", "Cherokee", "ᏣᎳᎩ", LanguageCategory.AMERICAS_INDIGENOUS, "USA - Oklahoma"),
    Language("ht", "Haitian Creole", "Kreyòl ayisyen", LanguageCategory.AMERICAS_INDIGENOUS, "Haiti"),
    Language("srn", "Sranan Tongo", "Sranan", LanguageCategory.AMERICAS_INDIGENOUS, "Suriname"),

    // ========================================
    // CLASSICAL & HISTORICAL LANGUAGES
    // ========================================
    Language("la", "Latin", "Latina", LanguageCategory.CLASSICAL, "Vatican, Scholarly"),
    Language("sa", "Sanskrit", "संस्कृतम्", LanguageCategory.CLASSICAL, "Hindu Rituals, Scholarly"),
    Language("grc", "Ancient Greek", "Ἑλληνική", LanguageCategory.CLASSICAL, "Scholarly, Theological"),
    Language("cu", "Church Slavonic", "Словѣ́ньскъ", LanguageCategory.CLASSICAL, "Orthodox Liturgy"),
    Language("pi", "Pali", "पालि", LanguageCategory.CLASSICAL, "Buddhist Texts"),
    Language("cop", "Coptic", "Ⲙⲉⲧⲣⲉⲙⲛ̀ⲭⲏⲙⲓ", LanguageCategory.CLASSICAL, "Coptic Christian Liturgy"),
    Language("syr", "Syriac", "ܠܫܢܐ ܣܘܪܝܝܐ", LanguageCategory.CLASSICAL, "Syriac Christian Liturgy"),
)

// MARK: - Countries List
val COUNTRIES = listOf(
    // Americas
    Country("US", "United States", "🇺🇸"),
    Country("CA", "Canada", "🇨🇦"),
    Country("MX", "Mexico", "🇲🇽"),
    Country("BR", "Brazil", "🇧🇷"),
    Country("AR", "Argentina", "🇦🇷"),
    Country("CO", "Colombia", "🇨🇴"),
    Country("PE", "Peru", "🇵🇪"),
    Country("CL", "Chile", "🇨🇱"),
    Country("VE", "Venezuela", "🇻🇪"),
    Country("EC", "Ecuador", "🇪🇨"),
    Country("BO", "Bolivia", "🇧🇴"),
    Country("PY", "Paraguay", "🇵🇾"),
    Country("UY", "Uruguay", "🇺🇾"),
    Country("CR", "Costa Rica", "🇨🇷"),
    Country("PA", "Panama", "🇵🇦"),
    Country("GT", "Guatemala", "🇬🇹"),
    Country("HN", "Honduras", "🇭🇳"),
    Country("SV", "El Salvador", "🇸🇻"),
    Country("NI", "Nicaragua", "🇳🇮"),
    Country("CU", "Cuba", "🇨🇺"),
    Country("DO", "Dominican Republic", "🇩🇴"),
    Country("PR", "Puerto Rico", "🇵🇷"),
    Country("JM", "Jamaica", "🇯🇲"),
    Country("HT", "Haiti", "🇭🇹"),
    Country("TT", "Trinidad and Tobago", "🇹🇹"),
    Country("SR", "Suriname", "🇸🇷"),

    // Europe
    Country("GB", "United Kingdom", "🇬🇧"),
    Country("FR", "France", "🇫🇷"),
    Country("DE", "Germany", "🇩🇪"),
    Country("IT", "Italy", "🇮🇹"),
    Country("ES", "Spain", "🇪🇸"),
    Country("PT", "Portugal", "🇵🇹"),
    Country("NL", "Netherlands", "🇳🇱"),
    Country("BE", "Belgium", "🇧🇪"),
    Country("CH", "Switzerland", "🇨🇭"),
    Country("AT", "Austria", "🇦🇹"),
    Country("SE", "Sweden", "🇸🇪"),
    Country("NO", "Norway", "🇳🇴"),
    Country("DK", "Denmark", "🇩🇰"),
    Country("FI", "Finland", "🇫🇮"),
    Country("IE", "Ireland", "🇮🇪"),
    Country("PL", "Poland", "🇵🇱"),
    Country("CZ", "Czech Republic", "🇨🇿"),
    Country("SK", "Slovakia", "🇸🇰"),
    Country("HU", "Hungary", "🇭🇺"),
    Country("RO", "Romania", "🇷🇴"),
    Country("BG", "Bulgaria", "🇧🇬"),
    Country("GR", "Greece", "🇬🇷"),
    Country("UA", "Ukraine", "🇺🇦"),
    Country("RU", "Russia", "🇷🇺"),
    Country("HR", "Croatia", "🇭🇷"),
    Country("RS", "Serbia", "🇷🇸"),
    Country("SI", "Slovenia", "🇸🇮"),
    Country("EE", "Estonia", "🇪🇪"),
    Country("LV", "Latvia", "🇱🇻"),
    Country("LT", "Lithuania", "🇱🇹"),
    Country("IS", "Iceland", "🇮🇸"),
    Country("LU", "Luxembourg", "🇱🇺"),
    Country("MT", "Malta", "🇲🇹"),
    Country("AL", "Albania", "🇦🇱"),
    Country("MK", "North Macedonia", "🇲🇰"),
    Country("BA", "Bosnia and Herzegovina", "🇧🇦"),
    Country("ME", "Montenegro", "🇲🇪"),
    Country("XK", "Kosovo", "🇽🇰"),
    Country("BY", "Belarus", "🇧🇾"),
    Country("MD", "Moldova", "🇲🇩"),

    // Asia
    Country("CN", "China", "🇨🇳"),
    Country("JP", "Japan", "🇯🇵"),
    Country("KR", "South Korea", "🇰🇷"),
    Country("IN", "India", "🇮🇳"),
    Country("ID", "Indonesia", "🇮🇩"),
    Country("TH", "Thailand", "🇹🇭"),
    Country("VN", "Vietnam", "🇻🇳"),
    Country("MY", "Malaysia", "🇲🇾"),
    Country("SG", "Singapore", "🇸🇬"),
    Country("PH", "Philippines", "🇵🇭"),
    Country("TW", "Taiwan", "🇹🇼"),
    Country("HK", "Hong Kong", "🇭🇰"),
    Country("PK", "Pakistan", "🇵🇰"),
    Country("BD", "Bangladesh", "🇧🇩"),
    Country("NP", "Nepal", "🇳🇵"),
    Country("LK", "Sri Lanka", "🇱🇰"),
    Country("MM", "Myanmar", "🇲🇲"),
    Country("KH", "Cambodia", "🇰🇭"),
    Country("LA", "Laos", "🇱🇦"),
    Country("MN", "Mongolia", "🇲🇳"),
    Country("MV", "Maldives", "🇲🇻"),
    Country("BT", "Bhutan", "🇧🇹"),

    // Middle East & Central Asia
    Country("TR", "Turkey", "🇹🇷"),
    Country("SA", "Saudi Arabia", "🇸🇦"),
    Country("AE", "United Arab Emirates", "🇦🇪"),
    Country("IL", "Israel", "🇮🇱"),
    Country("IR", "Iran", "🇮🇷"),
    Country("IQ", "Iraq", "🇮🇶"),
    Country("EG", "Egypt", "🇪🇬"),
    Country("JO", "Jordan", "🇯🇴"),
    Country("LB", "Lebanon", "🇱🇧"),
    Country("SY", "Syria", "🇸🇾"),
    Country("KW", "Kuwait", "🇰🇼"),
    Country("QA", "Qatar", "🇶🇦"),
    Country("BH", "Bahrain", "🇧🇭"),
    Country("OM", "Oman", "🇴🇲"),
    Country("YE", "Yemen", "🇾🇪"),
    Country("AF", "Afghanistan", "🇦🇫"),
    Country("AZ", "Azerbaijan", "🇦🇿"),
    Country("AM", "Armenia", "🇦🇲"),
    Country("GE", "Georgia", "🇬🇪"),
    Country("KZ", "Kazakhstan", "🇰🇿"),
    Country("UZ", "Uzbekistan", "🇺🇿"),
    Country("TM", "Turkmenistan", "🇹🇲"),
    Country("TJ", "Tajikistan", "🇹🇯"),
    Country("KG", "Kyrgyzstan", "🇰🇬"),

    // Africa
    Country("ZA", "South Africa", "🇿🇦"),
    Country("NG", "Nigeria", "🇳🇬"),
    Country("KE", "Kenya", "🇰🇪"),
    Country("ET", "Ethiopia", "🇪🇹"),
    Country("GH", "Ghana", "🇬🇭"),
    Country("TZ", "Tanzania", "🇹🇿"),
    Country("UG", "Uganda", "🇺🇬"),
    Country("MA", "Morocco", "🇲🇦"),
    Country("DZ", "Algeria", "🇩🇿"),
    Country("TN", "Tunisia", "🇹🇳"),
    Country("SN", "Senegal", "🇸🇳"),
    Country("CI", "Ivory Coast", "🇨🇮"),
    Country("CM", "Cameroon", "🇨🇲"),
    Country("AO", "Angola", "🇦🇴"),
    Country("MZ", "Mozambique", "🇲🇿"),
    Country("ZW", "Zimbabwe", "🇿🇼"),
    Country("RW", "Rwanda", "🇷🇼"),
    Country("BI", "Burundi", "🇧🇮"),
    Country("MW", "Malawi", "🇲🇼"),
    Country("MG", "Madagascar", "🇲🇬"),
    Country("SO", "Somalia", "🇸🇴"),
    Country("ER", "Eritrea", "🇪🇷"),
    Country("BW", "Botswana", "🇧🇼"),
    Country("NA", "Namibia", "🇳🇦"),
    Country("LS", "Lesotho", "🇱🇸"),
    Country("SZ", "Eswatini", "🇸🇿"),
    Country("CD", "DR Congo", "🇨🇩"),
    Country("CG", "Congo", "🇨🇬"),

    // Oceania
    Country("AU", "Australia", "🇦🇺"),
    Country("NZ", "New Zealand", "🇳🇿"),
    Country("FJ", "Fiji", "🇫🇯"),
    Country("PG", "Papua New Guinea", "🇵🇬"),
    Country("WS", "Samoa", "🇼🇸"),
    Country("TO", "Tonga", "🇹🇴"),
)

// MARK: - Helper Functions
fun getLanguageByCode(code: String): Language? = LANGUAGES.find { it.code == code }

fun getCountryByCode(code: String): Country? = COUNTRIES.find { it.code == code }

fun getLanguagesByCategory(category: LanguageCategory): List<Language> = 
    LANGUAGES.filter { it.category == category }

fun getAllLanguageCategories(): List<LanguageCategory> = LanguageCategory.values().toList()

// MARK: - Grouped Languages for UI
fun getGroupedLanguages(): List<Pair<LanguageCategory, List<Language>>> =
    LanguageCategory.values().map { category ->
        category to getLanguagesByCategory(category)
    }
