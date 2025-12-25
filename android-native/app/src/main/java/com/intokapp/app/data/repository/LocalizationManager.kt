package com.intokapp.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.intokapp.app.data.constants.LANGUAGES
import com.intokapp.app.data.constants.Language
import com.intokapp.app.data.constants.getLanguageByCode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LocalizationManager handles app UI language settings.
 * 
 * Architecture (Hybrid Approach per i18n-implementation-plan.md):
 * - Tier 1 Languages (top ~10): Use static strings.xml files + Android per-app language API
 * - Tier 2+ Languages (110+): Fetch from backend API, cache locally
 * 
 * Features:
 * - Auto-detect device language on first launch
 * - Allow manual override in Settings
 * - Support for ALL 120+ languages
 * - RTL language detection
 * - Uses Android 13+ per-app language API with AppCompat fallback
 */
@Singleton
class LocalizationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LocalizationManager"
        private const val PREFS_NAME = "intok_localization"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_APP_COUNTRY = "app_country"
        private const val VALUE_AUTO = "auto"
        
        // RTL language codes
        private val RTL_LANGUAGES = setOf(
            "ar", // Arabic
            "he", // Hebrew
            "fa", // Persian
            "ur", // Urdu
            "ps", // Pashto
            "sd", // Sindhi
            "ug", // Uyghur
            "dv", // Dhivehi
            "syr", // Syriac
            "ks"  // Kashmiri
        )
        
        // Tier 1 languages with bundled static translation files
        // These work offline and use Android's per-app language API
        val TIER_1_LANGUAGES = setOf(
            "en", // English (default)
            "es", // Spanish
            "fr", // French
            "de", // German
            "pt", // Portuguese
            "ja", // Japanese
            "ko", // Korean
            "ar", // Arabic (RTL)
            "zh"  // Chinese
        )
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Injected lazily to avoid circular dependency
    private var _translationService: UITranslationService? = null
    
    private val _currentLanguage = MutableStateFlow(getEffectiveLanguage())
    val currentLanguage: StateFlow<Language?> = _currentLanguage
    
    private val _isRTL = MutableStateFlow(false)
    val isRTL: StateFlow<Boolean> = _isRTL
    
    private val _isLoadingTranslations = MutableStateFlow(false)
    val isLoadingTranslations: StateFlow<Boolean> = _isLoadingTranslations
    
    init {
        // Initialize on first launch
        if (!prefs.contains(KEY_APP_LANGUAGE)) {
            // First launch - detect device language
            val deviceLanguage = getDeviceLanguage()
            Log.d(TAG, "🌐 First launch - detected device language: $deviceLanguage")
            prefs.edit().putString(KEY_APP_LANGUAGE, VALUE_AUTO).apply()
        }
        
        updateRTLState()
        Log.d(TAG, "🌐 Initialized with language: ${_currentLanguage.value?.name ?: "Auto"}, RTL: ${_isRTL.value}")
    }
    
    /**
     * Set the translation service (called after DI is complete)
     */
    fun setTranslationService(service: UITranslationService) {
        _translationService = service
    }
    
    /**
     * Get the stored language preference.
     * Returns "auto" or a specific language code.
     */
    fun getLanguagePreference(): String {
        return prefs.getString(KEY_APP_LANGUAGE, VALUE_AUTO) ?: VALUE_AUTO
    }
    
    /**
     * Get the stored country preference for the app language.
     */
    fun getCountryPreference(): String? {
        return prefs.getString(KEY_APP_COUNTRY, null)
    }
    
    /**
     * Check if using automatic (device) language.
     */
    fun isAutoLanguage(): Boolean {
        return getLanguagePreference() == VALUE_AUTO
    }
    
    /**
     * Get the device's current language code.
     */
    fun getDeviceLanguage(): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return locale.language
    }
    
    /**
     * Get the effective language being used.
     * If "auto", returns the device language. Otherwise returns the selected language.
     */
    fun getEffectiveLanguageCode(): String {
        val pref = getLanguagePreference()
        return if (pref == VALUE_AUTO) getDeviceLanguage() else pref
    }
    
    /**
     * Get the effective Language object.
     */
    fun getEffectiveLanguage(): Language? {
        return getLanguageByCode(getEffectiveLanguageCode())
    }
    
    /**
     * Check if a language is Tier 1 (has static translation files).
     */
    fun isTier1Language(languageCode: String): Boolean {
        return TIER_1_LANGUAGES.contains(languageCode)
    }
    
    /**
     * Check if a language needs backend translation (Tier 2+).
     */
    fun needsBackendTranslation(languageCode: String): Boolean {
        return !isTier1Language(languageCode)
    }
    
    /**
     * Set the app language.
     * For Tier 1 languages, uses Android's per-app language API.
     * For Tier 2+, fetches translations from backend.
     * 
     * @param languageCode The language code to set, or "auto" for device language.
     * @param countryCode Optional country code for regional variants.
     */
    fun setLanguage(languageCode: String, countryCode: String? = null) {
        Log.d(TAG, "🌐 Setting language to: $languageCode (country: $countryCode)")
        
        // Save preference
        prefs.edit()
            .putString(KEY_APP_LANGUAGE, languageCode)
            .putString(KEY_APP_COUNTRY, countryCode)
            .apply()
        
        val effectiveLanguage = if (languageCode == VALUE_AUTO) getDeviceLanguage() else languageCode
        
        // Apply locale change using AppCompat for Tier 1 languages
        if (languageCode == VALUE_AUTO) {
            // Clear per-app language, use system default
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else if (isTier1Language(effectiveLanguage)) {
            // Tier 1: Use Android's built-in localization
            val localeList = LocaleListCompat.forLanguageTags(effectiveLanguage)
            AppCompatDelegate.setApplicationLocales(localeList)
            Log.d(TAG, "✅ Applied Tier 1 locale: $effectiveLanguage")
        } else {
            // Tier 2+: Fetch from backend
            Log.d(TAG, "🌐 Tier 2+ language - fetching from backend: $effectiveLanguage")
            scope.launch {
                _isLoadingTranslations.value = true
                try {
                    _translationService?.loadTranslations(effectiveLanguage, countryCode)
                    // For Tier 2+, we still set a locale for date/number formatting
                    val localeList = LocaleListCompat.forLanguageTags(effectiveLanguage)
                    AppCompatDelegate.setApplicationLocales(localeList)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to load translations: ${e.message}", e)
                } finally {
                    _isLoadingTranslations.value = false
                }
            }
        }
        
        // Update state
        _currentLanguage.value = getEffectiveLanguage()
        updateRTLState()
        
        Log.d(TAG, "🌐 Language changed to: ${_currentLanguage.value?.name}, RTL: ${_isRTL.value}")
    }
    
    /**
     * Reset to automatic (device) language.
     */
    fun resetToAuto() {
        setLanguage(VALUE_AUTO)
    }
    
    /**
     * Check if a language code represents an RTL language.
     */
    fun isRTLLanguage(languageCode: String): Boolean {
        return RTL_LANGUAGES.contains(languageCode)
    }
    
    /**
     * Update the RTL state based on current language.
     */
    private fun updateRTLState() {
        _isRTL.value = isRTLLanguage(getEffectiveLanguageCode())
    }
    
    /**
     * Get display name for current language setting.
     * Returns "Auto (Device Language)" if auto, otherwise the language name.
     */
    fun getDisplayName(): String {
        val pref = getLanguagePreference()
        if (pref == VALUE_AUTO) {
            val deviceLang = getLanguageByCode(getDeviceLanguage())
            return "Auto (${deviceLang?.name ?: getDeviceLanguage()})"
        }
        return getLanguageByCode(pref)?.name ?: pref
    }
    
    /**
     * Get the native name display for current language.
     */
    fun getNativeDisplayName(): String {
        val pref = getLanguagePreference()
        if (pref == VALUE_AUTO) {
            val deviceLang = getLanguageByCode(getDeviceLanguage())
            return deviceLang?.native ?: getDeviceLanguage()
        }
        return getLanguageByCode(pref)?.native ?: pref
    }
    
    /**
     * Get all available languages for the picker.
     * Returns the full list of 120+ languages from Languages.kt.
     */
    fun getAvailableLanguages(): List<Language> {
        return LANGUAGES
    }
    
    /**
     * Get Tier 1 languages (instant, offline support).
     */
    fun getTier1Languages(): List<Language> {
        return LANGUAGES.filter { TIER_1_LANGUAGES.contains(it.code) }
    }
    
    /**
     * Get Tier 2+ languages (fetched from backend).
     */
    fun getTier2Languages(): List<Language> {
        return LANGUAGES.filter { !TIER_1_LANGUAGES.contains(it.code) }
    }
    
    /**
     * Apply the saved language preference.
     * Call this in Application.onCreate() or MainActivity.onCreate().
     */
    fun applyLanguagePreference() {
        val pref = getLanguagePreference()
        val countryPref = getCountryPreference()
        
        if (pref != VALUE_AUTO) {
            if (isTier1Language(pref)) {
                // Tier 1: Apply locale immediately
                val localeList = LocaleListCompat.forLanguageTags(pref)
                AppCompatDelegate.setApplicationLocales(localeList)
                Log.d(TAG, "🌐 Applied saved Tier 1 preference: $pref")
            } else {
                // Tier 2+: Load cached translations or fetch
                scope.launch {
                    try {
                        _translationService?.loadTranslations(pref, countryPref)
                        val localeList = LocaleListCompat.forLanguageTags(pref)
                        AppCompatDelegate.setApplicationLocales(localeList)
                        Log.d(TAG, "🌐 Applied saved Tier 2+ preference: $pref")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load Tier 2+ translations: ${e.message}", e)
                    }
                }
            }
        }
    }
    
    /**
     * Preload translations for a language without switching to it.
     * Useful for preparing translations before user switches.
     */
    fun preloadLanguage(languageCode: String, countryCode: String? = null) {
        if (!isTier1Language(languageCode)) {
            scope.launch {
                _translationService?.preloadTranslations(languageCode, countryCode)
            }
        }
    }
}
