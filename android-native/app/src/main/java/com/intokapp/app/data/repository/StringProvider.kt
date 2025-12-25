package com.intokapp.app.data.repository

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CompositionLocal for providing StringProvider throughout the app.
 */
val LocalStringProvider = compositionLocalOf<StringProvider?> { null }

/**
 * StringProvider provides a unified interface for getting localized strings.
 * 
 * It seamlessly handles both:
 * - Tier 1 Languages: Uses Android's static string resources (strings.xml)
 * - Tier 2+ Languages: Uses dynamically fetched translations from backend
 * 
 * Usage in Composables:
 * ```
 * val stringProvider = LocalStringProvider.current
 * Text(stringProvider.getString(R.string.login_title))
 * // or with key for dynamic:
 * Text(stringProvider.getString("auth.login_title", R.string.login_title))
 * ```
 */
@Singleton
class StringProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localizationManager: LocalizationManager,
    val translationService: UITranslationService
) {
    init {
        // Connect LocalizationManager to UITranslationService
        localizationManager.setTranslationService(translationService)
    }
    
    /**
     * Get a localized string by resource ID.
     * Uses dynamic translation if available, otherwise falls back to static resource.
     * 
     * @param resId The string resource ID (e.g., R.string.login_title)
     * @param key Optional translation key for dynamic translations (e.g., "auth.login_title")
     */
    fun getString(@StringRes resId: Int, key: String? = null): String {
        // Check if we have a dynamic translation for this key
        if (key != null && !localizationManager.isTier1Language(localizationManager.getEffectiveLanguageCode())) {
            val dynamicTranslation = translationService.getString(key)
            if (dynamicTranslation != null) {
                return dynamicTranslation
            }
        }
        
        // Fall back to static resource
        return context.getString(resId)
    }
    
    /**
     * Get a localized string with format arguments.
     * 
     * @param resId The string resource ID
     * @param key Optional translation key for dynamic translations
     * @param args Format arguments
     */
    fun getString(@StringRes resId: Int, key: String?, vararg args: Any): String {
        // Check if we have a dynamic translation for this key
        if (key != null && !localizationManager.isTier1Language(localizationManager.getEffectiveLanguageCode())) {
            val dynamicTranslation = translationService.getString(key, *args)
            if (dynamicTranslation != null) {
                return dynamicTranslation
            }
        }
        
        // Fall back to static resource
        return context.getString(resId, *args)
    }
    
    /**
     * Get a localized string by key only (for purely dynamic strings).
     * Falls back to the provided default if not found.
     * 
     * @param key The translation key (e.g., "auth.login_title")
     * @param default Default value if translation not found
     */
    fun getStringByKey(key: String, default: String): String {
        return translationService.getString(key) ?: default
    }
    
    /**
     * Get a localized string by key with format arguments.
     * 
     * @param key The translation key
     * @param default Default value if translation not found
     * @param args Format arguments
     */
    fun getStringByKey(key: String, default: String, vararg args: Any): String {
        return translationService.getString(key, *args) ?: String.format(default, *args)
    }
    
    /**
     * Check if dynamic translations are currently being loaded.
     */
    fun isLoading(): Boolean {
        return translationService.isLoading.value
    }
    
    /**
     * Check if the current language is Tier 1 (using static resources).
     */
    fun isUsingStaticResources(): Boolean {
        return localizationManager.isTier1Language(localizationManager.getEffectiveLanguageCode())
    }
    
    /**
     * Get the current language code.
     */
    fun getCurrentLanguageCode(): String {
        return localizationManager.getEffectiveLanguageCode()
    }
}

/**
 * Composable function to get localized strings in Compose UI.
 * 
 * Usage:
 * ```
 * val strings = rememberStrings()
 * Text(strings.getString(R.string.login_title))
 * ```
 */
@Composable
fun rememberStrings(stringProvider: StringProvider): StringProviderState {
    val isLoading = stringProvider.translationService.isLoading.collectAsState()
    val translations = stringProvider.translationService.translations.collectAsState()
    
    return remember(isLoading.value, translations.value) {
        StringProviderState(stringProvider)
    }
}

/**
 * State wrapper for StringProvider to trigger recomposition when translations change.
 */
class StringProviderState(private val provider: StringProvider) {
    fun getString(@StringRes resId: Int, key: String? = null): String {
        return provider.getString(resId, key)
    }
    
    fun getString(@StringRes resId: Int, key: String?, vararg args: Any): String {
        return provider.getString(resId, key, *args)
    }
    
    fun getStringByKey(key: String, default: String): String {
        return provider.getStringByKey(key, default)
    }
    
    fun getStringByKey(key: String, default: String, vararg args: Any): String {
        return provider.getStringByKey(key, default, *args)
    }
    
    val isLoading: Boolean get() = provider.isLoading()
    val isUsingStaticResources: Boolean get() = provider.isUsingStaticResources()
    val currentLanguageCode: String get() = provider.getCurrentLanguageCode()
}

/**
 * Composable helper function to get a localized string.
 * 
 * This automatically handles both Tier 1 (static) and Tier 2+ (dynamic) languages.
 * Use this instead of stringResource() for full localization support.
 * 
 * @param resId The string resource ID (e.g., R.string.login_title)
 * @param key The translation key for dynamic translations (should match backend key)
 */
@Composable
fun localizedString(@StringRes resId: Int, key: String): String {
    val stringProvider = LocalStringProvider.current
    
    // If no StringProvider available, fall back to static resource
    if (stringProvider == null) {
        return stringResource(resId)
    }
    
    // Observe translation changes to trigger recomposition
    val translations by stringProvider.translationService.translations.collectAsState()
    
    // Check for dynamic translation first
    val dynamicTranslation = translations[key]
    if (dynamicTranslation != null) {
        return dynamicTranslation
    }
    
    // Fall back to static resource
    return stringResource(resId)
}

/**
 * Composable helper function to get a localized string with format arguments.
 * 
 * @param resId The string resource ID
 * @param key The translation key for dynamic translations
 * @param formatArgs Format arguments for the string
 */
@Composable
fun localizedString(@StringRes resId: Int, key: String, vararg formatArgs: Any): String {
    val stringProvider = LocalStringProvider.current
    
    // If no StringProvider available, fall back to static resource
    if (stringProvider == null) {
        return stringResource(resId, *formatArgs)
    }
    
    // Observe translation changes to trigger recomposition
    val translations by stringProvider.translationService.translations.collectAsState()
    val currentLanguage by stringProvider.translationService.currentLanguage.collectAsState()
    
    // Use remember with dependencies to recompose when translations change
    return remember(translations, currentLanguage, resId, key, formatArgs) {
        stringProvider.getString(resId, key, *formatArgs)
    }
}
