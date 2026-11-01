package app.reportamelo.communityIssues.datamodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.AuthMethod
import app.reportamelo.commons.userNameValidator
import app.reportamelo.repository.UserError
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UserNameAvailabilityStatus {
    AVAILABLE,
    ERROR,
    LOADING,
    UNTOUCHED
}

enum class UserNameState {
    UPDATED,
    UNTOUCHED
}

class UserPersonalizationDataModel(
    private val userRepository: UserRepository = UserRepository.shared,
    private val debounceMillis: Long = 400L
) : ViewModel() {

    private var availabilityCheckJob: Job? = null

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userNameAvailability = MutableStateFlow(UserNameAvailabilityStatus.UNTOUCHED)
    val userNameAvailability: StateFlow<UserNameAvailabilityStatus> = _userNameAvailability.asStateFlow()
    val userNameAvailabilityStatus: StateFlow<UserNameAvailabilityStatus> = _userNameAvailability.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _isEmailValid = MutableStateFlow(false)
    val isEmailValid: StateFlow<Boolean> = _isEmailValid.asStateFlow()

    private val _isUserNameValid = MutableStateFlow(false)
    val isUserNameValid: StateFlow<Boolean> = _isUserNameValid.asStateFlow()

    private val _usernameState = MutableStateFlow(UserNameState.UNTOUCHED)
    val usernameState: StateFlow<UserNameState> = _usernameState.asStateFlow()

    private val _userNameErrorMessage = MutableStateFlow("")
    val userNameErrorMessage: StateFlow<String> = _userNameErrorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _showErrorAlert = MutableStateFlow(false)
    val showErrorAlert: StateFlow<Boolean> = _showErrorAlert.asStateFlow()

    private val _messageError = MutableStateFlow("")
    val messageError: StateFlow<String> = _messageError.asStateFlow()

    private val _isFormValid = MutableStateFlow(false)
    val isFormValidFlow: StateFlow<Boolean> = _isFormValid.asStateFlow()

    val isFormValid: Boolean
        get() = (_isUserNameValid.value && _userNameAvailability.value == UserNameAvailabilityStatus.AVAILABLE) ||
            _usernameState.value == UserNameState.UPDATED

    private fun refreshFormValidity() {
        _isFormValid.value = isFormValid
    }

    fun loadUserInfo() {
        val googleInfo = userRepository.getPublicInformation(AuthMethod.GOOGLE)
        val fallbackName = userRepository.getNames().takeIf { it.isNotBlank() && it != "Guest" }.orEmpty()
        val resolvedName = googleInfo?.username?.takeIf { it.isNotBlank() } ?: fallbackName

        if (resolvedName.isNotEmpty()) {
            _name.value = resolvedName
        }
        if (!googleInfo?.email.isNullOrBlank()) {
            _email.value = googleInfo?.email.orEmpty()
        }

        val formattedUsername = if (resolvedName.isNotEmpty()) {
            resolvedName
                .lowercase()
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .joinToString(".")
        } else {
            userRepository.getUsername().takeIf { it.isNotBlank() && it != "guest" }.orEmpty()
        }

        if (_userName.value.isEmpty() && formattedUsername.isNotEmpty()) {
            onUserNameChanged(formattedUsername)
        }
    }

    fun setName(name: String) {
        _name.value = name
    }

    fun setIsUserNameValid(valid: Boolean) {
        _isUserNameValid.value = valid
        refreshFormValidity()
    }

    fun setUserName(userName: String) {
        onUserNameChanged(userName)
    }

    fun onUserNameChanged(rawInput: String) {
        val filtered = rawInput.lowercase().filter { char ->
            char in 'a'..'z' || char in '0'..'9' || char == '.' || char == '_'
        }
        val previous = _userName.value
        _userName.value = filtered
        _isUserNameValid.value = userNameValidator.all { it.fn(filtered) }

        if (previous != filtered && _usernameState.value == UserNameState.UPDATED) {
            _usernameState.value = UserNameState.UNTOUCHED
        }
        refreshFormValidity()

        if (previous != filtered || _userNameAvailability.value == UserNameAvailabilityStatus.UNTOUCHED) {
            checkUserNameAvailability(filtered)
        }
    }

    fun checkUserNameAvailability(userName: String) {
        availabilityCheckJob?.cancel()

        val trimmed = userName.trim()
        if (trimmed.length < 3 || trimmed.length > 20) {
            _userNameAvailability.value = UserNameAvailabilityStatus.UNTOUCHED
            _userNameErrorMessage.value = ""
            refreshFormValidity()
            return
        }

        _userNameAvailability.value = UserNameAvailabilityStatus.LOADING
        _userNameErrorMessage.value = ""
        refreshFormValidity()

        availabilityCheckJob = viewModelScope.launch {
            delay(debounceMillis)
            userRepository.checkAvailability(trimmed) { result ->
                result.fold(
                    onSuccess = {
                        _userNameAvailability.value = UserNameAvailabilityStatus.AVAILABLE
                        _userNameErrorMessage.value = ""
                    },
                    onFailure = { error ->
                        _userNameAvailability.value = UserNameAvailabilityStatus.ERROR
                        _userNameErrorMessage.value = when (error) {
                            is UserError.Taken -> "User name is taken"
                            is UserError.InvalidUserName -> "User name is invalid"
                            else -> "Something went wrong, try again"
                        }
                    }
                )
                refreshFormValidity()
            }
        }
    }

    fun updateUsername(onSuccess: () -> Unit) {
        if (_usernameState.value == UserNameState.UPDATED) {
            onSuccess()
            return
        }

        val currentUserName = _userName.value.trim()
        _isLoading.value = true

        viewModelScope.launch {
            userRepository.setUsername(currentUserName)
            userRepository.change(currentUserName) { result ->
                _isLoading.value = false
                result.fold(
                    onSuccess = {
                        _usernameState.value = UserNameState.UPDATED
                        refreshFormValidity()
                        onSuccess()
                    },
                    onFailure = { error ->
                        _usernameState.value = UserNameState.UNTOUCHED
                        refreshFormValidity()
                        when (error) {
                            is UserError.ServerError -> show(error.message)
                            is UserError.UnknownError -> show(error.message)
                            else -> show("Something went wrong, please try again.")
                        }
                    }
                )
            }
        }
    }

    fun show(error: String) {
        _messageError.value = error
        _showErrorAlert.value = true
    }

    fun dismissError() {
        _showErrorAlert.value = false
    }
}
