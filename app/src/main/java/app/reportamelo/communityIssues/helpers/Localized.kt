package app.reportamelo.communityIssues.helpers

import androidx.annotation.StringRes
import app.reportamelo.commons.AppPreferences

/**
 * Resolves a string resource outside of a composable (view models, callbacks, coroutines).
 * Falls back to the English [fallback] when no application context is available (e.g. unit tests).
 */
fun localized(@StringRes id: Int, fallback: String, vararg args: Any): String {
    val formattedFallback = if (args.isEmpty()) fallback else String.format(fallback, *args)
    val context = AppPreferences.appContext?.let { runCatching { AppLanguageManager.wrap(it) }.getOrDefault(it) }
        ?: return formattedFallback
    return runCatching { context.getString(id, *args) as String? }.getOrNull() ?: formattedFallback
}
