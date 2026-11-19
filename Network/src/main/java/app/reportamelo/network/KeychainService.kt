package app.reportamelo.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.reportamelo.commons.AppPreferences
import java.util.concurrent.ConcurrentHashMap

enum class TokenKey {
    MUTATION,
    QUERY
}

object KeychainService {
    private const val PREFS_FILENAME = "secure_prefs"
    private var sharedPreferences: SharedPreferences? = null

    // Tokens are only persisted through EncryptedSharedPreferences. If that store is
    // unavailable they live here for the lifetime of the process, never in plaintext on disk.
    private val memoryTokens = ConcurrentHashMap<String, String>()

    fun init(context: Context) {
        AppPreferences.init(context)
        sharedPreferences = createEncryptedPreferences(context) ?: run {
            // The file can't be decrypted (e.g. Keystore key lost after a restore): start clean.
            runCatching { context.deleteSharedPreferences(PREFS_FILENAME) }
            createEncryptedPreferences(context)
        }
        migrateLegacyTokens()
    }

    private fun createEncryptedPreferences(context: Context): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            null
        }
    }

    // Older builds mirrored tokens into the plaintext AppPreferences file; move them out.
    private fun migrateLegacyTokens() {
        for (key in TokenKey.entries) {
            val legacy = AppPreferences.getString(key.name)
            if (legacy.isNullOrEmpty()) continue
            if (getToken(key).isEmpty()) setToken(key, legacy)
            AppPreferences.remove(key.name)
        }
    }

    fun getToken(key: TokenKey): String {
        memoryTokens[key.name]?.let { return it }
        return runCatching { sharedPreferences?.getString(key.name, "") }.getOrNull() ?: ""
    }

    fun setToken(key: TokenKey, token: String) {
        memoryTokens[key.name] = token
        sharedPreferences?.edit()?.putString(key.name, token)?.apply()
    }

    fun deleteToken(key: TokenKey) {
        memoryTokens.remove(key.name)
        AppPreferences.remove(key.name)
        sharedPreferences?.edit()?.remove(key.name)?.apply()
    }

    fun clear() {
        memoryTokens.clear()
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
