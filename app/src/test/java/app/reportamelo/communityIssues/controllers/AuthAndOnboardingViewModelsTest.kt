package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.AppPreferences
import app.reportamelo.commons.AuthMethod
import app.reportamelo.commons.CountryCode
import app.reportamelo.commons.LoginType
import app.reportamelo.commons.UserType
import app.reportamelo.commons.utils.KeychainKey
import app.reportamelo.commons.utils.KeychainService
import app.reportamelo.communityIssues.datamodel.AuthViewModel
import app.reportamelo.communityIssues.datamodel.CoordinateSpan
import app.reportamelo.communityIssues.datamodel.MapCameraRegion
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.datamodel.createdDate
import app.reportamelo.communityIssues.datamodel.issueType
import app.reportamelo.communityIssues.datamodel.reportedDate
import app.reportamelo.communityIssues.datamodel.severity
import app.reportamelo.communityIssues.datamodel.status
import app.reportamelo.communityIssues.datamodel.updatedDate
import app.reportamelo.communityIssues.ui.screens.modals.disableCellButton
import app.reportamelo.models.AuthPayload
import app.reportamelo.models.Coordinate
import app.reportamelo.repository.CitiesRepository
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthAndOnboardingViewModelsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AppPreferences.clearAll()
    }

    @After
    fun tearDown() {
        AppPreferences.clearAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `LoginViewModel loginWithGoogle with idToken emits Google AuthPayload and LoginType`() = runTest {
        val viewModel = LoginViewModel()
        var receivedPayload: AuthPayload? = null
        var receivedType: LoginType? = null

        viewModel.loginWithGoogle("google-id-token-xyz") { payload, type ->
            receivedPayload = payload
            receivedType = type
        }

        assertNotNull(receivedPayload)
        assertEquals("google-id-token-xyz", receivedPayload?.token)
        assertEquals(LoginType.User(AuthMethod.GOOGLE), receivedType)
        assertTrue(viewModel.disableLoginButtons.value)
    }

    @Test
    fun `LoginViewModel loginWithGoogle with blank token shows error alert`() = runTest {
        val viewModel = LoginViewModel()
        var callbackInvoked = false

        viewModel.loginWithGoogle("   ") { _, _ ->
            callbackInvoked = true
        }

        assertFalse(callbackInvoked)
        assertTrue(viewModel.showErrorAlert.value)
        assertTrue(viewModel.messageError.value.isNotEmpty())

        viewModel.dismissError()
        assertFalse(viewModel.showErrorAlert.value)
    }

    @Test
    fun `LandingViewModel Guest login and onboarding flow persists state and restores on checkStatus`() = runTest {
        val authViewModel = AuthViewModel()
        val landingViewModel = LandingViewModel(UserRepository())

        // Step 1: Guest login received
        landingViewModel.handleLogin(
            payload = AuthPayload(token = "guest-session-001"),
            type = LoginType.Guest,
            appState = authViewModel
        )

        assertTrue(landingViewModel.isGuest.value)
        assertNull(authViewModel.selectedCity.value)
        assertEquals("guest-session-001", KeychainService.getToken(KeychainKey.MUTATION))
        assertEquals(UserType.GUEST.description, KeychainService.getToken(KeychainKey.USER_TYPE))
        assertTrue(UserRepository.shared.isGuestUser())

        // Step 2: Select a city from SV.cities.json and complete Guest onboarding
        val cities = CitiesRepository.loadLocalCities(CountryCode.SV).documents!!
        val santaTecla = cities.first { it.thirdLevel.contains("Santa Tecla") }
        landingViewModel.setSelectedCity(santaTecla)
        landingViewModel.completeGuestOnboarding(authViewModel)

        assertNotNull(authViewModel.selectedCity.value)
        assertEquals(santaTecla.cityId, authViewModel.selectedCity.value?.cityId)
        assertEquals(
            "completion:state:successfully",
            KeychainService.getToken(KeychainKey.LANDING_PAGE_COMPLETE)
        )

        // Step 3: Simulate fresh app launch and verify checkStatus restores Guest session + city
        val freshAuthViewModel = AuthViewModel()
        val freshLandingViewModel = LandingViewModel(UserRepository())
        freshLandingViewModel.checkStatus(freshAuthViewModel)

        assertTrue(freshLandingViewModel.isGuest.value)
        assertNotNull(freshAuthViewModel.selectedCity.value)
        assertEquals(santaTecla.cityId, freshAuthViewModel.selectedCity.value?.cityId)
    }

    @Test
    fun `CitySelectionViewModel loads local cities on init and filters offline by city and department`() = runTest {
        val viewModel = CitySelectionViewModel(countryCode = CountryCode.SV)
        assertTrue(viewModel.cities.isNotEmpty())
        assertEquals(14, viewModel.cities.size)

        // Filter by city name (with accent insensitivity)
        viewModel.updateSearchText("Cojutepeque")
        assertTrue(viewModel.cities.any { it.thirdLevel.contains("Cojutepeque", ignoreCase = true) })

        // Filter by "cusca" in CITY mode returns only districts whose thirdLevel matches (Antiguo Cuscatlán, Cuscatancingo, Nuevo Cuscatlán)
        viewModel.updateSearchText("cusca")
        assertEquals(
            listOf("Antiguo Cuscatlán", "Cuscatancingo", "Nuevo Cuscatlán"),
            viewModel.cities.map { it.thirdLevel }
        )

        // Switch filter to LEGAL (Municipality) sets "Municipio de " and filters by groupingName
        viewModel.handleSearchOptionsSelectionChange(CityFilter.CITY, CityFilter.LEGAL)
        assertEquals("Municipio de ", viewModel.state.value.searchText)
        assertTrue(viewModel.cities.isNotEmpty())

        // Switch filter to STATE (Department)
        viewModel.handleSearchOptionsSelectionChange(CityFilter.LEGAL, CityFilter.STATE)
        viewModel.updateSearchText("San Miguel")
        assertTrue(viewModel.cities.isNotEmpty())
        assertTrue(viewModel.cities.all { it.secondLevel.contains("San Miguel", ignoreCase = true) })

        // Clearing search restores full local list
        viewModel.updateSearchText("")
        assertEquals(14, viewModel.cities.size)
    }

    @Test
    fun `AuthViewModel initializes with default cameraPosition center and span from iOS and updates on city selection`() = runTest {
        val authViewModel = AuthViewModel()
        val initialCamera = authViewModel.cameraPosition.value

        assertEquals(MapCameraRegion.DEFAULT_LATITUDE, initialCamera.center.lat, 1e-9)
        assertEquals(MapCameraRegion.DEFAULT_LONGITUDE, initialCamera.center.lng, 1e-9)
        assertEquals(CoordinateSpan.DEFAULT_LATITUDE_DELTA, initialCamera.span.latitudeDelta, 1e-9)
        assertEquals(CoordinateSpan.DEFAULT_LONGITUDE_DELTA, initialCamera.span.longitudeDelta, 1e-9)

        val cities = CitiesRepository.loadLocalCities(CountryCode.SV).documents!!
        val sanSalvador = cities.first { it.thirdLevel.contains("San Salvador") }

        authViewModel.setSelectedCity(sanSalvador)
        authViewModel.setCameraPosition(
            to = sanSalvador.coordinates,
            latitudeDelta = CoordinateSpan.CITY_SELECTION_LATITUDE_DELTA,
            longitudeDelta = CoordinateSpan.CITY_SELECTION_LONGITUDE_DELTA
        )

        val updatedCamera = authViewModel.cameraPosition.value
        assertEquals(sanSalvador.coordinates.lat, updatedCamera.center.lat, 1e-7)
        assertEquals(sanSalvador.coordinates.lng, updatedCamera.center.lng, 1e-7)
        assertEquals(CoordinateSpan.CITY_SELECTION_LATITUDE_DELTA, updatedCamera.span.latitudeDelta, 1e-9)
        assertEquals(CoordinateSpan.CITY_SELECTION_LONGITUDE_DELTA, updatedCamera.span.longitudeDelta, 1e-9)
        assertTrue(updatedCamera.toZoomLevel() > 14f)
    }

    @Test
    fun `AuthViewModel setupInitialCameraPosition respects saveLastLocation and persisted span deltas`() = runTest {
        val cities = CitiesRepository.loadLocalCities(CountryCode.SV).documents!!
        val santaAna = cities.first { it.thirdLevel.contains("Santa Ana") }
        CitiesRepository.saveSelectedCity(santaAna)

        val settings = SettingsStore()
        settings.saveLastLocation = true

        val tempAuth = AuthViewModel()
        tempAuth.updateLastLocation(latitude = 13.9942, longitude = -89.5597)
        tempAuth.updateCameraSpan(latitudeDelta = 0.012, longitudeDelta = 0.011)

        val restoredAuth = AuthViewModel()
        val restoredRegion = restoredAuth.cameraPosition.value

        assertEquals(13.9942, restoredRegion.center.lat, 1e-6)
        assertEquals(-89.5597, restoredRegion.center.lng, 1e-6)
        assertEquals(0.012, restoredRegion.span.latitudeDelta, 1e-6)
        assertEquals(0.011, restoredRegion.span.longitudeDelta, 1e-6)
    }

    @Test
    fun `MapExplorerViewModel centerMapOnUserLocation and performSearch update AuthViewModel cameraPosition`() = runTest {
        val authViewModel = AuthViewModel()
        val mapExplorerViewModel = MapExplorerViewModel()
        mapExplorerViewModel.authViewModel = authViewModel

        val customUserCoord = Coordinate(lat = 13.7012, lng = -89.2244)
        mapExplorerViewModel.centerMapOnUserLocation(customUserCoord)

        assertTrue(mapExplorerViewModel.hasCenteredOnUser.value)
        assertEquals(13.7012, authViewModel.cameraPosition.value.center.lat, 1e-6)
        assertEquals(-89.2244, authViewModel.cameraPosition.value.center.lng, 1e-6)
        assertEquals(0.0082, authViewModel.cameraPosition.value.span.latitudeDelta, 1e-6)

        val searchCoord = Coordinate(lat = 13.6800, lng = -89.2800)
        mapExplorerViewModel.applySuggestion("Parque El Boquerón", searchCoord)

        assertNotNull(mapExplorerViewModel.searchMarker.value)
        assertEquals("Parque El Boquerón", mapExplorerViewModel.searchMarker.value?.title)
        assertEquals(13.6800, authViewModel.cameraPosition.value.center.lat, 1e-6)
        assertEquals(-89.2800, authViewModel.cameraPosition.value.center.lng, 1e-6)
    }

    @Test
    fun `disableCellButton excludes op_settings and op_licenses for Guest user and enables all for Citizen`() = runTest {
        val userRepo = UserRepository()
        val authViewModel = AuthViewModel()
        val landingViewModel = LandingViewModel(userRepo)

        // Simulate Guest login (WelcomeView.swift saves UserType.guest.description)
        landingViewModel.handleLogin(
            payload = AuthPayload(token = "ephemeral-guest-token"),
            type = LoginType.Guest,
            appState = authViewModel
        )

        assertTrue(userRepo.isGuestUser())
        assertEquals("Guest", userRepo.getName())
        assertEquals("guest", userRepo.getUsername())

        assertTrue(disableCellButton("op:reports", userRepo))
        assertTrue(disableCellButton("op:comments", userRepo))
        assertFalse(disableCellButton("op:settings", userRepo))
        assertTrue(disableCellButton("op:moderationCenter", userRepo))
        assertFalse(disableCellButton("op:licenses", userRepo))

        // Simulate registered Citizen user session
        KeychainService.save(KeychainKey.USER_TYPE, UserType.CITIZEN.description)
        userRepo.setNames("tests.reportamelo")
        userRepo.setUsername("tests.reportamelo")
        landingViewModel.profile.value.refreshFromRepository()

        assertFalse(userRepo.isGuestUser())
        assertEquals("tests.reportamelo", userRepo.getName())
        assertEquals("tests.reportamelo", userRepo.getUsername())
        assertEquals("T", landingViewModel.profile.value.getMonogram())

        assertFalse(disableCellButton("op:reports", userRepo))
        assertFalse(disableCellButton("op:comments", userRepo))
        assertFalse(disableCellButton("op:settings", userRepo))
        assertFalse(disableCellButton("op:moderationCenter", userRepo))
        assertFalse(disableCellButton("op:licenses", userRepo))

        // Logout clears user preferences and returns defaults to Guest/@guest
        landingViewModel.logout(authViewModel)
        assertFalse(userRepo.isGuestUser())
        assertEquals("Guest", userRepo.getName())
        assertEquals("guest", userRepo.getUsername())
    }

    @Test
    fun `SubscriptionManager performUserLogin and handleUserLogout return false safely when Purchases is not configured`() = runTest {
        val loginResult = app.reportamelo.services.SubscriptionManager.performUserLogin("user-123")
        val logoutResult = app.reportamelo.services.SubscriptionManager.handleUserLogout()
        app.reportamelo.services.SubscriptionManager.checkEntitlement()

        assertFalse(loginResult)
        assertFalse(logoutResult)
    }

    @Test
    fun `MyReportsViewModel fetchReports maps ReportDAO with observations dates and badges`() = runTest {
        val fourWeeksAgoMillis = System.currentTimeMillis() - (28L * 24L * 3600L * 1000L)
        val oneMonthAgoMillis = System.currentTimeMillis() - (35L * 24L * 3600L * 1000L)

        val page1 = app.reportamelo.models.PaginatedResponse(
            documents = listOf(
                app.reportamelo.models.ReportDAO(
                    id = "SV-SSC-20260928-111",
                    coordinate = Coordinate(lat = 13.70, lng = -89.20),
                    title = "Dangerous potholes in the avenue",
                    description = "Dangerous potholes",
                    issueTypeId = 1,
                    severityId = 1,
                    statusId = 7,
                    createdAt = kotlinx.serialization.json.JsonPrimitive(fourWeeksAgoMillis),
                    updatedAt = kotlinx.serialization.json.JsonPrimitive(fourWeeksAgoMillis),
                    reportedAt = null,
                    observations = "La imagen no muestra el problema reportado de baches peligrosos en la avenida."
                ),
                app.reportamelo.models.ReportDAO(
                    id = "SV-SSC-20260920-222",
                    coordinate = Coordinate(lat = 13.71, lng = -89.21),
                    title = "Baches",
                    description = "Baches",
                    issueTypeId = 1,
                    severityId = 1,
                    statusId = 6,
                    createdAt = kotlinx.serialization.json.JsonPrimitive(oneMonthAgoMillis),
                    updatedAt = kotlinx.serialization.json.JsonPrimitive(oneMonthAgoMillis),
                    reportedAt = kotlinx.serialization.json.JsonPrimitive(oneMonthAgoMillis),
                    observations = "Las imágenes muestran claramente los baches reportados."
                )
            ),
            hasNext = false,
            hasPrev = false,
            totalPages = 1,
            total = 2,
            page = 1,
            documentsPerPage = 5
        )

        val viewModel = MyReportsViewModel { _ ->
            kotlinx.coroutines.flow.flowOf(page1)
        }

        viewModel.fetchReports()

        val loaded = viewModel.reports.value
        assertEquals(2, loaded.size)

        val first = loaded[0]
        assertEquals("Dangerous potholes in the avenue", first.title)
        assertEquals("Dangerous potholes", first.description)
        assertEquals("4 wk ago", first.createdDate)
        assertEquals("4 wk ago", first.updatedDate)
        assertEquals("Not yet", first.reportedDate)
        assertEquals(
            "La imagen no muestra el problema reportado de baches peligrosos en la avenida.",
            first.observations
        )
        assertEquals(app.reportamelo.commons.IssueTypes.ROAD, first.issueType)
        assertEquals(app.reportamelo.commons.Severity.LOW, first.severity)
        assertEquals(app.reportamelo.commons.IssueStatus.REJECTED, first.status)

        val second = loaded[1]
        assertEquals("1 mo ago", second.reportedDate)
        assertEquals(app.reportamelo.commons.IssueStatus.ASSIGNED, second.status)

        viewModel.confirmDeletion("SV-SSC-20260928-111")
        assertEquals(1, viewModel.reports.value.size)
        assertEquals("SV-SSC-20260920-222", viewModel.reports.value.first().id)
    }

    @Test
    fun `MyCommentsViewModel fetchComments and deleteComment load user comments and remove deleted item`() = runTest {
        val fourWeeksAgoMillis = System.currentTimeMillis() - (28L * 24L * 3600L * 1000L)
        val comment1 = app.reportamelo.models.Comment(
            id = "c-1",
            name = "tests.reportamelo",
            userName = "tests.reportamelo",
            profilePicture = "avatars/user1.png",
            profileId = "profile-1",
            commentFor = app.reportamelo.commons.CommentForType.REPORT,
            resourceId = "report-1",
            message = "Hello how are you",
            createdAt = fourWeeksAgoMillis.toString()
        )
        val comment2 = app.reportamelo.models.Comment(
            id = "c-2",
            name = "tests.reportamelo",
            userName = "tests.reportamelo",
            profilePicture = "https://development-api.reportamelo.app/avatars/user1.png",
            profileId = "profile-1",
            commentFor = app.reportamelo.commons.CommentForType.REPORT,
            resourceId = "report-2",
            message = "Second comment",
            createdAt = "2026-09-28T00:00:00.000Z"
        )

        var deletedId: String? = null
        val viewModel = MyCommentsViewModel(
            listByUserProvider = { page ->
                app.reportamelo.models.PaginatedResponse(
                    documents = listOf(comment1, comment2),
                    total = 2,
                    page = page,
                    documentsPerPage = 20,
                    totalPages = 1,
                    hasNext = false,
                    hasPrev = false
                )
            },
            deleteCommentProvider = { id ->
                deletedId = id
                app.reportamelo.commons.SuccessfulResult.DELETED
            }
        )

        viewModel.fetchComments(page = 1)
        assertFalse(viewModel.isLoading.value)
        assertEquals(2, viewModel.comments.value.size)
        assertEquals("Hello how are you", viewModel.comments.value.first().message)
        assertEquals(
            "4 wk ago",
            app.reportamelo.commons.utils.formatRelativeDate(viewModel.comments.value.first().createdAt)
        )
        assertEquals(
            "https://development-api.reportamelo.app/avatars/user1.png",
            app.reportamelo.commons.utils.resolveUrlString(viewModel.comments.value.first().profilePicture)
        )

        viewModel.deleteComment("c-1")
        assertEquals("c-1", deletedId)
        assertEquals(1, viewModel.comments.value.size)
        assertEquals("c-2", viewModel.comments.value.first().id)
    }

    @Test
    fun `SettingsStore enableAnonymousTelemetry persists in AppPreferences and updates isTelemetryCollectionEnabled`() = runTest {
        val settings = SettingsStore()
        // Default is true
        assertTrue(settings.enableAnonymousTelemetry)
        assertTrue(settings.isTelemetryCollectionEnabled.value)

        // Disable anonymous telemetry
        settings.enableAnonymousTelemetry = false
        assertFalse(settings.enableAnonymousTelemetry)
        assertFalse(AppPreferences.getBoolean("enableAnonymousTelemetry", true))
        assertFalse(settings.isTelemetryCollectionEnabled.value)

        // Re-enable anonymous telemetry
        settings.enableAnonymousTelemetry = true
        assertTrue(settings.enableAnonymousTelemetry)
        assertTrue(AppPreferences.getBoolean("enableAnonymousTelemetry", false))
        assertTrue(settings.isTelemetryCollectionEnabled.value)
    }

    @Test
    fun `SettingsSubViewModel updateNotificationSettings and updateDeviceToken send token after permission is granted`() = runTest {
        val settings = SettingsStore()
        val notificationManager = app.reportamelo.communityIssues.helpers.AppNotificationManager
        notificationManager.isPermissionGranted = false
        notificationManager.deviceToken = ""

        var sentNotifications: app.reportamelo.models.Notifications? = null
        var sentDeviceToken: String? = null
        var refreshCount = 0

        val viewModel = SettingsSubViewModel(
            modifyNotificationsProvider = { notifications ->
                sentNotifications = notifications
                Result.success("Notification settings updated")
            },
            sendDeviceProvider = { token ->
                sentDeviceToken = token
            },
            refreshProvider = {
                refreshCount++
            }
        )
        viewModel.inject(settings, notificationManager)

        // Toggle push notifications ON while permission is not yet granted and token is empty
        settings.enablePushNotifications = true
        settings.enableEmailNotifications = false
        viewModel.updateNotificationSettings()
        testScheduler.advanceUntilIdle()

        assertNotNull(sentNotifications)
        assertEquals(true, sentNotifications?.app)
        assertEquals(false, sentNotifications?.email)
        assertEquals(1, refreshCount)

        // Attempting to updateDeviceToken when token is empty does not call sendDevice
        viewModel.updateDeviceToken()
        testScheduler.advanceUntilIdle()
        assertNull(sentDeviceToken)

        // Grant permission and receive device token -> updateDeviceToken sends the token
        notificationManager.onPermissionResult(true)
        notificationManager.updateDeviceToken("fcm-device-token-abc-123")
        viewModel.updateDeviceToken()
        testScheduler.advanceUntilIdle()

        assertTrue(notificationManager.isPermissionGranted)
        assertEquals("fcm-device-token-abc-123", sentDeviceToken)
        assertTrue(viewModel.isDeviceTokenUpdated.value)
    }

    @Test
    fun `SettingsSubViewModel updatePrivacySettings and handleDeleteRequest manage alerts and account deletion`() = runTest {
        val settings = SettingsStore()
        var capturedPrivacy: app.reportamelo.models.PrivacySettings? = null
        var accountDeleted = false

        val viewModel = SettingsSubViewModel(
            privacyProvider = { privacy ->
                capturedPrivacy = privacy
            },
            deleteAccountProvider = {
                accountDeleted = true
            },
            refreshProvider = {}
        )
        viewModel.inject(settings)

        settings.showMyProfile = false
        settings.showMyUseNameWhenShare = true
        viewModel.updatePrivacySettings()
        testScheduler.advanceUntilIdle()

        assertNotNull(capturedPrivacy)
        assertEquals(false, capturedPrivacy?.showMyProfile)
        assertEquals(true, capturedPrivacy?.showMyUseNameWhenShare)
        assertFalse(viewModel.updatingPrivacySettingsLoading.value)

        // Pro user delete request shows active subscription alert first, then proceeds to delete alert
        viewModel.handleDeleteRequest(isPro = true)
        assertTrue(viewModel.showActiveSubscriptionAlert.value)
        assertFalse(viewModel.showDeleteAlert.value)

        viewModel.proceedToDeleteAlert()
        assertFalse(viewModel.showActiveSubscriptionAlert.value)
        assertTrue(viewModel.showDeleteAlert.value)

        viewModel.dismissDeleteAlert()
        assertFalse(viewModel.showDeleteAlert.value)
        viewModel.deleteAccount()
        testScheduler.advanceUntilIdle()

        assertTrue(accountDeleted)
        assertFalse(viewModel.isDeletingAccount.value)
    }

    @Test
    fun `UserPersonalizationDataModel debounces username availability checks shows LOADING and updates username`() = runTest {
        val checkedUserNames = mutableListOf<String>()
        var updatedUserName: String? = null

        val fakeRepo = object : UserRepository() {
            override suspend fun checkAvailability(userName: String, completion: (Result<String>) -> Unit) {
                checkedUserNames.add(userName)
                when (userName) {
                    "tests.reportamelo" -> completion(Result.success("Available"))
                    "taken.name" -> completion(Result.failure(app.reportamelo.repository.UserError.Taken))
                    else -> completion(Result.failure(app.reportamelo.repository.UserError.InvalidUserName))
                }
            }

            override suspend fun change(userName: String, completion: (Result<String>) -> Unit) {
                updatedUserName = userName
                completion(Result.success("Updated"))
            }
        }

        fakeRepo.setNames("tests.reportamelo")
        val viewModel = app.reportamelo.communityIssues.datamodel.UserPersonalizationDataModel(
            userRepository = fakeRepo,
            debounceMillis = 400L
        )

        // Rapid burst of edits should stay in LOADING and only send 1 request after 400ms debounce
        viewModel.onUserNameChanged("tes")
        assertEquals(
            app.reportamelo.communityIssues.datamodel.UserNameAvailabilityStatus.LOADING,
            viewModel.userNameAvailability.value
        )
        testScheduler.advanceTimeBy(200L)

        viewModel.onUserNameChanged("tests.rep")
        testScheduler.advanceTimeBy(200L)

        viewModel.onUserNameChanged("tests.reportamelo")
        assertEquals(
            app.reportamelo.communityIssues.datamodel.UserNameAvailabilityStatus.LOADING,
            viewModel.userNameAvailability.value
        )
        assertTrue(checkedUserNames.isEmpty())

        // Advance past the 400ms debounce window
        testScheduler.advanceTimeBy(400L)
        testScheduler.runCurrent()

        assertEquals(listOf("tests.reportamelo"), checkedUserNames)
        assertEquals(
            app.reportamelo.communityIssues.datamodel.UserNameAvailabilityStatus.AVAILABLE,
            viewModel.userNameAvailability.value
        )
        assertTrue(viewModel.isFormValid)

        // Taken username transitions from LOADING -> ERROR with "User name is taken"
        viewModel.onUserNameChanged("taken.name")
        assertEquals(
            app.reportamelo.communityIssues.datamodel.UserNameAvailabilityStatus.LOADING,
            viewModel.userNameAvailability.value
        )
        testScheduler.advanceTimeBy(400L)
        testScheduler.runCurrent()

        assertEquals(
            app.reportamelo.communityIssues.datamodel.UserNameAvailabilityStatus.ERROR,
            viewModel.userNameAvailability.value
        )
        assertEquals("User name is taken", viewModel.userNameErrorMessage.value)
        assertFalse(viewModel.isFormValid)

        // Switch back to available username and submit updateUsername
        viewModel.onUserNameChanged("tests.reportamelo")
        testScheduler.advanceTimeBy(400L)
        testScheduler.runCurrent()
        assertTrue(viewModel.isFormValid)

        var nextStepCalled = false
        viewModel.updateUsername {
            nextStepCalled = true
        }
        testScheduler.runCurrent()

        assertTrue(nextStepCalled)
        assertEquals("tests.reportamelo", updatedUserName)
        assertEquals(
            app.reportamelo.communityIssues.datamodel.UserNameState.UPDATED,
            viewModel.usernameState.value
        )
    }

    @Test
    fun `ProfileDataModel defaults to GOOGLE_AUTH generates monogram and initials and has 18 palette colors`() = runTest {
        val repo = UserRepository()
        repo.setNames("tests.reportamelo")
        repo.setGoogleAvatar("https://lh3.googleusercontent.com/a/google-avatar")

        val profile = app.reportamelo.communityIssues.datamodel.ProfileDataModel(repo)

        assertEquals(
            app.reportamelo.commons.AvatarCreatedFrom.GOOGLE_AUTH,
            profile.selectedAvatarOptionView.value
        )
        assertEquals("ec9458", profile.selectedAvatarColor.value)
        assertEquals(18, profile.backgroundColors.size)
        assertEquals("T", profile.getMonogram())
        assertEquals("TE", profile.getInitials())

        profile.setUserName("John Doe")
        assertEquals("J", profile.getMonogram())
        assertEquals("JD", profile.getInitials())

        profile.setSelectedAvatarOptionView(app.reportamelo.commons.AvatarCreatedFrom.MONOGRAM)
        assertEquals(
            app.reportamelo.commons.AvatarCreatedFrom.MONOGRAM,
            profile.selectedAvatarOptionView.value
        )

        var googleApplied = false
        profile.applyGoogleAvatar {
            googleApplied = true
        }
        assertTrue(googleApplied)
        assertEquals(
            app.reportamelo.commons.AvatarCreatedFrom.GOOGLE_AUTH,
            profile.selectedAvatarOptionView.value
        )
        assertNull(profile.profileImage.value)
    }
}
