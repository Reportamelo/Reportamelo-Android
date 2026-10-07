package app.reportamelo.repository

import android.graphics.Bitmap
import android.net.Uri
import app.reportamelo.network.services.*
import app.reportamelo.network.*
import app.reportamelo.commons.*
import app.reportamelo.models.*

import java.io.ByteArrayOutputStream

enum class UserOAuthResultState {
    FIRST_LOGIN,
    EXISTING,
    DISABLED,
    INEXISTENT,
    UNOWNED
}

sealed class NetworkError : Exception() {
    object NoNetwork : NetworkError()
    object NoData : NetworkError()
    object Unavailable : NetworkError()
    object Unknown : NetworkError()
}

sealed class UserError : Exception() {
    object InvalidUserName : UserError()
    object TooLarge : UserError()
    data class UnknownError(override val message: String) : UserError()
    object Taken : UserError()
    data class ServerError(override val message: String) : UserError()
}

sealed class ImageError : Exception() {
    object InvalidData : ImageError()
    data class TooLarge(override val message: String) : ImageError()
    data class UnknownError(override val message: String) : ImageError()
}

sealed class ReportError : Exception() {
    object NoIdentifier : ReportError()
    object NoCityIdentifier : ReportError()
}

class UserRepository private constructor() {

    companion object {
        val shared = UserRepository()
    }

    var service: UserService = UserService()
    var headers: Map<String, String> = mapOf(
        "Client-Type" to "Mobile-App",
        "CountryCode" to "SV",
        "CityId" to "a67b90f9-1d76-4835-a994-03cd04f1d619"
    )

    suspend fun login(token: String): Triple<UserOAuthResultState, String, PublicUserData> {
        return try {
            val result = service.login(OAuthSignInPayload(token), headers)
            when (result.code) {
                "TOKEN_GENERATED" -> Triple(UserOAuthResultState.EXISTING, result.authSessionId, result.publicUserData)
                "USER_CREATED_WITH_TOKEN" -> Triple(UserOAuthResultState.FIRST_LOGIN, result.authSessionId, result.publicUserData)
                else -> throw CommonIntercommunicationErrors.InvalidPetition(result.code)
            }
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.Forbidden(e.response)
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun signInOrLoginWithApple(payload: AuthPayload): Triple<UserOAuthResultState, String, PublicUserData> {
        return try {
            val result = service.signInOrLoginWithApple(payload, headers)
            when (result.code) {
                "TOKEN_GENERATED" -> Triple(UserOAuthResultState.EXISTING, result.authSessionId, result.publicUserData)
                "USER_CREATED_WITH_TOKEN" -> Triple(UserOAuthResultState.FIRST_LOGIN, result.authSessionId, result.publicUserData)
                else -> throw CommonIntercommunicationErrors.InvalidPetition(result.code)
            }
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.Forbidden(e.response)
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun loginAsGuest(): Pair<UserOAuthResultState, String> {
        return try {
            val result = service.loginAsGuest(headers)
            if (result.code == "GUEST_SESSION_CREATED") {
                Pair(UserOAuthResultState.INEXISTENT, result.authSessionId)
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    fun getProfilePicture(): String {
        return UserDefaults.standard.getString("avatar_url") ?: ""
    }

    fun getProfilePictureURL(): Uri? {
        val user = GIDSignIn.sharedInstance.currentUser
        val profile = user?.profile
        if (profile?.hasImage != true) {
            return null
        }
        return profile.imageURL(withDimension = 200)
    }

    fun getNames(): String {
        return UserDefaults.standard.getString("names") ?: "Guest"
    }

    fun getName(): String {
        val user = GIDSignIn.sharedInstance.currentUser
        val profile = user?.profile ?: return getNames()
        return profile.name
    }

    fun setNames(names: String) {
        UserDefaults.standard.setString("names", names)
    }

    fun getUsername(): String {
        return UserDefaults.standard.getString("user_name") ?: "guest"
    }

    fun setUsername(username: String) {
        UserDefaults.standard.setString("user_name", username)
    }

    fun setAvatar(url: String) {
        UserDefaults.standard.setString("avatar_url", url)
    }

    fun getPublicInformation(authMethod: AuthMethod): UserProfile? {
        if (authMethod == AuthMethod.GOOGLE) {
            val user = GIDSignIn.sharedInstance.currentUser
            val profile = user?.profile
            val email = profile?.email
            val username = profile?.name

            if (user == null || profile == null || email == null || username == null || !profile.hasImage) {
                return null
            }

            return UserProfile(
                username = username,
                avatar = profile.imageURL(withDimension = 200).toString(),
                email = email,
                profileId = ""
            )
        }

        if (authMethod == AuthMethod.APPLE) {
            val email = KeychainService.getToken(KeychainToken.EMAIL)
            val name = KeychainService.getToken(KeychainToken.NAME)

            return UserProfile(
                username = name,
                avatar = "https://development-api.reportamelo.app/avatars/1b11fcde-1fe1-1cca-11e1-111111111111.png",
                email = email,
                profileId = ""
            )
        }

        return null
    }

    fun getAvatar(): Uri? {
        val urlStr = UserDefaults.standard.getString("avatar_url")
        if (urlStr.isNullOrEmpty()) return null
        if (urlStr.startsWith("http")) {
            return Uri.parse(urlStr)
        }
        return getURL(from = urlStr)
    }

    suspend fun changeAvatar(image: Bitmap, from: AvatarCreatedFrom): String {
        val targetSize = image.downscaled(250, 250)
        val imageData = targetSize.jpegData(85) 
            ?: throw ImageError.UnknownError("Failed to get image data")

        val result = service.changeAvatar(imageData, from)

        if (result.code == "AVATAR_UPDATED") {
            val avatarUrl = result.data.avatarUrl
            setAvatar(url = avatarUrl)
            return avatarUrl
        } else {
            throw ImageError.UnknownError(result.message)
        }
    }

    suspend fun change(userName: String, completion: (Result<String>) -> Unit) {
        try {
            val result = service.change(userName, headers)
            if (result.code == "USER_NAME_UPDATED") {
                completion(Result.success(result.message))
            }
        } catch (genericResponse: ServiceError.BadRequest) {
            when (genericResponse.code) {
                "UPDATE_NAME_ERROR" -> completion(Result.failure(UserError.ServerError(genericResponse.message)))
                else -> completion(Result.failure(UserError.UnknownError(genericResponse.message)))
            }
        } catch (e: Exception) {
            completion(Result.failure(UserError.UnknownError(e.localizedMessage ?: "Unknown error")))
        }
    }

    suspend fun checkAvailability(userName: String, completion: (Result<String>) -> Unit) {
        try {
            val result = service.checkAvailability(userName, headers)
            if (result.code == "USER_NAME_AVAILABLE") {
                completion(Result.success(result.message))
            }
        } catch (genericResponse: ServiceError.BadRequest) {
            when (genericResponse.code) {
                "USER_NAME_TAKEN" -> completion(Result.failure(UserError.Taken))
                "USER_NAME_INVALID" -> completion(Result.failure(UserError.InvalidUserName))
                else -> completion(Result.failure(UserError.UnknownError(genericResponse.message)))
            }
        } catch (e: Exception) {
            completion(Result.failure(UserError.UnknownError(e.localizedMessage ?: "Unknown error")))
        }
    }

    suspend fun modify(notifications: Notifications): Result<String> {
        try {
            val result = service.modify(notifications, headers)
            if (result.code == "NOTIFICATIONS_UPDATED") {
                return Result.success(result.message)
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.message)
            }
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun completeLandingPage(): SuccessfulResult {
        try {
            val result = service.completeLandingPage()
            if (result.code == "LANDING_COMPLETED_UPDATED") {
                return SuccessfulResult.DONE
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.message)
            }
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun sendDevice(token: String): SuccessfulResult {
        try {
            val deviceId = DeviceService.shared.getDeviceId()
            val deviceToken = DeviceTokenRequest(
                deviceToken = token,
                deviceId = deviceId,
                platform = DeviceType.ANDROID
            )

            val result = service.send(deviceToken, headers)
            if (result.code == "DEVICE_TOKEN_UPDATED") {
                return SuccessfulResult.DONE
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    fun userHasCompletedLandingPage(): Boolean {
        val token = KeychainService.getToken(KeychainToken.DEVICE_ID)
        return token.contains("completion:state:successfully")
    }

    fun getAuthMethod(): AuthMethod? {
        val token = KeychainService.getToken(KeychainToken.AUTH_METHOD)
        if (token.isEmpty()) return null
        return AuthMethod.valueOf(token)
    }

    fun isSessionValid(): Boolean {
        val token = KeychainService.getToken(KeychainToken.SESSION_STATE_VERIFICATION)
        return token.isNotEmpty() && token.contains("session:state:valid")
    }

    fun isGuestUser(): Boolean {
        val token = KeychainService.getToken(KeychainToken.USER_TYPE)
        return token == UserType.GUEST.name
    }

    fun isOwnProfile(profileId: String): Boolean {
        val storedProfileId = KeychainService.getToken(KeychainToken.PROFILE_ID)
        return storedProfileId.isNotEmpty() && storedProfileId == profileId
    }

    suspend fun privacy(settings: PrivacySettings) {
        try {
            val result = service.privacy(settings, headers)
            if (result.code != "PRIVACY_SETTINGS_UPDATED") {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun updateDefaultReporting(cityId: String): SuccessfulResult {
        try {
            val result = service.defaultReportingCity(DefaultReportingCity(cityId), headers)
            if (result.code == "DEFAULT_CITY_UPDATED") {
                return SuccessfulResult.UPDATED
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun reportUser(reason: BlockUserReason): SuccessfulResult {
        try {
            val result = service.reportUser(reason, headers)
            if (result.code == "USER_REPORTED") {
                return SuccessfulResult.UPDATED
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun deleteMyAccount(): SuccessfulResult {
        try {
            val result = service.deleteMyAccount(headers)
            if (result.code == "ACCOUNT_DELETED") {
                return SuccessfulResult.UPDATED
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun logout(): SuccessfulResult {
        try {
            service.logout(headers)
            return SuccessfulResult.DELETED
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    fun signOutFromGoogle() {
        GIDSignIn.sharedInstance.signOut()
    }

    suspend fun refresh() {
        try {
            service.refresh(headers)
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun citizenProfile(id: String): User {
        return try {
            service.citizenProfile(id, headers)
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun block(reason: BlockUserReason) {
        try {
            service.block(reason, headers)
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun unlock(profileId: String) {
        try {
            service.unblock(profileId, headers)
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Unknown error")
        }
    }
}

fun Bitmap.downscaled(targetWidth: Int, targetHeight: Int): Bitmap {
    val aspectWidth = targetWidth.toFloat() / width
    val aspectHeight = targetHeight.toFloat() / height
    val scaleFactor = minOf(aspectWidth, aspectHeight)

    val scaledWidth = (width * scaleFactor).toInt()
    val scaledHeight = (height * scaleFactor).toInt()

    return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
}

fun Bitmap.jpegData(compressionQuality: Int): ByteArray? {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)
    return outputStream.toByteArray()
}
