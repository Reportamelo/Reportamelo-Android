package app.reportamelo.communityIssues.services

import kotlinx.coroutines.delay

enum class OtpResponseCode {
    INVALID_OTP,
    REMOVE_SUCCESS,
    EMAIL_NOT_FOUND,
    INTERNAL_ERROR,
    EMAIL_ALREADY_VERIFIED,
    OTP_EXPIRED,
    OTP_ALREADY_SENT,
    LIMIT_EXCEEDED,
    OTP_SENT
}

enum class OtpStatus {
    IDLE,
    VERIFYING,
    ERROR,
    SUCCESS
}

data class OtpResponse(
    val id: String,
    val message: String,
    val code: OtpResponseCode
)

/** Thrown by [EmailOtpService] when the backend answers with a non successful [OtpResponseCode]. */
class OtpResponseException(val code: OtpResponseCode, message: String? = null) : Exception(message ?: code.name)

interface EmailOtpService {
    suspend fun sendOtpToEmail(email: String): OtpResponse
    suspend fun verifyOtp(email: String, otp: String): OtpResponse
}

/**
 * TODO: falta definicion de backend, replace with the real implementation once the endpoint is ready.
 * It only simulates the latency and always answers successfully, no session is created.
 */
class PendingEmailOtpService : EmailOtpService {
    override suspend fun sendOtpToEmail(email: String): OtpResponse {
        delay(1200)
        return OtpResponse(id = "", message = "", code = OtpResponseCode.OTP_SENT)
    }

    override suspend fun verifyOtp(email: String, otp: String): OtpResponse {
        delay(1200)
        return OtpResponse(id = "", message = "", code = OtpResponseCode.OTP_SENT)
    }
}
