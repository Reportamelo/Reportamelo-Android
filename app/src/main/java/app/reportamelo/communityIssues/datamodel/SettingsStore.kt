package app.reportamelo.communityIssues.datamodel

import app.reportamelo.commons.AppPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class SettingsStore {
    companion object {
        val shared = SettingsStore()
    }

    private val _isTelemetryCollectionEnabled = MutableStateFlow(
        AppPreferences.getBoolean("enableAnonymousTelemetry", true)
    )
    val isTelemetryCollectionEnabled: StateFlow<Boolean> = _isTelemetryCollectionEnabled.asStateFlow()

    var selectedLanguageCode: String
        get() = AppPreferences.getString("selectedLanguageCode") ?: "en"
        set(value) = AppPreferences.setString("selectedLanguageCode", value)

    var commentsCount: Int
        get() = AppPreferences.getInt("commentsCount", 0)
        set(value) = AppPreferences.setInt("commentsCount", value)

    var petitionsCount: Int
        get() = AppPreferences.getInt("petitionsCount", 0)
        set(value) = AppPreferences.setInt("petitionsCount", value)

    var reportsCount: Int
        get() = AppPreferences.getInt("reportsCount", 0)
        set(value) = AppPreferences.setInt("reportsCount", value)

    var signaturesCount: Int
        get() = AppPreferences.getInt("signaturesCount", 0)
        set(value) = AppPreferences.setInt("signaturesCount", value)

    var geographicalRegion: Int
        get() = AppPreferences.getInt("geographicalRegion", 2)
        set(value) = AppPreferences.setInt("geographicalRegion", value)

    var selectedCountry: Int
        get() = AppPreferences.getInt("selectedCountry", 2)
        set(value) = AppPreferences.setInt("selectedCountry", value)

    var countryCode: String
        get() = AppPreferences.getString("countryCode") ?: "SV"
        set(value) = AppPreferences.setString("countryCode", value)

    var cityId: String
        get() = AppPreferences.getString("cityId") ?: "a67b90f9-1d76-4835-a994-03cd04f1d619"
        set(value) = AppPreferences.setString("cityId", value)

    var enableBackgroundSync: Boolean
        get() = AppPreferences.getBoolean("enableBackgroundSync", true)
        set(value) = AppPreferences.setBoolean("enableBackgroundSync", value)

    var enableAnonymousTelemetry: Boolean
        get() = AppPreferences.getBoolean("enableAnonymousTelemetry", true)
        set(value) {
            AppPreferences.setBoolean("enableAnonymousTelemetry", value)
            applyTelemetrySetting(value)
        }

    var enableNotifications: Boolean
        get() = AppPreferences.getBoolean("enableNotifications", false)
        set(value) = AppPreferences.setBoolean("enableNotifications", value)

    var enablePushNotifications: Boolean
        get() = AppPreferences.getBoolean("enablePushNotifications", false)
        set(value) = AppPreferences.setBoolean("enablePushNotifications", value)

    var enableEmailNotifications: Boolean
        get() = AppPreferences.getBoolean("enableEmailNotifications", true)
        set(value) = AppPreferences.setBoolean("enableEmailNotifications", value)

    var enableWebNotifications: Boolean
        get() = AppPreferences.getBoolean("enableWebNotifications", false)
        set(value) = AppPreferences.setBoolean("enableWebNotifications", value)

    var lastReportDate: Date? = null

    var showMyProfile: Boolean
        get() = AppPreferences.getBoolean("showMyProfile", true)
        set(value) = AppPreferences.setBoolean("showMyProfile", value)

    var showMyUseNameWhenShare: Boolean
        get() = AppPreferences.getBoolean("showMyUseNameWhenShare", true)
        set(value) = AppPreferences.setBoolean("showMyUseNameWhenShare", value)

    var saveLastLocation: Boolean
        get() = AppPreferences.getBoolean("saveLastLocation", false)
        set(value) = AppPreferences.setBoolean("saveLastLocation", value)

    var useMyCurrentLocation: Boolean
        get() = AppPreferences.getBoolean("useMyCurrentLocation", false)
        set(value) = AppPreferences.setBoolean("useMyCurrentLocation", value)

    fun applyTelemetrySetting(enabled: Boolean = enableAnonymousTelemetry) {
        _isTelemetryCollectionEnabled.value = enabled
        val context = AppPreferences.appContext ?: return
        runCatching {
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled)
        }
        runCatching {
            val crashlyticsClass = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
            val instance = crashlyticsClass.getMethod("getInstance").invoke(null)
            crashlyticsClass
                .getMethod("setCrashlyticsCollectionEnabled", Boolean::class.javaPrimitiveType)
                .invoke(instance, enabled)
        }
    }
}
