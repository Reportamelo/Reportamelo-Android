package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.AppPreferences
import app.reportamelo.communityIssues.services.EmailOtpService
import app.reportamelo.communityIssues.services.OtpResponse
import app.reportamelo.communityIssues.services.OtpResponseCode
import app.reportamelo.communityIssues.services.OtpResponseException
import app.reportamelo.communityIssues.services.OtpStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmailOtpViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeEmailOtpService(
        var sendCode: OtpResponseCode = OtpResponseCode.OTP_SENT,
        var sendError: Exception? = null,
        var verifyError: Exception? = null
    ) : EmailOtpService {
        val verifiedCodes = mutableListOf<String>()

        override suspend fun sendOtpToEmail(email: String): OtpResponse {
            sendError?.let { throw it }
            return OtpResponse(id = "1", message = "", code = sendCode)
        }

        override suspend fun verifyOtp(email: String, otp: String): OtpResponse {
            verifiedCodes += otp
            verifyError?.let { throw it }
            return OtpResponse(id = "1", message = "", code = OtpResponseCode.OTP_SENT)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AppPreferences.clearAll()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sanitizeOtp keeps letters and digits uppercased without the dash`() {
        assertEquals("AB1C2D", sanitizeOtp("ab1-c2d"))
        assertEquals("ABC123", sanitizeOtp(" abc 123 456"))
        assertEquals("", sanitizeOtp("-_ñ!"))
    }

    @Test
    fun `sendOtp is ignored while the email is invalid`() = runTest {
        val viewModel = EmailOtpViewModel(FakeEmailOtpService())
        viewModel.onEmailChange("not-an-email")
        viewModel.sendOtp()

        assertFalse(viewModel.isEmailValid.value)
        assertFalse(viewModel.showOtp.value)
    }

    @Test
    fun `sendOtp shows the otp step and starts the resend countdown`() = runTest {
        val viewModel = EmailOtpViewModel(FakeEmailOtpService())
        viewModel.onEmailChange("user@example.com")
        viewModel.sendOtp()

        assertTrue(viewModel.showOtp.value)
        assertEquals(OtpStatus.IDLE, viewModel.status.value)
        assertEquals(OTP_RESEND_SECONDS, viewModel.resendSecondsLeft.value)

        advanceTimeBy(OTP_RESEND_SECONDS * 1000L + 1)
        assertEquals(0, viewModel.resendSecondsLeft.value)
    }

    @Test
    fun `sendOtp with limit exceeded leaves no tries`() = runTest {
        val service = FakeEmailOtpService(sendError = OtpResponseException(OtpResponseCode.LIMIT_EXCEEDED))
        val viewModel = EmailOtpViewModel(service)
        viewModel.onEmailChange("user@example.com")
        viewModel.sendOtp()

        assertEquals(OtpStatus.ERROR, viewModel.status.value)
        assertEquals(0, viewModel.triesLeft.value)
        assertNotNull(viewModel.message.value)
    }

    @Test
    fun `typing the sixth character verifies the otp`() = runTest {
        val service = FakeEmailOtpService()
        val viewModel = EmailOtpViewModel(service)
        viewModel.onEmailChange("user@example.com")
        viewModel.sendOtp()

        viewModel.onOtpChange("ab1")
        assertTrue(service.verifiedCodes.isEmpty())

        viewModel.onOtpChange("ab1-c2d")
        assertEquals(listOf("AB1C2D"), service.verifiedCodes)
        assertEquals(OtpStatus.SUCCESS, viewModel.status.value)
    }

    @Test
    fun `invalid otp sets error, clears the code and consumes a try`() = runTest {
        val service = FakeEmailOtpService(verifyError = OtpResponseException(OtpResponseCode.INVALID_OTP))
        val viewModel = EmailOtpViewModel(service)
        viewModel.onEmailChange("user@example.com")
        viewModel.sendOtp()
        viewModel.onOtpChange("ABC123")

        assertEquals(OtpStatus.ERROR, viewModel.status.value)
        assertEquals("", viewModel.otp.value)
        assertEquals(OTP_MAX_TRIES - 1, viewModel.triesLeft.value)
        assertNotNull(viewModel.message.value)

        viewModel.onOtpChange("A")
        assertEquals(OtpStatus.IDLE, viewModel.status.value)

        repeat(OTP_MAX_TRIES - 1) { viewModel.onOtpChange("ABC12$it") }
        assertEquals(0, viewModel.triesLeft.value)

        viewModel.onOtpChange("ZZZ999")
        assertEquals(OTP_MAX_TRIES, service.verifiedCodes.size)
    }
}
