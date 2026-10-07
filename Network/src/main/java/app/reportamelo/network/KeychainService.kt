package app.reportamelo.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class TokenKey {
    MUTATION,
    QUERY
}

object KeychainService {
    private const val PREFS_FILENAME = "secure_prefs"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
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
    }

    fun getToken(key: TokenKey): String {
        return sharedPreferences?.getString(key.name, "") ?: ""
    }

    fun setToken(key: TokenKey, token: String) {
        sharedPreferences?.edit()?.putString(key.name, token)?.apply()
    }
    
    fun getArray(key: String): List<String> {
        val stringSet = sharedPreferences?.getStringSet(key, emptySet()) ?: emptySet()
        return stringSet.toList()
    }
    
    fun setArray(key: String, values: List<String>) {
        sharedPreferences?.edit()?.putStringSet(key, values.toSet())?.apply()
    }
}
