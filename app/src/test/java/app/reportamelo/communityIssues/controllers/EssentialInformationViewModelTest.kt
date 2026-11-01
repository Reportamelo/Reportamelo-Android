package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.SuccessfulResult
import app.reportamelo.commons.utils.KeychainKey
import app.reportamelo.commons.utils.KeychainService
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.helpers.AppNotificationManager
import app.reportamelo.models.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EssentialInformationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AppNotificationManager.updateDeviceToken("")
        AppNotificationManager.isPermissionGranted = false
        KeychainService.deleteToken(KeychainKey.LANDING_PAGE_COMPLETE)
        KeychainService.deleteToken(KeychainKey.DEVICE_ID)
    }

    @After
    fun tearDown() {
        AppNotificationManager.updateDeviceToken("")
        AppNotificationManager.isPermissionGranted = false
        KeychainService.deleteToken(KeychainKey.LANDING_PAGE_COMPLETE)
        KeychainService.deleteToken(KeychainKey.DEVICE_ID)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state starts with Configure my account button message and both toggles off`() {
        val settings = SettingsStore() // enableEmailNotifications defaults to true in SettingsStore
        val viewModel = EssentialInformationViewModel()
        viewModel.inject(
            settings = settings,
            notificationManager = AppNotificationManager,
            initialNotifications = Notifications(app = false, email = false, web = false)
        )

        assertEquals("Configure my account", viewModel.buttonMessage.value)
        assertEquals(R.string.configure_my_account, viewModel.buttonMessageRes.value)
        assertFalse(viewModel.notifications.value.app)
        assertFalse(viewModel.notifications.value.email)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isSavingNotifications.value)
        assertFalse(viewModel.showErrorAlert.value)
    }

    @Test
    fun `onPushPermissionResult when granted updates settings modifies notifications and sends device token`() = runTest(testDispatcher) {
        val settings = SettingsStore().apply {
            enablePushNotifications = false
            enableEmailNotifications = false
            enableWebNotifications = false
        }
        AppNotificationManager.updateDeviceToken("fcm-device-token-999")

        var capturedNotifications: Notifications? = null
        var sentDeviceToken: String? = null
        var callbackNotifications: Notifications? = null

        val viewModel = EssentialInformationViewModel(
            modifyNotificationsProvider = {
                capturedNotifications = it
                Result.success("NOTIFICATIONS_UPDATED")
            },
            sendDeviceProvider = {
                sentDeviceToken = it
                SuccessfulResult.DONE
            }
        )
        viewModel.inject(
            settings = settings,
            notificationManager = AppNotificationManager,
            initialNotifications = Notifications(app = false, email = true, web = false)
        )

        viewModel.onPushPermissionResult(granted = true) { updated ->
            callbackNotifications = updated
        }
        advanceUntilIdle()

        assertTrue(settings.enablePushNotifications)
        assertTrue(viewModel.notifications.value.app)
        assertTrue(viewModel.notifications.value.email)
        assertEquals(Notifications(app = true, email = true, web = false), capturedNotifications)
        assertEquals(Notifications(app = true, email = true, web = false), callbackNotifications)
        assertEquals("fcm-device-token-999", sentDeviceToken)
        assertTrue(viewModel.isDeviceTokenUpdated.value)
    }

    @Test
    fun `onPushPermissionResult when denied keeps push notifications disabled and does not call backend`() = runTest(testDispatcher) {
        val settings = SettingsStore().apply {
            enablePushNotifications = true
            enableEmailNotifications = false
            enableWebNotifications = false
        }
        AppNotificationManager.updateDeviceToken("fcm-device-token-999")

        var modifyCalled = false
        var sendDeviceCalled = false

        val viewModel = EssentialInformationViewModel(
            modifyNotificationsProvider = {
                modifyCalled = true
                Result.success("NOTIFICATIONS_UPDATED")
            },
            sendDeviceProvider = {
                sendDeviceCalled = true
                SuccessfulResult.DONE
            }
        )
        viewModel.inject(settings, AppNotificationManager)

        viewModel.onPushPermissionResult(granted = false)
        advanceUntilIdle()

        assertFalse(settings.enablePushNotifications)
        assertFalse(viewModel.notifications.value.app)
        assertFalse(modifyCalled)
        assertFalse(sendDeviceCalled)
    }

    @Test
    fun `onEmailNotificationChanged updates settings and syncs notifications with backend`() = runTest(testDispatcher) {
        val settings = SettingsStore().apply {
            enablePushNotifications = false
            enableEmailNotifications = false
            enableWebNotifications = false
        }

        var capturedNotifications: Notifications? = null
        val viewModel = EssentialInformationViewModel(
            modifyNotificationsProvider = {
                capturedNotifications = it
                Result.success("NOTIFICATIONS_UPDATED")
            }
        )
        viewModel.inject(settings, AppNotificationManager)

        viewModel.onEmailNotificationChanged(enabled = true)
        advanceUntilIdle()

        assertTrue(settings.enableEmailNotifications)
        assertTrue(viewModel.notifications.value.email)
        assertEquals(Notifications(app = false, email = true, web = false), capturedNotifications)
        assertFalse(viewModel.isSavingNotifications.value)
    }

    @Test
    fun `configureAccount completes landing page saves keychain state initializes insights and invokes finalStep`() = runTest(testDispatcher) {
        var completeLandingCalls = 0
        var initializeInsightsCalls = 0
        var finalStepCalled = false

        val viewModel = EssentialInformationViewModel(
            completeLandingPageProvider = {
                completeLandingCalls += 1
                SuccessfulResult.DONE
            },
            initializeInsightsProvider = {
                initializeInsightsCalls += 1
                SuccessfulResult.CREATED
            },
            retryDelayMs = 10L
        )

        viewModel.configureAccount {
            finalStepCalled = true
        }
        advanceUntilIdle()

        assertEquals(1, completeLandingCalls)
        assertEquals(1, initializeInsightsCalls)
        assertTrue(finalStepCalled)
        assertEquals(
            "completion:state:successfully",
            KeychainService.getToken(KeychainKey.LANDING_PAGE_COMPLETE)
        )
        assertFalse(viewModel.isLoading.value)
        assertEquals("Start reporting", viewModel.buttonMessage.value)
        assertEquals(R.string.start_reporting, viewModel.buttonMessageRes.value)
    }

    @Test
    fun `configureAccount retries completeLandingPage on failure and succeeds on subsequent attempt`() = runTest(testDispatcher) {
        var attempts = 0
        var finalStepCalled = false

        val viewModel = EssentialInformationViewModel(
            completeLandingPageProvider = {
                attempts += 1
                if (attempts < 3) {
                    throw CommonIntercommunicationErrors.NetworkError("Temporary network error")
                }
                SuccessfulResult.DONE
            },
            initializeInsightsProvider = { SuccessfulResult.CREATED },
            retryDelayMs = 10L
        )

        viewModel.configureAccount(maxRetries = 3) {
            finalStepCalled = true
        }
        advanceUntilIdle()

        assertEquals(3, attempts)
        assertTrue(finalStepCalled)
        assertFalse(viewModel.showErrorAlert.value)
        assertEquals("Start reporting", viewModel.buttonMessage.value)
    }

    @Test
    fun `configureAccount shows error alert and resets button message when all retries fail`() = runTest(testDispatcher) {
        var attempts = 0
        var finalStepCalled = false

        val viewModel = EssentialInformationViewModel(
            completeLandingPageProvider = {
                attempts += 1
                throw CommonIntercommunicationErrors.ServerError("500 Internal Server Error")
            },
            initializeInsightsProvider = { SuccessfulResult.CREATED },
            retryDelayMs = 10L
        )

        viewModel.configureAccount(maxRetries = 3) {
            finalStepCalled = true
        }
        advanceUntilIdle()

        assertEquals(3, attempts)
        assertFalse(finalStepCalled)
        assertFalse(viewModel.isLoading.value)
        assertEquals("Configure my account", viewModel.buttonMessage.value)
        assertEquals(R.string.configure_my_account, viewModel.buttonMessageRes.value)
        assertTrue(viewModel.showErrorAlert.value)
        assertEquals("Failed to save configuration after 3 attempts.", viewModel.messageError.value)
    }

    @Test
    fun `configureAccount does not block finalStep when insights initialization fails with ServerError`() = runTest(testDispatcher) {
        var finalStepCalled = false

        val viewModel = EssentialInformationViewModel(
            completeLandingPageProvider = { SuccessfulResult.DONE },
            initializeInsightsProvider = {
                throw CommonIntercommunicationErrors.ServerError("Insights service unavailable")
            },
            retryDelayMs = 10L
        )

        viewModel.configureAccount {
            finalStepCalled = true
        }
        advanceUntilIdle()

        assertTrue(finalStepCalled)
        assertFalse(viewModel.showErrorAlert.value)
        assertEquals("Start reporting", viewModel.buttonMessage.value)
    }

    @Test
    fun `configureAccount shows network delay alert without blocking finalStep when insights fails with NetworkError`() = runTest(testDispatcher) {
        var finalStepCalled = false

        val viewModel = EssentialInformationViewModel(
            completeLandingPageProvider = { SuccessfulResult.DONE },
            initializeInsightsProvider = {
                throw CommonIntercommunicationErrors.NetworkError("Insights network timeout")
            },
            retryDelayMs = 10L
        )

        viewModel.configureAccount {
            finalStepCalled = true
        }
        advanceUntilIdle()

        assertTrue(finalStepCalled)
        assertTrue(viewModel.showErrorAlert.value)
        assertEquals(
            "It looks like that your network is experiencing some delays, please try again.",
            viewModel.messageError.value
        )
        assertEquals(
            R.string.it_looks_like_that_your_network_is_experiencing_some_delays_please_try_again,
            viewModel.messageErrorRes.value
        )
        assertEquals("Start reporting", viewModel.buttonMessage.value)
    }

    @Test
    fun `updateNotificationSettings maps CommonIntercommunicationErrors to localized error messages`() = runTest(testDispatcher) {
        var errorToThrow: Exception = CommonIntercommunicationErrors.NetworkError("Timeout")
        val viewModel = EssentialInformationViewModel(
            modifyNotificationsProvider = {
                throw errorToThrow
            }
        )

        viewModel.updateNotificationSettings()
        advanceUntilIdle()

        assertTrue(viewModel.showErrorAlert.value)
        assertEquals(
            "It looks like that your network is experiencing some delays, please try again.",
            viewModel.messageError.value
        )
        assertEquals(
            R.string.it_looks_like_that_your_network_is_experiencing_some_delays_please_try_again,
            viewModel.messageErrorRes.value
        )

        viewModel.dismissErrorAlert()
        assertFalse(viewModel.showErrorAlert.value)

        errorToThrow = CommonIntercommunicationErrors.UnProcessable
        viewModel.updateNotificationSettings()
        advanceUntilIdle()

        assertTrue(viewModel.showErrorAlert.value)
        assertEquals(
            "Your petition cannot be processed, please try again.",
            viewModel.messageError.value
        )
        assertEquals(
            R.string.your_petition_cannot_be_processed_please_try_again,
            viewModel.messageErrorRes.value
        )
    }

    @Test
    fun `onPushPermissionResult rejects corrupted completion state string in DEVICE_ID and never sends it as deviceToken`() = runTest(testDispatcher) {
        val settings = SettingsStore().apply {
            enablePushNotifications = false
            enableEmailNotifications = false
            enableWebNotifications = false
        }
        // Simulate legacy corrupted value stored under DEVICE_ID
        KeychainService.save(KeychainKey.DEVICE_ID, "completion:state:successfully")
        AppNotificationManager.updateDeviceToken("completion:state:successfully")

        assertEquals("", AppNotificationManager.deviceToken)

        var sentDeviceToken: String? = null
        val viewModel = EssentialInformationViewModel(
            modifyNotificationsProvider = { Result.success("NOTIFICATIONS_UPDATED") },
            sendDeviceProvider = {
                sentDeviceToken = it
                SuccessfulResult.DONE
            }
        )
        viewModel.inject(
            settings = settings,
            notificationManager = AppNotificationManager,
            initialNotifications = Notifications(app = false, email = false, web = false)
        )

        viewModel.onPushPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals("", AppNotificationManager.deviceToken)
        assertEquals(null, sentDeviceToken)
        assertFalse(viewModel.isDeviceTokenUpdated.value)

        // When a valid device identifier/token is present, it is sent
        KeychainService.save(KeychainKey.DEVICE_ID, "0fff3cfb8d4ad5f8")
        viewModel.onPushPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals("0fff3cfb8d4ad5f8", AppNotificationManager.deviceToken)
        assertEquals("0fff3cfb8d4ad5f8", sentDeviceToken)
        assertTrue(viewModel.isDeviceTokenUpdated.value)
    }
}
