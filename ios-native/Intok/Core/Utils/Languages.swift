import Foundation

// MARK: - Language Type
struct Language: Identifiable, Hashable {
    let id: String
    let code: String
    let name: String
    let native: String
    
    init(code: String, name: String, native: String) {
        self.id = code
        self.code = code
        self.name = name
        self.native = native
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

// MARK: - Languages List
let LANGUAGES: [Language] = [
    Language(code: "en", name: "English", native: "English"),
    Language(code: "es", name: "Spanish", native: "Español"),
    Language(code: "fr", name: "French", native: "Français"),
    Language(code: "de", name: "German", native: "Deutsch"),
    Language(code: "it", name: "Italian", native: "Italiano"),
    Language(code: "pt", name: "Portuguese", native: "Português"),
    Language(code: "ru", name: "Russian", native: "Русский"),
    Language(code: "zh", name: "Chinese", native: "中文"),
    Language(code: "ja", name: "Japanese", native: "日本語"),
    Language(code: "ko", name: "Korean", native: "한국어"),
    Language(code: "ar", name: "Arabic", native: "العربية"),
    Language(code: "hi", name: "Hindi", native: "हिन्दी"),
    Language(code: "bn", name: "Bengali", native: "বাংলা"),
    Language(code: "pa", name: "Punjabi", native: "ਪੰਜਾਬੀ"),
    Language(code: "vi", name: "Vietnamese", native: "Tiếng Việt"),
    Language(code: "th", name: "Thai", native: "ไทย"),
    Language(code: "tr", name: "Turkish", native: "Türkçe"),
    Language(code: "pl", name: "Polish", native: "Polski"),
    Language(code: "nl", name: "Dutch", native: "Nederlands"),
    Language(code: "sv", name: "Swedish", native: "Svenska"),
    Language(code: "da", name: "Danish", native: "Dansk"),
    Language(code: "no", name: "Norwegian", native: "Norsk"),
    Language(code: "fi", name: "Finnish", native: "Suomi"),
    Language(code: "el", name: "Greek", native: "Ελληνικά"),
    Language(code: "he", name: "Hebrew", native: "עברית"),
    Language(code: "id", name: "Indonesian", native: "Bahasa Indonesia"),
    Language(code: "ms", name: "Malay", native: "Bahasa Melayu"),
    Language(code: "tl", name: "Filipino", native: "Filipino"),
    Language(code: "uk", name: "Ukrainian", native: "Українська"),
    Language(code: "cs", name: "Czech", native: "Čeština"),
    Language(code: "ro", name: "Romanian", native: "Română"),
    Language(code: "hu", name: "Hungarian", native: "Magyar"),
    Language(code: "sk", name: "Slovak", native: "Slovenčina"),
    Language(code: "bg", name: "Bulgarian", native: "Български"),
    Language(code: "hr", name: "Croatian", native: "Hrvatski"),
    Language(code: "sr", name: "Serbian", native: "Српски"),
    Language(code: "sl", name: "Slovenian", native: "Slovenščina"),
    Language(code: "et", name: "Estonian", native: "Eesti"),
    Language(code: "lv", name: "Latvian", native: "Latviešu"),
    Language(code: "lt", name: "Lithuanian", native: "Lietuvių"),
]

// MARK: - Countries List
let COUNTRIES: [Country] = [
    Country(code: "US", name: "United States", flag: "🇺🇸"),
    Country(code: "GB", name: "United Kingdom", flag: "🇬🇧"),
    Country(code: "CA", name: "Canada", flag: "🇨🇦"),
    Country(code: "AU", name: "Australia", flag: "🇦🇺"),
    Country(code: "DE", name: "Germany", flag: "🇩🇪"),
    Country(code: "FR", name: "France", flag: "🇫🇷"),
    Country(code: "ES", name: "Spain", flag: "🇪🇸"),
    Country(code: "IT", name: "Italy", flag: "🇮🇹"),
    Country(code: "PT", name: "Portugal", flag: "🇵🇹"),
    Country(code: "BR", name: "Brazil", flag: "🇧🇷"),
    Country(code: "MX", name: "Mexico", flag: "🇲🇽"),
    Country(code: "AR", name: "Argentina", flag: "🇦🇷"),
    Country(code: "CL", name: "Chile", flag: "🇨🇱"),
    Country(code: "CO", name: "Colombia", flag: "🇨🇴"),
    Country(code: "PE", name: "Peru", flag: "🇵🇪"),
    Country(code: "RU", name: "Russia", flag: "🇷🇺"),
    Country(code: "CN", name: "China", flag: "🇨🇳"),
    Country(code: "JP", name: "Japan", flag: "🇯🇵"),
    Country(code: "KR", name: "South Korea", flag: "🇰🇷"),
    Country(code: "IN", name: "India", flag: "🇮🇳"),
    Country(code: "PK", name: "Pakistan", flag: "🇵🇰"),
    Country(code: "BD", name: "Bangladesh", flag: "🇧🇩"),
    Country(code: "ID", name: "Indonesia", flag: "🇮🇩"),
    Country(code: "MY", name: "Malaysia", flag: "🇲🇾"),
    Country(code: "PH", name: "Philippines", flag: "🇵🇭"),
    Country(code: "VN", name: "Vietnam", flag: "🇻🇳"),
    Country(code: "TH", name: "Thailand", flag: "🇹🇭"),
    Country(code: "SG", name: "Singapore", flag: "🇸🇬"),
    Country(code: "NL", name: "Netherlands", flag: "🇳🇱"),
    Country(code: "BE", name: "Belgium", flag: "🇧🇪"),
    Country(code: "SE", name: "Sweden", flag: "🇸🇪"),
    Country(code: "NO", name: "Norway", flag: "🇳🇴"),
    Country(code: "DK", name: "Denmark", flag: "🇩🇰"),
    Country(code: "FI", name: "Finland", flag: "🇫🇮"),
    Country(code: "PL", name: "Poland", flag: "🇵🇱"),
    Country(code: "CZ", name: "Czech Republic", flag: "🇨🇿"),
    Country(code: "AT", name: "Austria", flag: "🇦🇹"),
    Country(code: "CH", name: "Switzerland", flag: "🇨🇭"),
    Country(code: "GR", name: "Greece", flag: "🇬🇷"),
    Country(code: "TR", name: "Turkey", flag: "🇹🇷"),
    Country(code: "IL", name: "Israel", flag: "🇮🇱"),
    Country(code: "AE", name: "UAE", flag: "🇦🇪"),
    Country(code: "SA", name: "Saudi Arabia", flag: "🇸🇦"),
    Country(code: "EG", name: "Egypt", flag: "🇪🇬"),
    Country(code: "ZA", name: "South Africa", flag: "🇿🇦"),
    Country(code: "NG", name: "Nigeria", flag: "🇳🇬"),
    Country(code: "KE", name: "Kenya", flag: "🇰🇪"),
    Country(code: "NZ", name: "New Zealand", flag: "🇳🇿"),
    Country(code: "IE", name: "Ireland", flag: "🇮🇪"),
    Country(code: "UA", name: "Ukraine", flag: "🇺🇦"),
]

// MARK: - Helper Functions
func getLanguageByCode(_ code: String) -> Language? {
    return LANGUAGES.first { $0.code == code }
}

func getCountryByCode(_ code: String) -> Country? {
    return COUNTRIES.first { $0.code == code }
}


