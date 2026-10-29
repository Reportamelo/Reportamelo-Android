package app.reportamelo.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.reportamelo.commons.AppPreferences

enum class TokenKey {
    MUTATION,
    QUERY
}

object KeychainService {
    private const val PREFS_FILENAME = "secure_prefs"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        AppPreferences.init(context)
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback to AppPreferences if EncryptedSharedPreferences fails
        }
    }

    fun getToken(key: TokenKey): String {
        val fromAppPrefs = AppPreferences.getString(key.name)
        if (!fromAppPrefs.isNullOrEmpty()) return fromAppPrefs
        return sharedPreferences?.getString(key.name, "") ?: ""
    }

    fun setToken(key: TokenKey, token: String) {
        AppPreferences.setString(key.name, token)
        sharedPreferences?.edit()?.putString(key.name, token)?.apply()
    }

    fun deleteToken(key: TokenKey) {
        AppPreferences.remove(key.name)
        sharedPreferences?.edit()?.remove(key.name)?.apply()
    }

    fun clear() {
        AppPreferences.clearAll()
        sharedPreferences?.edit()?.clear()?.apply()
    }
    
    fun getArray(key: String): List<String> {
        val stringSet = sharedPreferences?.getStringSet(key, emptySet()) ?: emptySet()
        return stringSet.toList()
    }
    
    fun setArray(key: String, values: List<String>) {
        sharedPreferences?.edit()?.putStringSet(key, values.toSet())?.apply()
    }

    fun removeFromArray(key: String, element: String) {
        val updated = getArray(key).filterNot { it == element }
        setArray(key, updated)
    }
}
