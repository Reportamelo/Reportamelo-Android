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
}
