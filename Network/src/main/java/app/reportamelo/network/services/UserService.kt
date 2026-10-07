package app.reportamelo.network.services

import app.reportamelo.models.*
import app.reportamelo.network.ServiceClient
import app.reportamelo.network.MultipartFormFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class UserService(private val client: ServiceClient) {

    suspend fun login(payload: OAuthSignInPayload, headers: Map<String, String>): LoginWithOAuthProviderResponse {
        return client.post("auth/Google/tokenSignInOrLogin", payload, headers)
    }

    suspend fun loginAsGuest(headers: Map<String, String>): LoginWithOAuthProviderResponse {
        return client.post("auth/guest/generate/session", emptyMap<String, String>(), headers)
    }

    suspend fun signInOrLoginWithApple(payload: AuthPayload, headers: Map<String, String>): LoginWithOAuthProviderResponse {
        return client.post("auth/Apple/tokenSignInOrLogin", payload, headers, withOAuth = true)
    }

    suspend fun refresh(headers: Map<String, String>) {
        client.get<EmptyResponse>("auth/refresh/token", headers, withOAuth = true)
    }

    suspend fun checkAvailability(userName: String, headers: Map<String, String>): GenericResponse {
        return client.post("user/check/availability", mapOf("userName" to userName), headers, withOAuth = true)
    }

    suspend fun modify(notifications: Notifications, headers: Map<String, String>): GenericResponse {
        return client.patch("user/notifications", notifications, headers, withOAuth = true)
    }

    suspend fun change(userName: String, headers: Map<String, String>): GenericResponse {
        return client.patch("user/userName", mapOf("userName" to userName), headers, withOAuth = true)
    }

    suspend fun completeLandingPage(): GenericResponse {
        return client.patch("user/landing/completed", emptyMap<String, String>(), emptyMap(), withOAuth = true)
    }

    suspend fun send(deviceToken: DeviceTokenRequest, headers: Map<String, String>): GenericResponse {
        return client.patch("user/device/token", deviceToken, headers, withOAuth = true)
    }

    suspend fun privacy(settings: PrivacySettings, headers: Map<String, String>): GenericResponse {
        return client.patch("user/privacy", settings, headers, withOAuth = true)
    }

    suspend fun defaultReportingCity(payload: DefaultReportingCity, headers: Map<String, String>): GenericResponse {
        return client.patch("user/default/city", payload, headers, withOAuth = true)
    }

    suspend fun changeAvatar(avatar: ByteArray, from: app.reportamelo.commons.AvatarCreatedFrom): CustomizedResponse<AvatarResponse> {
        val files = listOf(
            MultipartFormFile(
                name = "avatar",
                filename = "avatar.jpg",
                mimeType = "image/jpeg",
                data = avatar
            )
        )
        
        val body = mapOf(
            "avatarCreatedFrom" to from.name
        )

        return client.postMultipart(
            path = "user/change/avatar",
            body = body,
            files = files,
            headers = mapOf("Client-Type" to "Mobile-App"),
            withOAuth = true
        )
    }

    suspend fun reportUser(reason: BlockUserReason, headers: Map<String, String>): GenericResponse {
        return client.post("user/report/reason", reason, headers, withOAuth = true)
    }

    suspend fun deleteMyAccount(headers: Map<String, String>): GenericResponse {
        return client.delete("user/my-account", emptyMap<String, String>(), headers, withOAuth = true)
    }

    suspend fun logout(headers: Map<String, String>) {
        client.delete<EmptyResponse, Map<String, String>>("auth/logout", emptyMap(), headers, withOAuth = true)
    }

    suspend fun citizenProfile(id: String, headers: Map<String, String>): User {
        return client.get("user/citizen/$id", headers, withOAuth = true)
    }

    suspend fun block(reason: BlockUserReason, headers: Map<String, String>): GenericResponse {
        return client.post("user/blocked-users/", reason, headers, withOAuth = true)
    }

    suspend fun listBlockedUsers(headers: Map<String, String>): List<User> {
        return client.get("user/blocked-users/", headers, withOAuth = true)
    }

    suspend fun unblock(profileId: String, headers: Map<String, String>): GenericResponse {
        return client.delete("user/blocked-users/$profileId", emptyMap<String, String>(), headers, withOAuth = true)
    }
}
