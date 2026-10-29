package app.reportamelo.commons.utils

import app.reportamelo.commons.AppPreferences

object SubscriptionManager {
    var mockIsPro: Boolean = false

    fun checkEntitlement() {
        if (mockIsPro) {
            KeychainService.save(KeychainKey.PLAN_TYPE, "pro")
        }
    }
}

enum class KeychainToken {
    PLAN_TYPE,
    EMAIL,
    NAME,
    AUTH_METHOD,
    MUTATION,
    USER_ID,
    PROFILE_ID,
    USER_TYPE,
    SESSION_STATE_VERIFICATION,
    QUERY,
    DEVICE_ID,
    LANDING_PAGE_COMPLETE
}

object KeychainService {
    fun getToken(token: KeychainToken): String = AppPreferences.getString(token.name) ?: ""
    fun getToken(key: KeychainKey): String = AppPreferences.getString(key.name) ?: ""
    fun save(key: KeychainKey, value: String) {
        AppPreferences.setString(key.name, value)
    }
    fun save(value: String) {
        AppPreferences.setString(KeychainKey.MUTATION.name, value)
    }
    fun deleteToken(key: KeychainKey) {
        AppPreferences.remove(key.name)
    }
}

enum class KeychainKey {
    EMAIL,
    NAME,
    AUTH_METHOD,
    MUTATION,
    USER_ID,
    PROFILE_ID,
    USER_TYPE,
    PLAN_TYPE,
    SESSION_STATE_VERIFICATION,
    QUERY,
    DEVICE_ID,
    LANDING_PAGE_COMPLETE
}

object KeychainServiceMock {
    fun getToken(token: KeychainToken): String = KeychainService.getToken(token)
    fun save(key: KeychainKey, value: String) = KeychainService.save(key, value)
    fun save(value: String) = KeychainService.save(value)
    fun deleteToken(key: KeychainKey) = KeychainService.deleteToken(key)
}
