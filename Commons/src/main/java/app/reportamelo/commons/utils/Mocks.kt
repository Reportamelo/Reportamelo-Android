package app.reportamelo.commons.utils

import java.util.Date

object SubscriptionManager {
    fun checkEntitlement() {}
}
enum class KeychainToken { PLAN_TYPE }
object KeychainService {
    fun getToken(token: KeychainToken): String = ""
    fun save(key: KeychainKey, value: String) {}
    fun save(value: String) {}
    fun deleteToken(key: KeychainKey) {}
}


enum class KeychainKey { EMAIL, NAME, AUTH_METHOD, MUTATION, USER_ID, PROFILE_ID, USER_TYPE, PLAN_TYPE, SESSION_STATE_VERIFICATION, QUERY, DEVICE_ID, LANDING_PAGE_COMPLETE }

object KeychainServiceMock {
    fun getToken(token: KeychainToken): String = ""
    fun save(key: KeychainKey, value: String) {}
    fun save(value: String) {}
    fun deleteToken(key: KeychainKey) {}
}
