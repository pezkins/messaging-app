package com.intokapp.app.data.constants

// MARK: - Language Data Class
data class Language(
    val code: String,
    val name: String,
    val native: String
)

// MARK: - Country Data Class
data class Country(
    val code: String,
    val name: String,
    val flag: String
)

// MARK: - Languages List
val LANGUAGES = listOf(
    Language("en", "English", "English"),
    Language("es", "Spanish", "Español"),
    Language("fr", "French", "Français"),
    Language("de", "German", "Deutsch"),
    Language("it", "Italian", "Italiano"),
    Language("pt", "Portuguese", "Português"),
    Language("ru", "Russian", "Русский"),
    Language("zh", "Chinese", "中文"),
    Language("ja", "Japanese", "日本語"),
    Language("ko", "Korean", "한국어"),
    Language("ar", "Arabic", "العربية"),
    Language("hi", "Hindi", "हिन्दी"),
    Language("bn", "Bengali", "বাংলা"),
    Language("pa", "Punjabi", "ਪੰਜਾਬੀ"),
    Language("vi", "Vietnamese", "Tiếng Việt"),
    Language("th", "Thai", "ไทย"),
    Language("tr", "Turkish", "Türkçe"),
    Language("pl", "Polish", "Polski"),
    Language("nl", "Dutch", "Nederlands"),
    Language("sv", "Swedish", "Svenska"),
    Language("da", "Danish", "Dansk"),
    Language("no", "Norwegian", "Norsk"),
    Language("fi", "Finnish", "Suomi"),
    Language("el", "Greek", "Ελληνικά"),
    Language("he", "Hebrew", "עברית"),
    Language("id", "Indonesian", "Bahasa Indonesia"),
    Language("ms", "Malay", "Bahasa Melayu"),
    Language("tl", "Filipino", "Filipino"),
    Language("uk", "Ukrainian", "Українська"),
    Language("cs", "Czech", "Čeština"),
    Language("ro", "Romanian", "Română"),
    Language("hu", "Hungarian", "Magyar"),
    Language("sk", "Slovak", "Slovenčina"),
    Language("bg", "Bulgarian", "Български"),
    Language("hr", "Croatian", "Hrvatski"),
    Language("sr", "Serbian", "Српски"),
    Language("sl", "Slovenian", "Slovenščina"),
    Language("et", "Estonian", "Eesti"),
    Language("lv", "Latvian", "Latviešu"),
    Language("lt", "Lithuanian", "Lietuvių"),
)

// MARK: - Countries List
val COUNTRIES = listOf(
    Country("US", "United States", "🇺🇸"),
    Country("GB", "United Kingdom", "🇬🇧"),
    Country("CA", "Canada", "🇨🇦"),
    Country("AU", "Australia", "🇦🇺"),
    Country("DE", "Germany", "🇩🇪"),
    Country("FR", "France", "🇫🇷"),
    Country("ES", "Spain", "🇪🇸"),
    Country("IT", "Italy", "🇮🇹"),
    Country("PT", "Portugal", "🇵🇹"),
    Country("BR", "Brazil", "🇧🇷"),
    Country("MX", "Mexico", "🇲🇽"),
    Country("AR", "Argentina", "🇦🇷"),
    Country("CL", "Chile", "🇨🇱"),
    Country("CO", "Colombia", "🇨🇴"),
    Country("PE", "Peru", "🇵🇪"),
    Country("RU", "Russia", "🇷🇺"),
    Country("CN", "China", "🇨🇳"),
    Country("JP", "Japan", "🇯🇵"),
    Country("KR", "South Korea", "🇰🇷"),
    Country("IN", "India", "🇮🇳"),
    Country("PK", "Pakistan", "🇵🇰"),
    Country("BD", "Bangladesh", "🇧🇩"),
    Country("ID", "Indonesia", "🇮🇩"),
    Country("MY", "Malaysia", "🇲🇾"),
    Country("PH", "Philippines", "🇵🇭"),
    Country("VN", "Vietnam", "🇻🇳"),
    Country("TH", "Thailand", "🇹🇭"),
    Country("SG", "Singapore", "🇸🇬"),
    Country("NL", "Netherlands", "🇳🇱"),
    Country("BE", "Belgium", "🇧🇪"),
    Country("SE", "Sweden", "🇸🇪"),
    Country("NO", "Norway", "🇳🇴"),
    Country("DK", "Denmark", "🇩🇰"),
    Country("FI", "Finland", "🇫🇮"),
    Country("PL", "Poland", "🇵🇱"),
    Country("CZ", "Czech Republic", "🇨🇿"),
    Country("AT", "Austria", "🇦🇹"),
    Country("CH", "Switzerland", "🇨🇭"),
    Country("GR", "Greece", "🇬🇷"),
    Country("TR", "Turkey", "🇹🇷"),
    Country("IL", "Israel", "🇮🇱"),
    Country("AE", "UAE", "🇦🇪"),
    Country("SA", "Saudi Arabia", "🇸🇦"),
    Country("EG", "Egypt", "🇪🇬"),
    Country("ZA", "South Africa", "🇿🇦"),
    Country("NG", "Nigeria", "🇳🇬"),
    Country("KE", "Kenya", "🇰🇪"),
    Country("NZ", "New Zealand", "🇳🇿"),
    Country("IE", "Ireland", "🇮🇪"),
    Country("UA", "Ukraine", "🇺🇦"),
)

// MARK: - Helper Functions
fun getLanguageByCode(code: String): Language? = LANGUAGES.find { it.code == code }

fun getCountryByCode(code: String): Country? = COUNTRIES.find { it.code == code }


