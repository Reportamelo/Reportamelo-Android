package app.reportamelo.communityIssues.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.EmailOtpViewModel
import app.reportamelo.communityIssues.services.OtpStatus
import app.reportamelo.communityIssues.ui.components.OtpInput
import app.reportamelo.communityIssues.ui.components.TextInput

/**
 * Email login flow shown inside the login card: first the email, then the OTP sent to it.
 */
@Composable
fun EmailOtpView(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onVerified: (email: String, otp: String) -> Unit = { _, _ -> },
    viewModel: EmailOtpViewModel = EmailOtpViewModel.shared
) {
    val showOtp by viewModel.showOtp.collectAsState()
    val status by viewModel.status.collectAsState()
    val email by viewModel.email.collectAsState()
    val otp by viewModel.otp.collectAsState()

    LaunchedEffect(status) {
        if (status == OtpStatus.SUCCESS) {
            onVerified(email, otp)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (showOtp) viewModel.changeEmail() else onBack() },
                enabled = status != OtpStatus.VERIFYING,
                // Aligns the arrow with the card content
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(if (showOtp) R.string.otp_enter_code_title else R.string.sign_in_with_email),
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                modifier = Modifier.offset(x = (-8).dp)
            )
        }

        AnimatedContent(targetState = showOtp, label = "EmailOtpStep") { isOtpStep ->
            if (isOtpStep) {
                OtpStep(viewModel)
            } else {
                EmailStep(viewModel)
            }
        }
    }
}

@Composable
private fun EmailStep(viewModel: EmailOtpViewModel) {
    val email by viewModel.email.collectAsState()
    val isEmailValid by viewModel.isEmailValid.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val triesLeft by viewModel.triesLeft.collectAsState()
    val message by viewModel.message.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.email_login_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        TextInput(
            value = email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.email),
            placeholder = "name@example.com",
            isError = email.isNotEmpty() && !isEmailValid && !isSending,
            errorMessage = stringResource(R.string.your_email_is_invalid),
            isDisabled = isSending,
            focusRequester = focusRequester,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { viewModel.sendOtp() })
        )

        message?.let { StatusMessage(text = it, isError = true) }

        Button(
            onClick = { viewModel.sendOtp() },
            enabled = isEmailValid && !isSending && triesLeft > 0,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(stringResource(R.string.otp_send_code))
        }
    }
}

@Composable
private fun OtpStep(viewModel: EmailOtpViewModel) {
    val email by viewModel.email.collectAsState()
    val otp by viewModel.otp.collectAsState()
    val status by viewModel.status.collectAsState()
    val triesLeft by viewModel.triesLeft.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val resendSecondsLeft by viewModel.resendSecondsLeft.collectAsState()
    val message by viewModel.message.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Stays enabled while verifying (the view model ignores the input) so the keyboard doesn't hide and the card doesn't jump
    val canType = status != OtpStatus.SUCCESS && triesLeft > 0

    LaunchedEffect(canType) {
        if (canType) focusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.otp_sent_to, email),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )

        OtpInput(
            value = otp,
            onValueChange = viewModel::onOtpChange,
            status = status,
            enabled = canType,
            focusRequester = focusRequester
        )

        when {
            status == OtpStatus.VERIFYING -> StatusMessage(stringResource(R.string.otp_verifying))
            status == OtpStatus.SUCCESS -> StatusMessage(stringResource(R.string.otp_verified))
            message != null -> StatusMessage(text = message.orEmpty(), isError = true)
        }

        if (status != OtpStatus.SUCCESS) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.otp_did_not_get_code),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                if (resendSecondsLeft > 0) {
                    Text(
                        text = stringResource(
                            R.string.otp_resend_in,
                            "%d:%02d".format(resendSecondsLeft / 60, resendSecondsLeft % 60)
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    TextButton(
                        onClick = { viewModel.resendOtp() },
                        enabled = !isSending && status != OtpStatus.VERIFYING && triesLeft > 0
                    ) {
                        Text(stringResource(R.string.otp_resend_code))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(text: String, isError: Boolean = false) {
    Text(
        text = text,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
