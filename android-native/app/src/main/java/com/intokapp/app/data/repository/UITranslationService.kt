package com.intokapp.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intokapp.app.data.network.ApiService
import com.intokapp.app.data.network.TranslateUIStringsRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UITranslationService handles fetching and caching UI translations from the backend.
 * 
 * Architecture (Hybrid Approach per i18n-implementation-plan.md):
 * - Tier 1 Languages (top 20): Use static strings.xml files bundled in app
 * - Tier 2+ Languages (100+): Fetch from backend API and cache locally
 * 
 * Flow:
 * 1. Check if language has static translation (Tier 1) → Use Android resources
 * 2. Check local cache for fetched translation → Use cached if fresh
 * 3. Fetch from backend API → Cache locally → Apply
 */
@Singleton
class UITranslationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "UITranslationService"
        private const val PREFS_NAME = "ui_translations_cache"
        private const val KEY_TRANSLATIONS_PREFIX = "translations_"
        private const val KEY_VERSION_PREFIX = "version_"
        private const val KEY_TIMESTAMP_PREFIX = "timestamp_"
        private const val CACHE_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
        
        // Tier 1 languages with static strings.xml files
        // These are bundled in the app and don't need backend fetch
        val TIER_1_LANGUAGES = setOf(
            "en", "es", "fr", "de", "pt", "ja", "ko", "ar", "zh"
        )
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _translations = MutableStateFlow<Map<String, String>>(emptyMap())
    val translations: StateFlow<Map<String, String>> = _translations
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage
    
    private var currentVersion: String? = null
    
    /**
     * Check if a language uses static bundled translations (Tier 1)
     */
    fun isTier1Language(languageCode: String): Boolean {
        return TIER_1_LANGUAGES.contains(languageCode)
    }
    
    /**
     * Load translations for a language.
     * For Tier 1 languages, this is a no-op (use Android's string resources).
     * For Tier 2+, this fetches from backend and caches locally.
     */
    suspend fun loadTranslations(languageCode: String, countryCode: String? = null): Result<Map<String, String>> {
        Log.d(TAG, "🌐 Loading translations for: $languageCode")
        
        // Tier 1 languages use static resources
        if (isTier1Language(languageCode)) {
            Log.d(TAG, "✅ $languageCode is Tier 1 - using static strings.xml")
            _currentLanguage.value = languageCode
            _translations.value = emptyMap() // Empty means use static resources
            return Result.success(emptyMap())
        }
        
        // Tier 2+ - check cache first
        val cachedTranslations = getCachedTranslations(languageCode)
        if (cachedTranslations != null) {
            Log.d(TAG, "✅ Found cached translations for $languageCode (${cachedTranslations.size} strings)")
            _currentLanguage.value = languageCode
            _translations.value = cachedTranslations
            return Result.success(cachedTranslations)
        }
        
        // Fetch from backend
        return fetchAndCacheTranslations(languageCode, countryCode)
    }
    
    /**
     * Fetch translations from backend API and cache locally
     */
    suspend fun fetchAndCacheTranslations(
        languageCode: String,
        countryCode: String? = null,
        forceRefresh: Boolean = false
    ): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        
        try {
            Log.d(TAG, "🌐 Fetching translations from backend for: $languageCode")
            
            val response = apiService.translateUIStrings(
                TranslateUIStringsRequest(
                    targetLanguage = languageCode,
                    targetCountry = countryCode,
                    forceRefresh = forceRefresh
                )
            )
            
            val translations = response.translations
            Log.d(TAG, "✅ Received ${translations.size} translations (cached: ${response.cached})")
            
            // Cache locally
            cacheTranslations(languageCode, translations, response.version)
            
            _currentLanguage.value = languageCode
            _translations.value = translations
            currentVersion = response.version
            
            Result.success(translations)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch translations: ${e.message}", e)
            
            // Try to use stale cache if available
            val staleCache = getCachedTranslations(languageCode, ignoreExpiry = true)
            if (staleCache != null) {
                Log.w(TAG, "⚠️ Using stale cache for $languageCode")
                _translations.value = staleCache
                return@withContext Result.success(staleCache)
            }
            
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Get a translated string by key.
     * Returns null if not found (caller should fall back to static resources).
     */
    fun getString(key: String): String? {
        return _translations.value[key]
    }
    
    /**
     * Get a translated string with format arguments.
     */
    fun getString(key: String, vararg args: Any): String? {
        val template = _translations.value[key] ?: return null
        return try {
            String.format(template, *args)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to format string '$key': ${e.message}")
            template
        }
    }
    
    /**
     * Cache translations locally
     */
    private fun cacheTranslations(languageCode: String, translations: Map<String, String>, version: String) {
        try {
            val json = gson.toJson(translations)
            prefs.edit()
                .putString(KEY_TRANSLATIONS_PREFIX + languageCode, json)
                .putString(KEY_VERSION_PREFIX + languageCode, version)
                .putLong(KEY_TIMESTAMP_PREFIX + languageCode, System.currentTimeMillis())
                .apply()
            Log.d(TAG, "💾 Cached ${translations.size} strings for $languageCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache translations: ${e.message}", e)
        }
    }
    
    /**
     * Get cached translations if available and not expired
     */
    private fun getCachedTranslations(languageCode: String, ignoreExpiry: Boolean = false): Map<String, String>? {
        val json = prefs.getString(KEY_TRANSLATIONS_PREFIX + languageCode, null) ?: return null
        val timestamp = prefs.getLong(KEY_TIMESTAMP_PREFIX + languageCode, 0)
        
        // Check expiry
        if (!ignoreExpiry && System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS) {
            Log.d(TAG, "📦 Cache expired for $languageCode")
            return null
        }
        
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cached translations: ${e.message}", e)
            null
        }
    }
    
    /**
     * Clear cache for a specific language
     */
    fun clearCache(languageCode: String) {
        prefs.edit()
            .remove(KEY_TRANSLATIONS_PREFIX + languageCode)
            .remove(KEY_VERSION_PREFIX + languageCode)
            .remove(KEY_TIMESTAMP_PREFIX + languageCode)
            .apply()
        Log.d(TAG, "🗑️ Cleared cache for $languageCode")
    }
    
    /**
     * Clear all cached translations
     */
    fun clearAllCache() {
        prefs.edit().clear().apply()
        _translations.value = emptyMap()
        Log.d(TAG, "🗑️ Cleared all translation cache")
    }
    
    /**
     * Get the cached version for a language
     */
    fun getCachedVersion(languageCode: String): String? {
        return prefs.getString(KEY_VERSION_PREFIX + languageCode, null)
    }
    
    /**
     * Check if translations are loaded for a language
     */
    fun hasTranslations(languageCode: String): Boolean {
        return isTier1Language(languageCode) || getCachedTranslations(languageCode) != null
    }
    
    /**
     * Preload translations for a language in background
     */
    suspend fun preloadTranslations(languageCode: String, countryCode: String? = null) {
        if (!isTier1Language(languageCode) && !hasTranslations(languageCode)) {
            Log.d(TAG, "📥 Preloading translations for $languageCode")
            fetchAndCacheTranslations(languageCode, countryCode)
        }
    }
}
