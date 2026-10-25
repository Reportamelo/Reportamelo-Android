package app.reportamelo.commons

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

object AppPreferences {
    private const val PREFS_NAME = "reportamelo_app_preferences"
    private var sharedPreferences: SharedPreferences? = null
    private val memoryStore = ConcurrentHashMap<String, String>()

    @Volatile
    var appContext: Context? = null
        private set

    fun init(context: Context) {
        val applicationContext = context.applicationContext ?: context
        appContext = applicationContext
        sharedPreferences = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getString(key: String): String? {
        return memoryStore[key] ?: sharedPreferences?.getString(key, null)
    }

    fun setString(key: String, value: String) {
        memoryStore[key] = value
        sharedPreferences?.edit()?.putString(key, value)?.apply()
    }

    fun remove(key: String) {
        memoryStore.remove(key)
        sharedPreferences?.edit()?.remove(key)?.apply()
    }

    fun clearAll() {
        memoryStore.clear()
        sharedPreferences?.edit()?.clear()?.apply()
    }
}

enum class KeychainToken {
    EMAIL,
    NAME,
    DEVICE_ID,
    AUTH_METHOD,
    SESSION_STATE_VERIFICATION,
    USER_TYPE,
    PROFILE_ID,
    MUTATION,
    QUERY,
    USER_ID,
    PLAN_TYPE,
    LANDING_PAGE_COMPLETE
}

object LegacyKeychain {
    fun getToken(token: KeychainToken): String? = AppPreferences.getString(token.name)
    fun setToken(token: KeychainToken, value: String) = AppPreferences.setString(token.name, value)
    fun deleteToken(token: KeychainToken) = AppPreferences.remove(token.name)
}
