//
//  LocalizationManager.swift
//  Intok
//
//  Manages app UI language localization with support for 120+ languages.
//  - Auto-detects device language on first launch
//  - Allows manual override in Settings
//  - Handles RTL (right-to-left) languages
//

import Foundation
import SwiftUI
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "Localization")

// MARK: - LocalizationManager
class LocalizationManager: ObservableObject {
    static let shared = LocalizationManager()
    
    // UserDefaults keys
    private let appLanguageKey = "app_language"
    
    // "auto" means use device language, otherwise specific language code (e.g., "ja")
    @Published var appLanguage: String {
        didSet {
            UserDefaults.standard.set(appLanguage, forKey: appLanguageKey)
            updateBundle()
            logger.info("🌐 App language changed to: \(self.appLanguage, privacy: .public)")
        }
    }
    
    // The actual resolved language code being used
    @Published private(set) var currentLanguageCode: String
    
    // Layout direction for RTL support
    @Published private(set) var layoutDirection: LayoutDirection = .leftToRight
    
    // Custom bundle for loading localized strings
    private var localizedBundle: Bundle = .main
    
    // RTL language codes
    private let rtlLanguages = ["ar", "he", "fa", "ur", "ps", "sd", "ug", "yi", "dv", "ku", "syr"]
    
    private init() {
        // Load saved preference or default to "auto"
        let savedLanguage = UserDefaults.standard.string(forKey: appLanguageKey) ?? "auto"
        self.appLanguage = savedLanguage
        self.currentLanguageCode = "en" // Will be updated in updateBundle()
        
        NSLog("🌐 LocalizationManager init - savedLanguage: \(savedLanguage)")
        NSLog("🌐 Locale.preferredLanguages: \(Locale.preferredLanguages)")
        
        updateBundle()
        
        // Listen for locale changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(localeDidChange),
            name: NSLocale.currentLocaleDidChangeNotification,
            object: nil
        )
        
        NSLog("🌐 LocalizationManager initialized - currentLanguageCode: \(currentLanguageCode)")
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    // MARK: - Public Methods
    
    /// Get the display name for a language code
    func languageDisplayName(for code: String) -> String {
        if let language = getLanguageByCode(code) {
            return "\(language.native) (\(language.name))"
        }
        return Locale.current.localizedString(forLanguageCode: code) ?? code
    }
    
    /// Get the native name for a language code
    func languageNativeName(for code: String) -> String {
        if let language = getLanguageByCode(code) {
            return language.native
        }
        let locale = Locale(identifier: code)
        return locale.localizedString(forLanguageCode: code) ?? code
    }
    
    /// Check if current language is RTL
    var isRTL: Bool {
        rtlLanguages.contains(currentLanguageCode)
    }
    
    /// Get the resolved language code (handles "auto")
    var resolvedLanguageCode: String {
        if appLanguage == "auto" {
            return detectDeviceLanguage()
        }
        return appLanguage
    }
    
    /// Get flag emoji for a language code
    func flagEmoji(for languageCode: String) -> String {
        // Map language codes to common country codes for flag display
        let languageToCountry: [String: String] = [
            "en": "US", "es": "ES", "fr": "FR", "de": "DE", "it": "IT",
            "pt": "BR", "ru": "RU", "zh": "CN", "ja": "JP", "ko": "KR",
            "ar": "SA", "nl": "NL", "sv": "SE", "pl": "PL", "tr": "TR",
            "uk": "UA", "cs": "CZ", "el": "GR", "he": "IL", "ro": "RO",
            "hu": "HU", "da": "DK", "fi": "FI", "no": "NO", "th": "TH",
            "vi": "VN", "id": "ID", "ms": "MY", "tl": "PH", "hi": "IN",
            "bn": "BD", "ta": "IN", "fa": "IR", "sw": "KE", "af": "ZA"
        ]
        
        if let countryCode = languageToCountry[languageCode] {
            return flagEmoji(forCountryCode: countryCode)
        }
        return "🌐"
    }
    
    /// Get flag emoji for a country code
    func flagEmoji(forCountryCode code: String) -> String {
        let base: UInt32 = 127397
        var emoji = ""
        for scalar in code.uppercased().unicodeScalars {
            if let flagScalar = UnicodeScalar(base + scalar.value) {
                emoji.append(String(flagScalar))
            }
        }
        return emoji.isEmpty ? "🌐" : emoji
    }
    
    // MARK: - Private Methods
    
    private func updateBundle() {
        let targetLanguage = resolvedLanguageCode
        currentLanguageCode = targetLanguage
        
        // Update layout direction
        layoutDirection = rtlLanguages.contains(targetLanguage) ? .rightToLeft : .leftToRight
        
        NSLog("🌐 updateBundle: targetLanguage=\(targetLanguage)")
        
        // Find the appropriate .lproj bundle
        if let path = Bundle.main.path(forResource: targetLanguage, ofType: "lproj") {
            NSLog("🌐 updateBundle: Found path: \(path)")
            if let bundle = Bundle(path: path) {
                localizedBundle = bundle
                NSLog("🌐 Loaded bundle for: \(targetLanguage)")
                
                // Verify bundle loaded correctly
                let testKey = "settings_title"
                let testValue = bundle.localizedString(forKey: testKey, value: nil, table: "Localizable")
                NSLog("🌐 Test lookup '\(testKey)' = '\(testValue)'")
            } else {
                NSLog("🌐 Failed to create bundle from path")
            }
        } else if let path = Bundle.main.path(forResource: "en", ofType: "lproj"),
                  let bundle = Bundle(path: path) {
            // Fallback to English
            localizedBundle = bundle
            NSLog("🌐 Fallback to English bundle (target '\(targetLanguage)' not found)")
        } else {
            // Use main bundle
            localizedBundle = .main
            NSLog("🌐 Using main bundle (no .lproj found)")
        }
        
        // Post notification for UI updates
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: .languageDidChange, object: nil)
        }
    }
    
    private func detectDeviceLanguage() -> String {
        // Get device's preferred language
        NSLog("🌐 detectDeviceLanguage: Locale.preferredLanguages = \(Locale.preferredLanguages)")
        
        if let preferredLanguage = Locale.preferredLanguages.first {
            // Extract just the language code (e.g., "en-US" -> "en")
            let languageCode = Locale(identifier: preferredLanguage).language.languageCode?.identifier ?? "en"
            NSLog("🌐 detectDeviceLanguage: extracted languageCode = \(languageCode)")
            
            // Check if we support this language
            if LANGUAGES.contains(where: { $0.code == languageCode }) {
                NSLog("🌐 detectDeviceLanguage: language supported, returning \(languageCode)")
                return languageCode
            } else {
                NSLog("🌐 detectDeviceLanguage: language '\(languageCode)' NOT in LANGUAGES array")
            }
        }
        
        // Default to English
        NSLog("🌐 detectDeviceLanguage: defaulting to 'en'")
        return "en"
    }
    
    @objc private func localeDidChange() {
        if appLanguage == "auto" {
            updateBundle()
            logger.info("🌐 Device locale changed, updating bundle")
        }
    }
    
    // MARK: - Localized String Access
    
    /// Get a localized string for a key
    func localizedString(for key: String, comment: String = "") -> String {
        // First try the localized bundle
        let localizedValue = localizedBundle.localizedString(forKey: key, value: nil, table: "Localizable")
        
        // If we got back the key itself, the translation doesn't exist
        if localizedValue == key {
            // Try main bundle as fallback
            let mainValue = Bundle.main.localizedString(forKey: key, value: nil, table: "Localizable")
            if mainValue != key {
                return mainValue
            }
            // Return key as last resort (helps identify missing translations)
            logger.warning("⚠️ Missing translation for key: \(key, privacy: .public)")
            return key
        }
        
        return localizedValue
    }
    
    /// Get a localized string with format arguments
    func localizedString(for key: String, arguments: CVarArg...) -> String {
        let format = localizedString(for: key)
        return String(format: format, arguments: arguments)
    }
}

// MARK: - Notification Names
extension Notification.Name {
    static let languageDidChange = Notification.Name("languageDidChange")
}

// MARK: - String Extension for Localization
extension String {
    /// Returns the localized string for this key
    var localized: String {
        let result = LocalizationManager.shared.localizedString(for: self)
        // Debug: uncomment to log lookups
        // NSLog("🌐 Lookup: '\(self)' -> '\(result)'")
        return result
    }
    
    /// Returns the localized string with format arguments
    func localized(with arguments: CVarArg...) -> String {
        let format = LocalizationManager.shared.localizedString(for: self)
        return String(format: format, arguments: arguments)
    }
}

// MARK: - View Extension for RTL Support
extension View {
    /// Apply RTL layout direction if needed
    func localizedLayoutDirection() -> some View {
        self.environment(\.layoutDirection, LocalizationManager.shared.layoutDirection)
    }
}

// MARK: - SwiftUI Environment Key for Localization
private struct LocalizationManagerKey: EnvironmentKey {
    static let defaultValue = LocalizationManager.shared
}

extension EnvironmentValues {
    var localizationManager: LocalizationManager {
        get { self[LocalizationManagerKey.self] }
        set { self[LocalizationManagerKey.self] = newValue }
    }
}
