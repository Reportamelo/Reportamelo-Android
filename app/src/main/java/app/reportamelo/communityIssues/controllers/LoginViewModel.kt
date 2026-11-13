package app.reportamelo.communityIssues.controllers

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.*
import app.reportamelo.commons.KeychainToken
import app.reportamelo.commons.LegacyKeychain
import app.reportamelo.commons.utils.*
import app.reportamelo.communityIssues.datamodel.*
import app.reportamelo.models.*
import app.reportamelo.network.*
import app.reportamelo.network.services.*
import app.reportamelo.repository.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.helpers.localized

typealias OnTokenReceived = (AuthPayload, LoginType) -> Unit

class LoginViewModel(
    private val userRepository: UserRepository = UserRepository.shared,
    private val googleCredentialFetcher: (suspend (Context) -> String)? = null
) : ViewModel() {

    companion object {
        val shared = LoginViewModel()
    }

    private val _enableBorderBeam = MutableStateFlow(false)
    val enableBorderBeam: StateFlow<Boolean> = _enableBorderBeam.asStateFlow()

    private val _disableLoginButtons = MutableStateFlow(false)
    val disableLoginButtons: StateFlow<Boolean> = _disableLoginButtons.asStateFlow()

    private val _userOAuthState = MutableStateFlow(UserOAuthResultState.UNOWNED)
    val userOAuthState: StateFlow<UserOAuthResultState> = _userOAuthState.asStateFlow()

    private val _showErrorAlert = MutableStateFlow(false)
    val showErrorAlert: StateFlow<Boolean> = _showErrorAlert.asStateFlow()

    private val _messageError = MutableStateFlow("")
    val messageError: StateFlow<String> = _messageError.asStateFlow()

    private val _lastAuthMethod = MutableStateFlow<String?>(LegacyKeychain.getToken(KeychainToken.AUTH_METHOD))
    val lastAuthMethod: StateFlow<String?> = _lastAuthMethod.asStateFlow()

    fun setLoading(isLoading: Boolean) {
        _disableLoginButtons.value = isLoading
        _enableBorderBeam.value = isLoading
    }

    fun performLoginActions() {
        val next = !_disableLoginButtons.value
        setLoading(next)
    }

    fun refreshLastAuthMethod() {
        _lastAuthMethod.value = LegacyKeychain.getToken(KeychainToken.AUTH_METHOD)
    }

    fun dismissError() {
        _showErrorAlert.value = false
    }

    fun loginAsGuest(onTokenReceived: OnTokenReceived) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val result = userRepository.loginAsGuest()
                _userOAuthState.value = result.first
                onTokenReceived(AuthPayload(token = result.second), LoginType.Guest)
            } catch (e: Exception) {
                when (e) {
                    is CommonIntercommunicationErrors.UnProcessable -> showError(localized(R.string.your_petition_cannot_be_processed_please_try_again_text, "Your petition cannot be processed, please try again."))
                    is CommonIntercommunicationErrors.NetworkError -> showError(localized(R.string.it_looks_like_that_your_network_is_experiencing_some_delays_please_try, "It looks like that your network is experiencing some delays, please try again."))
                    is CommonIntercommunicationErrors.ServerError -> showError(localized(R.string.server_error_please_try_again_text, "Server error, please try again."))
                    else -> showError(localized(R.string.an_unexpected_error_has_occurred_please_try_again_text, "An unexpected error has occurred, please try again."))
                }
            } finally {
                setLoading(false)
            }
        }
    }

    fun loginWithGoogle(context: Context, onTokenReceived: OnTokenReceived) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val idToken = googleCredentialFetcher?.invoke(context) ?: fetchGoogleIdToken(context)
                if (idToken.isNotBlank()) {
                    onTokenReceived(AuthPayload(token = idToken), LoginType.User(AuthMethod.GOOGLE))
                } else {
                    setLoading(false)
                    showError(localized(R.string.could_not_retrieve_google_id_token_please_try_again, "Could not retrieve Google ID token, please try again."))
                }
            } catch (e: GetCredentialCancellationException) {
                setLoading(false)
                Log.e("LoginViewModel", "Google Sign-In cancelled", e)
            } catch (e: GetCredentialException) {
                setLoading(false)
                showError(e.message ?: localized(R.string.google_sign_in_could_not_be_completed_please_try_again, "Google Sign-In could not be completed, please try again."))
            } catch (e: Exception) {
                setLoading(false)
                handleError(e)
                Log.e("LoginViewModel", "Error logging in with Google", e)
            }
        }
    }

    fun loginWithGoogle(idToken: String, onTokenReceived: OnTokenReceived) {
        viewModelScope.launch {
            if (idToken.isBlank()) {
                showError(localized(R.string.could_not_retrieve_google_id_token_please_try_again, "Could not retrieve Google ID token, please try again."))
                return@launch
            }
            setLoading(true)
            onTokenReceived(AuthPayload(token = idToken), LoginType.User(AuthMethod.GOOGLE))
        }
    }

    fun loginWithGoogle(onTokenReceived: OnTokenReceived) {
        val context = AppPreferences.appContext
        if (context != null) {
            loginWithGoogle(context, onTokenReceived)
        } else {
            showError("Google Sign-In requires an active Activity context.")
        }
    }

    private suspend fun fetchGoogleIdToken(context: Context): String {
        val credentialManager = CredentialManager.create(context)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(Apis.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val result = try {
            credentialManager.getCredential(context = context, request = request)
        } catch (e: GetCredentialCancellationException) {
            throw e
        } catch (_: GetCredentialException) {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(Apis.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            credentialManager.getCredential(context = context, request = fallbackRequest)
        }

        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            googleIdTokenCredential.profilePictureUri?.let {
                userRepository.setGoogleAvatar(it.toString())
            }
            googleIdTokenCredential.displayName?.takeIf { it.isNotBlank() }?.let {
                userRepository.setNames(it)
            }
            return googleIdTokenCredential.idToken
        }
        throw IllegalStateException("Unexpected credential type: ${credential.type}")
    }

    fun loginWithApple(onTokenReceived: OnTokenReceived) {
        viewModelScope.launch {
            setLoading(true)
            val identityToken = "mock_apple_token"
            val payload = AuthPayload(
                token = identityToken,
                name = "Apple User",
                email = "user@apple.com"
            )
            onTokenReceived(payload, LoginType.User(AuthMethod.APPLE))
        }
    }

    fun showError(error: String) {
        _messageError.value = error
        _showErrorAlert.value = true
    }

    fun handleError(error: Exception?) {
        error?.let { showError(it.message ?: localized(R.string.unknown_error, "Unknown error")) }
    }
}
