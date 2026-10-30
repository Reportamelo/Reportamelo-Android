package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.helpers.AppNotificationManager
import app.reportamelo.models.Coordinate
import app.reportamelo.models.FriendlyCityDistribution
import app.reportamelo.models.Notifications
import app.reportamelo.models.PrivacySettings
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsSubViewModel(
    private val privacyProvider: suspend (PrivacySettings) -> Unit = { UserRepository.shared.privacy(it) },
    private val modifyNotificationsProvider: suspend (Notifications) -> Result<String> = { UserRepository.shared.modify(it) },
    private val sendDeviceProvider: suspend (String) -> Unit = { UserRepository.shared.sendDevice(it) },
    private val deleteAccountProvider: suspend () -> Unit = { UserRepository.shared.deleteMyAccount() },
    private val refreshProvider: suspend () -> Unit = { UserRepository.shared.refresh() }
) : ViewModel() {

    private val _showDeleteAlert = MutableStateFlow(false)
    val showDeleteAlert: StateFlow<Boolean> = _showDeleteAlert.asStateFlow()

    private val _showActiveSubscriptionAlert = MutableStateFlow(false)
    val showActiveSubscriptionAlert: StateFlow<Boolean> = _showActiveSubscriptionAlert.asStateFlow()

    private val _showManageSubscriptions = MutableStateFlow(false)
    val showManageSubscriptions: StateFlow<Boolean> = _showManageSubscriptions.asStateFlow()

    private val _selectedCity = MutableStateFlow(
        FriendlyCityDistribution(
            cityId = "a67b90f9-1d76-4835-a994-03cd04f1d619",
            firstLevel = "El Salvador",
            secondLevel = "San Salvador",
            thirdLevel = "San Salvador",
            ZipCode = "1101",
            legalGroupName = "Distrito de San Salvador",
            coordinates = Coordinate(lat = 13.6979857, lng = -89.1918083),
            isCapitalCity = 1,
            isDepartmentalCapital = 1
        )
    )
    val selectedCity: StateFlow<FriendlyCityDistribution> = _selectedCity.asStateFlow()

    private var settings: SettingsStore? = null
    private var notificationManager: AppNotificationManager? = null

    private val _showNetworkError = MutableStateFlow(false)
    val showNetworkError: StateFlow<Boolean> = _showNetworkError.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    private val _updatingPrivacySettingsLoading = MutableStateFlow(false)
    val updatingPrivacySettingsLoading: StateFlow<Boolean> = _updatingPrivacySettingsLoading.asStateFlow()

    private val _updatingNotificationSettingsLoading = MutableStateFlow(false)
    val updatingNotificationSettingsLoading: StateFlow<Boolean> = _updatingNotificationSettingsLoading.asStateFlow()

    private val _isPrivacySettingsUpdated = MutableStateFlow(false)
    val isPrivacySettingsUpdated: StateFlow<Boolean> = _isPrivacySettingsUpdated.asStateFlow()

    private val _isNotificationSettingsUpdated = MutableStateFlow(false)
    val isNotificationSettingsUpdated: StateFlow<Boolean> = _isNotificationSettingsUpdated.asStateFlow()

    private val _isDeviceTokenUpdated = MutableStateFlow(false)
    val isDeviceTokenUpdated: StateFlow<Boolean> = _isDeviceTokenUpdated.asStateFlow()

    fun inject(
        settings: SettingsStore,
        notificationManager: AppNotificationManager = AppNotificationManager
    ) {
        this.settings = settings
        this.notificationManager = notificationManager
    }

    fun updatePrivacySettings() {
        viewModelScope.launch {
            val currentSettings = settings ?: return@launch

            _updatingPrivacySettingsLoading.value = true
            try {
                delay(128)
                privacyProvider(
                    PrivacySettings(
                        showMyProfile = currentSettings.showMyProfile,
                        showMyUseNameWhenShare = currentSettings.showMyUseNameWhenShare
                    )
                )

                refreshProvider()
                _isPrivacySettingsUpdated.value = true
            } catch (e: CommonIntercommunicationErrors.NetworkError) {
                _showNetworkError.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _updatingPrivacySettingsLoading.value = false
                _isPrivacySettingsUpdated.value = false
            }
        }
    }

    fun updateNotificationSettings() {
        viewModelScope.launch {
            val currentSettings = settings ?: return@launch
            _updatingNotificationSettingsLoading.value = true

            try {
                delay(128)
                val result = modifyNotificationsProvider(
                    Notifications(
                        app = currentSettings.enablePushNotifications,
                        email = currentSettings.enableEmailNotifications,
                        web = currentSettings.enableWebNotifications
                    )
                )

                result.onSuccess { message ->
                    refreshProvider()
                    _isNotificationSettingsUpdated.value = true
                    println(message)
                }.onFailure { error ->
                    println(error.message)
                }
            } catch (e: CommonIntercommunicationErrors.NetworkError) {
                _showNetworkError.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isNotificationSettingsUpdated.value = true
                _updatingNotificationSettingsLoading.value = false
            }
        }
    }

    fun updateDeviceToken() {
        viewModelScope.launch {
            try {
                val manager = notificationManager ?: AppNotificationManager
                val token = manager.deviceToken
                if (token.isEmpty()) return@launch

                sendDeviceProvider(token)
                _isDeviceTokenUpdated.value = true
            } catch (e: CommonIntercommunicationErrors.NetworkError) {
                _showNetworkError.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isDeviceTokenUpdated.value = true
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isDeletingAccount.value = true
            try {
                deleteAccountProvider()
            } catch (e: CommonIntercommunicationErrors.NetworkError) {
                _showNetworkError.value = true
            } catch (e: Exception) {
                println("Failed to delete account: ${e.message}")
            } finally {
                _isDeletingAccount.value = false
            }
        }
    }

    fun handleDeleteRequest(isPro: Boolean) {
        if (isPro) {
            _showActiveSubscriptionAlert.value = true
        } else {
            _showDeleteAlert.value = true
        }
    }

    fun dismissActiveSubscriptionAlert() {
        _showActiveSubscriptionAlert.value = false
    }

    fun proceedToDeleteAlert() {
        _showActiveSubscriptionAlert.value = false
        _showDeleteAlert.value = true
    }

    fun dismissDeleteAlert() {
        _showDeleteAlert.value = false
    }

    fun dismissNetworkError() {
        _showNetworkError.value = false
    }
}

