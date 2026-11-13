package app.reportamelo.communityIssues.helpers

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Per-app language selection.
 * On Android 13+ it delegates to the system [LocaleManager] (the choice also shows up in the
 * system "App languages" screen). On older versions the choice is persisted locally and applied
 * by wrapping the base context, see [wrap].
 */
object AppLanguageManager {
    /// Empty tag means "follow the system language"
    const val SYSTEM_DEFAULT = ""

    /// Keep in sync with res/xml/locales_config.xml
    val supportedLanguageTags = listOf("en", "es-419")

    private const val PREFERENCES_NAME = "app_language"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    fun currentLanguageTag(context: Context): String {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.takeIf { !it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
                .orEmpty()
        } else {
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE_TAG, SYSTEM_DEFAULT)
                .orEmpty()
        }
        return matchSupported(tag)
    }

    /**
     * Persists and applies the language. On Android 13+ the system recreates the activity;
     * on older versions the caller has to recreate it so [wrap] picks up the new value.
     */
    fun setLanguage(context: Context, languageTag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (languageTag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(languageTag)
        } else {
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE_TAG, languageTag)
                .apply()
        }
    }

    /// Returns a context that resolves resources in the selected language (no-op on Android 13+).
    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
        val tag = currentLanguageTag(context)
        if (tag.isEmpty()) return context

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    /// Language name written in its own language, e.g. "Español (Latinoamérica)"
    fun displayName(languageTag: String): String {
        val locale = Locale.forLanguageTag(languageTag)
        return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }

    private fun matchSupported(tag: String): String {
        if (tag.isEmpty()) return SYSTEM_DEFAULT
        val language = Locale.forLanguageTag(tag).language
        return supportedLanguageTags.firstOrNull { it == tag }
            ?: supportedLanguageTags.firstOrNull { Locale.forLanguageTag(it).language == language }
            ?: SYSTEM_DEFAULT
    }
}
