package app.reportamelo.communityIssues.controllers

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Logger
import app.reportamelo.commons.*
import app.reportamelo.commons.utils.*
import app.reportamelo.commons.utils.KeychainKey
import app.reportamelo.commons.utils.KeychainService
import app.reportamelo.commons.utils.KeychainToken
import app.reportamelo.communityIssues.datamodel.*
import app.reportamelo.communityIssues.ui.screens.LandingNavigation
import app.reportamelo.models.*
import app.reportamelo.network.*
import app.reportamelo.network.services.*
import app.reportamelo.repository.*
import app.reportamelo.services.SubscriptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LandingViewModel(
    private val userRepository: UserRepository = UserRepository.shared
) : ViewModel() {

    companion object {
        val shared = LandingViewModel()
    }

    private val _accountDeleted = MutableStateFlow(false)
    val accountDeleted = _accountDeleted.asStateFlow()

    private val _userPersonalizationDataModel = MutableStateFlow(UserPersonalizationDataModel())
    val userPersonalizationDataModel = _userPersonalizationDataModel.asStateFlow()

    private val _notifications = MutableStateFlow(Notifications(app = false, email = false, web = false))
    val notifications = _notifications.asStateFlow()

    private val _presentAlert = MutableStateFlow(false)
    val presentAlert = _presentAlert.asStateFlow()

    private val _alertTitle = MutableStateFlow("Error")
    val alertTitle = _alertTitle.asStateFlow()

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    private val _path = MutableStateFlow<List<LandingNavigation>>(emptyList())
    val path = _path.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest = _isGuest.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _isCheckingStatus = MutableStateFlow(false)
    val isCheckingStatus = _isCheckingStatus.asStateFlow()

    private val _userOAuthState = MutableStateFlow(UserOAuthResultState.UNOWNED)
    val userOAuthState = _userOAuthState.asStateFlow()

    private val _selectedCity = MutableStateFlow(
        CitiesRepository.getSavedSelectedCity() ?: FriendlyCityDistribution(
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
    val selectedCity = _selectedCity.asStateFlow()

    private val _countryCode = MutableStateFlow(CountryCode.SV)
    val countryCode = _countryCode.asStateFlow()

    private var settings: SettingsStore? = null

    private val _profile = MutableStateFlow(ProfileDataModel())
    val profile = _profile.asStateFlow()

    fun inject(settings: SettingsStore) {
        this.settings = settings
    }

    fun setSelectedCity(city: FriendlyCityDistribution) {
        _selectedCity.value = city
        CitiesRepository.saveSelectedCity(city)
    }

    fun dismissAlert() {
        _presentAlert.value = false
    }

    fun handleLogin(payload: AuthPayload, type: LoginType, appState: AuthViewModel) {
        Log.i("LandingViewModel", "handleLogin ${payload.token}")
        Log.i("LandingViewModel", "handleLogin ${payload.email}")
        Log.i("LandingViewModel", "handleLogin ${payload.token.isEmpty()}")
        Log.i("LandingViewModel", "handleLogin ${payload.token.isNotEmpty()}")
        if (payload.token.isNotEmpty()) {
            if (type == LoginType.Guest) {
                saveSession(payload.token)
                KeychainService.save(KeychainKey.USER_TYPE, UserType.GUEST.description)
                AppPreferences.remove("names")
                AppPreferences.remove("user_name")
                AppPreferences.remove("avatar_url")
                AppPreferences.remove("selected_avatar_option")
                AppPreferences.remove("selected_avatar_color")
                appState.setSelectedCity(null)
                _isGuest.value = true
                _profile.value.refreshFromRepository()
            } else {
                if (type is LoginType.User && type.authMethod == AuthMethod.APPLE) {
                    performLoginActionsWithAppleProvider(payload, appState)
                }
                if (type is LoginType.User && type.authMethod == AuthMethod.GOOGLE) {
                    performLoginActionsWithGoogleProvider(payload.token, appState)
                }
            }
        } else {
            showAlert(message = "Something went wrong")
        }
    }

    fun completeGuestOnboarding(appState: AuthViewModel) {
        val city = _selectedCity.value
        CitiesRepository.saveSelectedCity(city)
        KeychainService.save(KeychainKey.USER_TYPE, UserType.GUEST.description)
        KeychainService.save(KeychainKey.PLAN_TYPE, PlanType.FREEMIUM_FOR_GUESTS.value)
        KeychainService.save(KeychainKey.DEVICE_ID, "completion:state:successfully")
        KeychainService.save(KeychainKey.LANDING_PAGE_COMPLETE, "completion:state:successfully")
        appState.setSelectedCity(city)
        appState.setCameraPosition(
            to = city.coordinates,
            latitudeDelta = CoordinateSpan.CITY_SELECTION_LATITUDE_DELTA,
            longitudeDelta = CoordinateSpan.CITY_SELECTION_LONGITUDE_DELTA
        )
        _isGuest.value = true
        _profile.value.refreshFromRepository()
    }

    fun completeUserOnboarding(
        notifications: Notifications = _notifications.value,
        appState: AuthViewModel
    ) {
        viewModelScope.launch {
            _notifications.value = notifications
            try {
                userRepository.modify(notifications)
            } catch (_: Exception) {
                // Allow onboarding completion to proceed even if notification preferences fail to sync
            }
            try {
                userRepository.completeLandingPage()
            } catch (_: Exception) {
                // Allow local completion state to persist
            }
            val city = _selectedCity.value
            CitiesRepository.saveSelectedCity(city)
            KeychainService.save(KeychainKey.SESSION_STATE_VERIFICATION, "session:state:valid")
            KeychainService.save(KeychainKey.DEVICE_ID, "completion:state:successfully")
            KeychainService.save(KeychainKey.LANDING_PAGE_COMPLETE, "completion:state:successfully")
            appState.setSelectedCity(city)
            appState.setCameraPosition(
                to = city.coordinates,
                latitudeDelta = CoordinateSpan.CITY_SELECTION_LATITUDE_DELTA,
                longitudeDelta = CoordinateSpan.CITY_SELECTION_LONGITUDE_DELTA
            )
            _isGuest.value = false
            _isLoggedIn.value = true
            _profile.value.refreshFromRepository()
        }
    }

    private fun performLoginActionsWithAppleProvider(payload: AuthPayload, appState: AuthViewModel) {
        viewModelScope.launch {
            try {
                payload.email?.let { if (it.isNotEmpty()) KeychainService.save(KeychainKey.EMAIL, it) }
                payload.name?.let {
                    if (it.isNotEmpty()) {
                        KeychainService.save(KeychainKey.NAME, it)
                        userRepository.setNames(it)
                    }
                }

                KeychainService.save(KeychainKey.AUTH_METHOD, AuthMethod.APPLE.value)
                LoginViewModel.shared.refreshLastAuthMethod()

                val (state, sessionId, data) = userRepository.signInOrLoginWithApple(payload)
                _userOAuthState.value = state
                saveSession(sessionId)

                if (state == UserOAuthResultState.FIRST_LOGIN) {
                    setIntoKeychain(data)
                    _path.value = _path.value + LandingNavigation.SelectCity
                } else {
                    if (data.landingPageCompleted) {
                        setSettingsFromAuthenticatedUser(data)
                        setCameraPositionByCityId(appState)
                        runCatching { SubscriptionManager.performUserLogin(data.userId) }
                        _isLoggedIn.value = true
                    } else {
                        setIntoKeychain(data)
                        _path.value = _path.value + LandingNavigation.SelectCity
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            } finally {
                LoginViewModel.shared.setLoading(false)
            }
        }
    }

    private fun performLoginActionsWithGoogleProvider(session: String, appState: AuthViewModel) {
       Log.i("LandingViewModel", "performLoginActionsWithGoogleProvider $session")
        viewModelScope.launch {
            try {

                val (state, sessionId, data) = userRepository.login(session)
                KeychainService.save(KeychainKey.AUTH_METHOD, AuthMethod.GOOGLE.value)
                LoginViewModel.shared.refreshLastAuthMethod()
                _userOAuthState.value = state
                saveSession(sessionId)

                if (state == UserOAuthResultState.FIRST_LOGIN) {
                    setIntoKeychain(data)
                    _path.value = _path.value + LandingNavigation.SelectCity
                } else {
                    if (data.landingPageCompleted) {
                        setSettingsFromAuthenticatedUser(data)
                        setCameraPositionByCityId(appState)
                        runCatching { SubscriptionManager.performUserLogin(data.userId) }
                        _isLoggedIn.value = true
                    } else {
                        setIntoKeychain(data)
                        _path.value = _path.value + LandingNavigation.SelectCity
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            } finally {
                LoginViewModel.shared.setLoading(false)
            }
        }
    }

    private fun handleException(e: Exception) {
        Log.e("LandingViewModel", "handleException", e)
        when (e) {
            is CommonIntercommunicationErrors.InvalidPetition -> showAlert(message = "CLNT: ${e.message ?: "Unknown Code"}")
            is CommonIntercommunicationErrors.ServerError -> showAlert(message = "SVR: ${e.message ?: "Unknown Code"}")
            is CommonIntercommunicationErrors.NetworkError -> showAlert(message = "There was a problem with the network: ${e.message}")
            is CommonIntercommunicationErrors.Forbidden -> showAlert(title = "Access Denied", message = e.message ?: "")
            else -> showAlert(message = "Something went wrong")
        }
    }

    private fun saveSession(sessionId: String) {
        KeychainService.save(KeychainKey.MUTATION, sessionId)
        app.reportamelo.network.KeychainService.setToken(TokenKey.MUTATION, sessionId)
    }

    private fun showAlert(title: String = "Error", message: String) {
        _alertTitle.value = title
        _message.value = message
        _presentAlert.value = true
    }

    private fun setCameraPositionByCityId(appState: AuthViewModel) {
        val targetCityId = settings?.cityId
        val localCities = CitiesRepository.loadLocalCities(_countryCode.value).documents.orEmpty()
        val matchedCity = localCities.firstOrNull { it.cityId == targetCityId }
            ?: CitiesRepository.getSavedSelectedCity()
            ?: _selectedCity.value
        _selectedCity.value = matchedCity
        CitiesRepository.saveSelectedCity(matchedCity)
        appState.setSelectedCity(matchedCity)
        appState.setCameraPosition(
            to = matchedCity.coordinates,
            latitudeDelta = CoordinateSpan.CITY_SELECTION_LATITUDE_DELTA,
            longitudeDelta = CoordinateSpan.CITY_SELECTION_LONGITUDE_DELTA
        )
    }

    private fun setIntoKeychain(data: PublicUserData) {
        KeychainService.save(KeychainKey.USER_ID, data.userId)
        KeychainService.save(KeychainKey.PROFILE_ID, data.profileId)
        KeychainService.save(KeychainKey.USER_TYPE, data.userType.description)
        KeychainService.save(KeychainKey.PLAN_TYPE, "freemium")
        if (data.email.isNotEmpty()) {
            KeychainService.save(KeychainKey.EMAIL, data.email)
        }
        if (data.names.isNotEmpty()) {
            KeychainService.save(KeychainKey.NAME, data.names)
            userRepository.setNames(data.names)
        }
        if (data.userName.isNotEmpty()) {
            userRepository.setUsername(data.userName)
        }
        if (data.profilePicture.isNotEmpty()) {
            userRepository.setAvatar(data.profilePicture)
        }
        _profile.value.refreshFromRepository()
    }

    private fun setSettingsFromAuthenticatedUser(data: PublicUserData) {
        userRepository.setUsername(data.userName)
        userRepository.setNames(data.names)
        userRepository.setAvatar(data.profilePicture)
        setAvatar(data.profilePicture, data.settings.avatarCreatedFrom)

        settings?.enableEmailNotifications = data.settings.notifications.email
        settings?.enableWebNotifications = data.settings.notifications.web
        settings?.showMyProfile = data.settings.privacySettings.showMyProfile
        settings?.showMyUseNameWhenShare = data.settings.privacySettings.showMyUseNameWhenShare
        settings?.countryCode = data.settings.reportLocatorSettings.countryCode
        settings?.cityId = data.settings.reportLocatorSettings.cityId

        setIntoKeychain(data)
        KeychainService.save(KeychainKey.SESSION_STATE_VERIFICATION, "session:state:valid")
        KeychainService.save(KeychainKey.LANDING_PAGE_COMPLETE, "completion:state:successfully")
        _isGuest.value = false
        _profile.value.refreshFromRepository()
    }

    private fun setAvatar(url: String, createdFrom: AvatarCreatedFrom) {
        if (createdFrom == AvatarCreatedFrom.GOOGLE_AUTH) {
            userRepository.setAvatar(url)
        }
    }

    fun checkStatus(appState: AuthViewModel = AuthViewModel.shared) {
        val savedCity = CitiesRepository.getSavedSelectedCity()
        if (savedCity != null) {
            _selectedCity.value = savedCity
            appState.setSelectedCity(savedCity)
        }
        val validSession = userRepository.isSessionValid()
        _isLoggedIn.value = validSession
        if (validSession) {
            _isGuest.value = false
            val activeCity = savedCity ?: _selectedCity.value
            appState.setSelectedCity(activeCity)
            appState.setupInitialCameraPosition()
        } else if (userRepository.isGuestUser() && userRepository.userHasCompletedLandingPage()) {
            val guestCity = savedCity ?: _selectedCity.value
            appState.setSelectedCity(guestCity)
            appState.setupInitialCameraPosition()
            _isGuest.value = true
        }
        _profile.value.refreshFromRepository()
    }

    fun logout(appState: AuthViewModel = AuthViewModel.shared) {
        userRepository.signOutFromGoogle()
        appState.setSelectedCity(null)

        AppPreferences.remove(AuthViewModel.KEY_MAP_LATITUDE_DELTA)
        AppPreferences.remove(AuthViewModel.KEY_MAP_LONGITUDE_DELTA)
        AppPreferences.remove(AuthViewModel.KEY_MAP_LATITUDE)
        AppPreferences.remove(AuthViewModel.KEY_MAP_LONGITUDE)
        AppPreferences.remove(AuthViewModel.KEY_SELECTED_CITY)
        AppPreferences.remove("names")
        AppPreferences.remove("user_name")
        AppPreferences.remove("avatar_url")
        AppPreferences.remove("selected_avatar_option")
        AppPreferences.remove("selected_avatar_color")

        KeychainService.deleteToken(KeychainKey.NAME)
        KeychainService.deleteToken(KeychainKey.EMAIL)
        KeychainService.deleteToken(KeychainKey.QUERY)
        KeychainService.deleteToken(KeychainKey.USER_ID)
        KeychainService.deleteToken(KeychainKey.DEVICE_ID)
        KeychainService.deleteToken(KeychainKey.PLAN_TYPE)
        KeychainService.deleteToken(KeychainKey.MUTATION)
        KeychainService.deleteToken(KeychainKey.USER_TYPE)
        KeychainService.deleteToken(KeychainKey.PROFILE_ID)
        KeychainService.deleteToken(KeychainKey.AUTH_METHOD)
        KeychainService.deleteToken(KeychainKey.LANDING_PAGE_COMPLETE)
        KeychainService.deleteToken(KeychainKey.SESSION_STATE_VERIFICATION)
        app.reportamelo.network.KeychainService.deleteToken(TokenKey.MUTATION)
        app.reportamelo.network.KeychainService.deleteToken(TokenKey.QUERY)

        _isLoggedIn.value = false
        _isGuest.value = false
        _path.value = emptyList()
        _profile.value.refreshFromRepository()

        viewModelScope.launch {
            try {
                SubscriptionManager.handleUserLogout()
            } catch (_: Exception) {
            }
        }
    }
}
