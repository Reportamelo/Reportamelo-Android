package app.reportamelo.communityIssues.controllers

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.helpers.localized
import app.reportamelo.communityIssues.services.EmailOtpService
import app.reportamelo.communityIssues.services.OtpResponseCode
import app.reportamelo.communityIssues.services.OtpResponseException
import app.reportamelo.communityIssues.services.OtpStatus
import app.reportamelo.communityIssues.services.PendingEmailOtpService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

const val OTP_LENGTH = 6
const val OTP_MAX_TRIES = 3
const val OTP_RESEND_SECONDS = 90

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$")

/** Keeps only letters and digits (the dash is just decoration), uppercased and capped to [OTP_LENGTH]. */
fun sanitizeOtp(input: String): String =
    input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(OTP_LENGTH)

class EmailOtpViewModel(
    private val service: EmailOtpService = PendingEmailOtpService()
) : ViewModel() {

    companion object {
        val shared = EmailOtpViewModel()
    }

    private val _showOtp = MutableStateFlow(false)
    val showOtp: StateFlow<Boolean> = _showOtp.asStateFlow()

    private val _status = MutableStateFlow(OtpStatus.IDLE)
    val status: StateFlow<OtpStatus> = _status.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _otp = MutableStateFlow("")
    val otp: StateFlow<String> = _otp.asStateFlow()

    private val _triesLeft = MutableStateFlow(OTP_MAX_TRIES)
    val triesLeft: StateFlow<Int> = _triesLeft.asStateFlow()

    private val _isEmailValid = MutableStateFlow(false)
    val isEmailValid: StateFlow<Boolean> = _isEmailValid.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _resendSecondsLeft = MutableStateFlow(0)
    val resendSecondsLeft: StateFlow<Int> = _resendSecondsLeft.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var resendJob: Job? = null

    fun onEmailChange(value: String) {
        _email.value = value.trim()
        _isEmailValid.value = EMAIL_REGEX.matches(_email.value)
        _message.value = null
    }

    fun onOtpChange(value: String) {
        if (_status.value == OtpStatus.VERIFYING || _status.value == OtpStatus.SUCCESS || _triesLeft.value == 0) return
        val sanitized = sanitizeOtp(value)
        if (sanitized == _otp.value) return

        _otp.value = sanitized
        _status.value = OtpStatus.IDLE
        _message.value = null

        if (sanitized.length == OTP_LENGTH) {
            verifyOtp(sanitized)
        }
    }

    fun sendOtp() {
        if (_isSending.value || !_isEmailValid.value || _triesLeft.value == 0) return

        viewModelScope.launch {
            _isSending.value = true
            _message.value = null
            try {
                val response = service.sendOtpToEmail(_email.value)

                if (response.code == OtpResponseCode.OTP_SENT) {
                    _showOtp.value = true
                    _status.value = OtpStatus.IDLE
                    _otp.value = ""
                    startResendCountdown()
                }

                if (response.code == OtpResponseCode.OTP_ALREADY_SENT) {
                    // The previous code is still valid, so the user can type it
                    _showOtp.value = true
                    _message.value = localized(R.string.otp_already_sent, "The code has already been sent. Try again in a minute.")
                }
            } catch (e: Exception) {
                _status.value = OtpStatus.ERROR
                Log.e("EmailOtpViewModel", "Error sending OTP", e)
                handle(e)
            } finally {
                _isSending.value = false
            }
        }
    }

    fun resendOtp() {
        if (_resendSecondsLeft.value > 0 || _status.value == OtpStatus.VERIFYING) return
        sendOtp()
    }

    fun verifyOtp(codeToVerify: String? = null) {
        if (_status.value == OtpStatus.VERIFYING) return
        val otpToVerify = codeToVerify?.takeIf { it.isNotEmpty() } ?: _otp.value
        if (otpToVerify.isEmpty()) return

        _otp.value = otpToVerify
        _status.value = OtpStatus.VERIFYING
        _message.value = null

        viewModelScope.launch {
            try {
                service.verifyOtp(_email.value, otpToVerify)
                _status.value = OtpStatus.SUCCESS
                resendJob?.cancel()
            } catch (e: Exception) {
                _status.value = OtpStatus.ERROR
                Log.e("EmailOtpViewModel", "Error verifying OTP", e)

                if (_triesLeft.value > 0) {
                    _triesLeft.value -= 1
                }
                handle(e)
                // Boxes stay in error state, but empty so the user can type the code again
                _otp.value = ""
            }
        }
    }

    /** Returns to the email step keeping the typed email. */
    fun changeEmail() {
        resendJob?.cancel()
        _resendSecondsLeft.value = 0
        _showOtp.value = false
        _status.value = OtpStatus.IDLE
        _otp.value = ""
        _message.value = null
    }

    fun reset() {
        changeEmail()
        _email.value = ""
        _isEmailValid.value = false
        _isSending.value = false
        _triesLeft.value = OTP_MAX_TRIES
    }

    private fun handle(error: Exception) {
        _message.value = when (error) {
            is OtpResponseException -> when (error.code) {
                OtpResponseCode.LIMIT_EXCEEDED -> {
                    _triesLeft.value = 0
                    localized(R.string.otp_limit_exceeded, "You have exceeded the limit of code requests.")
                }
                OtpResponseCode.OTP_EXPIRED -> localized(R.string.otp_expired, "The code you entered has expired.")
                OtpResponseCode.INVALID_OTP -> if (_triesLeft.value > 0) {
                    localized(R.string.otp_invalid, "Incorrect code. Attempts left: %1\$d", _triesLeft.value)
                } else {
                    localized(R.string.otp_no_tries_left, "Incorrect code. You have no attempts left.")
                }
                OtpResponseCode.EMAIL_NOT_FOUND -> localized(R.string.otp_email_not_found, "We couldn't find an account with that email.")
                else -> unknownError()
            }
            is IOException, is CommonIntercommunicationErrors.NetworkError ->
                localized(R.string.it_looks_like_that_your_network_is_experiencing_some_delays_please_try, "It looks like that your network is experiencing some delays, please try again.")
            else -> unknownError()
        }
    }

    private fun unknownError() =
        localized(R.string.an_unexpected_error_has_occurred_please_try_again_text, "An unexpected error has occurred, please try again.")

    private fun startResendCountdown() {
        resendJob?.cancel()
        _resendSecondsLeft.value = OTP_RESEND_SECONDS
        resendJob = viewModelScope.launch {
            while (_resendSecondsLeft.value > 0) {
                delay(1000)
                _resendSecondsLeft.value -= 1
            }
        }
    }
}
