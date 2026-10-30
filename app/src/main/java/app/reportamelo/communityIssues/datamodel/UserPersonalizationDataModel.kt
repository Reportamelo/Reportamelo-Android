package app.reportamelo.communityIssues.datamodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Stubs
enum class UserNameAvailabilityStatus {
    UNTOUCHED, AVAILABLE, UNAVAILABLE
}

enum class UserNameState {
    UNTOUCHED, UPDATED
}

class UserPersonalizationDataModel : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userNameAvailabilityStatus = MutableStateFlow(UserNameAvailabilityStatus.UNTOUCHED)
    val userNameAvailabilityStatus: StateFlow<UserNameAvailabilityStatus> = _userNameAvailabilityStatus.asStateFlow()

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

    val isFormValid: Boolean
        get() = (_isUserNameValid.value && _userNameAvailabilityStatus.value == UserNameAvailabilityStatus.AVAILABLE) ||
                _usernameState.value == UserNameState.UPDATED

    fun show(error: String) {
        _messageError.value = error
        _showErrorAlert.value = true
    }

    fun setUserName(userName: String) {
        _userName.value = userName
    }
}
