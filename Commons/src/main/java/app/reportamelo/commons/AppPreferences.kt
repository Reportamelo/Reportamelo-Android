package app.reportamelo.commons


object AppPreferences {
    fun getString(key: String): String? = null
    fun setString(key: String, value: String) {}
}

enum class KeychainToken {
    EMAIL,
    NAME,
    DEVICE_ID,
    AUTH_METHOD,
    SESSION_STATE_VERIFICATION,
    USER_TYPE,
    PROFILE_ID
}

// Temporary shim to fix UserRepository
object LegacyKeychain {
    fun getToken(token: KeychainToken): String? = null
}
