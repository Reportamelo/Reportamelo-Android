package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.AppPreferences
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.DeviceService
import app.reportamelo.commons.SuccessfulResult
import app.reportamelo.commons.utils.KeychainKey
import app.reportamelo.commons.utils.KeychainService
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.helpers.AppNotificationManager
import app.reportamelo.models.Notifications
import app.reportamelo.repository.InsightsRepository
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.reportamelo.communityIssues.helpers.localized

class EssentialInformationViewModel(
    private val modifyNotificationsProvider: suspend (Notifications) -> Result<String> = { UserRepository.shared.modify(it) },
    private val sendDeviceProvider: suspend (String) -> SuccessfulResult = { UserRepository.shared.sendDevice(it) },
    private val completeLandingPageProvider: suspend () -> SuccessfulResult = { UserRepository.shared.completeLandingPage() },
    private val initializeInsightsProvider: suspend () -> SuccessfulResult = { InsightsRepository.shared.initialize() },
    private val retryDelayMs: Long = 1000L
) : ViewModel() {

    private var settings: SettingsStore? = null
    private var notificationManager: AppNotificationManager? = null

    private val _notifications = MutableStateFlow(Notifications(app = false, email = false, web = false))
    val notifications: StateFlow<Notifications> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSavingNotifications = MutableStateFlow(false)
    val isSavingNotifications: StateFlow<Boolean> = _isSavingNotifications.asStateFlow()

    private val _buttonMessage = MutableStateFlow(localized(R.string.configure_my_account, "Configure my account"))
    val buttonMessage: StateFlow<String> = _buttonMessage.asStateFlow()

    private val _buttonMessageRes = MutableStateFlow(R.string.configure_my_account)
    val buttonMessageRes: StateFlow<Int> = _buttonMessageRes.asStateFlow()

    private val _showErrorAlert = MutableStateFlow(false)
    val showErrorAlert: StateFlow<Boolean> = _showErrorAlert.asStateFlow()

    private val _messageError = MutableStateFlow("")
    val messageError: StateFlow<String> = _messageError.asStateFlow()

    private val _messageErrorRes = MutableStateFlow<Int?>(null)
    val messageErrorRes: StateFlow<Int?> = _messageErrorRes.asStateFlow()

    private val _isDeviceTokenUpdated = MutableStateFlow(false)
    val isDeviceTokenUpdated: StateFlow<Boolean> = _isDeviceTokenUpdated.asStateFlow()

    fun inject(
        settings: SettingsStore,
        notificationManager: AppNotificationManager = AppNotificationManager,
        initialNotifications: Notifications? = null
    ) {
        this.settings = settings
        this.notificationManager = notificationManager
        if (initialNotifications != null) {
            _notifications.value = initialNotifications
        }
    }

    fun setNotifications(notifications: Notifications) {
        _notifications.value = notifications
    }

    fun onPushPermissionResult(
        granted: Boolean,
        onNotificationsChanged: ((Notifications) -> Unit)? = null
    ) {
        val manager = notificationManager ?: AppNotificationManager
        manager.onPermissionResult(granted)
        if (!granted) {
            settings?.enablePushNotifications = false
            val updated = _notifications.value.copy(app = false)
            _notifications.value = updated
            onNotificationsChanged?.invoke(updated)
        } else {
            settings?.enablePushNotifications = true
            val updated = _notifications.value.copy(app = true)
            _notifications.value = updated
            onNotificationsChanged?.invoke(updated)
            updateNotificationSettings()
            updateDeviceToken()
        }
    }

    fun onPushNotificationDisabled(
        onNotificationsChanged: ((Notifications) -> Unit)? = null
    ) {
        settings?.enablePushNotifications = false
        val updated = _notifications.value.copy(app = false)
        _notifications.value = updated
        onNotificationsChanged?.invoke(updated)
        updateNotificationSettings()
    }

    fun onEmailNotificationChanged(
        enabled: Boolean,
        onNotificationsChanged: ((Notifications) -> Unit)? = null
    ) {
        settings?.enableEmailNotifications = enabled
        val updated = _notifications.value.copy(email = enabled)
        _notifications.value = updated
        onNotificationsChanged?.invoke(updated)
        updateNotificationSettings()
    }

    fun updateNotificationSettings() {
        viewModelScope.launch {
            _isSavingNotifications.value = true
            try {
                delay(128)
                val current = _notifications.value
                val result = modifyNotificationsProvider(
                    Notifications(
                        app = current.app,
                        email = current.email,
                        web = current.web
                    )
                )
                result.onSuccess { message ->
                    println(message)
                }.onFailure { error ->
                    println(error.localizedMessage ?: error.message)
                }
            } catch (e: Exception) {
                handleNotificationError(e)
            } finally {
                _isSavingNotifications.value = false
            }
        }
    }

    fun updateDeviceToken() {
        viewModelScope.launch {
            try {
                val manager = notificationManager ?: AppNotificationManager
                var token = manager.deviceToken.takeIf { AppNotificationManager.isValidDeviceToken(it) }.orEmpty()
                if (token.isEmpty()) {
                    val fallbackDeviceId = AppPreferences.appContext?.let { context ->
                        runCatching { DeviceService(context).getDeviceId() }.getOrNull()
                    }?.takeIf { AppNotificationManager.isValidDeviceToken(it) }
                    if (fallbackDeviceId != null) {
                        manager.updateDeviceToken(fallbackDeviceId)
                        token = fallbackDeviceId
                    } else {
                        manager.updateDeviceToken("")
                    }
                }
                if (token.isEmpty()) return@launch

                sendDeviceProvider(token)
                _isDeviceTokenUpdated.value = true
            } catch (e: Exception) {
                handleNotificationError(e)
            }
        }
    }

    fun configureAccount(
        maxRetries: Int = 3,
        finalStep: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _buttonMessage.value = localized(R.string.saving_my_configuration, "Saving my configuration")
            _buttonMessageRes.value = R.string.saving_my_configuration

            try {
                completeLandingPage(maxRetries = maxRetries)
                initializeStats()

                _isLoading.value = false
                _buttonMessage.value = localized(R.string.start_reporting, "Start reporting")
                _buttonMessageRes.value = R.string.start_reporting
                finalStep()
            } catch (e: Exception) {
                _isLoading.value = false
                _buttonMessage.value = localized(R.string.configure_my_account, "Configure my account")
                _buttonMessageRes.value = R.string.configure_my_account
                _messageErrorRes.value = null
                _messageError.value = localized(R.string.failed_to_save_configuration_after_d_attempts, "Failed to save configuration after %1\$d attempts.", maxRetries)
                _showErrorAlert.value = true
            }
        }
    }

    private suspend fun completeLandingPage(maxRetries: Int) {
        var currentRetry = 0
        var lastError: Exception? = null

        while (currentRetry < maxRetries) {
            try {
                val result = completeLandingPageProvider()
                if (result == SuccessfulResult.DONE) {
                    runCatching {
                        KeychainService.save(KeychainKey.LANDING_PAGE_COMPLETE, "completion:state:successfully")
                    }
                    return
                }
                lastError = IllegalStateException(localized(R.string.failed_to_save_configuration_after_d_attempts, "Failed to save configuration after %1\$d attempts.", maxRetries))
                currentRetry += 1
                if (currentRetry < maxRetries && retryDelayMs > 0) {
                    delay(retryDelayMs * currentRetry)
                }
            } catch (e: Exception) {
                lastError = e
                currentRetry += 1
                if (currentRetry < maxRetries && retryDelayMs > 0) {
                    delay(retryDelayMs * currentRetry)
                }
            }
        }

        throw lastError ?: IllegalStateException(localized(R.string.failed_to_save_configuration_after_d_attempts, "Failed to save configuration after %1\$d attempts.", maxRetries))
    }

    private suspend fun initializeStats() {
        _buttonMessage.value = localized(R.string.initializing_insights, "Initializing Insights")
        _buttonMessageRes.value = R.string.initializing_insights

        try {
            val result = initializeInsightsProvider()
            if (result == SuccessfulResult.CREATED) {
                println("Insights initialized successfully")
            }
        } catch (e: CommonIntercommunicationErrors.NetworkError) {
            handleNotificationError(e)
        } catch (e: Exception) {
            // We don't block the user if insights fail to initialize, just log it
            println("Failed to initialize insights: $e")
        }
    }

    private fun handleNotificationError(error: Exception) {
        when (error) {
            is CommonIntercommunicationErrors.UnProcessable,
            is CommonIntercommunicationErrors.InvalidPetition -> {
                _messageError.value = localized(R.string.your_petition_cannot_be_processed_please_try_again_text, "Your petition cannot be processed, please try again.")
                _messageErrorRes.value = R.string.your_petition_cannot_be_processed_please_try_again
            }
            is CommonIntercommunicationErrors.NetworkError -> {
                _messageError.value = localized(R.string.it_looks_like_that_your_network_is_experiencing_some_delays_please_try, "It looks like that your network is experiencing some delays, please try again.")
                _messageErrorRes.value = R.string.it_looks_like_that_your_network_is_experiencing_some_delays_please_try_again
            }
            is CommonIntercommunicationErrors.ServerError -> {
                _messageError.value = localized(R.string.server_error_please_try_again_text, "Server error, please try again.")
                _messageErrorRes.value = R.string.server_error_please_try_again
            }
            is CommonIntercommunicationErrors -> {
                _messageError.value = localized(R.string.an_unexpected_error_has_occurred_please_try_again_text, "An unexpected error has occurred, please try again.")
                _messageErrorRes.value = R.string.an_unexpected_error_has_occurred_please_try_again
            }
            else -> {
                _messageError.value = error.localizedMessage ?: error.message ?: localized(R.string.an_unexpected_error_has_occurred_please_try_again_text, "An unexpected error has occurred, please try again.")
                _messageErrorRes.value = null
            }
        }
        _showErrorAlert.value = true
    }

    fun dismissErrorAlert() {
        _showErrorAlert.value = false
    }
}
