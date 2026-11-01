package app.reportamelo.repository

import app.reportamelo.commons.Apis
import app.reportamelo.commons.AppPreferences
import app.reportamelo.commons.AvatarCreatedFrom
import app.reportamelo.commons.CommentForType
import app.reportamelo.commons.CountryCode
import app.reportamelo.commons.KeychainToken
import app.reportamelo.commons.LegacyKeychain
import app.reportamelo.commons.SuccessfulResult
import app.reportamelo.commons.UserType
import app.reportamelo.commons.utils.KeychainKey
import app.reportamelo.models.Comment
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.LoginWithOAuthProviderResponse
import app.reportamelo.models.PaginatedRequestQueryParams
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.network.CommentsService
import app.reportamelo.network.KeychainService
import app.reportamelo.network.TokenKey
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserAndCitiesRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        AppPreferences.clearAll()
    }

    @After
    fun tearDown() {
        AppPreferences.clearAll()
    }

    @Test
    fun `Apis constants match Mobile-App client type and v1 prefix`() {
        assertTrue(Apis.apiV1.endsWith("/v1"))
        assertEquals("Client-Type", Apis.CLIENT_TYPE_HEADER)
        assertEquals("Mobile-App", Apis.CLIENT_TYPE_ANDROID)
        assertEquals(
            "214707515100-p8u4l62dlv7120v4dmfogp7hjn74f2c8.apps.googleusercontent.com",
            Apis.GOOGLE_WEB_CLIENT_ID
        )
    }

    @Test
    fun `CitiesRepository loads local SV cities and persists selected city`() {
        val citiesResponse = CitiesRepository.loadLocalCities(CountryCode.SV)
        val cities = citiesResponse.documents
        assertNotNull(cities)
        assertTrue("Expected SV.cities.json to load cities", cities!!.isNotEmpty())
        assertEquals(14, cities.size)

        val sanSalvador = cities.first { it.thirdLevel == "San Salvador" }
        assertNull(CitiesRepository.getSavedSelectedCity())

        CitiesRepository.saveSelectedCity(sanSalvador)
        val restored = CitiesRepository.getSavedSelectedCity()
        assertNotNull(restored)
        assertEquals(sanSalvador.cityId, restored?.cityId)
        assertEquals("San Salvador", restored?.thirdLevel)
    }

    @Test
    fun `CitiesRepository filterLocal searches all 262 SV districts by thirdLevel groupingName and stateName`() {
        val byCity = CitiesRepository.filterLocal(
            countryCode = CountryCode.SV.name,
            cityName = "cusca"
        ).documents.orEmpty()
        assertEquals(
            listOf("Antiguo Cuscatlán", "Cuscatancingo", "Nuevo Cuscatlán"),
            byCity.map { it.thirdLevel }
        )
        assertFalse(byCity.any { it.thirdLevel == "Cojutepeque" })

        val byGrouping = CitiesRepository.filterLocal(
            countryCode = CountryCode.SV.name,
            groupingName = "Municipio de Cuscatlan"
        ).documents.orEmpty()
        assertEquals(16, byGrouping.size)
        assertTrue(byGrouping.any { it.thirdLevel == "Cojutepeque" })

        val byState = CitiesRepository.filterLocal(
            countryCode = CountryCode.SV.name,
            stateName = "Cuscatlan"
        ).documents.orEmpty()
        assertEquals(16, byState.size)
    }

    @Test
    fun `UserRepository session and guest state syncs across KeychainService and AppPreferences`() {
        val repo = UserRepository()
        assertFalse(repo.isSessionValid())
        assertFalse(repo.isGuestUser())
        assertFalse(repo.userHasCompletedLandingPage())

        // Simulate Guest login token save via commons KeychainService
        app.reportamelo.commons.utils.KeychainService.save("guest-session-token-123")
        assertEquals("guest-session-token-123", KeychainService.getToken(TokenKey.MUTATION))

        app.reportamelo.commons.utils.KeychainService.save(
            KeychainKey.USER_TYPE,
            UserType.GUEST.name
        )
        app.reportamelo.commons.utils.KeychainService.save(
            KeychainKey.LANDING_PAGE_COMPLETE,
            "completion:state:successfully"
        )

        assertTrue(repo.isGuestUser())
        assertTrue(repo.userHasCompletedLandingPage())

        // Also verify Guest description ("Guest") matches isGuestUser
        app.reportamelo.commons.utils.KeychainService.save(
            KeychainKey.USER_TYPE,
            UserType.GUEST.description
        )
        assertTrue(repo.isGuestUser())

        // Simulate authenticated user session
        LegacyKeychain.setToken(KeychainToken.SESSION_STATE_VERIFICATION, "session:state:valid")
        assertTrue(repo.isSessionValid())
    }

    @Test
    fun `LoginWithOAuthProviderResponse deserializes Guest payload with publicUserData and iOS enum rawValues`() {
        val rawGuestWithUserDataJson = """
            {
                "code": "GUEST_SESSION_CREATED",
                "authSessionId": "guest_session_abc123",
                "authProvider": "guest",
                "publicUserData": {
                    "profileId": "prof-guest-1",
                    "userId": "usr-guest-1",
                    "userName": "guest_123",
                    "names": "Guest",
                    "email": "",
                    "profilePicture": "",
                    "sessionDuration": 3600,
                    "userType": "guest",
                    "settings": {
                        "notifications": { "app": false, "email": false, "web": false },
                        "privacySettings": { "showMyProfile": false, "showMyUseNameWhenShare": false },
                        "avatarCreatedFrom": "optionsSelector",
                        "reportLocatorSettings": { "countryCode": "SV", "cityId": "a67b90f9-1d76-4835-a994-03cd04f1d619" }
                    },
                    "landingPageCompleted": false
                }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<LoginWithOAuthProviderResponse>(rawGuestWithUserDataJson)
        assertEquals("GUEST_SESSION_CREATED", decoded.code)
        assertEquals("guest_session_abc123", decoded.authSessionId)
        assertEquals("guest", decoded.authProvider)
        assertNotNull(decoded.publicUserData)
        assertEquals(UserType.GUEST, decoded.publicUserData?.userType)
        assertEquals(AvatarCreatedFrom.OPTIONS_SELECTOR, decoded.publicUserData?.settings?.avatarCreatedFrom)
    }

    @Test
    fun `LoginWithOAuthProviderResponse deserializes Google OAuth payload with citizen and GoogleAuth enums`() {
        val rawGoogleOAuthJson = """
            {
                "code": "TOKEN_GENERATED",
                "authSessionId": "google_session_xyz",
                "authProvider": "Google",
                "publicUserData": {
                    "profileId": "prof-citizen-1",
                    "userId": "usr-citizen-1",
                    "userName": "citizen_sv",
                    "names": "Francisco",
                    "email": "test@reportamelo.app",
                    "profilePicture": "https://development-api.reportamelo.app/avatars/1.png",
                    "sessionDuration": 86400,
                    "userType": "citizen",
                    "settings": {
                        "notifications": { "app": true, "email": true, "web": false },
                        "privacySettings": { "showMyProfile": true, "showMyUseNameWhenShare": true },
                        "avatarCreatedFrom": "GoogleAuth",
                        "reportLocatorSettings": { "countryCode": "SV", "cityId": "a67b90f9-1d76-4835-a994-03cd04f1d619" }
                    },
                    "landingPageCompleted": true
                }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<LoginWithOAuthProviderResponse>(rawGoogleOAuthJson)
        assertEquals("TOKEN_GENERATED", decoded.code)
        assertEquals("google_session_xyz", decoded.authSessionId)
        assertEquals(UserType.CITIZEN, decoded.publicUserData?.userType)
        assertEquals(AvatarCreatedFrom.GOOGLE_AUTH, decoded.publicUserData?.settings?.avatarCreatedFrom)
        assertTrue(decoded.publicUserData?.landingPageCompleted == true)
    }

    @Test
    fun `CommentsRepository listByUser delete update and post delegate to CommentsService and map responses`() = runBlocking {
        val originalService = CommentsRepository.service
        try {
            var lastQuery: PaginatedRequestQueryParams? = null
            var lastDeletedId: String? = null

            CommentsRepository.service = object : CommentsService() {
                override suspend fun listByUser(q: PaginatedRequestQueryParams): PaginatedResponse<Comment> {
                    lastQuery = q
                    return PaginatedResponse(
                        documents = listOf(
                            Comment(
                                id = "comment-1",
                                name = "tests.reportamelo",
                                userName = "tests.reportamelo",
                                profilePicture = "avatars/test.jpg",
                                profileId = "profile-1",
                                commentFor = CommentForType.REPORT,
                                resourceId = "SV-SSC-1",
                                message = "Hello how are you",
                                createdAt = "2026-09-28T00:00:00.000Z"
                            )
                        ),
                        total = 1,
                        page = q.page,
                        documentsPerPage = q.limit,
                        totalPages = 1,
                        hasNext = false,
                        hasPrev = false
                    )
                }

                override suspend fun delete(id: String): GenericResponse {
                    lastDeletedId = id
                    return GenericResponse(code = "COMMENT_DELETED", message = "Deleted")
                }

                override suspend fun update(comment: Comment): GenericResponse {
                    return GenericResponse(code = "COMMENT_UPDATED", message = "Updated")
                }

                override suspend fun post(comment: Comment, headers: Map<String, String>): GenericResponse {
                    return GenericResponse(code = "COMMENT_CREATED", message = "Created")
                }
            }

            val page = CommentsRepository.listByUser(page = 1)
            assertEquals(1, lastQuery?.page)
            assertEquals(20, lastQuery?.limit)
            assertEquals(1, page.documents?.size)
            assertEquals("Hello how are you", page.documents?.first()?.message)

            val sample = page.documents!!.first()
            assertEquals(SuccessfulResult.DONE, CommentsRepository.post(sample))
            assertEquals(SuccessfulResult.UPDATED, CommentsRepository.update(sample))
            assertEquals(SuccessfulResult.DELETED, CommentsRepository.delete("comment-1"))
            assertEquals("comment-1", lastDeletedId)
        } finally {
            CommentsRepository.service = originalService
        }
    }

    @Test
    fun `UserRepository checkAvailability and change map backend codes and BadRequest errors`() = runBlocking {
        val repo = UserRepository()
        val checkedNames = mutableListOf<String>()
        var changedName: String? = null

        repo.service = object : app.reportamelo.network.services.UserService() {
            override suspend fun checkAvailability(
                userName: String,
                headers: Map<String, String>
            ): GenericResponse {
                checkedNames.add(userName)
                return when (userName) {
                    "tests.reportamelo" -> GenericResponse(
                        code = "USER_NAME_AVAILABLE",
                        message = "Available"
                    )
                    "taken.user" -> throw app.reportamelo.network.ServiceError.BadRequest(
                        GenericResponse(code = "USER_NAME_TAKEN", message = "User name is taken")
                    )
                    else -> throw app.reportamelo.network.ServiceError.BadRequest(
                        GenericResponse(code = "USER_NAME_INVALID", message = "User name is invalid")
                    )
                }
            }

            override suspend fun change(
                userName: String,
                headers: Map<String, String>
            ): GenericResponse {
                changedName = userName
                return if (userName == "tests.reportamelo") {
                    GenericResponse(code = "USER_NAME_UPDATED", message = "Updated")
                } else {
                    throw app.reportamelo.network.ServiceError.BadRequest(
                        GenericResponse(code = "UPDATE_NAME_ERROR", message = "Could not update username")
                    )
                }
            }
        }

        var availableResult: Result<String>? = null
        repo.checkAvailability("tests.reportamelo") { availableResult = it }
        assertTrue(availableResult?.isSuccess == true)

        var takenResult: Result<String>? = null
        repo.checkAvailability("taken.user") { takenResult = it }
        assertTrue(takenResult?.exceptionOrNull() is UserError.Taken)

        var invalidResult: Result<String>? = null
        repo.checkAvailability("bad") { invalidResult = it }
        assertTrue(invalidResult?.exceptionOrNull() is UserError.InvalidUserName)

        assertEquals(listOf("tests.reportamelo", "taken.user", "bad"), checkedNames)

        var changeSuccess: Result<String>? = null
        repo.change("tests.reportamelo") { changeSuccess = it }
        assertTrue(changeSuccess?.isSuccess == true)
        assertEquals("tests.reportamelo", changedName)

        var changeError: Result<String>? = null
        repo.change("fail.user") { changeError = it }
        assertTrue(changeError?.exceptionOrNull() is UserError.ServerError)
    }

    @Test
    fun `UserRepository getPublicInformation for Google returns saved user profile and preserves googleAvatar`() {
        val repo = UserRepository()
        assertNull(repo.getPublicInformation(app.reportamelo.commons.AuthMethod.GOOGLE))

        repo.setNames("tests.reportamelo")
        LegacyKeychain.setToken(KeychainToken.EMAIL, "tests.reportamelo@gmail.com")
        repo.setGoogleAvatar("https://lh3.googleusercontent.com/a/default-user")
        repo.setAvatar("avatars/custom-monogram.jpg")

        val googleProfile = repo.getPublicInformation(app.reportamelo.commons.AuthMethod.GOOGLE)
        assertNotNull(googleProfile)
        assertEquals("tests.reportamelo", googleProfile?.username)
        assertEquals("tests.reportamelo@gmail.com", googleProfile?.email)
        assertEquals("https://lh3.googleusercontent.com/a/default-user", googleProfile?.avatar)
        assertEquals("https://lh3.googleusercontent.com/a/default-user", repo.getProfilePicture())
    }

    @Test
    fun `UserRepository sendDevice accepts DEVICE_TOKEN_UPDATED in id or code and sanitizes invalid state strings`() = runBlocking {
        val repo = UserRepository()
        var capturedPayload: app.reportamelo.models.DeviceTokenRequest? = null

        repo.service = object : app.reportamelo.network.services.UserService() {
            override suspend fun send(
                deviceToken: app.reportamelo.models.DeviceTokenRequest,
                headers: Map<String, String>
            ): GenericResponse {
                capturedPayload = deviceToken
                return GenericResponse(
                    id = "DEVICE_TOKEN_UPDATED",
                    message = "",
                    code = "Device token updated successfully."
                )
            }
        }

        val result = repo.sendDevice("0fff3cfb8d4ad5f8")
        assertEquals(SuccessfulResult.DONE, result)
        assertEquals("0fff3cfb8d4ad5f8", capturedPayload?.deviceToken)
        assertEquals(app.reportamelo.commons.DeviceType.ANDROID, capturedPayload?.platform)

        // If an invalid state string is passed, it is sanitized and never sent as "completion:state:successfully"
        repo.sendDevice("completion:state:successfully")
        assertFalse(capturedPayload?.deviceToken?.contains("completion:state") == true)
    }
}
