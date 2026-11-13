package app.reportamelo.communityIssues.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.commons.LoginType
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.EmailOtpViewModel
import app.reportamelo.communityIssues.controllers.LoginViewModel
import app.reportamelo.communityIssues.services.OtpStatus
import app.reportamelo.communityIssues.ui.components.LinksView
import app.reportamelo.communityIssues.ui.screens.animations.borderBeam
import app.reportamelo.communityIssues.ui.theme.LoraFontFamily
import app.reportamelo.models.AuthPayload
import androidx.compose.ui.res.stringResource

@Composable
fun LoginView(
    modifier: Modifier = Modifier,
    onTokenReceived: (AuthPayload, LoginType) -> Unit = { _, _ -> },
    viewModel: LoginViewModel = LoginViewModel.shared,
    emailOtpViewModel: EmailOtpViewModel = EmailOtpViewModel.shared
) {
    val context = LocalContext.current
    val disableLoginButtons by viewModel.disableLoginButtons.collectAsState()
    val lastAuthMethod by viewModel.lastAuthMethod.collectAsState()
    val showErrorAlert by viewModel.showErrorAlert.collectAsState()
    val messageError by viewModel.messageError.collectAsState()
    val enableBorderBeam by viewModel.enableBorderBeam.collectAsState()
    val isSendingOtp by emailOtpViewModel.isSending.collectAsState()
    val otpStatus by emailOtpViewModel.status.collectAsState()
    var showEmailLogin by rememberSaveable { mutableStateOf(false) }
    val isEmailLoginBusy = isSendingOtp || otpStatus == OtpStatus.VERIFYING

    BackHandler(enabled = showEmailLogin && !isEmailLoginBusy) {
        showEmailLogin = false
    }

    if (showErrorAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(messageError) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            shape = RoundedCornerShape(52.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth()
                .borderBeam(
                    border = MaterialTheme.colorScheme.primary,
                    cornerRadius = 52.dp,
                    isEnabled = enableBorderBeam || isEmailLoginBusy
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 32.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedContent(targetState = showEmailLogin, label = "LoginCardContent") { isEmailLogin ->
                    if (isEmailLogin) {
                        EmailOtpView(
                            onBack = { showEmailLogin = false },
                            onVerified = { _, _ ->
                                // TODO: falta definicion de backend, intercambiar el OTP verificado por el token de sesion y llamar a onTokenReceived
                            },
                            viewModel = emailOtpViewModel
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Repórtamelo",
                                    fontFamily = LoraFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 24.sp,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.report_your_problems_and_improve_your_community),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.loginWithGoogle(context, onTokenReceived) },
                                    enabled = !disableLoginButtons,
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.google),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.sign_in_with_google))
                                }
                                if (lastAuthMethod == "google") {
                                    Text(stringResource(R.string.last_login), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                                }

                                OutlinedButton(
                                    onClick = { showEmailLogin = true },
                                    enabled = !disableLoginButtons,
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(stringResource(R.string.sign_in_with_email))
                                }

                                OutlinedButton(
                                    onClick = { viewModel.loginAsGuest(onTokenReceived) },
                                    enabled = !disableLoginButtons,
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(stringResource(R.string.sign_in_as_guest))
                                }
                            }
                        }
                    }
                }

                LinksView()
            }
        }
    }
}
